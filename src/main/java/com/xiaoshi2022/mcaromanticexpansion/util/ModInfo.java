package com.xiaoshi2022.mcaromanticexpansion.util;

import java.io.InputStream;
import java.util.Properties;

public class ModInfo {
    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    private static String MOD_ID = "mcaromanticexpansion";
    private static String MOD_NAME = "MCA: Romantic Expansion";
    private static String MOD_VERSION = "1.0.0";
    private static String MOD_GROUP_ID = "com.xiaoshi2022.mcaromanticexpansion";
    private static String MOD_LICENSE = "GNU General Public License v3.0 (GPL-3.0)";
    private static String MINECRAFT_VERSION = "1.20.1";
    private static String FORGE_VERSION = "47.3.0";

    static {
        loadProperties();
    }

    private static void loadProperties() {
        if (loaded) return;

        try {
            InputStream input = ModInfo.class.getClassLoader()
                    .getResourceAsStream("version.properties");

            if (input != null) {
                properties.load(input);
                input.close();

                MOD_ID = properties.getProperty("mod_id", MOD_ID);
                MOD_NAME = properties.getProperty("mod_name", MOD_NAME);
                MOD_VERSION = properties.getProperty("mod_version", MOD_VERSION);
                MOD_GROUP_ID = properties.getProperty("mod_group_id", MOD_GROUP_ID);
                MOD_LICENSE = properties.getProperty("mod_license", MOD_LICENSE);
                MINECRAFT_VERSION = properties.getProperty("minecraft_version", MINECRAFT_VERSION);
                FORGE_VERSION = properties.getProperty("forge_version", FORGE_VERSION);

                loaded = true;
                System.out.println("[ModInfo] Loaded mod info: version=" + MOD_VERSION);
            } else {
                loaded = true;
                System.out.println("[ModInfo] version.properties not found, using default values");
            }
        } catch (Exception e) {
            loaded = true;
            System.err.println("[ModInfo] Failed to load version.properties: " + e.getMessage());
        }
    }

    public static String getModId() { return MOD_ID; }
    public static String getModName() { return MOD_NAME; }
    public static String getModVersion() { return MOD_VERSION; }
    public static String getModGroupId() { return MOD_GROUP_ID; }
    public static String getModLicense() { return MOD_LICENSE; }
    public static String getMinecraftVersion() { return MINECRAFT_VERSION; }
    public static String getForgeVersion() { return FORGE_VERSION; }
    public static String getFullVersionInfo() { return MOD_NAME + " v" + MOD_VERSION + " for Minecraft " + MINECRAFT_VERSION; }
    public static Properties getProperties() { return new Properties(properties); }
}