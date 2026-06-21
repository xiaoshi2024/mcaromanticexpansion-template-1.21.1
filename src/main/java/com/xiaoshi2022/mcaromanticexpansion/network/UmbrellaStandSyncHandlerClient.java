package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.content.block.UmbrellaStandBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UmbrellaStandSyncHandlerClient {
    @OnlyIn(Dist.CLIENT)
    public static void handle(final UmbrellaStandSyncPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            MCARomanticExpansion.LOGGER.warn("Level is null, cannot sync umbrella stand");
            return;
        }

        BlockPos pos = packet.pos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof UmbrellaStandBlockEntity umbrellaStand) {
            ItemStack stack = packet.stack();
            MCARomanticExpansion.LOGGER.debug("Client received sync: pos={}, hasStack={}",
                    pos, !stack.isEmpty());

            if (stack.isEmpty()) {
                umbrellaStand.removeTheItem();
            } else {
                umbrellaStand.setTheItem(stack);
            }

            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos),
                    Block.UPDATE_ALL | Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        } else {
            MCARomanticExpansion.LOGGER.warn("BlockEntity at {} is not UmbrellaStandBlockEntity", pos);
        }
    }
}
