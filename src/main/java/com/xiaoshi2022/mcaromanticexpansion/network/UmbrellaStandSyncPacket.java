package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.content.block.UmbrellaStandBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UmbrellaStandSyncPacket {
    private final BlockPos pos;
    private final ItemStack stack;

    public UmbrellaStandSyncPacket(BlockPos pos, ItemStack stack) {
        this.pos = pos;
        this.stack = stack;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ItemStack getStack() {
        return stack;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeItemStack(stack, false);
    }

    public static UmbrellaStandSyncPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ItemStack stack = buf.readItem();
        return new UmbrellaStandSyncPacket(pos, stack);
    }

    public static void handle(UmbrellaStandSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            handleOnClient(packet);
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(UmbrellaStandSyncPacket packet) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            if (level == null) return;

            if (level.getBlockEntity(packet.pos) instanceof UmbrellaStandBlockEntity blockEntity) {
                blockEntity.setTheItem(packet.stack);
                MCARomanticExpansion.LOGGER.debug("Client synced umbrella stand at {}: {}",
                        packet.pos, packet.stack.isEmpty() ? "EMPTY" : "HAS ITEM");
            }
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to handle UmbrellaStandSyncPacket: {}", e.getMessage());
        }
    }
}