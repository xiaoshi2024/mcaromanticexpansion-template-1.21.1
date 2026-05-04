package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    private static final Map<UUID, Long> proposalCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> bouquetCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> marriageCooldown = new ConcurrentHashMap<>();

    private static final long COOLDOWN_MS = 30000; // 30秒冷却

    public static boolean isOnCooldown(UUID playerId, String type) {
        long currentTime = System.currentTimeMillis();
        Long lastTime;

        switch (type) {
            case "proposal":
                lastTime = proposalCooldown.get(playerId);
                break;
            case "bouquet":
                lastTime = bouquetCooldown.get(playerId);
                break;
            case "marriage":
                lastTime = marriageCooldown.get(playerId);
                break;
            default:
                return false;
        }

        if (lastTime == null) return false;
        return (currentTime - lastTime) < COOLDOWN_MS;
    }

    public static void setCooldown(UUID playerId, String type) {
        long currentTime = System.currentTimeMillis();
        switch (type) {
            case "proposal":
                proposalCooldown.put(playerId, currentTime);
                break;
            case "bouquet":
                bouquetCooldown.put(playerId, currentTime);
                break;
            case "marriage":
                marriageCooldown.put(playerId, currentTime);
                break;
        }
        MCARomanticExpansion.LOGGER.info("Set {} cooldown for player {}", type, playerId);
    }

    public static long getRemainingCooldown(UUID playerId, String type) {
        long currentTime = System.currentTimeMillis();
        Long lastTime;

        switch (type) {
            case "proposal":
                lastTime = proposalCooldown.get(playerId);
                break;
            case "bouquet":
                lastTime = bouquetCooldown.get(playerId);
                break;
            case "marriage":
                lastTime = marriageCooldown.get(playerId);
                break;
            default:
                return 0;
        }

        if (lastTime == null) return 0;
        long remaining = COOLDOWN_MS - (currentTime - lastTime);
        return Math.max(0, remaining);
    }

    public static void clearCooldown(UUID playerId) {
        proposalCooldown.remove(playerId);
        bouquetCooldown.remove(playerId);
        marriageCooldown.remove(playerId);
    }
}