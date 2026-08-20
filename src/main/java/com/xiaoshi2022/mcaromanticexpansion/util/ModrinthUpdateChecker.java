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
//import net.minecraftforge.event.entity.player.PlayerEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//@Mod.EventBusSubscriber(modid = MCARomanticExpansion.MODID)
//public class ModrinthUpdateChecker {
//    private static final String MODRINTH_SLUG = "mca-romantic-expansion";
//    private static final String CURRENT_VERSION = ModInfo.getModVersion();
//    private static final String MOD_NAME = ModInfo.getModName();
//
//    private static String latestVersion = null;
//    private static String latestVersionUrl = null;
//    private static String changelog = null;
//    private static boolean hasChecked = false;
//    private static long lastCheckTime = 0;
//    private static final long CHECK_INTERVAL = 3600000;
//
//    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
//
//    // 【修复】1.20.1 Forge 中 PlayerLoggedInEvent 是有效的
//    @SubscribeEvent
//    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
//        if (event.getEntity() instanceof ServerPlayer player) {
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
//        executor.submit(() -> {
//            try {
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
//                    for (int i = 0; i < versions.size(); i++) {
//                        JsonObject versionObj = versions.get(i).getAsJsonObject();
//                        String versionNumber = versionObj.get("version_number").getAsString();
//                        String versionType = versionObj.get("version_type").getAsString();
//
//                        if (!"release".equals(versionType)) {
//                            continue;
//                        }
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
//                    if (latestVersion == null && versions.size() > 0) {
//                        JsonObject latest = versions.get(0).getAsJsonObject();
//                        latestVersion = latest.get("version_number").getAsString();
//                    }
//
//                    hasChecked = true;
//                    lastCheckTime = System.currentTimeMillis();
//                    MCARomanticExpansion.LOGGER.info("Modrinth latest version: {}, current: {}",
//                            latestVersion, CURRENT_VERSION);
//
//                    if (!latestVersion.equals(CURRENT_VERSION) && player != null) {
//                        notifyPlayer(player);
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
//    public static String getLatestVersion() { return latestVersion; }
//    public static boolean hasUpdate() { return latestVersion != null && !latestVersion.equals(CURRENT_VERSION); }
//}