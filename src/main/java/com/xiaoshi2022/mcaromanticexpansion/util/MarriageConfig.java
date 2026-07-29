package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class MarriageConfig {
    private static boolean allowSameGenderMarriage = false;
    private static final ConcurrentHashMap<String, Boolean> playerOverrides = new ConcurrentHashMap<>();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("mcaromanticexpansion-marriage.properties");

    static {
        loadConfig();
    }

    public static void loadConfig() {
        Properties props = new Properties();

        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
                allowSameGenderMarriage = Boolean.parseBoolean(props.getProperty("allowSameGender", "false"));
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
                MCARomanticExpansion.LOGGER.debug("Loaded marriage config: allowSameGender={}, playerOverrides={}",
                        allowSameGenderMarriage, playerOverrides.size());
            } catch (IOException e) {
                MCARomanticExpansion.LOGGER.warn("Failed to load marriage config: {}", e.getMessage());
            }
        } else {
            saveConfig();
        }
    }

    public static void saveConfig() {
        Properties props = new Properties();
        props.setProperty("allowSameGender", String.valueOf(allowSameGenderMarriage));
        StringBuilder sb = new StringBuilder();
        for (var entry : playerOverrides.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        props.setProperty("playerOverrides", sb.toString());
        props.setProperty("version", "1.0");

        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            props.store(out, "MCARomanticExpansion Marriage Config");
            MCARomanticExpansion.LOGGER.debug("Saved marriage config to: {}", CONFIG_PATH);
        } catch (IOException e) {
            MCARomanticExpansion.LOGGER.error("Failed to save marriage config: {}", e.getMessage());
        }
    }

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
        saveConfig();
    }

    public static boolean isGlobalAllowSameGenderMarriage() {
        return allowSameGenderMarriage;
    }

    public static void setPlayerAllowSameGenderMarriage(String playerName, Boolean allow) {
        if (allow == null) {
            playerOverrides.remove(playerName);
        } else {
            playerOverrides.put(playerName, allow);
        }
        saveConfig();
    }

    public static Boolean getPlayerAllowSameGenderMarriage(String playerName) {
        return playerOverrides.get(playerName);
    }

    public static ConcurrentHashMap<String, Boolean> getAllPlayerOverrides() {
        return new ConcurrentHashMap<>(playerOverrides);
    }

    public static void reload() {
        loadConfig();
    }
}