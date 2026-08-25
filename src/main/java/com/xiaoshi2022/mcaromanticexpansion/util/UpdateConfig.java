package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 更新通知的玩家偏好配置。
 * 每个玩家可以独立关闭登录时弹出的更新聊天消息，
 * 配置持久化到 config/mcaromanticexpansion-update.properties。
 */
public class UpdateConfig {

    // 默认开启通知
    private static final boolean DEFAULT_ENABLED = true;

    // 玩家名 -> 是否启用通知；未包含时使用 DEFAULT_ENABLED
    private static final ConcurrentHashMap<String, Boolean> playerNotifyPrefs = new ConcurrentHashMap<>();

    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("mcaromanticexpansion-update.properties");

    static {
        loadConfig();
    }

    private static void loadConfig() {
        Properties props = new Properties();

        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);

                playerNotifyPrefs.clear();
                String raw = props.getProperty("playerNotifyPrefs", "");
                if (!raw.isEmpty()) {
                    for (String entry : raw.split(",")) {
                        String[] parts = entry.split("=", 2);
                        if (parts.length == 2) {
                            playerNotifyPrefs.put(parts[0], Boolean.parseBoolean(parts[1]));
                        }
                    }
                }

                MCARomanticExpansion.LOGGER.debug("Loaded update config: {} player preferences",
                        playerNotifyPrefs.size());
            } catch (IOException e) {
                MCARomanticExpansion.LOGGER.warn("Failed to load update config: {}", e.getMessage());
            }
        } else {
            // 配置不存在则生成默认文件
            saveConfig();
        }
    }

    private static void saveConfig() {
        Properties props = new Properties();

        StringBuilder sb = new StringBuilder();
        for (var entry : playerNotifyPrefs.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        props.setProperty("playerNotifyPrefs", sb.toString());
        props.setProperty("version", "1.0");

        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            props.store(out, "MCARomanticExpansion Update Notify Config");
            MCARomanticExpansion.LOGGER.debug("Saved update config to: {}", CONFIG_PATH);
        } catch (IOException e) {
            MCARomanticExpansion.LOGGER.error("Failed to save update config: {}", e.getMessage());
        }
    }

    /**
     * 判断玩家是否启用了登录更新通知（默认启用）。
     */
    public static boolean isNotificationEnabled(String playerName) {
        if (playerName == null || playerName.isEmpty()) return DEFAULT_ENABLED;
        return playerNotifyPrefs.getOrDefault(playerName, DEFAULT_ENABLED);
    }

    /**
     * 设置某个玩家的更新通知开关，并立即持久化。
     */
    public static void setNotificationEnabled(String playerName, boolean enabled) {
        if (playerName == null || playerName.isEmpty()) return;
        playerNotifyPrefs.put(playerName, enabled);
        saveConfig();
        MCARomanticExpansion.LOGGER.debug("Player {} update notification set to {}", playerName, enabled);
    }

    /**
     * 热重载配置文件。
     */
    public static void reload() {
        loadConfig();
    }
}
