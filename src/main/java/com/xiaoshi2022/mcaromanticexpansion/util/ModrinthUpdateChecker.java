package com.xiaoshi2022.mcaromanticexpansion.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@EventBusSubscriber(modid = MCARomanticExpansion.MODID)
public class ModrinthUpdateChecker {

    // ========== Modrinth 项目信息 ==========
    private static final String MODRINTH_SLUG = "mca-romantic-expansion";

    // ========== 从 ModInfo 读取版本信息 ==========
    private static final String CURRENT_VERSION = ModInfo.getModVersion();
    private static final String MOD_NAME = ModInfo.getModName();
    private static final String MINECRAFT_VERSION = ModInfo.getMinecraftVersion();

    private static String latestVersion = null;
    private static String latestVersionUrl = null;
    private static String changelog = null;
    private static boolean hasChecked = false;
    private static long lastCheckTime = 0;
    private static final long CHECK_INTERVAL = 3600000;

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            long currentTime = System.currentTimeMillis();

            if (!hasChecked || (currentTime - lastCheckTime) > CHECK_INTERVAL) {
                checkForUpdatesAsync(player);
            } else if (latestVersion != null && !latestVersion.equals(CURRENT_VERSION)) {
                notifyPlayer(player);
            }
        }
    }

    public static void checkForUpdatesAsync(ServerPlayer player) {
        executor.submit(() -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/" + MODRINTH_SLUG + "/version");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", MOD_NAME + "/" + CURRENT_VERSION);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    MCARomanticExpansion.LOGGER.warn("Modrinth API returned code: {}", responseCode);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonArray versions = JsonParser.parseString(response.toString()).getAsJsonArray();

                if (versions != null && versions.size() > 0) {
                    boolean foundCompatibleVersion = false;

                    for (int i = 0; i < versions.size(); i++) {
                        JsonObject versionObj = versions.get(i).getAsJsonObject();
                        String versionNumber = versionObj.get("version_number").getAsString();
                        String versionType = versionObj.get("version_type").getAsString();

                        // 只检查 release 版本
                        if (!"release".equals(versionType)) {
                            continue;
                        }

                        // 检查是否支持当前 Minecraft 版本
                        JsonArray gameVersions = versionObj.getAsJsonArray("game_versions");
                        boolean supportsCurrentMC = false;
                        if (gameVersions != null) {
                            for (int j = 0; j < gameVersions.size(); j++) {
                                String supportedVersion = gameVersions.get(j).getAsString();
                                if (MINECRAFT_VERSION.equals(supportedVersion)) {
                                    supportsCurrentMC = true;
                                    break;
                                }
                            }
                        }

                        if (!supportsCurrentMC) {
                            // 不支持当前 Minecraft 版本，跳过
                            MCARomanticExpansion.LOGGER.debug("Skipping version {} (not compatible with {})",
                                    versionNumber, MINECRAFT_VERSION);
                            continue;
                        }

                        // 找到了一个兼容的版本
                        foundCompatibleVersion = true;

                        // 检查版本号是否比当前版本新
                        if (isNewerVersion(versionNumber, CURRENT_VERSION)) {
                            latestVersion = versionNumber;

                            // 获取下载 URL
                            JsonArray files = versionObj.getAsJsonArray("files");
                            if (files != null && files.size() > 0) {
                                JsonObject file = files.get(0).getAsJsonObject();
                                if (file.has("url")) {
                                    latestVersionUrl = file.get("url").getAsString();
                                }
                            }

                            // 获取更新日志
                            if (versionObj.has("changelog")) {
                                changelog = versionObj.get("changelog").getAsString();
                                if (changelog.length() > 200) {
                                    changelog = changelog.substring(0, 200) + "...";
                                }
                            }
                            break;
                        }
                    }

                    // 只有在找到了兼容版本的情况下才更新状态
                    if (foundCompatibleVersion) {
                        hasChecked = true;
                        lastCheckTime = System.currentTimeMillis();

                        if (latestVersion != null) {
                            MCARomanticExpansion.LOGGER.info("Modrinth latest version for {}: {}, current: {}",
                                    MINECRAFT_VERSION, latestVersion, CURRENT_VERSION);
                        } else {
                            MCARomanticExpansion.LOGGER.info("Already on latest version for {}: {}",
                                    MINECRAFT_VERSION, CURRENT_VERSION);
                        }

                        if (latestVersion != null && !latestVersion.equals(CURRENT_VERSION) && player != null) {
                            notifyPlayer(player);
                        }
                    } else {
                        // 没有找到任何兼容的版本
                        MCARomanticExpansion.LOGGER.info("No compatible versions found for Minecraft {}", MINECRAFT_VERSION);
                        hasChecked = true;
                        lastCheckTime = System.currentTimeMillis();
                    }
                }
            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.warn("Failed to check Modrinth updates: {}", e.getMessage());
            }
        });
    }

    private static boolean isNewerVersion(String version1, String version2) {
        if (version1 == null || version2 == null) return false;

        String v1 = version1.replaceAll("^v", "").replaceAll("-.*$", "");
        String v2 = version2.replaceAll("^v", "").replaceAll("-.*$", "");

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (num1 != num2) {
                return num1 > num2;
            }
        }
        return false;
    }

    private static int parseVersionPart(String part) {
        try {
            String[] subParts = part.split("-");
            return Integer.parseInt(subParts[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void notifyPlayer(ServerPlayer player) {
        if (player == null) return;

        String divider = "═══════════════════════════════════════";

        player.sendSystemMessage(Component.literal("")
                .append(Component.literal(divider).withStyle(ChatFormatting.GOLD)));

        player.sendSystemMessage(Component.literal("")
                .append(Component.literal("§6[§a" + MOD_NAME + "§6]"))
                .append(Component.literal(" §e🎉 发现新版本更新！")));

        player.sendSystemMessage(Component.literal("")
                .append(Component.literal("§7当前版本: §c" + CURRENT_VERSION))
                .append(Component.literal("  §7→  "))
                .append(Component.literal("§a" + latestVersion)));

        player.sendSystemMessage(Component.literal("")
                .append(Component.literal("§7Minecraft: §f" + MINECRAFT_VERSION)));

        if (changelog != null && !changelog.isEmpty()) {
            player.sendSystemMessage(Component.literal("")
                    .append(Component.literal("§7更新内容: §f" + changelog)));
        }

        Component downloadLink = Component.literal("§b§n[📥 点击下载更新]")
                .withStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                latestVersionUrl != null ? latestVersionUrl :
                                        "https://modrinth.com/mod/" + MODRINTH_SLUG + "/versions"))
                        .withUnderlined(true));

        player.sendSystemMessage(Component.literal("")
                .append(downloadLink)
                .append(Component.literal("  "))
                .append(Component.literal("§7(Modrinth)")));

        Component projectLink = Component.literal("§b§n[📖 查看项目]")
                .withStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                "https://modrinth.com/mod/" + MODRINTH_SLUG))
                        .withUnderlined(true));

        player.sendSystemMessage(Component.literal("")
                .append(projectLink));

        player.sendSystemMessage(Component.literal("")
                .append(Component.literal("§a💡 请及时更新以获得最新功能和修复！"))
                .withStyle(ChatFormatting.GREEN));

        player.sendSystemMessage(Component.literal(divider).withStyle(ChatFormatting.GOLD));
    }

    public static void checkNow(ServerPlayer player) {
        if (player == null) return;

        hasChecked = false;
        latestVersion = null;
        latestVersionUrl = null;
        changelog = null;

        player.sendSystemMessage(Component.literal("§a🔍 正在检查 Modrinth 更新..."));
        checkForUpdatesAsync(player);
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static boolean hasUpdate() {
        return latestVersion != null && !latestVersion.equals(CURRENT_VERSION);
    }
}