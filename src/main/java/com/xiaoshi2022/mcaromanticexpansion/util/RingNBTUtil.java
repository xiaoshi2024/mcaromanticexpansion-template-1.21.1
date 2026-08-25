package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

public class RingNBTUtil {

    private static final String TAG_PARTNER_UUID = "PartnerUUID";
    private static final String TAG_PARTNER_NAME = "PartnerName";
    private static final String TAG_RING_TYPE = "RingType";
    private static final String TAG_FROM_PLAYER = "FromPlayer";

    // 获取或创建 CustomData 组件
    private static CustomData getOrCreateCustomData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            customData = CustomData.EMPTY;
        }
        return customData;
    }

    // 设置 NBT 数据到 ItemStack
    private static void setCustomData(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // 为订婚戒指设置对方信息 - 修复: 使用 putString 存储 UUID
    public static ItemStack setEngagementRingTarget(ItemStack ring, Player target) {
        if (ring.isEmpty()) return ring;

        CustomData customData = getOrCreateCustomData(ring);
        CompoundTag tag = customData.copyTag();

        // 修复: putUUID 不存在，使用 putString 存储 UUID 字符串
        tag.putString(TAG_PARTNER_UUID, target.getUUID().toString());
        tag.putString(TAG_PARTNER_NAME, target.getName().getString());
        tag.putString(TAG_RING_TYPE, "engagement");
        tag.putString(TAG_FROM_PLAYER, target.getName().getString());

        setCustomData(ring, tag);

        // 自定义显示名称
        Component displayName = Component.translatable("item.mcaromanticexpansion.engagement_ring.target", target.getName().getString());
        ring.set(DataComponents.CUSTOM_NAME, displayName);

        return ring;
    }

    // 为结婚戒指设置对方信息 - 修复: 使用 putString 存储 UUID
    public static ItemStack setWeddingRingPartner(ItemStack ring, Player partner, boolean isReceiver) {
        if (ring.isEmpty()) {
            MCARomanticExpansion.LOGGER.warn("setWeddingRingPartner called with empty ring");
            return ring;
        }

        MCARomanticExpansion.LOGGER.debug("Setting wedding ring partner: {}, isReceiver={}", partner.getName().getString(), isReceiver);

        CustomData customData = getOrCreateCustomData(ring);
        CompoundTag tag = customData.copyTag();

        // 修复: putUUID 不存在，使用 putString 存储 UUID 字符串
        tag.putString(TAG_PARTNER_UUID, partner.getUUID().toString());
        tag.putString(TAG_PARTNER_NAME, partner.getName().getString());
        tag.putString(TAG_RING_TYPE, "wedding");
        tag.putString(TAG_FROM_PLAYER, partner.getName().getString());

        setCustomData(ring, tag);

        // 修复: getString 返回 Optional<String>，使用 orElse()
        String partnerName = tag.getString(TAG_PARTNER_NAME).orElse("Unknown");
        String partnerUUID = tag.getString(TAG_PARTNER_UUID).orElse("Unknown");
        MCARomanticExpansion.LOGGER.debug("NBT data saved: PartnerName={}, PartnerUUID={}",
                partnerName, partnerUUID);

        // 根据持有者身份设置不同显示名称
        Component displayName;
        if (isReceiver) {
            displayName = Component.translatable("item.mcaromanticexpansion.wedding_ring.given", partner.getName().getString());
        } else {
            displayName = Component.translatable("item.mcaromanticexpansion.wedding_ring.received", partner.getName().getString());
        }
        ring.set(DataComponents.CUSTOM_NAME, displayName);

        MCARomanticExpansion.LOGGER.debug("Set custom name: {}", displayName.getString());

        return ring;
    }

    // 获取戒指关联的玩家名字 - 修复: getString 返回 Optional<String>
    public static String getPartnerName(ItemStack ring) {
        CustomData customData = ring.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(TAG_PARTNER_NAME)) {
                return tag.getString(TAG_PARTNER_NAME).orElse(null);
            }
        }
        return null;
    }

    // 获取戒指关联的玩家 UUID - 修复: 使用 getString 读取 UUID 字符串
    public static UUID getPartnerUUID(ItemStack ring) {
        CustomData customData = ring.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(TAG_PARTNER_UUID)) {
                String uuidStr = tag.getString(TAG_PARTNER_UUID).orElse(null);
                if (uuidStr != null) {
                    try {
                        return UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        MCARomanticExpansion.LOGGER.warn("Invalid UUID string in ring: {}", uuidStr);
                    }
                }
            }
        }
        return null;
    }

    // 检查是否是指定给某人的戒指
    public static boolean isRingForPlayer(ItemStack ring, Player player) {
        UUID partnerUUID = getPartnerUUID(ring);
        return partnerUUID != null && partnerUUID.equals(player.getUUID());
    }

    // 清除自定义名称（重置为默认）
    public static void resetCustomName(ItemStack ring) {
        ring.remove(DataComponents.CUSTOM_NAME);
    }
}