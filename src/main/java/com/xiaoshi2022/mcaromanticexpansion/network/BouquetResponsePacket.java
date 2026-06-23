package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import net.conczin.mca.item.BouquetItem;
import net.conczin.mca.registry.ItemsMCA;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public record BouquetResponsePacket(UUID giverUUID, boolean accepted) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BouquetResponsePacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("bouquet_response"));

    public static final StreamCodec<FriendlyByteBuf, BouquetResponsePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.giverUUID());
                buf.writeBoolean(packet.accepted());
            },
            buf -> new BouquetResponsePacket(buf.readUUID(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer receiver) {
        if (!accepted) return;

        ServerPlayer giver = receiver.getServer().getPlayerList().getPlayer(giverUUID);
        if (giver == null) return;

        // 从赠送者背包中找到花束并移除
        boolean found = false;
        for (int i = 0; i < giver.getInventory().getContainerSize(); i++) {
            ItemStack stack = giver.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BouquetItem) {
                stack.shrink(1);
                found = true;
                break;
            }
        }

        // 如果找到并移除了花束，给受礼者添加一个花束
        if (found) {
            receiver.getInventory().add(new ItemStack(ItemsMCA.BOUQUET));
            
            // 【关键修复】增加双方好感度（只调用一次，双方都会增加）
            MCARomanticExpansion.LOGGER.info("Bouquet accepted! Adding affection for {} and {}",
                    giver.getName().getString(), receiver.getName().getString());
            // 送花只增加一次好感度（赠送者对受礼者的好感）
            AffectionManager.handleInteraction(AffectionManager.InteractionType.BOUQUET, giver, receiver);
            
            // 发送成功消息
            giver.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§a" + receiver.getName().getString() + " 接受了你的花束！")
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
            receiver.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§a你接受了 " + giver.getName().getString() + " 的花束！")
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
        }
    }
}