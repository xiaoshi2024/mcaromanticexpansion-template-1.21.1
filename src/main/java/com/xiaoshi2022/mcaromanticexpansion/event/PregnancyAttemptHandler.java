package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.PregnancyManager;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PregnancyAttemptHandler {
    private static final ConcurrentHashMap<UUID, BlockPos> sleepingPlayers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastCheckTime = new ConcurrentHashMap<>();
    private static final long CHECK_INTERVAL = 100;

    private static final Object genderCheckLock = new Object();

    private static final ConcurrentHashMap<UUID, Long> genderChangingPlayers = new ConcurrentHashMap<>();
    private static final long GENDER_CHANGE_DURATION = 5000;

    private static final ConcurrentHashMap<UUID, Gender> lastKnownGender = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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

    public static Gender getGenderFromMCA(ServerPlayer player) {
        if (player == null) return Gender.UNASSIGNED;

        UUID playerId = player.getUUID();

        Gender cached = lastKnownGender.get(playerId);
        if (cached != null && cached != Gender.UNASSIGNED) {
            return cached;
        }

        try {
            PlayerSaveData data = PlayerSaveData.get(player);
            if (data != null) {
                Gender gender = data.getGender();
                if (gender != null && gender != Gender.UNASSIGNED) {
                    lastKnownGender.put(playerId, gender);
                    MCARomanticExpansion.LOGGER.debug("✅ Read gender via MCA API for {}: {}",
                            player.getName().getString(), gender);
                    return gender;
                }
            }
            return Gender.UNASSIGNED;
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to get gender via MCA API for {}: {}",
                    player.getName().getString(), e.getMessage());
            return Gender.UNASSIGNED;
        }
    }

    public static Gender getGenderFromNBT(ServerPlayer player) {
        return getGenderFromMCA(player);
    }

    public static Gender getGenderFromMCAForce(ServerPlayer player) {
        if (player == null) return Gender.UNASSIGNED;

        try {
            PlayerSaveData data = PlayerSaveData.get(player);
            if (data != null) {
                Gender gender = data.getGender();
                if (gender != null && gender != Gender.UNASSIGNED) {
                    lastKnownGender.put(player.getUUID(), gender);
                    MCARomanticExpansion.LOGGER.debug("✅ Force read gender for {}: {}",
                            player.getName().getString(), gender);
                    return gender;
                }
            }
            return Gender.UNASSIGNED;
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Force read gender failed for {}: {}",
                    player.getName().getString(), e.getMessage());
            return Gender.UNASSIGNED;
        }
    }

    // ==================== 性别设置方法 ====================

    public static void setGenderData(ServerPlayer player, Gender gender) {
        if (gender == Gender.UNASSIGNED) {
            MCARomanticExpansion.LOGGER.warn("Attempted to set UNASSIGNED gender for {}, ignoring",
                    player.getName().getString());
            return;
        }

        synchronized (genderCheckLock) {
            UUID playerId = player.getUUID();
            genderChangingPlayers.put(playerId, System.currentTimeMillis());

            try {
                PlayerSaveData data = PlayerSaveData.get(player);
                CompoundTag entityData = data.getEntityData();

                // 使用 Optional 处理
                entityData.putInt("gender", gender.getId());
                entityData.putInt("Gender", gender.getId());

                Optional<CompoundTag> geneticsOpt = entityData.getCompound("Genetics");
                CompoundTag genetics;
                if (geneticsOpt.isPresent()) {
                    genetics = geneticsOpt.get();
                } else {
                    genetics = new CompoundTag();
                    entityData.put("Genetics", genetics);
                }
                genetics.putInt("Gender", gender.getId());
                genetics.putInt("gender", gender.getId());

                CompoundTag persistentData = player.getPersistentData();
                persistentData.putInt("gender", gender.getId());
                persistentData.putInt("Gender", gender.getId());

                Optional<CompoundTag> mcaOpt = persistentData.getCompound("mca");
                if (mcaOpt.isPresent()) {
                    CompoundTag mcaData = mcaOpt.get();
                    mcaData.putInt("gender", gender.getId());
                }

                data.setEntityDataSet(true);
                data.setDirty();

                lastKnownGender.put(playerId, gender);

                MCARomanticExpansion.LOGGER.info("✅ Set gender for {} to {}",
                        player.getName().getString(), gender);

                player.sendSystemMessage(Component.translatable(
                        gender == Gender.MALE
                                ? "message.mcaromanticexpansion.gender.set.male"
                                : "message.mcaromanticexpansion.gender.set.female"));

            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.error("Failed to set gender for {}: {}",
                        player.getName().getString(), e.getMessage());
            } finally {
                scheduler.schedule(() -> {
                    genderChangingPlayers.remove(playerId);
                    MCARomanticExpansion.LOGGER.debug("Removed gender change protection for {}",
                            player.getName().getString());
                }, GENDER_CHANGE_DURATION, TimeUnit.MILLISECONDS);
            }
        }
    }

    public static void forceSetGender(ServerPlayer player, Gender gender) {
        if (gender == Gender.UNASSIGNED) return;

        synchronized (genderCheckLock) {
            try {
                PlayerSaveData data = PlayerSaveData.get(player);
                CompoundTag entityData = data.getEntityData();

                entityData.putInt("gender", gender.getId());
                entityData.putInt("Gender", gender.getId());

                Optional<CompoundTag> geneticsOpt = entityData.getCompound("Genetics");
                CompoundTag genetics;
                if (geneticsOpt.isPresent()) {
                    genetics = geneticsOpt.get();
                } else {
                    genetics = new CompoundTag();
                    entityData.put("Genetics", genetics);
                }
                genetics.putInt("gender", gender.getId());
                genetics.putInt("Gender", gender.getId());

                CompoundTag persistentData = player.getPersistentData();
                persistentData.putInt("gender", gender.getId());
                persistentData.putInt("Gender", gender.getId());

                Optional<CompoundTag> mcaOpt = persistentData.getCompound("mca");
                if (mcaOpt.isPresent()) {
                    CompoundTag mcaData = mcaOpt.get();
                    mcaData.putInt("gender", gender.getId());
                }

                data.setEntityDataSet(true);
                data.setDirty();

                lastKnownGender.put(player.getUUID(), gender);
                MCARomanticExpansion.LOGGER.debug("✅ Force set gender for {} to {}",
                        player.getName().getString(), gender);

            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.error("Failed to force set gender: {}", e.getMessage());
            }
        }
    }

    // ==================== 事件处理 ====================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

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
            long worldTime = serverPlayer.level().getGameTime();
            int elapsedTicks = (int) (worldTime - data.getStartTime());

            if (elapsedTicks >= data.getDurationTicks()) {
                attemptPregnancy(serverPlayer, data);
                PregnancyManager.removePregnancyPeriod(playerId);
                if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                    serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                            Component.translatable("message.mcaromanticexpansion.pregnancy_success_partner",
                                    serverPlayer.getName().getString()), false);
                }
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
        Gender currentGender = getGenderFromMCA(player);
        Gender cachedGender = lastKnownGender.get(playerId);

        if (cachedGender == null) {
            if (currentGender != Gender.UNASSIGNED) {
                lastKnownGender.put(playerId, currentGender);
                MCARomanticExpansion.LOGGER.debug("Initial gender cached for {}: {}",
                        player.getName().getString(), currentGender);
            }
            return;
        }

        if (currentGender != Gender.UNASSIGNED && currentGender != cachedGender) {
            MCARomanticExpansion.LOGGER.debug("🔄 Gender updated for {}: {} -> {} (cache updated)",
                    player.getName().getString(), cachedGender, currentGender);
            lastKnownGender.put(playerId, currentGender);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // ✅ 登录时清除缓存，强制重新读取
            lastKnownGender.remove(serverPlayer.getUUID());
            fixGenderData(serverPlayer);

            Gender gender = getGenderFromMCA(serverPlayer);
            if (gender == Gender.UNASSIGNED) {
                MCARomanticExpansion.LOGGER.info("Player {} has no gender data, please use MCA editor to set",
                        serverPlayer.getName().getString());
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.mcaromanticexpansion.need_set_gender"));
            } else {
                MCARomanticExpansion.LOGGER.debug("Player {} logged in with gender: {}",
                        serverPlayer.getName().getString(), gender);
                lastKnownGender.put(serverPlayer.getUUID(), gender);
            }

            diagnoseGenderData(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            UUID playerId = serverPlayer.getUUID();

            Gender currentGender = getGenderFromMCA(serverPlayer);
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

    // ✅ 修复：重生时清除性别缓存
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            MCARomanticExpansion.LOGGER.debug("Player {} respawned, clearing gender cache",
                    serverPlayer.getName().getString());

            // ✅ 清除性别缓存，强制重新读取
            lastKnownGender.remove(serverPlayer.getUUID());

            // 修复性别数据
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

            // 读取大写和小写 - 使用 Optional
            Gender upperGender = Gender.UNASSIGNED;
            Optional<Integer> upperOpt = entityData.getInt("Gender");
            if (upperOpt.isPresent()) {
                int id = upperOpt.get();
                Gender g = Gender.byId(id);
                if (g != null && g != Gender.UNASSIGNED) {
                    upperGender = g;
                }
            }

            Gender lowerGender = Gender.UNASSIGNED;
            Optional<Integer> lowerOpt = entityData.getInt("gender");
            if (lowerOpt.isPresent()) {
                int id = lowerOpt.get();
                Gender g = Gender.byId(id);
                if (g != null && g != Gender.UNASSIGNED) {
                    lowerGender = g;
                }
            }

            if (upperGender == Gender.UNASSIGNED && lowerGender == Gender.UNASSIGNED) {
                MCARomanticExpansion.LOGGER.info("No gender data found for {}, please use MCA editor to set gender",
                        player.getName().getString());
                return;
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
                Optional<CompoundTag> geneticsOpt = entityData.getCompound("Genetics");
                if (geneticsOpt.isPresent()) {
                    CompoundTag genetics = geneticsOpt.get();
                    if (genetics.contains("Gender") || genetics.contains("gender")) {
                        genetics.putInt("gender", finalGender.getId());
                        genetics.putInt("Gender", finalGender.getId());
                        entityData.put("Genetics", genetics);
                        changed = true;
                    }
                }

                if (changed) {
                    data.setDirty();
                    MCARomanticExpansion.LOGGER.debug("✅ Fixed gender for {}: {}",
                            player.getName().getString(), finalGender);
                }
                lastKnownGender.put(player.getUUID(), finalGender);
            }

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to fix gender data for {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    // ==================== 诊断方法 ====================

    public static void diagnoseGenderData(ServerPlayer player) {
        if (player == null) return;

        MCARomanticExpansion.LOGGER.info("========== Gender Diagnosis for {} ==========",
                player.getName().getString());

        try {
            CompoundTag persistentData = player.getPersistentData();
            MCARomanticExpansion.LOGGER.info("persistentData keys: {}", persistentData.keySet());

            Optional<Integer> persistGenderOpt = persistentData.getInt("gender");
            if (persistGenderOpt.isPresent()) {
                int id = persistGenderOpt.get();
                MCARomanticExpansion.LOGGER.info("  - persistent.gender = {} ({})", id, Gender.byId(id));
            }

            Optional<Integer> persistGenderUpperOpt = persistentData.getInt("Gender");
            if (persistGenderUpperOpt.isPresent()) {
                int id = persistGenderUpperOpt.get();
                MCARomanticExpansion.LOGGER.info("  - persistent.Gender = {} ({})", id, Gender.byId(id));
            }

            Optional<CompoundTag> mcaPersistentOpt = persistentData.getCompound("mca");
            if (mcaPersistentOpt.isPresent()) {
                CompoundTag mcaData = mcaPersistentOpt.get();
                Optional<Integer> mcaGenderOpt = mcaData.getInt("gender");
                if (mcaGenderOpt.isPresent()) {
                    int id = mcaGenderOpt.get();
                    MCARomanticExpansion.LOGGER.info("  - mca.gender = {} ({})", id, Gender.byId(id));
                }
            }

            PlayerSaveData data = PlayerSaveData.get(player);
            if (data != null) {
                CompoundTag entityData = data.getEntityData();
                MCARomanticExpansion.LOGGER.info("entityData keys: {}", entityData.keySet());

                Optional<Integer> entityGenderOpt = entityData.getInt("gender");
                if (entityGenderOpt.isPresent()) {
                    int id = entityGenderOpt.get();
                    MCARomanticExpansion.LOGGER.info("  - entityData.gender = {} ({})", id, Gender.byId(id));
                }

                Optional<Integer> entityGenderUpperOpt = entityData.getInt("Gender");
                if (entityGenderUpperOpt.isPresent()) {
                    int id = entityGenderUpperOpt.get();
                    MCARomanticExpansion.LOGGER.info("  - entityData.Gender = {} ({})", id, Gender.byId(id));
                }

                Optional<CompoundTag> geneticsOpt = entityData.getCompound("Genetics");
                if (geneticsOpt.isPresent()) {
                    CompoundTag genetics = geneticsOpt.get();
                    Optional<Integer> genLowerOpt = genetics.getInt("gender");
                    if (genLowerOpt.isPresent()) {
                        int id = genLowerOpt.get();
                        MCARomanticExpansion.LOGGER.info("  - Genetics.gender = {} ({})", id, Gender.byId(id));
                    }
                    Optional<Integer> genUpperOpt = genetics.getInt("Gender");
                    if (genUpperOpt.isPresent()) {
                        int id = genUpperOpt.get();
                        MCARomanticExpansion.LOGGER.info("  - Genetics.Gender = {} ({})", id, Gender.byId(id));
                    }
                }

                try {
                    Gender apiGender = data.getGender();
                    MCARomanticExpansion.LOGGER.info("  - MCA API getGender() = {}", apiGender);
                } catch (Exception e) {
                    MCARomanticExpansion.LOGGER.warn("  - MCA API getGender() failed: {}", e.getMessage());
                }
            }

            Gender cached = lastKnownGender.get(player.getUUID());
            MCARomanticExpansion.LOGGER.info("  - cached gender: {}", cached);

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.error("Diagnosis failed: {}", e.getMessage());
        }

        MCARomanticExpansion.LOGGER.info("================================================");
    }

    // ==================== 怀孕相关方法 ====================

    private static void checkSleepingStatus(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (player.isSleeping() && player.getSleepingPos().isPresent()) {
            BlockPos bedPos = player.getSleepingPos().get();
            if (bedPos != null && !sleepingPlayers.containsKey(playerId)) {
                sleepingPlayers.put(playerId, bedPos);
                MCARomanticExpansion.LOGGER.debug("Player {} is sleeping at position {}",
                        player.getName().getString(), bedPos);
                if (player.level() instanceof ServerLevel serverLevel) {
                    checkNearbySleepingPlayers(player, bedPos, serverLevel);
                }
            }
        } else if (!player.isSleeping() && sleepingPlayers.containsKey(playerId)) {
            sleepingPlayers.remove(playerId);
            MCARomanticExpansion.LOGGER.debug("Player {} woke up", player.getName().getString());
        }
    }

    private static void checkNearbySleepingPlayers(ServerPlayer player, BlockPos bedPos, ServerLevel level) {
        UUID playerId = player.getUUID();

        for (UUID otherPlayerId : new ArrayList<>(sleepingPlayers.keySet())) {
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
        MCARomanticExpansion.LOGGER.debug("=== Pregnancy Check Started ===");
        MCARomanticExpansion.LOGGER.debug("Checking pregnancy conditions for {} and {}",
                player1.getName().getString(), player2.getName().getString());

        if (!PregnancyManager.canPlayerPregnancy(player1, player2)) {
            MCARomanticExpansion.LOGGER.debug("Basic pregnancy conditions not met, check skipped");
            return;
        }

        if (PregnancyManager.isPlayerInPregnancyPeriodForce(player1) ||
                PregnancyManager.isPlayerInPregnancyPeriodForce(player2)) {
            MCARomanticExpansion.LOGGER.debug("One player already in pregnancy period, check skipped");
            return;
        }

        boolean player1Satiated = PregnancyManager.isFullSatiated(player1);
        boolean player2Satiated = PregnancyManager.isFullSatiated(player2);

        MCARomanticExpansion.LOGGER.debug("{} satiated: {}, {} satiated: {}",
                player1.getName().getString(), player1Satiated,
                player2.getName().getString(), player2Satiated);

        if (!player1Satiated || !player2Satiated) {
            MCARomanticExpansion.LOGGER.debug("Not both players are satiated, check skipped");
            return;
        }

        double chance = PregnancyManager.calculatePregnancyChance(player1, player2);
        double random = player1.getRandom().nextDouble();

        MCARomanticExpansion.LOGGER.debug("Pregnancy chance: {}%, random roll: {}",
                String.format("%.2f", chance * 100), String.format("%.4f", random));

        if (random < chance) {
            Gender gender1 = PregnancyAttemptHandler.getGenderFromMCAForce(player1);
            Gender gender2 = PregnancyAttemptHandler.getGenderFromMCAForce(player2);

            ServerPlayer femalePlayer = gender1 == Gender.FEMALE ? player1 : player2;
            ServerPlayer malePlayer = gender1 == Gender.MALE ? player1 : player2;

            MCARomanticExpansion.LOGGER.info("🎉 PREGNANCY TRIGGERED! {} (female) and {} (male)",
                    femalePlayer.getName().getString(), malePlayer.getName().getString());

            long gameTime = femalePlayer.level() instanceof ServerLevel serverLevel ? serverLevel.getGameTime() : 0;
            PregnancyManager.startPregnancyPeriod(femalePlayer, malePlayer, gameTime);

            femalePlayer.sendSystemMessage(Component.translatable(
                    "message.mcaromanticexpansion.pregnancy_start",
                    malePlayer.getName().getString()));
            malePlayer.sendSystemMessage(Component.translatable(
                    "message.mcaromanticexpansion.pregnancy_start_partner",
                    femalePlayer.getName().getString()));

            MCARomanticExpansion.LOGGER.debug("=== Pregnancy Check Completed (SUCCESS) ===");
        } else {
            MCARomanticExpansion.LOGGER.debug("Pregnancy not triggered this time");
            MCARomanticExpansion.LOGGER.debug("=== Pregnancy Check Completed (FAILED) ===");
        }
    }

    public static void attemptPregnancy(ServerPlayer player, PregnancyManager.PregnancyData data) {
        ServerPlayer partner = null;
        if (player.level() instanceof ServerLevel serverLevel) {
            partner = serverLevel.getServer().getPlayerList().getPlayer(data.getPartnerUUID());
        }

        if (partner == null) {
            MCARomanticExpansion.LOGGER.warn("Partner not found for player {}", player.getName().getString());
            return;
        }

        MCARomanticExpansion.LOGGER.debug("Attempting pregnancy for player {} with partner {}",
                player.getName().getString(), partner.getName().getString());

        try {
            Class<?> babyItemClass = Class.forName("net.conczin.mca.item.BabyItem");
            java.lang.reflect.Method createItemMethod = babyItemClass.getDeclaredMethod("createItem",
                    net.minecraft.world.entity.Entity.class,
                    net.minecraft.world.entity.Entity.class,
                    long.class);

            ItemStack babyItem = (ItemStack) createItemMethod.invoke(null, player, partner, player.getRandom().nextLong());

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

    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}