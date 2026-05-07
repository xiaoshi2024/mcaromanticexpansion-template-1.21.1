package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.PregnancyManager;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

public class PregnancyAttemptHandler {

    private static final Map<UUID, BlockPos> sleepingPlayers = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PregnancyManager.PregnancyData data = PregnancyManager.getPregnancyData(player.getUUID());
        if (data != null && data.isActive()) {
            long worldTime = player.serverLevel().getGameTime();
            int elapsedTicks = (int) (worldTime - data.getStartTime());

            if (elapsedTicks >= data.getDurationTicks()) {
                attemptPregnancy(player, data);
                PregnancyManager.removePregnancyPeriod(player.getUUID());
            }
        }

        checkSleepingStatus(player);
    }

    private static void checkSleepingStatus(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        if (player.isSleeping()) {
            BlockPos bedPos = player.getSleepingPos().orElse(null);
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

    private static void checkNearbySleepingPlayers(ServerPlayer player, BlockPos bedPos, ServerLevel level) {
        List<ServerPlayer> nearbySleepingPlayers = new ArrayList<>();

        for (Map.Entry<UUID, BlockPos> entry : sleepingPlayers.entrySet()) {
            if (entry.getKey().equals(player.getUUID())) {
                continue;
            }

            BlockPos otherBedPos = entry.getValue();
            if (areBedsAdjacent(bedPos, otherBedPos, level)) {
                ServerPlayer otherPlayer = level.getServer().getPlayerList().getPlayer(entry.getKey());
                if (otherPlayer != null && otherPlayer.isSleeping()) {
                    nearbySleepingPlayers.add(otherPlayer);
                }
            }
        }

        for (ServerPlayer otherPlayer : nearbySleepingPlayers) {
            checkPregnancyConditions(player, otherPlayer);
        }
    }

    private static boolean areBedsAdjacent(BlockPos pos1, BlockPos pos2, ServerLevel level) {
        int dx = Math.abs(pos1.getX() - pos2.getX());
        int dy = Math.abs(pos1.getY() - pos2.getY());
        int dz = Math.abs(pos1.getZ() - pos2.getZ());

        if (dx <= 1 && dy <= 1 && dz <= 1) {
            BlockState state1 = level.getBlockState(pos1);
            BlockState state2 = level.getBlockState(pos2);
            
            return state1.getBlock() instanceof BedBlock && state2.getBlock() instanceof BedBlock;
        }

        return false;
    }

    private static void checkPregnancyConditions(ServerPlayer player1, ServerPlayer player2) {
        MCARomanticExpansion.LOGGER.info("Checking pregnancy conditions for {} and {}", 
                player1.getName().getString(), player2.getName().getString());
        
        PlayerSaveData data1 = PlayerSaveData.get(player1);
        PlayerSaveData data2 = PlayerSaveData.get(player2);

        Gender gender1 = getOrDetermineGender(player1, data1);
        Gender gender2 = getOrDetermineGender(player2, data2);
        
        MCARomanticExpansion.LOGGER.info("Player {} gender: {}, Player {} gender: {}", 
                player1.getName().getString(), gender1, player2.getName().getString(), gender2);

        if (gender1 == gender2 || gender1 == Gender.UNASSIGNED || gender2 == Gender.UNASSIGNED) {
            if (gender1 == gender2) {
                MCARomanticExpansion.LOGGER.info("Same gender players ({} and {}), pregnancy check skipped", 
                        player1.getName().getString(), player2.getName().getString());
            } else {
                MCARomanticExpansion.LOGGER.info("One or both players have UNASSIGNED gender, pregnancy check skipped");
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

        double roll = player1.getRandom().nextDouble();
        MCARomanticExpansion.LOGGER.info("Random roll: {}, required: {}", roll, chance);
        
        if (roll < chance) {
            ServerPlayer femalePlayer = gender1 == Gender.FEMALE ? player1 : player2;
            ServerPlayer malePlayer = gender1 == Gender.MALE ? player1 : player2;
            
            MCARomanticExpansion.LOGGER.info("Pregnancy triggered! Female: {}, Male: {}", 
                    femalePlayer.getName().getString(), malePlayer.getName().getString());

            PregnancyManager.startPregnancyPeriod(femalePlayer, malePlayer, femalePlayer.serverLevel().getGameTime());
        } else {
            MCARomanticExpansion.LOGGER.info("Pregnancy not triggered this time");
        }
    }

    private static Gender getOrDetermineGender(ServerPlayer player, PlayerSaveData data) {
        Gender gender = data.getFamilyEntry().gender();
        
        if (gender == Gender.UNASSIGNED) {
            long uuidHash = player.getUUID().getLeastSignificantBits();
            gender = (uuidHash & 1) == 0 ? Gender.MALE : Gender.FEMALE;
            MCARomanticExpansion.LOGGER.info("Determined gender for player {} based on UUID hash: {}", 
                    player.getName().getString(), gender);
        }
        
        return gender;
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
            
            net.minecraft.world.item.ItemStack babyItem = (net.minecraft.world.item.ItemStack) 
                    createItemMethod.invoke(null, player, partner, player.getRandom().nextLong());

            if (!player.addItem(babyItem)) {
                player.drop(babyItem, false);
            }

            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.mcaromanticexpansion.pregnancy_success"));
            partner.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.mcaromanticexpansion.pregnancy_success_partner",
                    player.getName().getString()));

            MCARomanticExpansion.LOGGER.info("Pregnancy successful for player {}", player.getName().getString());
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.error("Failed to trigger procreation: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}