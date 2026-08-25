package com.xiaoshi2022.mcaromanticexpansion.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.time.LocalDate;
import java.util.Optional;

public class PlayerBirthdayData {
    private static final String KEY_BIRTHDAY_MONTH = "birthday_month";
    private static final String KEY_BIRTHDAY_DAY = "birthday_day";

    public static void setBirthday(Player player, int month, int day) {
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            return;
        }

        CompoundTag tag = getOrCreateTag(player);
        tag.putInt(KEY_BIRTHDAY_MONTH, month);
        tag.putInt(KEY_BIRTHDAY_DAY, day);
        setTag(player, tag);
    }

    public static Optional<LocalDate> getBirthday(Player player) {
        CompoundTag tag = getTag(player);
        // 修复: contains 直接检查
        if (tag.contains(KEY_BIRTHDAY_MONTH) && tag.contains(KEY_BIRTHDAY_DAY)) {
            // 修复: getInt 返回 Optional<Integer>，使用 orElse(0)
            int month = tag.getInt(KEY_BIRTHDAY_MONTH).orElse(0);
            int day = tag.getInt(KEY_BIRTHDAY_DAY).orElse(0);
            if (month > 0 && day > 0) {
                return Optional.of(LocalDate.of(2000, month, day));
            }
        }
        return Optional.empty();
    }

    public static boolean isBirthdayToday(Player player) {
        Optional<LocalDate> birthday = getBirthday(player);
        if (birthday.isEmpty()) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return birthday.get().getMonthValue() == today.getMonthValue()
                && birthday.get().getDayOfMonth() == today.getDayOfMonth();
    }

    // 修复: getCompound 返回 Optional<CompoundTag>
    private static CompoundTag getTag(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag persistentData = serverPlayer.getPersistentData();
            return persistentData.getCompound("mcaromanticexpansion").orElse(new CompoundTag());
        }
        return new CompoundTag();
    }

    // 修复: getCompound 返回 Optional<CompoundTag>
    private static CompoundTag getOrCreateTag(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag persistentData = serverPlayer.getPersistentData();
            if (!persistentData.contains("mcaromanticexpansion")) {
                persistentData.put("mcaromanticexpansion", new CompoundTag());
            }
            return persistentData.getCompound("mcaromanticexpansion").orElse(new CompoundTag());
        }
        return new CompoundTag();
    }

    private static void setTag(Player player, CompoundTag tag) {
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag persistentData = serverPlayer.getPersistentData();
            persistentData.put("mcaromanticexpansion", tag);
        }
    }
}