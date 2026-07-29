package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.PregnancyManager;
import forge.net.mca.entity.ai.relationship.Gender;
import forge.net.mca.server.world.data.PlayerSaveData;
import net.minecraft.ChatFormatting;
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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // ✅ Forge 1.20.1: 检查 side 和 phase
        if (event.side.isClient()) return;
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
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

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            fixGenderData(serverPlayer);
        }
    }

    /**
     * 修复 MCA 的性别大小写 Bug
     */
    private static void fixGenderData(ServerPlayer player) {
        try {
            PlayerSaveData data = PlayerSaveData.get(player);
            if (data == null) {
                MCARomanticExpansion.LOGGER.warn("PlayerSaveData is null for {}", player.getName().getString());
                return;
            }

            CompoundTag entityData = data.getEntityData();

            // 检查是否有大写 Gender 字段
            if (entityData.contains("Gender")) {
                int genderId = entityData.getInt("Gender");
                Gender gender = Gender.byId(genderId);

                if (gender != null && gender != Gender.UNASSIGNED) {
                    // 同步到小写 gender 字段
                    if (!entityData.contains("gender") || entityData.getInt("gender") != genderId) {
                        entityData.putInt("gender", genderId);
                        data.setDirty();
                        MCARomanticExpansion.LOGGER.debug("Fixed gender for {}: copied from 'Gender' to 'gender'",
                                player.getName().getString());
                    }
                    return;
                }
            }

            // 检查是否有小写 gender 字段
            if (entityData.contains("gender")) {
                int genderId = entityData.getInt("gender");
                Gender gender = Gender.byId(genderId);
                if (gender != null && gender != Gender.UNASSIGNED) {
                    // 同步到大写 Gender 字段
                    if (!entityData.contains("Gender") || entityData.getInt("Gender") != genderId) {
                        entityData.putInt("Gender", genderId);
                        data.setDirty();
                        MCARomanticExpansion.LOGGER.debug("Fixed gender for {}: copied from 'gender' to 'Gender'",
                                player.getName().getString());
                    }
                    return;
                }
            }

            // 都没有，初始化默认性别
            if (!data.isEntityDataSet()) {
                data.setEntityDataSet(true);
                setGenderData(player, Gender.MALE);
            }

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to fix gender data for {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    /**
     * 设置性别（同时设置所有字段，兼容 MCA）
     */
    public static void setGenderData(ServerPlayer player, Gender gender) {
        if (player == null || gender == null || gender == Gender.UNASSIGNED) return;

        synchronized (genderCheckLock) {
            UUID playerId = player.getUUID();
            genderChangingPlayers.put(playerId, System.currentTimeMillis());

            try {
                PlayerSaveData data = PlayerSaveData.get(player);
                if (data == null) {
                    MCARomanticExpansion.LOGGER.warn("PlayerSaveData is null for {}", player.getName().getString());
                    return;
                }

                CompoundTag entityData = data.getEntityData();

                // 同时设置大小写字段
                entityData.putInt("gender", gender.getId());
                entityData.putInt("Gender", gender.getId());

                // 同时更新 Genetics 标签
                CompoundTag genetics;
                if (entityData.contains("Genetics")) {
                    genetics = entityData.getCompound("Genetics");
                } else {
                    genetics = new CompoundTag();
                    entityData.put("Genetics", genetics);
                }
                genetics.putInt("Gender", gender.getId());
                genetics.putInt("gender", gender.getId());

                data.setEntityDataSet(true);
                data.setDirty();

                MCARomanticExpansion.LOGGER.debug("Set gender for {} to {}",
                        player.getName().getString(), gender);

                player.sendSystemMessage(Component.literal("§a[MCA] 你的性别已设置为: " +
                        (gender == Gender.MALE ? "男性" : "女性")));

            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.error("Failed to set gender for {}: {}",
                        player.getName().getString(), e.getMessage());
            } finally {
                // 延迟清除标记，给 NBT 保存一些时间
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

    /**
     * 从 NBT 读取性别（直接读取，绕过 MCA 的 getGender() Bug）
     */
    public static Gender getGenderFromNBT(ServerPlayer player) {
        if (player == null) return Gender.UNASSIGNED;

        synchronized (genderCheckLock) {
            try {
                PlayerSaveData data = PlayerSaveData.get(player);
                if (data == null) {
                    MCARomanticExpansion.LOGGER.debug("PlayerSaveData is null for {}", player.getName().getString());
                    return Gender.UNASSIGNED;
                }

                CompoundTag entityData = data.getEntityData();

                // 确保数据已初始化
                if (!data.isEntityDataSet()) {
                    data.setEntityDataSet(true);
                    setGenderData(player, Gender.MALE);
                    return Gender.MALE;
                }

                // 1. 优先读取大写 Gender（Genetics 实际保存的位置）
                if (entityData.contains("Gender")) {
                    int genderId = entityData.getInt("Gender");
                    Gender gender = Gender.byId(genderId);
                    if (gender != null && gender != Gender.UNASSIGNED) {
                        return gender;
                    }
                }

                // 2. 从 Genetics 读取
                if (entityData.contains("Genetics")) {
                    CompoundTag genetics = entityData.getCompound("Genetics");
                    if (genetics.contains("Gender")) {
                        int genderId = genetics.getInt("Gender");
                        Gender gender = Gender.byId(genderId);
                        if (gender != null && gender != Gender.UNASSIGNED) {
                            return gender;
                        }
                    }
                    if (genetics.contains("gender")) {
                        int genderId = genetics.getInt("gender");
                        Gender gender = Gender.byId(genderId);
                        if (gender != null && gender != Gender.UNASSIGNED) {
                            return gender;
                        }
                    }
                }

                // 3. 后备：读取小写 gender
                if (entityData.contains("gender")) {
                    int genderId = entityData.getInt("gender");
                    Gender gender = Gender.byId(genderId);
                    if (gender != null && gender != Gender.UNASSIGNED) {
                        return gender;
                    }
                }

                // 4. 没有性别数据，默认男性
                MCARomanticExpansion.LOGGER.debug("No gender data for {}, defaulting to MALE",
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
                player1.getName().getString(), gender1,
                player2.getName().getString(), gender2);

        // 检查性别是否已设置
        if (gender1 == Gender.UNASSIGNED || gender2 == Gender.UNASSIGNED) {
            if (gender1 == Gender.UNASSIGNED) {
                player1.sendSystemMessage(Component.literal("§c请使用 /mca editor 设置性别！")
                        .withStyle(ChatFormatting.RED));
            }
            if (gender2 == Gender.UNASSIGNED) {
                player2.sendSystemMessage(Component.literal("§c请使用 /mca editor 设置性别！")
                        .withStyle(ChatFormatting.RED));
            }
            return;
        }

        // 检查是否同性
        if (gender1 == gender2) {
            MCARomanticExpansion.LOGGER.debug("Same gender, pregnancy check skipped");
            player1.sendSystemMessage(Component.literal("§c需要异性才能怀孕！")
                    .withStyle(ChatFormatting.RED));
            player2.sendSystemMessage(Component.literal("§c需要异性才能怀孕！")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // 检查是否已在怀孕期
        if (PregnancyManager.isPlayerInPregnancyPeriod(player1.getUUID()) ||
                PregnancyManager.isPlayerInPregnancyPeriod(player2.getUUID())) {
            MCARomanticExpansion.LOGGER.debug("One player already in pregnancy period");
            return;
        }

        // 检查饱腹度
        boolean player1Satiated = PregnancyManager.isFullSatiated(player1);
        boolean player2Satiated = PregnancyManager.isFullSatiated(player2);

        if (!player1Satiated || !player2Satiated) {
            MCARomanticExpansion.LOGGER.debug("Not both players are satiated");
            return;
        }

        // 计算怀孕概率
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

            MCARomanticExpansion.LOGGER.info("🎉 Pregnancy started: {} (female) and {} (male)",
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
            // 使用 MCA 的 BabyItem 创建婴儿
            Class<?> babyItemClass = Class.forName("forge.net.mca.item.BabyItem");
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

            MCARomanticExpansion.LOGGER.info("👶 Pregnancy completed: {} and {}",
                    player.getName().getString(), partner.getName().getString());

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.error("Failed to trigger procreation: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public static ConcurrentHashMap<UUID, Long> getGenderChangingPlayers() {
        return genderChangingPlayers;
    }
}