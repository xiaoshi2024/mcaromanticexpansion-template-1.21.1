package com.xiaoshi2022.mcaromanticexpansion.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AffectionManager {
    private static final String AFFECTION_TAG = "RomanticAffection";
    private static final String TARGET_UUID_TAG = "TargetUUID";
    private static final String AFFECTION_VALUE_TAG = "AffectionValue";
    private static final String LAST_INTERACTION_TAG = "LastInteractionTime";

    public static int getAffection(Player player, Player target) {
        CompoundTag persistentData = player.getPersistentData();
        return getAffectionFromNBT(persistentData, target.getUUID());
    }

    public static void addAffection(Player player, Player target, int amount) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        CompoundTag persistentData = serverPlayer.getPersistentData();
        int current = getAffectionFromNBT(persistentData, target.getUUID());
        int newValue = Math.min(current + amount, 100);
        setAffectionToNBT(persistentData, target.getUUID(), newValue);
    }

    public static void setAffection(Player player, Player target, int value) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        CompoundTag persistentData = serverPlayer.getPersistentData();
        setAffectionToNBT(persistentData, target.getUUID(), Math.min(value, 100));
    }

    private static int getAffectionFromNBT(CompoundTag tag, UUID targetUUID) {
        ListTag affectionList = tag.getList(AFFECTION_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < affectionList.size(); i++) {
            CompoundTag entry = affectionList.getCompound(i);
            if (entry.getString(TARGET_UUID_TAG).equals(targetUUID.toString())) {
                return entry.getInt(AFFECTION_VALUE_TAG);
            }
        }
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
        int amount = switch (type) {
            case GIFT -> 10;
            case BOUQUET -> 15;
            case SHARED_UMBRELLA -> 2;
            case KISS -> 20;
            case DANCE -> 12;
            case PROPOSAL_ACCEPT -> 30;
            case MARRIAGE -> 50;
            case HUG -> 8;
            case WHISPER -> 5;
        };
        addAffection(player, target, amount);
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
}
