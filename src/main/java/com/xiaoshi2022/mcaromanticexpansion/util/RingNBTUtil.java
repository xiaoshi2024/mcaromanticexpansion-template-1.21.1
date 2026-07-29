package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class RingNBTUtil {

    private static final String TAG_PARTNER_UUID = "PartnerUUID";
    private static final String TAG_PARTNER_NAME = "PartnerName";
    private static final String TAG_RING_TYPE = "RingType";
    private static final String TAG_FROM_PLAYER = "FromPlayer";

    // 获取或创建 NBT 数据
    private static CompoundTag getOrCreateTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            tag = new CompoundTag();
            stack.setTag(tag);
        }
        return tag;
    }

    // 获取 NBT 数据（不创建）
    private static CompoundTag getTag(ItemStack stack) {
        return stack.getTag();
    }

    // 为订婚戒指设置对方信息
    public static ItemStack setEngagementRingTarget(ItemStack ring, Player target) {
        if (ring.isEmpty()) return ring;

        CompoundTag tag = getOrCreateTag(ring);
        tag.putUUID(TAG_PARTNER_UUID, target.getUUID());
        tag.putString(TAG_PARTNER_NAME, target.getName().getString());
        tag.putString(TAG_RING_TYPE, "engagement");
        tag.putString(TAG_FROM_PLAYER, target.getName().getString());

        // 自定义显示名称
        Component displayName = Component.translatable("item.mcaromanticexpansion.engagement_ring.target", target.getName().getString());
        ring.setHoverName(displayName);

        return ring;
    }

    // 为结婚戒指设置对方信息
    public static ItemStack setWeddingRingPartner(ItemStack ring, Player partner, boolean isReceiver) {
        if (ring.isEmpty()) {
            MCARomanticExpansion.LOGGER.warn("setWeddingRingPartner called with empty ring");
            return ring;
        }

        MCARomanticExpansion.LOGGER.debug("Setting wedding ring partner: {}, isReceiver={}", partner.getName().getString(), isReceiver);

        CompoundTag tag = getOrCreateTag(ring);
        tag.putUUID(TAG_PARTNER_UUID, partner.getUUID());
        tag.putString(TAG_PARTNER_NAME, partner.getName().getString());
        tag.putString(TAG_RING_TYPE, "wedding");
        tag.putString(TAG_FROM_PLAYER, partner.getName().getString());

        MCARomanticExpansion.LOGGER.debug("NBT data saved: PartnerName={}, PartnerUUID={}",
                tag.getString(TAG_PARTNER_NAME), tag.getUUID(TAG_PARTNER_UUID));

        // 根据持有者身份设置不同显示名称
        Component displayName;
        if (isReceiver) {
            displayName = Component.translatable("item.mcaromanticexpansion.wedding_ring.given", partner.getName().getString());
        } else {
            displayName = Component.translatable("item.mcaromanticexpansion.wedding_ring.received", partner.getName().getString());
        }
        ring.setHoverName(displayName);

        MCARomanticExpansion.LOGGER.debug("Set custom name: {}", displayName.getString());

        return ring;
    }

    // 获取戒指关联的玩家名字
    public static String getPartnerName(ItemStack ring) {
        CompoundTag tag = getTag(ring);
        if (tag != null && tag.contains(TAG_PARTNER_NAME)) {
            return tag.getString(TAG_PARTNER_NAME);
        }
        return null;
    }

    // 获取戒指关联的玩家 UUID
    public static UUID getPartnerUUID(ItemStack ring) {
        CompoundTag tag = getTag(ring);
        if (tag != null && tag.hasUUID(TAG_PARTNER_UUID)) {
            return tag.getUUID(TAG_PARTNER_UUID);
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
        ring.resetHoverName();
    }

    // 获取戒指类型
    public static String getRingType(ItemStack ring) {
        CompoundTag tag = getTag(ring);
        if (tag != null && tag.contains(TAG_RING_TYPE)) {
            return tag.getString(TAG_RING_TYPE);
        }
        return null;
    }

    // 检查戒指是否已绑定
    public static boolean isRingBound(ItemStack ring) {
        CompoundTag tag = getTag(ring);
        return tag != null && tag.contains(TAG_PARTNER_UUID);
    }

    // 清除戒指绑定信息
    public static void clearRingBinding(ItemStack ring) {
        CompoundTag tag = getTag(ring);
        if (tag != null) {
            tag.remove(TAG_PARTNER_UUID);
            tag.remove(TAG_PARTNER_NAME);
            tag.remove(TAG_RING_TYPE);
            tag.remove(TAG_FROM_PLAYER);
            resetCustomName(ring);
        }
    }
}