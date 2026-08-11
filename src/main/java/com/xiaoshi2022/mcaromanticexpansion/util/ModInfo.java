package com.xiaoshi2022.mcaromanticexpansion.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * 从生成的 version.properties 读取模组信息
 */
public class ModInfo {

    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    // 模组信息缓存
    private static String MOD_ID = "mcaromanticexpansion";
    private static String MOD_NAME = "MCA: Romantic Expansion";
    private static String MOD_VERSION = "1.1.0";
    private static String MOD_GROUP_ID = "com.xiaoshi2022.mcaromanticexpansion";
    private static String MOD_LICENSE = "GNU General Public License v3.0 (GPL-3.0)";
    private static String MINECRAFT_VERSION = "1.21.1";
    private static String NEO_VERSION = "21.1.228";
    private static String PARCHMENT_MINECRAFT_VERSION = "1.21.1";
    private static String PARCHMENT_MAPPINGS_VERSION = "2024.11.17";

    static {
        loadProperties();
    }

    /**
     * 加载 version.properties 文件
     */
    private static void loadProperties() {
        if (loaded) return;

        try {
            // 从类路径加载 version.properties
            InputStream input = ModInfo.class.getClassLoader()
                    .getResourceAsStream("version.properties");

            if (input != null) {
                properties.load(input);
                input.close();

                // 读取所有属性
                MOD_ID = properties.getProperty("mod_id", MOD_ID);
                MOD_NAME = properties.getProperty("mod_name", MOD_NAME);
                MOD_VERSION = properties.getProperty("mod_version", MOD_VERSION);
                MOD_GROUP_ID = properties.getProperty("mod_group_id", MOD_GROUP_ID);
                MOD_LICENSE = properties.getProperty("mod_license", MOD_LICENSE);
                MINECRAFT_VERSION = properties.getProperty("minecraft_version", MINECRAFT_VERSION);
                NEO_VERSION = properties.getProperty("neo_version", NEO_VERSION);
                PARCHMENT_MINECRAFT_VERSION = properties.getProperty("parchment_minecraft_version", PARCHMENT_MINECRAFT_VERSION);
                PARCHMENT_MAPPINGS_VERSION = properties.getProperty("parchment_mappings_version", PARCHMENT_MAPPINGS_VERSION);

                loaded = true;
                System.out.println("[ModInfo] Loaded mod info: version=" + MOD_VERSION);
            } else {
                // version.properties 不存在，使用默认值
                loaded = true;
                System.out.println("[ModInfo] version.properties not found, using default values");
            }
        } catch (Exception e) {
            loaded = true;
            System.err.println("[ModInfo] Failed to load version.properties: " + e.getMessage());
        }
    }

    // ========== Getter 方法 ==========

    public static String getModId() {
        return MOD_ID;
    }

    public static String getModName() {
        return MOD_NAME;
    }

    public static String getModVersion() {
        return MOD_VERSION;
    }

    public static String getModGroupId() {
        return MOD_GROUP_ID;
    }

    public static String getModLicense() {
        return MOD_LICENSE;
    }

    public static String getMinecraftVersion() {
        return MINECRAFT_VERSION;
    }

    public static String getNeoVersion() {
        return NEO_VERSION;
    }

    public static String getParchmentMinecraftVersion() {
        return PARCHMENT_MINECRAFT_VERSION;
    }

    public static String getParchmentMappingsVersion() {
        return PARCHMENT_MAPPINGS_VERSION;
    }

    public static String getFullVersionInfo() {
        return MOD_NAME + " v" + MOD_VERSION + " for Minecraft " + MINECRAFT_VERSION;
    }

    public static Properties getProperties() {
        return new Properties(properties);
    }
}