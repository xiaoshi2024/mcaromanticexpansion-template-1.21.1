package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.PregnancyManager;
import forge.net.conczin.mca.entity.ai.relationship.Gender;
import forge.net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PregnancyAttemptHandler {
    private static final ConcurrentHashMap<UUID, BlockPos> sleepingPlayers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastCheckTime = new ConcurrentHashMap<>();
    private static final long CHECK_INTERVAL = 100;

    private static final Object genderCheckLock = new Object();
    private static final ConcurrentHashMap<UUID, Long> genderChangingPlayers = new ConcurrentHashMap<>();
    private static final long GENDER_CHANGE_DURATION = 5000;

    private static final ConcurrentHashMap<UUID, Gender> lastKnownGender = new ConcurrentHashMap<>();

    // ==================== 性别缓存管理 ====================

    /**
     * 清除玩家的性别缓存（用于死亡/重生时强制刷新）
     */
    public static void clearGenderCache(ServerPlayer player) {
        if (player != null) {
            lastKnownGender.remove(player.getUUID());
            MCARomanticExpansion.LOGGER.debug("Cleared gender cache for {}", player.getName().getString());
        }
    }

    /**
     * 清除所有性别缓存
     */
    public static void clearAllGenderCache() {
        lastKnownGender.clear();
        MCARomanticExpansion.LOGGER.debug("Cleared all gender cache");
    }

    // ==================== 性别读取方法 ====================

    private static Gender readGenderFromNBT(ServerPlayer player) {
        try {
            PlayerSaveData data = PlayerSaveData.get(player);
            if (data == null) return Gender.UNASSIGNED;

            CompoundTag entityData = data.getEntityData();
            if (entityData == null) return Gender.UNASSIGNED;

            Gender lowerGender = Gender.UNASSIGNED;
            Gender upperGender = Gender.UNASSIGNED;

            if (entityData.contains("gender")) {
                int id = entityData.getInt("gender");
                Gender g = Gender.byId(id);
                if (g != null && g != Gender.UNASSIGNED) {
                    lowerGender = g;
                }
            }

            if (entityData.contains("Gender")) {
                int id = entityData.getInt("Gender");
                Gender g = Gender.byId(id);
                if (g != null && g != Gender.UNASSIGNED) {
                    upperGender = g;
                }
            }

            if (lowerGender != Gender.UNASSIGNED) {
                if (upperGender == Gender.UNASSIGNED || upperGender != lowerGender) {
                    entityData.putInt("Gender", lowerGender.getId());
                    data.setDirty();
                }
                return lowerGender;
            }

            if (upperGender != Gender.UNASSIGNED) {
                entityData.putInt("gender", upperGender.getId());
                data.setDirty();
                return upperGender;
            }

            if (entityData.contains("Genetics")) {
                CompoundTag genetics = entityData.getCompound("Genetics");
                if (genetics.contains("gender")) {
                    int id = genetics.getInt("gender");
                    Gender g = Gender.byId(id);
                    if (g != null && g != Gender.UNASSIGNED) {
                        entityData.putInt("gender", g.getId());
                        entityData.putInt("Gender", g.getId());
                        data.setDirty();
                        return g;
                    }
                }
                if (genetics.contains("Gender")) {
                    int id = genetics.getInt("Gender");
                    Gender g = Gender.byId(id);
                    if (g != null && g != Gender.UNASSIGNED) {
                        entityData.putInt("gender", g.getId());
                        entityData.putInt("Gender", g.getId());
                        data.setDirty();
                        return g;
                    }
                }
            }

            return Gender.UNASSIGNED;
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.debug("Failed to read gender: {}", e.getMessage());
            return Gender.UNASSIGNED;
        }
    }

    public static Gender getGenderFromNBT(ServerPlayer player) {
        if (player == null) return Gender.UNASSIGNED;

        UUID playerId = player.getUUID();
        Gender cached = lastKnownGender.get(playerId);
        if (cached != null && cached != Gender.UNASSIGNED) {
            return cached;
        }

        Gender gender = readGenderFromNBT(player);
        if (gender != Gender.UNASSIGNED) {
            lastKnownGender.put(playerId, gender);
        }
        return gender;
    }

    public static Gender getCachedGender(ServerPlayer player) {
        if (player == null) return Gender.UNASSIGNED;
        return lastKnownGender.getOrDefault(player.getUUID(), Gender.UNASSIGNED);
    }

    // ==================== 性别设置方法 ====================

    public static void forceSetGender(ServerPlayer player, Gender gender) {
        if (player == null || gender == null || gender == Gender.UNASSIGNED) return;

        synchronized (genderCheckLock) {
            UUID playerId = player.getUUID();
            genderChangingPlayers.put(playerId, System.currentTimeMillis());

            try {
                PlayerSaveData data = PlayerSaveData.get(player);
                if (data == null) return;

                CompoundTag entityData = data.getEntityData();

                entityData.putInt("gender", gender.getId());
                entityData.putInt("Gender", gender.getId());

                CompoundTag genetics;
                if (entityData.contains("Genetics")) {
                    genetics = entityData.getCompound("Genetics");
                } else {
                    genetics = new CompoundTag();
                    entityData.put("Genetics", genetics);
                }
                genetics.putInt("gender", gender.getId());
                genetics.putInt("Gender", gender.getId());

                CompoundTag persistentData = player.getPersistentData();
                persistentData.putInt("gender", gender.getId());
                persistentData.putInt("Gender", gender.getId());

                if (persistentData.contains("mca")) {
                    CompoundTag mcaData = persistentData.getCompound("mca");
                    mcaData.putInt("gender", gender.getId());
                }

                data.setEntityDataSet(true);
                data.setDirty();

                lastKnownGender.put(playerId, gender);

                MCARomanticExpansion.LOGGER.debug("✅ Set gender for {} to {}", player.getName().getString(), gender);

            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.error("Failed to set gender for {}: {}",
                        player.getName().getString(), e.getMessage());
            } finally {
                new Thread(() -> {
                    try {
                        Thread.sleep(GENDER_CHANGE_DURATION);
                        genderChangingPlayers.remove(playerId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }

    // ==================== 事件处理 ====================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient()) return;
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (!(player instanceof ServerPlayer serverPlayer) || player instanceof FakePlayer) {
            return;
        }

        checkGenderChange(serverPlayer);
        checkSleepingStatus(serverPlayer);

        long currentTime = System.currentTimeMillis();
        UUID playerId = serverPlayer.getUUID();

        if (lastCheckTime.containsKey(playerId)) {
            long lastTime = lastCheckTime.get(playerId);
            if (currentTime - lastTime < CHECK_INTERVAL) {
                return;
            }
        }

        PregnancyManager.PregnancyData data = PregnancyManager.getPregnancyData(playerId);
        if (data != null && data.isActive()) {
            long worldTime = serverPlayer.serverLevel().getGameTime();
            int elapsedTicks = (int) (worldTime - data.getStartTime());

            if (elapsedTicks >= data.getDurationTicks()) {
                attemptPregnancy(serverPlayer, data);
                PregnancyManager.removePregnancyPeriod(playerId);
                serverPlayer.getServer().getPlayerList().broadcastSystemMessage(
                        Component.translatable("message.mcaromanticexpansion.pregnancy_success_partner",
                                serverPlayer.getName().getString()), false);
            }
        }

        lastCheckTime.put(playerId, currentTime);

        if (genderChangingPlayers.containsKey(playerId)) {
            Long changeTime = genderChangingPlayers.get(playerId);
            if (currentTime - changeTime > GENDER_CHANGE_DURATION) {
                genderChangingPlayers.remove(playerId);
            }
        }
    }

    private static void checkGenderChange(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Gender currentGender = getGenderFromNBT(player);
        Gender cachedGender = lastKnownGender.get(playerId);

        if (cachedGender == null) {
            if (currentGender != Gender.UNASSIGNED) {
                lastKnownGender.put(playerId, currentGender);
            }
            return;
        }

        if (currentGender != Gender.UNASSIGNED && currentGender != cachedGender) {
            MCARomanticExpansion.LOGGER.debug("🔄 Gender updated for {}: {} -> {}",
                    player.getName().getString(), cachedGender, currentGender);
            lastKnownGender.put(playerId, currentGender);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            UUID playerId = serverPlayer.getUUID();

            // 清除缓存，强制重新读取
            lastKnownGender.remove(playerId);

            CompoundTag persistentData = serverPlayer.getPersistentData();
            Gender savedGender = Gender.UNASSIGNED;

            if (persistentData.contains("gender")) {
                int id = persistentData.getInt("gender");
                Gender g = Gender.byId(id);
                if (g != null && g != Gender.UNASSIGNED) {
                    savedGender = g;
                }
            }

            if (savedGender != Gender.UNASSIGNED) {
                lastKnownGender.put(playerId, savedGender);
                forceSetGender(serverPlayer, savedGender);
                MCARomanticExpansion.LOGGER.debug("✅ Restored gender for {} from persistentData: {}",
                        serverPlayer.getName().getString(), savedGender);
                return;
            }

            Gender gender = readGenderFromNBT(serverPlayer);
            if (gender != Gender.UNASSIGNED) {
                lastKnownGender.put(playerId, gender);
                persistentData.putInt("gender", gender.getId());
                persistentData.putInt("Gender", gender.getId());
                MCARomanticExpansion.LOGGER.debug("✅ Loaded gender for {} from NBT: {}",
                        serverPlayer.getName().getString(), gender);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            UUID playerId = serverPlayer.getUUID();

            Gender currentGender = readGenderFromNBT(serverPlayer);
            if (currentGender != Gender.UNASSIGNED) {
                lastKnownGender.put(playerId, currentGender);
                forceSetGender(serverPlayer, currentGender);

                CompoundTag persistentData = serverPlayer.getPersistentData();
                persistentData.putInt("gender", currentGender.getId());
                persistentData.putInt("Gender", currentGender.getId());

                MCARomanticExpansion.LOGGER.debug("💾 Saved gender on logout for {}: {}",
                        serverPlayer.getName().getString(), currentGender);
            }

            sleepingPlayers.remove(playerId);
            lastCheckTime.remove(playerId);
            genderChangingPlayers.remove(playerId);
        }
    }

    // ✅ 新增：重生时清除性别缓存
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            MCARomanticExpansion.LOGGER.debug("Player {} respawned, clearing gender cache",
                    serverPlayer.getName().getString());

            // ✅ 清除性别缓存，强制重新读取
            lastKnownGender.remove(serverPlayer.getUUID());

            fixGenderData(serverPlayer);

            // 确保备孕期数据在死亡时已被正确清除
            PregnancyManager.PregnancyData data = PregnancyManager.getPregnancyData(serverPlayer.getUUID());
            if (data != null && data.isActive()) {
                MCARomanticExpansion.LOGGER.warn("Player {} still has active pregnancy data after death, force removing",
                        serverPlayer.getName().getString());
                PregnancyManager.removePregnancyPeriod(serverPlayer);
            }

            // 同时清除伴侣的残留备孕期数据
            PregnancyManager.clearPartnerPregnancyIfDead(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            UUID playerId = serverPlayer.getUUID();
            Gender cached = lastKnownGender.get(playerId);

            if (cached != null && cached != Gender.UNASSIGNED) {
                forceSetGender(serverPlayer, cached);
                CompoundTag persistentData = serverPlayer.getPersistentData();
                persistentData.putInt("gender", cached.getId());
                persistentData.putInt("Gender", cached.getId());
            }
        }
    }

    private static void fixGenderData(ServerPlayer player) {
        try {
            PlayerSaveData data = PlayerSaveData.get(player);
            if (data == null) {
                MCARomanticExpansion.LOGGER.warn("PlayerSaveData is null for {}", player.getName().getString());
                return;
            }

            CompoundTag entityData = data.getEntityData();
            boolean changed = false;
            Gender finalGender = Gender.UNASSIGNED;

            Gender upperGender = Gender.UNASSIGNED;
            if (entityData.contains("Gender")) {
                int id = entityData.getInt("Gender");
                Gender g = Gender.byId(id);
                if (g != null && g != Gender.UNASSIGNED) {
                    upperGender = g;
                }
            }

            Gender lowerGender = Gender.UNASSIGNED;
            if (entityData.contains("gender")) {
                int id = entityData.getInt("gender");
                Gender g = Gender.byId(id);
                if (g != null && g != Gender.UNASSIGNED) {
                    lowerGender = g;
                }
            }

            if (upperGender != Gender.UNASSIGNED && lowerGender == Gender.UNASSIGNED) {
                entityData.putInt("gender", upperGender.getId());
                finalGender = upperGender;
                changed = true;
            } else if (lowerGender != Gender.UNASSIGNED && upperGender == Gender.UNASSIGNED) {
                entityData.putInt("Gender", lowerGender.getId());
                finalGender = lowerGender;
                changed = true;
            } else if (upperGender != Gender.UNASSIGNED && lowerGender != Gender.UNASSIGNED) {
                if (upperGender == lowerGender) {
                    finalGender = upperGender;
                } else {
                    finalGender = lowerGender;
                    entityData.putInt("Gender", lowerGender.getId());
                    changed = true;
                    MCARomanticExpansion.LOGGER.warn("⚠️ Gender mismatch for {}, using gender ({})",
                            player.getName().getString(), lowerGender);
                }
            }

            if (finalGender != Gender.UNASSIGNED) {
                if (entityData.contains("Genetics")) {
                    CompoundTag genetics = entityData.getCompound("Genetics");
                    if (genetics.contains("Gender") || genetics.contains("gender")) {
                        genetics.putInt("gender", finalGender.getId());
                        genetics.putInt("Gender", finalGender.getId());
                        entityData.put("Genetics", genetics);
                        changed = true;
                    }
                }

                if (changed) {
                    data.setDirty();
                    MCARomanticExpansion.LOGGER.debug("✅ Fixed gender for {}: {}", player.getName().getString(), finalGender);
                }
                lastKnownGender.put(player.getUUID(), finalGender);
            }

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to fix gender data for {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    // ==================== 诊断方法 ====================

    public static void diagnoseGenderData(ServerPlayer player) {
        if (player == null) return;

        MCARomanticExpansion.LOGGER.debug("========== Gender Diagnosis for {} ==========",
                player.getName().getString());

        try {
            CompoundTag persistentData = player.getPersistentData();
            MCARomanticExpansion.LOGGER.debug("persistentData keys: {}", persistentData.getAllKeys());

            if (persistentData.contains("gender")) {
                int id = persistentData.getInt("gender");
                MCARomanticExpansion.LOGGER.debug("  - persistent.gender = {} ({})", id, Gender.byId(id));
            }
            if (persistentData.contains("Gender")) {
                int id = persistentData.getInt("Gender");
                MCARomanticExpansion.LOGGER.debug("  - persistent.Gender = {} ({})", id, Gender.byId(id));
            }

            PlayerSaveData data = PlayerSaveData.get(player);
            if (data != null) {
                CompoundTag entityData = data.getEntityData();
                MCARomanticExpansion.LOGGER.debug("entityData keys: {}", entityData.getAllKeys());

                if (entityData.contains("Genetics")) {
                    CompoundTag genetics = entityData.getCompound("Genetics");
                    MCARomanticExpansion.LOGGER.debug("Genetics keys: {}", genetics.getAllKeys());
                }
                if (entityData.contains("Gender")) {
                    int id = entityData.getInt("Gender");
                    MCARomanticExpansion.LOGGER.debug("  - entityData.Gender = {} ({})", id, Gender.byId(id));
                }
                if (entityData.contains("gender")) {
                    int id = entityData.getInt("gender");
                    MCARomanticExpansion.LOGGER.debug("  - entityData.gender = {} ({})", id, Gender.byId(id));
                }
            }

            Gender cached = lastKnownGender.get(player.getUUID());
            MCARomanticExpansion.LOGGER.debug("  - cached gender: {}", cached);

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.error("Diagnosis failed: {}", e.getMessage());
        }

        MCARomanticExpansion.LOGGER.debug("================================================");
    }

    // ==================== 怀孕相关方法 ====================

    private static void checkSleepingStatus(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (player.isSleeping() && player.getSleepingPos().isPresent()) {
            BlockPos bedPos = player.getSleepingPos().get();
            if (bedPos != null && !sleepingPlayers.containsKey(playerId)) {
                sleepingPlayers.put(playerId, bedPos);
                checkNearbySleepingPlayers(player, bedPos, player.serverLevel());
            }
        } else if (!player.isSleeping() && sleepingPlayers.containsKey(playerId)) {
            sleepingPlayers.remove(playerId);
        }
    }

    private static void checkNearbySleepingPlayers(ServerPlayer player, BlockPos bedPos,
                                                   net.minecraft.server.level.ServerLevel level) {
        UUID playerId = player.getUUID();

        for (UUID otherPlayerId : sleepingPlayers.keySet()) {
            if (otherPlayerId.equals(playerId)) continue;

            BlockPos otherBedPos = sleepingPlayers.get(otherPlayerId);
            if (otherBedPos == null) continue;

            ServerPlayer otherPlayer = level.getServer().getPlayerList().getPlayer(otherPlayerId);
            if (otherPlayer == null || !otherPlayer.isSleeping()) continue;

            if (areBedsAdjacent(bedPos, otherBedPos)) {
                synchronized (genderCheckLock) {
                    if (!player.isSleeping() || !otherPlayer.isSleeping()) continue;
                    if (isGenderChanging(player) || isGenderChanging(otherPlayer)) continue;
                    checkPregnancyConditions(player, otherPlayer);
                }
            }
        }
    }

    private static boolean isGenderChanging(ServerPlayer player) {
        Long changeTime = genderChangingPlayers.get(player.getUUID());
        if (changeTime == null) return false;
        return System.currentTimeMillis() - changeTime < GENDER_CHANGE_DURATION;
    }

    private static boolean areBedsAdjacent(BlockPos pos1, BlockPos pos2) {
        int dx = Math.abs(pos1.getX() - pos2.getX());
        int dy = Math.abs(pos1.getY() - pos2.getY());
        int dz = Math.abs(pos1.getZ() - pos2.getZ());
        return dx <= 1 && dy <= 1 && dz <= 1;
    }

    private static void checkPregnancyConditions(ServerPlayer player1, ServerPlayer player2) {
        MCARomanticExpansion.LOGGER.debug("Checking pregnancy conditions for {} and {}",
                player1.getName().getString(), player2.getName().getString());

        Gender gender1 = getGenderFromNBT(player1);
        Gender gender2 = getGenderFromNBT(player2);

        MCARomanticExpansion.LOGGER.debug("Player {} gender: {}, Player {} gender: {}",
                player1.getName().getString(), gender1, player2.getName().getString(), gender2);

        if (gender1 == Gender.UNASSIGNED || gender2 == Gender.UNASSIGNED) {
            if (gender1 == Gender.UNASSIGNED) {
                player1.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.need_set_gender"));
            }
            if (gender2 == Gender.UNASSIGNED) {
                player2.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.need_set_gender"));
            }
            return;
        }

        if (gender1 == gender2) {
            MCARomanticExpansion.LOGGER.debug("Same gender, pregnancy check skipped");
            return;
        }

        if (PregnancyManager.isPlayerInPregnancyPeriod(player1.getUUID()) ||
                PregnancyManager.isPlayerInPregnancyPeriod(player2.getUUID())) {
            MCARomanticExpansion.LOGGER.debug("One player already in pregnancy period");
            return;
        }

        boolean player1Satiated = PregnancyManager.isFullSatiated(player1);
        boolean player2Satiated = PregnancyManager.isFullSatiated(player2);

        if (!player1Satiated || !player2Satiated) {
            MCARomanticExpansion.LOGGER.debug("Not both players are satiated");
            return;
        }

        double chance = PregnancyManager.calculatePregnancyChance(player1, player2);
        double random = player1.getRandom().nextDouble();

        MCARomanticExpansion.LOGGER.debug("Pregnancy chance: {}, random: {}", chance, random);

        if (random < chance) {
            ServerPlayer femalePlayer = gender1 == Gender.FEMALE ? player1 : player2;
            ServerPlayer malePlayer = gender1 == Gender.MALE ? player1 : player2;

            PregnancyManager.startPregnancyPeriod(femalePlayer, malePlayer,
                    femalePlayer.serverLevel().getGameTime());

            femalePlayer.sendSystemMessage(Component.translatable(
                    "message.mcaromanticexpansion.pregnancy_start",
                    malePlayer.getName().getString()));
            malePlayer.sendSystemMessage(Component.translatable(
                    "message.mcaromanticexpansion.pregnancy_start_partner",
                    femalePlayer.getName().getString()));

            MCARomanticExpansion.LOGGER.debug("🎉 Pregnancy started: {} (female) and {} (male)",
                    femalePlayer.getName().getString(), malePlayer.getName().getString());
        }
    }

    public static void attemptPregnancy(ServerPlayer player, PregnancyManager.PregnancyData data) {
        ServerPlayer partner = player.getServer().getPlayerList().getPlayer(data.getPartnerUUID());

        if (partner == null) {
            MCARomanticExpansion.LOGGER.warn("Partner not found for player {}", player.getName().getString());
            return;
        }

        try {
            Class<?> babyItemClass = Class.forName("net.conczin.mca.item.BabyItem");
            java.lang.reflect.Method createItemMethod = babyItemClass.getDeclaredMethod("createItem",
                    net.minecraft.world.entity.Entity.class,
                    net.minecraft.world.entity.Entity.class,
                    long.class);
            createItemMethod.setAccessible(true);

            ItemStack babyItem = (ItemStack) createItemMethod.invoke(null, player, partner,
                    player.getRandom().nextLong());

            if (!player.addItem(babyItem)) {
                player.drop(babyItem, false);
            }

            player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.pregnancy_success"));
            partner.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.pregnancy_success_partner",
                    player.getName().getString()));

            MCARomanticExpansion.LOGGER.debug("👶 Pregnancy completed: {} and {}",
                    player.getName().getString(), partner.getName().getString());

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.error("Failed to trigger procreation: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public static ConcurrentHashMap<UUID, Long> getGenderChangingPlayers() {
        return genderChangingPlayers;
    }

    /**
     * 强制修复性别数据 - 修复不一致，但不覆盖用户设置
     * 供 PlayerInteractionHandler 调用
     */
    public static void forceFixGenderData(ServerPlayer player) {
        if (player == null) return;
        fixGenderData(player);
    }
}