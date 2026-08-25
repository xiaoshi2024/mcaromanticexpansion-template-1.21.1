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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PregnancyAttemptHandler {
    // 使用线程安全的 ConcurrentHashMap
    private static final ConcurrentHashMap<UUID, BlockPos> sleepingPlayers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastCheckTime = new ConcurrentHashMap<>();
    private static final long CHECK_INTERVAL = 100;

    // 添加锁对象，用于保护性别相关的操作
    private static final Object genderCheckLock = new Object();

    // 记录正在修改性别的玩家，避免怀孕检查干扰
    private static final ConcurrentHashMap<UUID, Long> genderChangingPlayers = new ConcurrentHashMap<>();
    private static final long GENDER_CHANGE_DURATION = 5000; // 5秒保护时间

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (!(player instanceof ServerPlayer serverPlayer) || player instanceof FakePlayer) {
            return;
        }

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
            // 修复: 使用 level() 获取 ServerLevel
            long worldTime = serverPlayer.level().getGameTime();
            int elapsedTicks = (int) (worldTime - data.getStartTime());

            if (elapsedTicks >= data.getDurationTicks()) {
                attemptPregnancy(serverPlayer, data);
                PregnancyManager.removePregnancyPeriod(playerId);
                // 修复: 通过 level() 获取 Server
                if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                    serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                            Component.translatable("message.mcaromanticexpansion.pregnancy_success_partner", serverPlayer.getName().getString()), false);
                }
            }
        }

        lastCheckTime.put(playerId, currentTime);

        // 清理过期的性别修改标记
        if (genderChangingPlayers.containsKey(playerId)) {
            Long changeTime = genderChangingPlayers.get(playerId);
            if (currentTime - changeTime > GENDER_CHANGE_DURATION) {
                genderChangingPlayers.remove(playerId);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 登录时修复性别数据（修复MCA的大小写Bug）
            fixGenderData(serverPlayer);
        }
    }

    /**
     * 修复MCA的性别大小写Bug
     */
    private static void fixGenderData(ServerPlayer player) {
        try {
            PlayerSaveData data = PlayerSaveData.get(player);
            CompoundTag entityData = data.getEntityData();

            // 修复: getInt 返回 Optional<Integer>，需要使用 .orElse(0)
            if (entityData.contains("Gender")) {
                int genderId = entityData.getInt("Gender").orElse(0);
                Gender gender = Gender.byId(genderId);

                if (gender != Gender.UNASSIGNED) {
                    MCARomanticExpansion.LOGGER.debug("Found Gender field for {}: {}",
                            player.getName().getString(), gender);

                    // 修复: getInt 返回 Optional<Integer>，使用 .orElse(0)
                    if (!entityData.contains("gender") || entityData.getInt("gender").orElse(0) != genderId) {
                        entityData.putInt("gender", genderId);
                        data.setDirty();
                        MCARomanticExpansion.LOGGER.debug("Fixed gender for {}: copied from 'Gender' to 'gender'",
                                player.getName().getString());
                    }
                    return;
                }
            }

            // 如果都没有，初始化默认性别
            if (!data.isEntityDataSet()) {
                data.setEntityDataSet(true);
                // 默认设置为男性
                setGenderData(player, Gender.MALE);
            }

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to fix gender data for {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    /**
     * 正确设置性别数据（同时设置大写和小写，兼容MCA）
     * 外部调用此方法来修改性别时使用
     */
    public static void setGenderData(ServerPlayer player, Gender gender) {
        synchronized (genderCheckLock) {
            UUID playerId = player.getUUID();
            // 标记性别正在修改，防止怀孕检查干扰
            genderChangingPlayers.put(playerId, System.currentTimeMillis());

            try {
                PlayerSaveData data = PlayerSaveData.get(player);
                CompoundTag entityData = data.getEntityData();

                // 同时设置大写和小写，确保兼容性
                entityData.putInt("gender", gender.getId());  // 小写 - 给MCA的getGender()用
                entityData.putInt("Gender", gender.getId());  // 大写 - Genetics实际保存的位置

                // 同时更新Genetics标签
                CompoundTag genetics;
                if (entityData.contains("Genetics")) {
                    // 修复: getCompound 返回 Optional<CompoundTag>
                    genetics = entityData.getCompound("Genetics").orElse(new CompoundTag());
                } else {
                    genetics = new CompoundTag();
                    entityData.put("Genetics", genetics);
                }
                genetics.putInt("Gender", gender.getId());
                genetics.putInt("gender", gender.getId());

                data.setEntityDataSet(true);
                data.setDirty();

                MCARomanticExpansion.LOGGER.debug("Set gender for {} to {} (both fields)",
                        player.getName().getString(), gender);

                player.sendSystemMessage(Component.translatable(
                        gender == Gender.MALE
                                ? "message.mcaromanticexpansion.gender.set.male"
                                : "message.mcaromanticexpansion.gender.set.female"));

            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.error("Failed to set gender for {}: {}",
                        player.getName().getString(), e.getMessage());
            } finally {
                // 延迟清除标记，给NBT保存一些时间
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

    private static void checkSleepingStatus(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (player.isSleeping() && player.getSleepingPos().isPresent()) {
            BlockPos bedPos = player.getSleepingPos().get();
            if (bedPos != null && !sleepingPlayers.containsKey(playerId)) {
                sleepingPlayers.put(playerId, bedPos);
                MCARomanticExpansion.LOGGER.debug("Player {} is sleeping at position {}",
                        player.getName().getString(), bedPos);
                // 修复: 使用 player.level() 获取 ServerLevel
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

        // 使用 keySet 的快照遍历，避免 ConcurrentModificationException
        for (UUID otherPlayerId : sleepingPlayers.keySet()) {
            if (otherPlayerId.equals(playerId)) continue;

            BlockPos otherBedPos = sleepingPlayers.get(otherPlayerId);
            if (otherBedPos == null) continue;

            ServerPlayer otherPlayer = level.getServer().getPlayerList().getPlayer(otherPlayerId);
            if (otherPlayer == null || !otherPlayer.isSleeping()) continue;

            if (areBedsAdjacent(bedPos, otherBedPos)) {
                // 添加同步块，确保性别检查期间不被修改
                synchronized (genderCheckLock) {
                    // 重新验证两个玩家都还在睡觉
                    if (!player.isSleeping() || !otherPlayer.isSleeping()) {
                        continue;
                    }
                    // 检查是否有玩家正在修改性别
                    if (isGenderChanging(player) || isGenderChanging(otherPlayer)) {
                        MCARomanticExpansion.LOGGER.debug("Gender changing in progress, skipping pregnancy check");
                        continue;
                    }
                    checkPregnancyConditions(player, otherPlayer);
                }
            }
        }
    }

    /**
     * 检查玩家是否正在修改性别
     */
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

        // 直接从 NBT 读取大写 Gender 字段（绕过MCA的getGender() Bug）
        Gender gender1 = getGenderFromNBT(player1);
        Gender gender2 = getGenderFromNBT(player2);

        MCARomanticExpansion.LOGGER.debug("Player {} gender: {}, Player {} gender: {}",
                player1.getName().getString(), gender1, player2.getName().getString(), gender2);

        if (gender1 == gender2 || gender1 == Gender.UNASSIGNED || gender2 == Gender.UNASSIGNED) {
            if (gender1 == gender2) {
                MCARomanticExpansion.LOGGER.debug("Same gender players ({} and {}), pregnancy check skipped",
                        player1.getName().getString(), player2.getName().getString());
            } else {
                MCARomanticExpansion.LOGGER.debug("One or both players have UNASSIGNED gender, pregnancy check skipped");
                // 提示玩家设置性别
                if (gender1 == Gender.UNASSIGNED) {
                    player1.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.need_set_gender"));
                }
                if (gender2 == Gender.UNASSIGNED) {
                    player2.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.need_set_gender"));
                }
            }
            return;
        }

        if (PregnancyManager.isPlayerInPregnancyPeriod(player1.getUUID()) ||
                PregnancyManager.isPlayerInPregnancyPeriod(player2.getUUID())) {
            MCARomanticExpansion.LOGGER.debug("One of the players is already in pregnancy period");
            return;
        }

        boolean player1Satiated = PregnancyManager.isFullSatiated(player1);
        boolean player2Satiated = PregnancyManager.isFullSatiated(player2);
        MCARomanticExpansion.LOGGER.debug("Player {} satiated: {}, Player {} satiated: {}",
                player1.getName().getString(), player1Satiated, player2.getName().getString(), player2Satiated);

        if (!player1Satiated || !player2Satiated) {
            MCARomanticExpansion.LOGGER.debug("Players are not fully satiated, pregnancy check skipped");
            return;
        }

        double chance = PregnancyManager.calculatePregnancyChance(player1, player2);
        MCARomanticExpansion.LOGGER.debug("Pregnancy chance between {} and {}: {}%",
                player1.getName().getString(), player2.getName().getString(), chance * 100);

        double random = player1.getRandom().nextDouble();
        MCARomanticExpansion.LOGGER.debug("Random roll: {}, required: {}", random, chance);

        if (random < chance) {
            ServerPlayer femalePlayer = gender1 == Gender.FEMALE ? player1 : player2;
            ServerPlayer malePlayer = gender1 == Gender.MALE ? player1 : player2;

            MCARomanticExpansion.LOGGER.debug("Pregnancy triggered! Female: {}, Male: {}",
                    femalePlayer.getName().getString(), malePlayer.getName().getString());

            // 修复: 使用 femalePlayer.level() 获取 ServerLevel
            long gameTime = femalePlayer.level() instanceof ServerLevel serverLevel ? serverLevel.getGameTime() : 0;
            PregnancyManager.startPregnancyPeriod(femalePlayer, malePlayer, gameTime);

            femalePlayer.sendSystemMessage(Component.translatable(
                    "message.mcaromanticexpansion.pregnancy_start",
                    malePlayer.getName().getString()));
            malePlayer.sendSystemMessage(Component.translatable(
                    "message.mcaromanticexpansion.pregnancy_start_partner",
                    femalePlayer.getName().getString()));
        } else {
            MCARomanticExpansion.LOGGER.debug("Pregnancy not triggered this time");
        }
    }

    /**
     * 直接从 NBT 读取性别 - 绕过 MCA 的 getGender() Bug
     * 优先读取大写 "Gender" 字段（Genetics实际保存的位置）
     */
    public static Gender getGenderFromNBT(ServerPlayer player) {
        synchronized (genderCheckLock) {
            try {
                PlayerSaveData data = PlayerSaveData.get(player);
                CompoundTag entityData = data.getEntityData();

                // 确保数据已初始化
                if (!data.isEntityDataSet()) {
                    MCARomanticExpansion.LOGGER.debug("Initializing MCA data for {}", player.getName().getString());
                    data.setEntityDataSet(true);
                    setGenderData(player, Gender.MALE); // 默认男性
                    return Gender.MALE;
                }

                // 1. 优先读取大写 Gender 字段（Genetics 保存的位置）
                // 修复: getInt 返回 Optional<Integer>
                if (entityData.contains("Gender")) {
                    int genderId = entityData.getInt("Gender").orElse(0);
                    Gender gender = Gender.byId(genderId);
                    if (gender != Gender.UNASSIGNED) {
                        MCARomanticExpansion.LOGGER.debug("Gender from 'Gender' field for {}: {}",
                                player.getName().getString(), gender);
                        return gender;
                    }
                }

                // 2. 从 Genetics 标签读取
                // 修复: getCompound 返回 Optional<CompoundTag>
                if (entityData.contains("Genetics")) {
                    CompoundTag genetics = entityData.getCompound("Genetics").orElse(new CompoundTag());
                    if (genetics.contains("Gender")) {
                        int genderId = genetics.getInt("Gender").orElse(0);
                        Gender gender = Gender.byId(genderId);
                        if (gender != Gender.UNASSIGNED) {
                            MCARomanticExpansion.LOGGER.debug("Gender from Genetics.Gender for {}: {}",
                                    player.getName().getString(), gender);
                            return gender;
                        }
                    }
                }

                // 3. 后备：尝试小写 gender（MCA的getGender()读取的位置）
                if (entityData.contains("gender")) {
                    int genderId = entityData.getInt("gender").orElse(0);
                    Gender gender = Gender.byId(genderId);
                    if (gender != Gender.UNASSIGNED) {
                        MCARomanticExpansion.LOGGER.debug("Gender from 'gender' field for {}: {}",
                                player.getName().getString(), gender);
                        return gender;
                    }
                }

                // 4. 还是没有，初始化默认性别
                MCARomanticExpansion.LOGGER.warn("No gender data found for {}, initializing to MALE",
                        player.getName().getString());
                setGenderData(player, Gender.MALE);
                return Gender.MALE;

            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.error("Failed to get gender for {}: {}",
                        player.getName().getString(), e.getMessage());
                return Gender.UNASSIGNED;
            }
        }
    }

    public static void attemptPregnancy(ServerPlayer player, PregnancyManager.PregnancyData data) {
        // 修复: 通过 level() 获取 Server
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

            player.sendSystemMessage(Component.translatable(
                    "message.mcaromanticexpansion.pregnancy_success"));
            partner.sendSystemMessage(Component.translatable(
                    "message.mcaromanticexpansion.pregnancy_success_partner",
                    player.getName().getString()));

            MCARomanticExpansion.LOGGER.debug("Pregnancy successful for player {}", player.getName().getString());
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.error("Failed to trigger procreation: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取正在修改性别的玩家列表（用于调试）
     */
    public static ConcurrentHashMap<UUID, Long> getGenderChangingPlayers() {
        return genderChangingPlayers;
    }
}