package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.conczin.mca.item.WeddingRingItem;
import net.conczin.mca.registry.ItemsMCA;
import net.conczin.mca.server.world.data.PlayerSaveData;
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

        // 使用 MCA 的婚姻系统 - 注意 marry 方法需要传入 Player 而不是 PlayerSaveData
        try {
            PlayerSaveData receiverData = PlayerSaveData.get(receiver);
            PlayerSaveData partnerData = PlayerSaveData.get(partner);

            // 正确的调用方式：marry(PlayerSaveData) 或 marry(Player)
            // 根据错误信息，marry 应该接受 PlayerSaveData 参数
            receiverData.marry(partner);
            partnerData.marry(receiver);

            MCARomanticExpansion.LOGGER.info("Successfully married {} and {}",
                    receiver.getName().getString(), partner.getName().getString());

            // 交换戒指
            receiver.getInventory().add(partnerRing);
            partner.getInventory().add(receiverRing);

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
}