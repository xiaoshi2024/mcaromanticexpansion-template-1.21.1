// 文件路径: src/main/java/com/xiaoshi2022/mcaromanticexpansion/util/MarriageConfig.java

package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ConcurrentHashMap;

public class MarriageConfig {
    // 全局配置：默认不允许同性结婚
    private static boolean allowSameGenderMarriage = false;

    // 玩家覆盖配置（用于管理员单独设置某个玩家）
    private static final ConcurrentHashMap<String, Boolean> playerOverrides = new ConcurrentHashMap<>();

    /**
     * 检查是否允许同性结婚
     * @param player 要检查的玩家（可选，用于玩家级覆盖）
     * @return 是否允许同性结婚
     */
    public static boolean isSameGenderMarriageAllowed(ServerPlayer player) {
        if (player != null) {
            String playerName = player.getName().getString();
            if (playerOverrides.containsKey(playerName)) {
                return playerOverrides.get(playerName);
            }
        }
        return allowSameGenderMarriage;
    }

    /**
     * 设置全局同性结婚权限
     */
    public static void setGlobalAllowSameGenderMarriage(boolean allow) {
        allowSameGenderMarriage = allow;
        MCARomanticExpansion.LOGGER.info("Global same-gender marriage setting changed to: {}", allow);
    }

    /**
     * 获取全局同性结婚权限
     */
    public static boolean isGlobalAllowSameGenderMarriage() {
        return allowSameGenderMarriage;
    }

    /**
     * 设置玩家的同性结婚权限（覆盖全局设置）
     */
    public static void setPlayerAllowSameGenderMarriage(String playerName, Boolean allow) {
        if (allow == null) {
            playerOverrides.remove(playerName);
        } else {
            playerOverrides.put(playerName, allow);
        }
        MCARomanticExpansion.LOGGER.info("Player {} same-gender marriage setting changed to: {}", playerName, allow);
    }

    /**
     * 获取玩家的同性结婚权限
     * @return null 表示使用全局设置，否则返回具体设置
     */
    public static Boolean getPlayerAllowSameGenderMarriage(String playerName) {
        return playerOverrides.get(playerName);
    }

    /**
     * 获取所有覆盖设置的玩家
     */
    public static ConcurrentHashMap<String, Boolean> getAllPlayerOverrides() {
        return new ConcurrentHashMap<>(playerOverrides);
    }
}