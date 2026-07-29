package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import forge.net.mca.item.BouquetItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class BouquetResponsePacket {
    private final UUID giverUUID;
    private final boolean accepted;

    public BouquetResponsePacket(UUID giverUUID, boolean accepted) {
        this.giverUUID = giverUUID;
        this.accepted = accepted;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(giverUUID);
        buf.writeBoolean(accepted);
    }

    public static BouquetResponsePacket decode(FriendlyByteBuf buf) {
        UUID giverUUID = buf.readUUID();
        boolean accepted = buf.readBoolean();
        return new BouquetResponsePacket(giverUUID, accepted);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer receiver = context.getSender();
            if (receiver == null) return;

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
                // 【1.20.1 正确写法】使用 new ResourceLocation
                ResourceLocation bouquetId = new ResourceLocation("mca", "bouquet");
                Item bouquetItem = BuiltInRegistries.ITEM.get(bouquetId);

                if (bouquetItem != null) {
                    receiver.getInventory().add(new ItemStack(bouquetItem));
                } else {
                    MCARomanticExpansion.LOGGER.error("Failed to find bouquet item in registry!");
                    return;
                }

                MCARomanticExpansion.LOGGER.debug("Bouquet accepted! Adding affection for {} and {}",
                        giver.getName().getString(), receiver.getName().getString());

                AffectionManager.handleInteraction(AffectionManager.InteractionType.BOUQUET, giver, receiver);

                // 发送成功消息
                giver.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§a" + receiver.getName().getString() + " 接受了你的花束！")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
                receiver.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§a你接受了 " + giver.getName().getString() + " 的花束！")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
            }
        });
        context.setPacketHandled(true);
    }
}