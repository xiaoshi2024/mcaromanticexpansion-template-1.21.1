package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PregnancyManager {
    
    private static final Map<UUID, PregnancyData> playerPregnancyData = new ConcurrentHashMap<>();
    
    public static final int PREGNANCY_PERIOD_TICKS = 12000;
    public static final double BASE_PREGNANCY_CHANCE = 0.15;
    public static final double MARRIED_BONUS_MULTIPLIER = 2.0;
    public static final double HAS_CHILDREN_PENALTY = 0.5;
    public static final double FULL_SATIATION_BONUS = 1.5;

    public static class PregnancyData {
        private final UUID partnerUUID;
        private long startTime;
        private int durationTicks;
        private boolean active;

        public PregnancyData(UUID partnerUUID, long startTime, int durationTicks) {
            this.partnerUUID = partnerUUID;
            this.startTime = startTime;
            this.durationTicks = durationTicks;
            this.active = true;
        }

        public UUID getPartnerUUID() {
            return partnerUUID;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public int getDurationTicks() {
            return durationTicks;
        }

        public void setDurationTicks(int durationTicks) {
            this.durationTicks = durationTicks;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    public static boolean isPlayerInPregnancyPeriod(UUID playerId) {
        PregnancyData data = playerPregnancyData.get(playerId);
        return data != null && data.isActive();
    }

    public static PregnancyData getPregnancyData(UUID playerId) {
        return playerPregnancyData.get(playerId);
    }

    public static void startPregnancyPeriod(ServerPlayer player, ServerPlayer partner, long worldTime) {
        UUID playerId = player.getUUID();
        UUID partnerId = partner.getUUID();
        
        PregnancyData data = new PregnancyData(partnerId, worldTime, PREGNANCY_PERIOD_TICKS);
        playerPregnancyData.put(playerId, data);
        
        MCARomanticExpansion.LOGGER.info("Player {} started pregnancy period with partner {}", 
                player.getName().getString(), partner.getName().getString());
        
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.mcaromanticexpansion.pregnancy_start", 
                partner.getName().getString()));
        partner.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.mcaromanticexpansion.pregnancy_start_partner", 
                player.getName().getString()));
    }

    public static void removePregnancyPeriod(UUID playerId) {
        playerPregnancyData.remove(playerId);
        MCARomanticExpansion.LOGGER.info("Removed pregnancy period for player {}", playerId);
    }

    public static void deactivatePregnancyPeriod(UUID playerId) {
        PregnancyData data = playerPregnancyData.get(playerId);
        if (data != null) {
            data.setActive(false);
            MCARomanticExpansion.LOGGER.info("Deactivated pregnancy period for player {}", playerId);
        }
    }

    public static boolean hasChildren(ServerPlayer player) {
        PlayerSaveData playerData = PlayerSaveData.get(player);
        return playerData.getFamilyEntry().streamChildren().findAny().isPresent();
    }

    public static boolean isMarriedTo(ServerPlayer player, ServerPlayer partner) {
        PlayerSaveData playerData = PlayerSaveData.get(player);
        return playerData.isMarriedTo(partner.getUUID());
    }

    public static boolean isFullSatiated(Player player) {
        return player.getFoodData().getFoodLevel() >= 20 && player.getFoodData().getSaturationLevel() >= 5.0F;
    }

    public static double calculatePregnancyChance(ServerPlayer player, ServerPlayer partner) {
        double chance = BASE_PREGNANCY_CHANCE;
        
        if (isMarriedTo(player, partner)) {
            chance *= MARRIED_BONUS_MULTIPLIER;
        }
        
        if (hasChildren(player) || hasChildren(partner)) {
            chance *= HAS_CHILDREN_PENALTY;
        }
        
        if (isFullSatiated(player) && isFullSatiated(partner)) {
            chance *= FULL_SATIATION_BONUS;
        }
        
        return Math.min(chance, 1.0);
    }

    public static void onPlayerDeath(ServerPlayer player) {
        UUID playerId = player.getUUID();
        removePregnancyPeriod(playerId);
        
        PregnancyData partnerData = playerPregnancyData.values().stream()
                .filter(data -> data.isActive() && data.getPartnerUUID().equals(playerId))
                .findFirst().orElse(null);
        
        if (partnerData != null) {
            for (Map.Entry<UUID, PregnancyData> entry : playerPregnancyData.entrySet()) {
                if (entry.getValue().equals(partnerData)) {
                    removePregnancyPeriod(entry.getKey());
                    ServerPlayer partner = player.getServer().getPlayerList().getPlayer(entry.getKey());
                    if (partner != null) {
                        partner.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                "message.mcaromanticexpansion.pregnancy_ended_death"));
                    }
                    break;
                }
            }
        }
    }
}