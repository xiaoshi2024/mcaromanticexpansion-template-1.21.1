//package com.xiaoshi2022.mcaromanticexpansion.util;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
//import net.minecraft.ChatFormatting;
//import net.minecraft.network.chat.ClickEvent;
//import net.minecraft.network.chat.Component;
//import net.minecraft.network.chat.Style;
//import net.minecraft.server.level.ServerPlayer;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.event.entity.player.PlayerEvent;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//
//@EventBusSubscriber(modid = MCARomanticExpansion.MODID)
//public class ModrinthUpdateChecker {
//
//    // ========== Modrinth 项目信息 ==========
//    private static final String MODRINTH_SLUG = "mca-romantic-expansion";
//
//    // ========== 从 ModInfo 读取版本信息 ==========
//    private static final String CURRENT_VERSION = ModInfo.getModVersion();
//    private static final String MOD_NAME = ModInfo.getModName();
//    private static final String MINECRAFT_VERSION = ModInfo.getMinecraftVersion();
//
//    private static String latestVersion = null;
//    private static String latestVersionUrl = null;
//    private static String changelog = null;
//    private static boolean hasChecked = false;
//    private static long lastCheckTime = 0;
//    private static final long CHECK_INTERVAL = 3600000; // 1小时
//
//    // ★★★ 修复1: 使用 volatile 确保线程可见性，并延迟初始化 ★★★
//    private static volatile ScheduledExecutorService executor = null;
//
//    // ★★★ 修复2: 线程安全的懒加载初始化 ★★★
//    private static ScheduledExecutorService getExecutor() {
//        if (executor == null) {
//            synchronized (ModrinthUpdateChecker.class) {
//                if (executor == null) {
//                    // 使用守护线程，避免阻止JVM关闭
//                    executor = Executors.newSingleThreadScheduledExecutor(r -> {
//                        Thread t = new Thread(r, "MCA-UpdateChecker");
//                        t.setDaemon(true);
//                        return t;
//                    });
//                }
//            }
//        }
//        return executor;
//    }
//
//    // ★★★ 修复3: 主动关闭线程池的方法（可在服务器停止时调用）★★★
//    public static void shutdownExecutor() {
//        if (executor != null) {
//            executor.shutdownNow();
//            executor = null;
//        }
//    }
//
//    @SubscribeEvent
//    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
//        // ★★★ 核心修复: 专用服务器直接跳过更新检查 ★★★
//        if (event.getEntity() instanceof ServerPlayer player) {
//            // 检查是否为专用服务器（Dedicated Server）
//            boolean isDedicatedServer = player.server != null && player.server.isDedicatedServer();
//
//            if (isDedicatedServer) {
//                // 专用服务器：完全跳过更新检查，不创建任何线程
//                MCARomanticExpansion.LOGGER.debug("Update checker disabled on dedicated server");
//                return;
//            }
//
//            // 玩家通过 /mcaupdate notify false 关闭了登录更新提醒，则跳过自动检查和提醒
//            if (!UpdateConfig.isNotificationEnabled(player.getName().getString())) {
//                MCARomanticExpansion.LOGGER.debug("Update notification disabled by player {}", player.getName().getString());
//                return;
//            }
//
//            // 客户端或内置服务端：正常执行更新检查
//            long currentTime = System.currentTimeMillis();
//
//            if (!hasChecked || (currentTime - lastCheckTime) > CHECK_INTERVAL) {
//                checkForUpdatesAsync(player);
//            } else if (latestVersion != null && !latestVersion.equals(CURRENT_VERSION)) {
//                notifyPlayer(player);
//            }
//        }
//    }
//
//    public static void checkForUpdatesAsync(ServerPlayer player) {
//        // ★★★ 修复4: 获取 executor 时使用懒加载 ★★★
//        ScheduledExecutorService exec = getExecutor();
//
//        // 检查线程池是否已关闭
//        if (exec.isShutdown() || exec.isTerminated()) {
//            MCARomanticExpansion.LOGGER.warn("Executor is shut down, cannot check for updates");
//            return;
//        }
//
//        exec.submit(() -> {
//            try {
//                // ★★★ 修复5: 增加超时和重试机制 ★★★
//                URL url = new URL("https://api.modrinth.com/v2/project/" + MODRINTH_SLUG + "/version");
//                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                conn.setRequestMethod("GET");
//                conn.setRequestProperty("User-Agent", MOD_NAME + "/" + CURRENT_VERSION);
//                conn.setConnectTimeout(5000);
//                conn.setReadTimeout(5000);
//
//                int responseCode = conn.getResponseCode();
//                if (responseCode != 200) {
//                    MCARomanticExpansion.LOGGER.warn("Modrinth API returned code: {}", responseCode);
//                    return;
//                }
//
//                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                StringBuilder response = new StringBuilder();
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    response.append(line);
//                }
//                reader.close();
//
//                JsonArray versions = JsonParser.parseString(response.toString()).getAsJsonArray();
//
//                if (versions != null && versions.size() > 0) {
//                    boolean foundCompatibleVersion = false;
//
//                    for (int i = 0; i < versions.size(); i++) {
//                        JsonObject versionObj = versions.get(i).getAsJsonObject();
//                        String versionNumber = versionObj.get("version_number").getAsString();
//                        String versionType = versionObj.get("version_type").getAsString();
//
//                        // 只检查 release 版本
//                        if (!"release".equals(versionType)) {
//                            continue;
//                        }
//
//                        // 检查是否支持当前 Minecraft 版本
//                        JsonArray gameVersions = versionObj.getAsJsonArray("game_versions");
//                        boolean supportsCurrentMC = false;
//                        if (gameVersions != null) {
//                            for (int j = 0; j < gameVersions.size(); j++) {
//                                String supportedVersion = gameVersions.get(j).getAsString();
//                                if (MINECRAFT_VERSION.equals(supportedVersion)) {
//                                    supportsCurrentMC = true;
//                                    break;
//                                }
//                            }
//                        }
//
//                        if (!supportsCurrentMC) {
//                            MCARomanticExpansion.LOGGER.debug("Skipping version {} (not compatible with {})",
//                                    versionNumber, MINECRAFT_VERSION);
//                            continue;
//                        }
//
//                        foundCompatibleVersion = true;
//
//                        if (isNewerVersion(versionNumber, CURRENT_VERSION)) {
//                            latestVersion = versionNumber;
//
//                            JsonArray files = versionObj.getAsJsonArray("files");
//                            if (files != null && files.size() > 0) {
//                                JsonObject file = files.get(0).getAsJsonObject();
//                                if (file.has("url")) {
//                                    latestVersionUrl = file.get("url").getAsString();
//                                }
//                            }
//
//                            if (versionObj.has("changelog")) {
//                                changelog = versionObj.get("changelog").getAsString();
//                                if (changelog.length() > 200) {
//                                    changelog = changelog.substring(0, 200) + "...";
//                                }
//                            }
//                            break;
//                        }
//                    }
//
//                    if (foundCompatibleVersion) {
//                        hasChecked = true;
//                        lastCheckTime = System.currentTimeMillis();
//
//                        if (latestVersion != null) {
//                            MCARomanticExpansion.LOGGER.info("Modrinth latest version for {}: {}, current: {}",
//                                    MINECRAFT_VERSION, latestVersion, CURRENT_VERSION);
//                        } else {
//                            MCARomanticExpansion.LOGGER.info("Already on latest version for {}: {}",
//                                    MINECRAFT_VERSION, CURRENT_VERSION);
//                        }
//
//                        if (latestVersion != null && !latestVersion.equals(CURRENT_VERSION) && player != null) {
//                            notifyPlayer(player);
//                        }
//                    } else {
//                        MCARomanticExpansion.LOGGER.info("No compatible versions found for Minecraft {}", MINECRAFT_VERSION);
//                        hasChecked = true;
//                        lastCheckTime = System.currentTimeMillis();
//                    }
//                }
//            } catch (Exception e) {
//                MCARomanticExpansion.LOGGER.warn("Failed to check Modrinth updates: {}", e.getMessage());
//            }
//        });
//    }
//
//    private static boolean isNewerVersion(String version1, String version2) {
//        if (version1 == null || version2 == null) return false;
//
//        String v1 = version1.replaceAll("^v", "").replaceAll("-.*$", "");
//        String v2 = version2.replaceAll("^v", "").replaceAll("-.*$", "");
//
//        String[] parts1 = v1.split("\\.");
//        String[] parts2 = v2.split("\\.");
//
//        int maxLength = Math.max(parts1.length, parts2.length);
//        for (int i = 0; i < maxLength; i++) {
//            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
//            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
//            if (num1 != num2) {
//                return num1 > num2;
//            }
//        }
//        return false;
//    }
//
//    private static int parseVersionPart(String part) {
//        try {
//            String[] subParts = part.split("-");
//            return Integer.parseInt(subParts[0]);
//        } catch (NumberFormatException e) {
//            return 0;
//        }
//    }
//
//    private static void notifyPlayer(ServerPlayer player) {
//        if (player == null) return;
//
//        String divider = "═══════════════════════════════════════";
//
//        player.sendSystemMessage(Component.literal("")
//                .append(Component.literal(divider).withStyle(ChatFormatting.GOLD)));
//
//        player.sendSystemMessage(Component.literal("")
//                .append(Component.literal("§6[§a" + MOD_NAME + "§6]"))
//                .append(Component.translatable("message.mcaromanticexpansion.update.found")));
//
//        player.sendSystemMessage(Component.literal("")
//                .append(Component.translatable("message.mcaromanticexpansion.update.current_version", CURRENT_VERSION))
//                .append(Component.literal("  §7→  "))
//                .append(Component.literal("§a" + latestVersion)));
//
//        player.sendSystemMessage(Component.literal("")
//                .append(Component.literal("§7Minecraft: §f" + MINECRAFT_VERSION)));
//
//        if (changelog != null && !changelog.isEmpty()) {
//            player.sendSystemMessage(Component.literal("")
//                    .append(Component.translatable("message.mcaromanticexpansion.update.changelog", changelog)));
//        }
//
//        Component downloadLink = Component.translatable("message.mcaromanticexpansion.update.download")
//                .withStyle(Style.EMPTY
//                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
//                                latestVersionUrl != null ? latestVersionUrl :
//                                        "https://modrinth.com/mod/" + MODRINTH_SLUG + "/versions"))
//                        .withUnderlined(true));
//
//        player.sendSystemMessage(Component.literal("")
//                .append(downloadLink)
//                .append(Component.literal("  "))
//                .append(Component.literal("§7(Modrinth)")));
//
//        Component projectLink = Component.translatable("message.mcaromanticexpansion.update.view_project")
//                .withStyle(Style.EMPTY
//                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
//                                "https://modrinth.com/mod/" + MODRINTH_SLUG))
//                        .withUnderlined(true));
//
//        player.sendSystemMessage(Component.literal("")
//                .append(projectLink));
//
//        player.sendSystemMessage(Component.literal("")
//                .append(Component.translatable("message.mcaromanticexpansion.update.reminder"))
//                .withStyle(ChatFormatting.GREEN));
//
//        player.sendSystemMessage(Component.literal(divider).withStyle(ChatFormatting.GOLD));
//    }
//
//    public static void checkNow(ServerPlayer player) {
//        if (player == null) return;
//
//        hasChecked = false;
//        latestVersion = null;
//        latestVersionUrl = null;
//        changelog = null;
//
//        player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.update.checking"));
//        checkForUpdatesAsync(player);
//    }
//
//    public static String getLatestVersion() {
//        return latestVersion;
//    }
//
//    public static boolean hasUpdate() {
//        return latestVersion != null && !latestVersion.equals(CURRENT_VERSION);
//    }
//}