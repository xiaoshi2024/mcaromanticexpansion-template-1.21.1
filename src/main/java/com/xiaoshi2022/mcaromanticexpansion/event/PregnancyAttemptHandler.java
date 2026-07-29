package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.PregnancyManager;
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

    // Gender 类需要根据实际 MCA 模组路径调整
    // 可能的路径: forge.net.mca.entity.ai.relationship.Gender
    // 或者 net.conczin.mca.entity.ai.relationship.Gender

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务端执行
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
                        Component.translatable("message.mcaromanticexpansion.pregnancy_success_partner", serverPlayer.getName().getString()), false);
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

    private static void fixGenderData(ServerPlayer player) {
        try {
            // 使用反射获取 PlayerSaveData，避免硬依赖
            Class<?> playerSaveDataClass = Class.forName("net.conczin.mca.server.world.data.PlayerSaveData");
            java.lang.reflect.Method getMethod = playerSaveDataClass.getMethod("get", ServerPlayer.class);
            Object data = getMethod.invoke(null, player);

            java.lang.reflect.Method getEntityDataMethod = playerSaveDataClass.getMethod("getEntityData");
            CompoundTag entityData = (CompoundTag) getEntityDataMethod.invoke(data);

            java.lang.reflect.Method isEntityDataSetMethod = playerSaveDataClass.getMethod("isEntityDataSet");
            boolean isSet = (boolean) isEntityDataSetMethod.invoke(data);

            if (!isSet) {
                java.lang.reflect.Method setEntityDataSetMethod = playerSaveDataClass.getMethod("setEntityDataSet", boolean.class);
                setEntityDataSetMethod.invoke(data, true);
                setGenderData(player, getGenderMale());
                return;
            }

            if (entityData.contains("Gender")) {
                int genderId = entityData.getInt("Gender");
                Object gender = getGenderById(genderId);
                if (gender != null && !gender.toString().equals("UNASSIGNED")) {
                    if (!entityData.contains("gender") || entityData.getInt("gender") != genderId) {
                        entityData.putInt("gender", genderId);
                        java.lang.reflect.Method setDirtyMethod = playerSaveDataClass.getMethod("setDirty");
                        setDirtyMethod.invoke(data);
                    }
                    return;
                }
            }

            if (!entityData.contains("gender") && !entityData.contains("Gender")) {
                setGenderData(player, getGenderMale());
            }

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to fix gender data for {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    private static Object getGenderById(int id) {
        try {
            Class<?> genderClass = Class.forName("net.conczin.mca.entity.ai.relationship.Gender");
            java.lang.reflect.Method byIdMethod = genderClass.getMethod("byId", int.class);
            return byIdMethod.invoke(null, id);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getGenderMale() {
        try {
            Class<?> genderClass = Class.forName("net.conczin.mca.entity.ai.relationship.Gender");
            java.lang.reflect.Field maleField = genderClass.getField("MALE");
            return maleField.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getGenderFemale() {
        try {
            Class<?> genderClass = Class.forName("net.conczin.mca.entity.ai.relationship.Gender");
            java.lang.reflect.Field femaleField = genderClass.getField("FEMALE");
            return femaleField.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static void setGenderData(ServerPlayer player, Object gender) {
        synchronized (genderCheckLock) {
            UUID playerId = player.getUUID();
            genderChangingPlayers.put(playerId, System.currentTimeMillis());

            try {
                Class<?> playerSaveDataClass = Class.forName("net.conczin.mca.server.world.data.PlayerSaveData");
                java.lang.reflect.Method getMethod = playerSaveDataClass.getMethod("get", ServerPlayer.class);
                Object data = getMethod.invoke(null, player);

                java.lang.reflect.Method getEntityDataMethod = playerSaveDataClass.getMethod("getEntityData");
                CompoundTag entityData = (CompoundTag) getEntityDataMethod.invoke(data);

                java.lang.reflect.Method getIdMethod = gender.getClass().getMethod("getId");
                int genderId = (int) getIdMethod.invoke(gender);

                entityData.putInt("gender", genderId);
                entityData.putInt("Gender", genderId);

                CompoundTag genetics;
                if (entityData.contains("Genetics")) {
                    genetics = entityData.getCompound("Genetics");
                } else {
                    genetics = new CompoundTag();
                    entityData.put("Genetics", genetics);
                }
                genetics.putInt("Gender", genderId);
                genetics.putInt("gender", genderId);

                java.lang.reflect.Method setEntityDataSetMethod = playerSaveDataClass.getMethod("setEntityDataSet", boolean.class);
                setEntityDataSetMethod.invoke(data, true);

                java.lang.reflect.Method setDirtyMethod = playerSaveDataClass.getMethod("setDirty");
                setDirtyMethod.invoke(data);

                player.sendSystemMessage(Component.literal("§a[MCA] 你的性别已设置成功"));

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

    private static void checkNearbySleepingPlayers(ServerPlayer player, BlockPos bedPos, net.minecraft.server.level.ServerLevel level) {
        UUID playerId = player.getUUID();

        for (UUID otherPlayerId : sleepingPlayers.keySet()) {
            if (otherPlayerId.equals(playerId)) continue;

            BlockPos otherBedPos = sleepingPlayers.get(otherPlayerId);
            if (otherBedPos == null) continue;

            ServerPlayer otherPlayer = level.getServer().getPlayerList().getPlayer(otherPlayerId);
            if (otherPlayer == null || !otherPlayer.isSleeping()) continue;

            if (areBedsAdjacent(bedPos, otherBedPos)) {
                synchronized (genderCheckLock) {
                    if (!player.isSleeping() || !otherPlayer.isSleeping()) {
                        continue;
                    }
                    if (isGenderChanging(player) || isGenderChanging(otherPlayer)) {
                        continue;
                    }
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

        Object gender1 = getGenderFromNBT(player1);
        Object gender2 = getGenderFromNBT(player2);

        if (gender1 == null || gender2 == null) return;

        // 使用字符串比较判断性别
        String g1 = gender1.toString();
        String g2 = gender2.toString();

        if (g1.equals(g2) || g1.equals("UNASSIGNED") || g2.equals("UNASSIGNED")) {
            if (g1.equals(g2)) {
                MCARomanticExpansion.LOGGER.debug("Same gender players, pregnancy check skipped");
            } else {
                MCARomanticExpansion.LOGGER.debug("UNASSIGNED gender, pregnancy check skipped");
                if (g1.equals("UNASSIGNED")) {
                    player1.sendSystemMessage(Component.literal("§c请使用 /mca editor 打开编辑器设置性别！")
                            .withStyle(ChatFormatting.RED));
                }
                if (g2.equals("UNASSIGNED")) {
                    player2.sendSystemMessage(Component.literal("§c请使用 /mca editor 打开编辑器设置性别！")
                            .withStyle(ChatFormatting.RED));
                }
            }
            return;
        }

        if (PregnancyManager.isPlayerInPregnancyPeriod(player1.getUUID()) ||
                PregnancyManager.isPlayerInPregnancyPeriod(player2.getUUID())) {
            return;
        }

        boolean player1Satiated = PregnancyManager.isFullSatiated(player1);
        boolean player2Satiated = PregnancyManager.isFullSatiated(player2);

        if (!player1Satiated || !player2Satiated) {
            return;
        }

        double chance = PregnancyManager.calculatePregnancyChance(player1, player2);
        double random = player1.getRandom().nextDouble();

        if (random < chance) {
            Object femalePlayer = g1.equals("FEMALE") ? player1 : player2;
            Object malePlayer = g1.equals("MALE") ? player1 : player2;

            if (femalePlayer instanceof ServerPlayer female && malePlayer instanceof ServerPlayer male) {
                PregnancyManager.startPregnancyPeriod(female, male, female.serverLevel().getGameTime());

                female.sendSystemMessage(Component.translatable(
                        "message.mcaromanticexpansion.pregnancy_start",
                        male.getName().getString()));
                male.sendSystemMessage(Component.translatable(
                        "message.mcaromanticexpansion.pregnancy_start_partner",
                        female.getName().getString()));
            }
        }
    }

    public static Object getGenderFromNBT(ServerPlayer player) {
        synchronized (genderCheckLock) {
            try {
                Class<?> playerSaveDataClass = Class.forName("net.conczin.mca.server.world.data.PlayerSaveData");
                java.lang.reflect.Method getMethod = playerSaveDataClass.getMethod("get", ServerPlayer.class);
                Object data = getMethod.invoke(null, player);

                java.lang.reflect.Method getEntityDataMethod = playerSaveDataClass.getMethod("getEntityData");
                CompoundTag entityData = (CompoundTag) getEntityDataMethod.invoke(data);

                java.lang.reflect.Method isEntityDataSetMethod = playerSaveDataClass.getMethod("isEntityDataSet");
                boolean isSet = (boolean) isEntityDataSetMethod.invoke(data);

                if (!isSet) {
                    java.lang.reflect.Method setEntityDataSetMethod = playerSaveDataClass.getMethod("setEntityDataSet", boolean.class);
                    setEntityDataSetMethod.invoke(data, true);
                    Object male = getGenderMale();
                    setGenderData(player, male);
                    return male;
                }

                if (entityData.contains("Gender")) {
                    int genderId = entityData.getInt("Gender");
                    Object gender = getGenderById(genderId);
                    if (gender != null && !gender.toString().equals("UNASSIGNED")) {
                        return gender;
                    }
                }

                if (entityData.contains("Genetics")) {
                    CompoundTag genetics = entityData.getCompound("Genetics");
                    if (genetics.contains("Gender")) {
                        int genderId = genetics.getInt("Gender");
                        Object gender = getGenderById(genderId);
                        if (gender != null && !gender.toString().equals("UNASSIGNED")) {
                            return gender;
                        }
                    }
                }

                if (entityData.contains("gender")) {
                    int genderId = entityData.getInt("gender");
                    Object gender = getGenderById(genderId);
                    if (gender != null && !gender.toString().equals("UNASSIGNED")) {
                        return gender;
                    }
                }

                Object male = getGenderMale();
                setGenderData(player, male);
                return male;

            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.error("Failed to get gender for {}: {}",
                        player.getName().getString(), e.getMessage());
                return null;
            }
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

            ItemStack babyItem = (ItemStack) createItemMethod.invoke(null, player, partner, player.getRandom().nextLong());

            if (!player.addItem(babyItem)) {
                player.drop(babyItem, false);
            }

            player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.pregnancy_success"));
            partner.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.pregnancy_success_partner",
                    player.getName().getString()));

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.error("Failed to trigger procreation: {}", e.getMessage());
        }
    }

    public static ConcurrentHashMap<UUID, Long> getGenderChangingPlayers() {
        return genderChangingPlayers;
    }
}