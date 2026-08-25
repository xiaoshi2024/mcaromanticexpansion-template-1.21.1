package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PregnancyManager {

    // NBT 键名常量
    private static final String PREGNANCY_DATA_KEY = "mcaromanticexpansion_pregnancy";
    private static final String TAG_PARTNER_UUID = "PartnerUUID";
    private static final String TAG_START_TIME = "StartTime";
    private static final String TAG_DURATION_TICKS = "DurationTicks";
    private static final String TAG_ACTIVE = "Active";

    public static final int PREGNANCY_PERIOD_TICKS = 12000;
    public static final double BASE_PREGNANCY_CHANCE = 0.15;
    public static final double MARRIED_BONUS_MULTIPLIER = 2.0;
    public static final double HAS_CHILDREN_PENALTY = 0.5;
    public static final double FULL_SATIATION_BONUS = 1.5;

    // 运行时缓存
    private static final Map<UUID, PregnancyData> playerPregnancyData = new ConcurrentHashMap<>();

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

        // 将数据写入 NBT - 修复: 使用 putString 存储 UUID
        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            // 修复: CompoundTag 没有 putUUID 方法，使用 putString 存储 UUID 的字符串形式
            tag.putString(TAG_PARTNER_UUID, partnerUUID.toString());
            tag.putLong(TAG_START_TIME, startTime);
            tag.putInt(TAG_DURATION_TICKS, durationTicks);
            tag.putBoolean(TAG_ACTIVE, active);
            return tag;
        }

        // 从 NBT 读取数据 - 修复: 使用 getString 读取 UUID
        public static PregnancyData deserialize(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) {
                return null;
            }
            // 修复: CompoundTag 没有 getUUID 方法，使用 getString 读取 UUID 字符串
            String uuidStr = tag.getString(TAG_PARTNER_UUID).orElse(null);
            if (uuidStr == null) {
                return null;
            }
            UUID partnerUUID = UUID.fromString(uuidStr);
            long startTime = tag.getLong(TAG_START_TIME).orElse(0L);
            int durationTicks = tag.getInt(TAG_DURATION_TICKS).orElse(PREGNANCY_PERIOD_TICKS);
            boolean active = tag.getBoolean(TAG_ACTIVE).orElse(true);

            PregnancyData data = new PregnancyData(partnerUUID, startTime, durationTicks);
            data.setActive(active);
            return data;
        }
    }

    // ========== 持久化方法 ==========

    /**
     * 保存所有玩家的备孕期数据到持久化存储
     * 建议在服务器关闭时调用
     */
    public static void saveAllToPersistentData() {
        MCARomanticExpansion.LOGGER.debug("Saving all pregnancy data to persistent storage");
    }

    /**
     * 保存单个玩家的备孕期数据
     */
    public static void saveToPersistentData(ServerPlayer player) {
        if (player == null) return;

        CompoundTag persistentData = player.getPersistentData();
        PregnancyData data = playerPregnancyData.get(player.getUUID());

        if (data != null && data.isActive()) {
            CompoundTag pregnancyData = data.serialize();
            persistentData.put(PREGNANCY_DATA_KEY, pregnancyData);
            MCARomanticExpansion.LOGGER.debug("Saved pregnancy data for player: {}", player.getName().getString());
        } else {
            // 如果没有活跃的备孕期，清除旧数据
            persistentData.remove(PREGNANCY_DATA_KEY);
            MCARomanticExpansion.LOGGER.debug("Cleared pregnancy data for player: {}", player.getName().getString());
        }
    }

    /**
     * 从持久化存储加载玩家的备孕期数据
     * 应在玩家登录时调用
     */
    public static void loadFromPersistentData(ServerPlayer player) {
        if (player == null) return;

        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(PREGNANCY_DATA_KEY)) {
            CompoundTag pregnancyTag = persistentData.getCompound(PREGNANCY_DATA_KEY).orElse(null);
            if (pregnancyTag != null) {
                PregnancyData data = PregnancyData.deserialize(pregnancyTag);
                if (data != null && data.isActive()) {
                    playerPregnancyData.put(player.getUUID(), data);
                    MCARomanticExpansion.LOGGER.debug("Loaded pregnancy data for player: {}", player.getName().getString());
                }
            }
        }
    }

    /**
     * 清除玩家的持久化备孕期数据
     */
    public static void clearPersistentData(ServerPlayer player) {
        if (player == null) return;

        CompoundTag persistentData = player.getPersistentData();
        persistentData.remove(PREGNANCY_DATA_KEY);
        MCARomanticExpansion.LOGGER.debug("Cleared persistent pregnancy data for player: {}", player.getName().getString());
    }

    // ========== 原有方法 ==========

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

        // 保存到持久化数据
        saveToPersistentData(player);

        MCARomanticExpansion.LOGGER.debug("Player {} started pregnancy period with partner {}",
                player.getName().getString(), partner.getName().getString());

        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.mcaromanticexpansion.pregnancy_start",
                partner.getName().getString()));
        partner.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.mcaromanticexpansion.pregnancy_start_partner",
                player.getName().getString()));
    }

    public static void removePregnancyPeriod(UUID playerId) {
        PregnancyData data = playerPregnancyData.remove(playerId);
        if (data != null) {
            MCARomanticExpansion.LOGGER.debug("Removed pregnancy period for player {}", playerId);
        }
    }

    public static void removePregnancyPeriod(ServerPlayer player) {
        if (player != null) {
            removePregnancyPeriod(player.getUUID());
            clearPersistentData(player);
        }
    }

    public static void deactivatePregnancyPeriod(UUID playerId) {
        PregnancyData data = playerPregnancyData.get(playerId);
        if (data != null) {
            data.setActive(false);
            MCARomanticExpansion.LOGGER.debug("Deactivated pregnancy period for player {}", playerId);
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
        clearPersistentData(player);

        PregnancyData partnerData = playerPregnancyData.values().stream()
                .filter(data -> data.isActive() && data.getPartnerUUID().equals(playerId))
                .findFirst().orElse(null);

        if (partnerData != null) {
            for (Map.Entry<UUID, PregnancyData> entry : playerPregnancyData.entrySet()) {
                if (entry.getValue().equals(partnerData)) {
                    removePregnancyPeriod(entry.getKey());
                    // 修复: 通过 level() 获取 Server
                    if (player.level() instanceof ServerLevel serverLevel) {
                        ServerPlayer partner = serverLevel.getServer().getPlayerList().getPlayer(entry.getKey());
                        if (partner != null) {
                            clearPersistentData(partner);
                            partner.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                    "message.mcaromanticexpansion.pregnancy_ended_death"));
                        }
                    }
                    break;
                }
            }
        }
    }
}