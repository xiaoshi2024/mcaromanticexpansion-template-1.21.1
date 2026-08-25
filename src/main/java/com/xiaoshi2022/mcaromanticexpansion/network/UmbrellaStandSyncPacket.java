package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;  // 使用 Identifier 替代 ResourceLocation
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record UmbrellaStandSyncPacket(BlockPos pos, ItemStack stack) implements CustomPacketPayload {

    public static final Type<UmbrellaStandSyncPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "umbrella_stand_sync"));

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

                        // 创建 ItemStack - 使用 Identifier.parse()
                        Identifier location = Identifier.parse(itemId);
                        // BuiltInRegistries.ITEM.get() 返回 Optional<Holder.Reference<Item>>
                        Optional<Holder.Reference<Item>> optionalItem = BuiltInRegistries.ITEM.get(location);
                        if (optionalItem.isPresent()) {
                            Item item = optionalItem.get().value();
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
                        // 写入物品 ID - 使用 Identifier
                        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
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