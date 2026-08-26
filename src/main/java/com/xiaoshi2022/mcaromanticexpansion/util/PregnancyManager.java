package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.event.PregnancyAttemptHandler;
import net.conczin.mca.entity.ai.relationship.Gender;
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

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.putString(TAG_PARTNER_UUID, partnerUUID.toString());
            tag.putLong(TAG_START_TIME, startTime);
            tag.putInt(TAG_DURATION_TICKS, durationTicks);
            tag.putBoolean(TAG_ACTIVE, active);
            return tag;
        }

        public static PregnancyData deserialize(CompoundTag tag) {
            if (tag == null || tag.isEmpty()) {
                return null;
            }
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

    public static void saveAllToPersistentData() {
        MCARomanticExpansion.LOGGER.debug("Saving all pregnancy data to persistent storage");
    }

    public static void saveToPersistentData(ServerPlayer player) {
        if (player == null) return;

        CompoundTag persistentData = player.getPersistentData();
        PregnancyData data = playerPregnancyData.get(player.getUUID());

        if (data != null && data.isActive()) {
            CompoundTag pregnancyData = data.serialize();
            persistentData.put(PREGNANCY_DATA_KEY, pregnancyData);
            MCARomanticExpansion.LOGGER.debug("Saved pregnancy data for player: {}", player.getName().getString());
        } else {
            persistentData.remove(PREGNANCY_DATA_KEY);
            MCARomanticExpansion.LOGGER.debug("Cleared pregnancy data for player: {}", player.getName().getString());
        }
    }

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

    /**
     * 计算怀孕概率 - 保留结婚加成，但不需要强制结婚
     */
    public static double calculatePregnancyChance(ServerPlayer player, ServerPlayer partner) {
        double chance = BASE_PREGNANCY_CHANCE;

        // 已婚加成（如果已婚则提升概率，但不是必须条件）
        if (isMarriedTo(player, partner)) {
            chance *= MARRIED_BONUS_MULTIPLIER;
            MCARomanticExpansion.LOGGER.debug("Married bonus applied: {} x2", BASE_PREGNANCY_CHANCE);
        }

        if (hasChildren(player) || hasChildren(partner)) {
            chance *= HAS_CHILDREN_PENALTY;
            MCARomanticExpansion.LOGGER.debug("Has children penalty applied: x0.5");
        }

        if (isFullSatiated(player) && isFullSatiated(partner)) {
            chance *= FULL_SATIATION_BONUS;
            MCARomanticExpansion.LOGGER.debug("Full satiation bonus applied: x1.5");
        }

        double result = Math.min(chance, 1.0);
        MCARomanticExpansion.LOGGER.debug("Final pregnancy chance: {}%", result * 100);
        return result;
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

    // ========== 强制读取方法（不使用缓存）==========

    /**
     * 强制检查怀孕条件 - 不需要结婚，只需要异性 + 在线存活
     */
    public static boolean canPlayerPregnancy(ServerPlayer player1, ServerPlayer player2) {
        // 强制读取双方最新性别
        Gender gender1 = PregnancyAttemptHandler.getGenderFromMCAForce(player1);
        Gender gender2 = PregnancyAttemptHandler.getGenderFromMCAForce(player2);

        MCARomanticExpansion.LOGGER.debug("canPlayerPregnancy: {} is {}, {} is {}",
                player1.getName().getString(), gender1,
                player2.getName().getString(), gender2);

        // 性别检查 - 必须是异性
        if (gender1 == Gender.UNASSIGNED || gender2 == Gender.UNASSIGNED) {
            MCARomanticExpansion.LOGGER.debug("canPlayerPregnancy: UNASSIGNED gender detected, returning false");
            return false;
        }

        if (gender1 == gender2) {
            MCARomanticExpansion.LOGGER.debug("canPlayerPregnancy: Same gender ({}), returning false", gender1);
            return false;
        }

        MCARomanticExpansion.LOGGER.debug("canPlayerPregnancy: All conditions met (heterosexual + alive)");
        return true;
    }

    /**
     * 强制获取最新怀孕数据 - 从 NBT 实时读取
     */
    public static PregnancyData getPregnancyDataForce(ServerPlayer player) {
        if (player == null) return null;

        UUID playerId = player.getUUID();

        // 1. 检查内存缓存
        PregnancyData data = playerPregnancyData.get(playerId);
        if (data != null && data.isActive()) {
            return data;
        }

        // 2. 从 NBT 读取
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(PREGNANCY_DATA_KEY)) {
            CompoundTag pregnancyTag = persistentData.getCompound(PREGNANCY_DATA_KEY).orElse(null);
            if (pregnancyTag != null) {
                PregnancyData nbtData = PregnancyData.deserialize(pregnancyTag);
                if (nbtData != null && nbtData.isActive()) {
                    // 同步到内存缓存
                    playerPregnancyData.put(playerId, nbtData);
                    MCARomanticExpansion.LOGGER.debug("Loaded pregnancy data from NBT for {}: partner={}",
                            player.getName().getString(), nbtData.getPartnerUUID());
                    return nbtData;
                }
            }
        }

        return null;
    }

    /**
     * 强制检查玩家是否在怀孕期 - 从 NBT 实时读取
     */
    public static boolean isPlayerInPregnancyPeriodForce(ServerPlayer player) {
        PregnancyData data = getPregnancyDataForce(player);
        boolean result = data != null && data.isActive();
        MCARomanticExpansion.LOGGER.debug("isPlayerInPregnancyPeriodForce for {}: {}",
                player.getName().getString(), result);
        return result;
    }
}