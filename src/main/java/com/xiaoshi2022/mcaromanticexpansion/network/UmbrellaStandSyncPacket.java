package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record UmbrellaStandSyncPacket(BlockPos pos, ItemStack stack) implements CustomPacketPayload {

    public static final Type<UmbrellaStandSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "umbrella_stand_sync"));

    public static final StreamCodec<FriendlyByteBuf, UmbrellaStandSyncPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UmbrellaStandSyncPacket decode(FriendlyByteBuf buf) {
                    BlockPos pos = buf.readBlockPos();

                    // 读取是否有物品
                    boolean hasItem = buf.readBoolean();
                    ItemStack stack;

                    if (hasItem) {
                        // 读取物品 ID
                        String itemId = buf.readUtf();
                        int count = buf.readInt();

                        // 创建 ItemStack
                        var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId)).orElse(null);
                        if (item != null) {
                            stack = new ItemStack(item, count);
                        } else {
                            stack = ItemStack.EMPTY;
                        }
                    } else {
                        stack = ItemStack.EMPTY;
                    }

                    return new UmbrellaStandSyncPacket(pos, stack);
                }

                @Override
                public void encode(FriendlyByteBuf buf, UmbrellaStandSyncPacket packet) {
                    buf.writeBlockPos(packet.pos());

                    ItemStack stack = packet.stack();
                    boolean hasItem = !stack.isEmpty() && stack.getItem() != null;
                    buf.writeBoolean(hasItem);

                    if (hasItem) {
                        // 写入物品 ID
                        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                        buf.writeUtf(itemId.toString());
                        buf.writeInt(stack.getCount());
                    }
                }
            };

    @Override
    public @NotNull Type<UmbrellaStandSyncPacket> type() {
        return TYPE;
    }
}