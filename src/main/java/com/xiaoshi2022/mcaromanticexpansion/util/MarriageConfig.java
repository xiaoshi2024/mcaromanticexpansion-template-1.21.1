package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class MarriageConfig {
    // 全局配置：默认不允许同性结婚
    private static boolean allowSameGenderMarriage = false;

    // 备孕是否必须先结婚。默认 false = 任何人都能备孕（保持原行为）。
    private static boolean pregnancyRequiresMarriage = false;

    // 玩家覆盖配置
    private static final ConcurrentHashMap<String, Boolean> playerOverrides = new ConcurrentHashMap<>();

    // 备孕需结婚的 per-player 覆盖：null = 跟随全局，true/false = 强制要求/豁免
    private static final ConcurrentHashMap<String, Boolean> pregnancyPlayerOverrides = new ConcurrentHashMap<>();

    // 配置文件路径
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("mcaromanticexpansion-marriage.properties");

    // 静态初始化块：加载配置文件
    static {
        loadConfig();
    }

    /**
     * 从配置文件加载设置
     */
    public static void loadConfig() {
        Properties props = new Properties();

        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);

                // 加载全局设置
                allowSameGenderMarriage = Boolean.parseBoolean(props.getProperty("allowSameGender", "false"));
                pregnancyRequiresMarriage = Boolean.parseBoolean(props.getProperty("pregnancyRequiresMarriage", "false"));

                // 加载玩家覆盖设置
                playerOverrides.clear();
                String playerOverridesStr = props.getProperty("playerOverrides", "");
                if (!playerOverridesStr.isEmpty()) {
                    for (String entry : playerOverridesStr.split(",")) {
                        String[] parts = entry.split("=");
                        if (parts.length == 2) {
                            playerOverrides.put(parts[0], Boolean.parseBoolean(parts[1]));
                        }
                    }
                }

                // 加载备孕需结婚的玩家覆盖
                pregnancyPlayerOverrides.clear();
                String pregOverridesStr = props.getProperty("pregnancyPlayerOverrides", "");
                if (!pregOverridesStr.isEmpty()) {
                    for (String entry : pregOverridesStr.split(",")) {
                        String[] parts = entry.split("=");
                        if (parts.length == 2) {
                            pregnancyPlayerOverrides.put(parts[0], Boolean.parseBoolean(parts[1]));
                        }
                    }
                }

                MCARomanticExpansion.LOGGER.debug("Loaded marriage config: allowSameGender={}, pregnancyRequiresMarriage={}, playerOverrides={}, pregnancyPlayerOverrides={}",
                        allowSameGenderMarriage, pregnancyRequiresMarriage, playerOverrides.size(), pregnancyPlayerOverrides.size());
            } catch (IOException e) {
                MCARomanticExpansion.LOGGER.warn("Failed to load marriage config: {}", e.getMessage());
            }
        } else {
            // 配置文件不存在，创建默认配置
            saveConfig();
        }
    }

    /**
     * 保存配置到文件
     */
    public static void saveConfig() {
        Properties props = new Properties();

        props.setProperty("allowSameGender", String.valueOf(allowSameGenderMarriage));
        props.setProperty("pregnancyRequiresMarriage", String.valueOf(pregnancyRequiresMarriage));

        // 保存玩家覆盖设置
        StringBuilder sb = new StringBuilder();
        for (var entry : playerOverrides.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        props.setProperty("playerOverrides", sb.toString());

        // 保存备孕需结婚的玩家覆盖
        StringBuilder pregSb = new StringBuilder();
        for (var entry : pregnancyPlayerOverrides.entrySet()) {
            if (pregSb.length() > 0) pregSb.append(",");
            pregSb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        props.setProperty("pregnancyPlayerOverrides", pregSb.toString());
        props.setProperty("version", "1.0");

        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            props.store(out, "MCARomanticExpansion Marriage Config");
            MCARomanticExpansion.LOGGER.debug("Saved marriage config to: {}", CONFIG_PATH);
        } catch (IOException e) {
            MCARomanticExpansion.LOGGER.error("Failed to save marriage config: {}", e.getMessage());
        }
    }

    // ========== 原有方法（修改后自动保存） ==========

    public static boolean isSameGenderMarriageAllowed(ServerPlayer player) {
        if (player != null) {
            String playerName = player.getName().getString();
            if (playerOverrides.containsKey(playerName)) {
                return playerOverrides.get(playerName);
            }
        }
        return allowSameGenderMarriage;
    }

    public static void setGlobalAllowSameGenderMarriage(boolean allow) {
        allowSameGenderMarriage = allow;
        saveConfig();  // 自动保存
        MCARomanticExpansion.LOGGER.debug("Global same-gender marriage setting changed to: {}", allow);
    }

    public static boolean isGlobalAllowSameGenderMarriage() {
        return allowSameGenderMarriage;
    }

    // 备孕是否必须先结婚
    public static void setPregnancyRequiresMarriage(boolean require) {
        pregnancyRequiresMarriage = require;
        saveConfig();
        MCARomanticExpansion.LOGGER.debug("Pregnancy requires marriage setting changed to: {}", require);
    }

    public static boolean isPregnancyRequiresMarriage() {
        return pregnancyRequiresMarriage;
    }

    // per-player 覆盖：null = 重置回跟随全局
    public static void setPlayerPregnancyRequiresMarriage(String playerName, Boolean require) {
        if (require == null) {
            pregnancyPlayerOverrides.remove(playerName);
        } else {
            pregnancyPlayerOverrides.put(playerName, require);
        }
        saveConfig();
        MCARomanticExpansion.LOGGER.debug("Player {} pregnancy marriage requirement changed to: {}", playerName, require);
    }

    public static Boolean getPlayerPregnancyRequiresMarriage(String playerName) {
        return pregnancyPlayerOverrides.get(playerName);
    }

    public static ConcurrentHashMap<String, Boolean> getAllPlayerPregnancyOverrides() {
        return new ConcurrentHashMap<>(pregnancyPlayerOverrides);
    }

    // 玩家自己的有效规则：per-player 覆盖优先，否则回退到全局
    private static boolean getPlayerEffectiveRequirement(ServerPlayer player) {
        if (player != null) {
            String name = player.getName().getString();
            if (pregnancyPlayerOverrides.containsKey(name)) {
                return pregnancyPlayerOverrides.get(name);
            }
        }
        return pregnancyRequiresMarriage;
    }

    /**
     * 一对玩家是否能进入备孕期前必须先结婚。
     * 任一方要求结婚即视为必须结婚（"或" 语义）。
     */
    public static boolean shouldRequireMarriageForPregnancy(ServerPlayer player1, ServerPlayer player2) {
        return getPlayerEffectiveRequirement(player1) || getPlayerEffectiveRequirement(player2);
    }

    public static void setPlayerAllowSameGenderMarriage(String playerName, Boolean allow) {
        if (allow == null) {
            playerOverrides.remove(playerName);
        } else {
            playerOverrides.put(playerName, allow);
        }
        saveConfig();  // 自动保存
        MCARomanticExpansion.LOGGER.debug("Player {} same-gender marriage setting changed to: {}", playerName, allow);
    }

    public static Boolean getPlayerAllowSameGenderMarriage(String playerName) {
        return playerOverrides.get(playerName);
    }

    public static ConcurrentHashMap<String, Boolean> getAllPlayerOverrides() {
        return new ConcurrentHashMap<>(playerOverrides);
    }

    /**
     * 重新加载配置（用于热重载）
     */
    public static void reload() {
        loadConfig();
    }
}