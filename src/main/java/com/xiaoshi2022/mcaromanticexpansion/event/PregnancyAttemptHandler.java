package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.PregnancyManager;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.UUID;

public class PregnancyAttemptHandler {
    private static final HashMap<UUID, BlockPos> sleepingPlayers = new HashMap<>();
    private static final HashMap<UUID, Long> lastCheckTime = new HashMap<>();
    private static final long CHECK_INTERVAL = 100;

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
            long worldTime = serverPlayer.serverLevel().getGameTime();
            int elapsedTicks = (int) (worldTime - data.getStartTime());

            if (elapsedTicks >= data.getDurationTicks()) {
                attemptPregnancy(serverPlayer, data);
                PregnancyManager.removePregnancyPeriod(playerId);
                serverPlayer.getServer().getPlayerList().broadcastSystemMessage(
                        Component.translatable("message.mcaromanticexpansion.pregnancy_success_partner", serverPlayer.getName().getString()), false);
            }
        }

        lastCheckTime.put(playerId, currentTime);
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

            // 检查是否有大写 Gender 字段（Genetics保存的）
            if (entityData.contains("Gender")) {
                int genderId = entityData.getInt("Gender");
                Gender gender = Gender.byId(genderId);

                if (gender != Gender.UNASSIGNED) {
                    MCARomanticExpansion.LOGGER.info("Found Gender field for {}: {}",
                            player.getName().getString(), gender);

                    // 修复：同步到小写 gender 字段（让MCA的getGender()也能读到）
                    if (!entityData.contains("gender") || entityData.getInt("gender") != genderId) {
                        entityData.putInt("gender", genderId);
                        data.setDirty();
                        MCARomanticExpansion.LOGGER.info("Fixed gender for {}: copied from 'Gender' to 'gender'",
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
     */
    private static void setGenderData(ServerPlayer player, Gender gender) {
        try {
            PlayerSaveData data = PlayerSaveData.get(player);
            CompoundTag entityData = data.getEntityData();

            // 同时设置大写和小写，确保兼容性
            entityData.putInt("gender", gender.getId());  // 小写 - 给MCA的getGender()用
            entityData.putInt("Gender", gender.getId());  // 大写 - Genetics实际保存的位置

            // 同时更新Genetics标签
            if (!entityData.contains("Genetics")) {
                CompoundTag genetics = new CompoundTag();
                genetics.putInt("Gender", gender.getId());
                genetics.putInt("gender", gender.getId());
                entityData.put("Genetics", genetics);
            } else {
                CompoundTag genetics = entityData.getCompound("Genetics");
                genetics.putInt("Gender", gender.getId());
                genetics.putInt("gender", gender.getId());
            }

            data.setEntityDataSet(true);
            data.setDirty();

            MCARomanticExpansion.LOGGER.info("Set gender for {} to {} (both fields)",
                    player.getName().getString(), gender);

            player.sendSystemMessage(Component.literal("§a[MCA] 你的性别已设置为: " +
                    (gender == Gender.MALE ? "男性" : "女性")));

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.error("Failed to set gender for {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    private static void checkSleepingStatus(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (player.isSleeping() && player.getSleepingPos().isPresent()) {
            BlockPos bedPos = player.getSleepingPos().get();
            if (bedPos != null && !sleepingPlayers.containsKey(playerId)) {
                sleepingPlayers.put(playerId, bedPos);
                MCARomanticExpansion.LOGGER.info("Player {} is sleeping at position {}",
                        player.getName().getString(), bedPos);
                checkNearbySleepingPlayers(player, bedPos, player.serverLevel());
            }
        } else if (!player.isSleeping() && sleepingPlayers.containsKey(playerId)) {
            sleepingPlayers.remove(playerId);
            MCARomanticExpansion.LOGGER.info("Player {} woke up", player.getName().getString());
        }
    }

    private static void checkNearbySleepingPlayers(ServerPlayer player, BlockPos bedPos, net.minecraft.server.level.ServerLevel level) {
        UUID playerId = player.getUUID();

        for (UUID otherPlayerId : sleepingPlayers.keySet()) {
            if (otherPlayerId.equals(playerId)) continue;

            BlockPos otherBedPos = sleepingPlayers.get(otherPlayerId);
            if (otherBedPos == null) continue;

            ServerPlayer otherPlayer = level.getServer().getPlayerList().getPlayer(otherPlayerId);
            if (otherPlayer == null || !otherPlayer.isSleeping()) continue;

            if (areBedsAdjacent(bedPos, otherBedPos)) {
                MCARomanticExpansion.LOGGER.info("Found adjacent beds: {} at {} and {} at {}",
                        player.getName().getString(), bedPos,
                        otherPlayer.getName().getString(), otherBedPos);
                checkPregnancyConditions(player, otherPlayer);
            }
        }
    }

    private static boolean areBedsAdjacent(BlockPos pos1, BlockPos pos2) {
        int dx = Math.abs(pos1.getX() - pos2.getX());
        int dy = Math.abs(pos1.getY() - pos2.getY());
        int dz = Math.abs(pos1.getZ() - pos2.getZ());
        return dx <= 1 && dy <= 1 && dz <= 1;
    }

    private static void checkPregnancyConditions(ServerPlayer player1, ServerPlayer player2) {
        MCARomanticExpansion.LOGGER.info("Checking pregnancy conditions for {} and {}",
                player1.getName().getString(), player2.getName().getString());

        // 直接从 NBT 读取大写 Gender 字段（绕过MCA的getGender() Bug）
        Gender gender1 = getGenderFromNBT(player1);
        Gender gender2 = getGenderFromNBT(player2);

        MCARomanticExpansion.LOGGER.info("Player {} gender: {}, Player {} gender: {}",
                player1.getName().getString(), gender1, player2.getName().getString(), gender2);

        if (gender1 == gender2 || gender1 == Gender.UNASSIGNED || gender2 == Gender.UNASSIGNED) {
            if (gender1 == gender2) {
                MCARomanticExpansion.LOGGER.info("Same gender players ({} and {}), pregnancy check skipped",
                        player1.getName().getString(), player2.getName().getString());
            } else {
                MCARomanticExpansion.LOGGER.info("One or both players have UNASSIGNED gender, pregnancy check skipped");
                // 提示玩家设置性别
                if (gender1 == Gender.UNASSIGNED) {
                    player1.sendSystemMessage(Component.literal("§c请使用 /mca gender set " + player1.getName().getString() + " MALE/FEMALE 设置性别"));
                }
                if (gender2 == Gender.UNASSIGNED) {
                    player2.sendSystemMessage(Component.literal("§c请使用 /mca gender set " + player2.getName().getString() + " MALE/FEMALE 设置性别"));
                }
            }
            return;
        }

        if (PregnancyManager.isPlayerInPregnancyPeriod(player1.getUUID()) ||
                PregnancyManager.isPlayerInPregnancyPeriod(player2.getUUID())) {
            MCARomanticExpansion.LOGGER.info("One of the players is already in pregnancy period");
            return;
        }

        boolean player1Satiated = PregnancyManager.isFullSatiated(player1);
        boolean player2Satiated = PregnancyManager.isFullSatiated(player2);
        MCARomanticExpansion.LOGGER.info("Player {} satiated: {}, Player {} satiated: {}",
                player1.getName().getString(), player1Satiated, player2.getName().getString(), player2Satiated);

        if (!player1Satiated || !player2Satiated) {
            MCARomanticExpansion.LOGGER.info("Players are not fully satiated, pregnancy check skipped");
            return;
        }

        double chance = PregnancyManager.calculatePregnancyChance(player1, player2);
        MCARomanticExpansion.LOGGER.info("Pregnancy chance between {} and {}: {}%",
                player1.getName().getString(), player2.getName().getString(), chance * 100);

        double random = player1.getRandom().nextDouble();
        MCARomanticExpansion.LOGGER.info("Random roll: {}, required: {}", random, chance);

        if (random < chance) {
            ServerPlayer femalePlayer = gender1 == Gender.FEMALE ? player1 : player2;
            ServerPlayer malePlayer = gender1 == Gender.MALE ? player1 : player2;

            MCARomanticExpansion.LOGGER.info("Pregnancy triggered! Female: {}, Male: {}",
                    femalePlayer.getName().getString(), malePlayer.getName().getString());

            PregnancyManager.startPregnancyPeriod(femalePlayer, malePlayer, femalePlayer.serverLevel().getGameTime());

            femalePlayer.sendSystemMessage(Component.translatable(
                    "message.mcaromanticexpansion.pregnancy_start",
                    malePlayer.getName().getString()));
            malePlayer.sendSystemMessage(Component.translatable(
                    "message.mcaromanticexpansion.pregnancy_start_partner",
                    femalePlayer.getName().getString()));
        } else {
            MCARomanticExpansion.LOGGER.info("Pregnancy not triggered this time");
        }
    }

    /**
     * 直接从 NBT 读取性别 - 绕过 MCA 的 getGender() Bug
     * 优先读取大写 "Gender" 字段（Genetics实际保存的位置）
     */
    private static Gender getGenderFromNBT(ServerPlayer player) {
        try {
            PlayerSaveData data = PlayerSaveData.get(player);
            CompoundTag entityData = data.getEntityData();

            // 确保数据已初始化
            if (!data.isEntityDataSet()) {
                MCARomanticExpansion.LOGGER.info("Initializing MCA data for {}", player.getName().getString());
                data.setEntityDataSet(true);
                setGenderData(player, Gender.MALE); // 默认男性
                return Gender.MALE;
            }

            // 1. 优先读取大写 Gender 字段（Genetics 保存的位置）
            if (entityData.contains("Gender")) {
                int genderId = entityData.getInt("Gender");
                Gender gender = Gender.byId(genderId);
                if (gender != Gender.UNASSIGNED) {
                    MCARomanticExpansion.LOGGER.info("Gender from 'Gender' field for {}: {}",
                            player.getName().getString(), gender);
                    return gender;
                }
            }

            // 2. 从 Genetics 标签读取
            if (entityData.contains("Genetics")) {
                CompoundTag genetics = entityData.getCompound("Genetics");
                if (genetics.contains("Gender")) {
                    int genderId = genetics.getInt("Gender");
                    Gender gender = Gender.byId(genderId);
                    if (gender != Gender.UNASSIGNED) {
                        MCARomanticExpansion.LOGGER.info("Gender from Genetics.Gender for {}: {}",
                                player.getName().getString(), gender);
                        return gender;
                    }
                }
            }

            // 3. 后备：尝试小写 gender（MCA的getGender()读取的位置）
            if (entityData.contains("gender")) {
                int genderId = entityData.getInt("gender");
                Gender gender = Gender.byId(genderId);
                if (gender != Gender.UNASSIGNED) {
                    MCARomanticExpansion.LOGGER.info("Gender from 'gender' field for {}: {}",
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

    public static void attemptPregnancy(ServerPlayer player, PregnancyManager.PregnancyData data) {
        ServerPlayer partner = player.getServer().getPlayerList().getPlayer(data.getPartnerUUID());

        if (partner == null) {
            MCARomanticExpansion.LOGGER.warn("Partner not found for player {}", player.getName().getString());
            return;
        }

        MCARomanticExpansion.LOGGER.info("Attempting pregnancy for player {} with partner {}",
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

            MCARomanticExpansion.LOGGER.info("Pregnancy successful for player {}", player.getName().getString());
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.error("Failed to trigger procreation: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}