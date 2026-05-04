package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.RingNBTUtil;
import net.conczin.mca.item.WeddingRingItem;
import net.conczin.mca.registry.ItemsMCA;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public record MarriageResponsePacket(UUID partnerUUID, boolean confirmed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MarriageResponsePacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("marriage_response"));

    public static final StreamCodec<FriendlyByteBuf, MarriageResponsePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.partnerUUID());
                buf.writeBoolean(packet.confirmed());
            },
            buf -> new MarriageResponsePacket(buf.readUUID(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer receiver) {
        ServerPlayer partner = receiver.getServer().getPlayerList().getPlayer(partnerUUID);
        if (partner == null) return;

        if (!confirmed) {
            receiver.sendSystemMessage(Component.translatable("mcaromanticexpansion.marriage.declined"));
            return;
        }

        // 检查双方是否都有结婚戒指
        ItemStack receiverRing = findAndRemoveWeddingRing(receiver);
        ItemStack partnerRing = findAndRemoveWeddingRing(partner);

        if (receiverRing.isEmpty() || partnerRing.isEmpty()) {
            if (!receiverRing.isEmpty()) receiver.getInventory().add(receiverRing);
            if (!partnerRing.isEmpty()) partner.getInventory().add(partnerRing);
            receiver.sendSystemMessage(Component.translatable("mcaromanticexpansion.marriage.missing_ring"));
            return;
        }

        // 使用 MCA 的婚姻系统
        try {
            PlayerSaveData receiverData = PlayerSaveData.get(receiver);
            PlayerSaveData partnerData = PlayerSaveData.get(partner);

            receiverData.marry(partner);
            partnerData.marry(receiver);

            MCARomanticExpansion.LOGGER.info("Successfully married {} and {}",
                    receiver.getName().getString(), partner.getName().getString());

            // ========== 关键修复：使用 createWeddingRingWithPartner 方法 ==========
            // 创建带有对方名字的结婚戒指
            ItemStack customReceiverRing = createWeddingRingWithPartner(receiverRing, partner, true);
            ItemStack customPartnerRing = createWeddingRingWithPartner(partnerRing, receiver, false);

            // 添加定制戒指（而不是原始戒指）
            receiver.getInventory().add(customReceiverRing);
            partner.getInventory().add(customPartnerRing);

            MCARomanticExpansion.LOGGER.info("Added custom wedding rings to both players");

            receiver.sendSystemMessage(Component.translatable("mcaromanticexpansion.marriage.success", partner.getName()));
            partner.sendSystemMessage(Component.translatable("mcaromanticexpansion.marriage.success", receiver.getName()));

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.error("Failed to marry players", e);
            receiver.getInventory().add(receiverRing);
            partner.getInventory().add(partnerRing);
            receiver.sendSystemMessage(Component.translatable("mcaromanticexpansion.marriage.failed"));
        }
    }

    private ItemStack findAndRemoveWeddingRing(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof WeddingRingItem) {
                ItemStack copy = stack.copy();
                stack.shrink(1);
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    // 创建带有伴侣名字的结婚戒指
    private ItemStack createWeddingRingWithPartner(ItemStack originalRing, ServerPlayer partner, boolean isReceiver) {
        if (originalRing.isEmpty()) {
            originalRing = new ItemStack(ItemsMCA.WEDDING_RING);
        } else {
            originalRing = originalRing.copy();
            originalRing.setCount(1);
        }

        // 清除原有的自定义名称
        originalRing.remove(DataComponents.CUSTOM_NAME);

        // 设置伴侣信息
        ItemStack result = RingNBTUtil.setWeddingRingPartner(originalRing, partner, isReceiver);
        MCARomanticExpansion.LOGGER.info("Created wedding ring for: {} (isReceiver={})", partner.getName().getString(), isReceiver);
        return result;
    }
}