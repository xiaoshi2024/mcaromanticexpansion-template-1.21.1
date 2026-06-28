package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.api.event.AffectionChangedEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AffectionManager {
    private static final String AFFECTION_TAG = "RomanticAffection";
    private static final String TARGET_UUID_TAG = "TargetUUID";
    private static final String AFFECTION_VALUE_TAG = "AffectionValue";
    private static final String LAST_INTERACTION_TAG = "LastInteractionTime";

    public static int getAffection(Player player, Player target) {
        // 如果是服务端玩家，直接从 NBT 读取
        if (player instanceof ServerPlayer) {
            CompoundTag persistentData = player.getPersistentData();
            int affection = getAffectionFromNBT(persistentData, target.getUUID());
//            MCARomanticExpansion.LOGGER.debug("Getting affection for {} -> {}: {} (from server NBT)",
//                    player.getName().getString(), target.getName().getString(), affection);
            return affection;
        }
        // 如果是客户端玩家，从缓存读取
        int affection = ClientCache.getAffection(player.getUUID(), target.getUUID());
//        MCARomanticExpansion.LOGGER.debug("Getting affection for {} -> {}: {} (from client cache)",
//                player.getName().getString(), target.getName().getString(), affection);
        return affection;
    }

    /**
     * 初始化玩家的好感度数据，确保从0开始
     */
    public static void initializeAffectionData(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        // 如果没有好感度列表，创建一个空列表
        if (!persistentData.contains(AFFECTION_TAG)) {
            persistentData.put(AFFECTION_TAG, new ListTag());
            MCARomanticExpansion.LOGGER.debug("Initialized empty affection data for {}", player.getName().getString());
        }
    }

    public static void addAffection(Player player, Player target, int amount) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(target instanceof ServerPlayer serverTarget)) {
            return;
        }
        CompoundTag persistentData = serverPlayer.getPersistentData();
        int current = getAffectionFromNBT(persistentData, target.getUUID());
        int newValue = current + amount;
        if (newValue < -100) newValue = -100;

        AffectionChangedEvent event = new AffectionChangedEvent(
                serverPlayer, serverTarget, current, newValue, AffectionChangedEvent.ChangeReason.ADD
        );
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            MCARomanticExpansion.LOGGER.debug("Affection change canceled by event for {} -> {}",
                    player.getName().getString(), target.getName().getString());
            return;
        }
        int finalValue = event.getNewValue();

        MCARomanticExpansion.LOGGER.debug("Affection change for {} -> {}: {} + {} = {}",
                player.getName().getString(), target.getName().getString(), current, amount, finalValue);

        setAffectionToNBT(persistentData, target.getUUID(), finalValue);

        sendAffectionSync(serverPlayer, target.getUUID(), finalValue);
    }

    public static void setAffection(Player player, Player target, int value) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(target instanceof ServerPlayer serverTarget)) {
            return;
        }
        CompoundTag persistentData = serverPlayer.getPersistentData();
        int current = getAffectionFromNBT(persistentData, target.getUUID());
        int clampedValue = Math.min(value, 100);

        AffectionChangedEvent event = new AffectionChangedEvent(
                serverPlayer, serverTarget, current, clampedValue, AffectionChangedEvent.ChangeReason.SET
        );
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return;
        }
        clampedValue = event.getNewValue();

        setAffectionToNBT(persistentData, target.getUUID(), clampedValue);
        
        sendAffectionSync(serverPlayer, target.getUUID(), clampedValue);
    }

    private static void sendAffectionSync(ServerPlayer player, UUID targetUUID, int affection) {
        try {
            Class<?> packetClass = Class.forName("com.xiaoshi2022.mcaromanticexpansion.network.AffectionSyncPacket");
            java.lang.reflect.Method sendMethod = packetClass.getMethod("sendToClient", ServerPlayer.class, UUID.class, int.class);
            sendMethod.invoke(null, player, targetUUID, affection);
        } catch (Exception e) {
            // 客户端环境下可能找不到类，忽略
        }
    }

    private static int getAffectionFromNBT(CompoundTag tag, UUID targetUUID) {
        ListTag affectionList = tag.getList(AFFECTION_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < affectionList.size(); i++) {
            CompoundTag entry = affectionList.getCompound(i);
            if (entry.getString(TARGET_UUID_TAG).equals(targetUUID.toString())) {
                int value = entry.getInt(AFFECTION_VALUE_TAG);
                MCARomanticExpansion.LOGGER.debug("Loaded affection from NBT: {} = {}", targetUUID, value);
                return value;
            }
        }
        MCARomanticExpansion.LOGGER.debug("No affection found for {}, returning 0", targetUUID);
        return 0;
    }

    private static void setAffectionToNBT(CompoundTag tag, UUID targetUUID, int value) {
        ListTag affectionList = tag.getList(AFFECTION_TAG, Tag.TAG_COMPOUND);
        
        boolean found = false;
        for (int i = 0; i < affectionList.size(); i++) {
            CompoundTag entry = affectionList.getCompound(i);
            if (entry.getString(TARGET_UUID_TAG).equals(targetUUID.toString())) {
                entry.putInt(AFFECTION_VALUE_TAG, value);
                entry.putLong(LAST_INTERACTION_TAG, System.currentTimeMillis());
                found = true;
                break;
            }
        }
        
        if (!found) {
            CompoundTag newEntry = new CompoundTag();
            newEntry.putString(TARGET_UUID_TAG, targetUUID.toString());
            newEntry.putInt(AFFECTION_VALUE_TAG, value);
            newEntry.putLong(LAST_INTERACTION_TAG, System.currentTimeMillis());
            affectionList.add(newEntry);
        }
        
        tag.put(AFFECTION_TAG, affectionList);
    }

    public static void handleInteraction(InteractionType type, Player player, Player target) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(target instanceof ServerPlayer serverTarget)) {
            return;
        }
        int amount = switch (type) {
            case GIFT -> 5;
            case BOUQUET -> 8;
            case SHARED_UMBRELLA -> 1;
            case KISS -> 15;
            case DANCE -> 10;
            case PROPOSAL_ACCEPT -> 20;
            case MARRIAGE -> 30;
            case HUG -> 6;
            case WHISPER -> 3;
        };
        CompoundTag persistentData = serverPlayer.getPersistentData();
        int current = getAffectionFromNBT(persistentData, target.getUUID());
        int newValue = current + amount;
        if (newValue < -100) newValue = -100;

        AffectionChangedEvent event = new AffectionChangedEvent(
                serverPlayer, serverTarget, current, newValue, AffectionChangedEvent.ChangeReason.INTERACTION
        );
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return;
        }
        int finalValue = event.getNewValue();
        setAffectionToNBT(persistentData, target.getUUID(), finalValue);
        sendAffectionSync(serverPlayer, target.getUUID(), finalValue);
    }

    public enum InteractionType {
        GIFT,
        BOUQUET,
        SHARED_UMBRELLA,
        KISS,
        DANCE,
        PROPOSAL_ACCEPT,
        MARRIAGE,
        HUG,
        WHISPER
    }

    // 客户端缓存系统
    public static class ClientCache {
        private static final Map<UUID, Map<UUID, Integer>> affectionCache = new HashMap<>();

        public static int getAffection(UUID playerUUID, UUID targetUUID) {
            return affectionCache.getOrDefault(playerUUID, new HashMap<>()).getOrDefault(targetUUID, 0);
        }

        public static void setAffection(UUID playerUUID, UUID targetUUID, int affection) {
            affectionCache.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(targetUUID, affection);
        }

        public static void clearCache(UUID playerUUID) {
            affectionCache.remove(playerUUID);
        }
    }
}
