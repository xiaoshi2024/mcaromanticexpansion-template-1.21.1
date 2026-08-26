package com.xiaoshi2022.mcaromanticexpansion.content.block;

import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
import com.xiaoshi2022.mcaromanticexpansion.network.UmbrellaStandSyncPacket;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.ticks.ContainerSingleItem;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UmbrellaStandBlockEntity extends BlockEntity implements ContainerSingleItem.BlockContainerSingleItem {
    private ItemStack umbrellaStack = ItemStack.EMPTY;
    private boolean updating = false;  // 防止循环

    public UmbrellaStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UMBRELLA_STAND_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.umbrellaStack = input.read("UmbrellaItem", ItemStack.CODEC)
                .orElse(ItemStack.EMPTY);

        // 加载完成后同步 BlockState
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            boolean hasUmbrella = !this.umbrellaStack.isEmpty();
            if (state.getValue(UmbrellaStandBlock.HAS_UMBRELLA) != hasUmbrella) {
                level.setBlock(worldPosition, state.setValue(UmbrellaStandBlock.HAS_UMBRELLA, hasUmbrella), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.umbrellaStack.isEmpty()) {
            output.store("UmbrellaItem", ItemStack.CODEC, this.umbrellaStack);
        }
    }

    @Override
    public @NotNull ItemStack getTheItem() {
        return this.umbrellaStack;
    }

    @Override
    public void setTheItem(ItemStack stack) {
        this.umbrellaStack = stack.copy();
        this.setChanged();
        // 更新 BlockState
        updateBlockState(true);
        this.syncToClient();
        System.out.println("setTheItem: set umbrella");
    }

    public boolean hasStack() {
        return !this.umbrellaStack.isEmpty();
    }

    @Override
    public @NotNull ItemStack removeTheItem() {
        ItemStack stack = this.umbrellaStack.copy();
        this.umbrellaStack = ItemStack.EMPTY;
        this.setChanged();
        // 更新 BlockState
        updateBlockState(false);
        this.syncToClient();
        System.out.println("removeTheItem: removed umbrella");
        return stack;
    }

    @Override
    public @NotNull ItemStack splitTheItem(int count) {
        ItemStack stack = ContainerSingleItem.BlockContainerSingleItem.super.splitTheItem(count);
        if (this.umbrellaStack.isEmpty()) {
            updateBlockState(false);
        }
        syncToClient();
        return stack;
    }

    /**
     * 更新 BlockState 的 HAS_UMBRELLA 属性
     */
    private void updateBlockState(boolean hasUmbrella) {
        if (updating) return;
        updating = true;
        try {
            Level level = this.getLevel();
            if (level != null && !level.isClientSide()) {
                BlockState state = level.getBlockState(worldPosition);
                if (state.getValue(UmbrellaStandBlock.HAS_UMBRELLA) != hasUmbrella) {
                    level.setBlock(worldPosition, state.setValue(UmbrellaStandBlock.HAS_UMBRELLA, hasUmbrella),
                            Block.UPDATE_ALL | Block.UPDATE_CLIENTS);
                }
            }
        } finally {
            updating = false;
        }
    }

    private void syncToClient() {
        if (updating) return;
        updating = true;
        try {
            Level level = this.getLevel();
            if (level != null && !level.isClientSide()) {
                this.setChanged();

                if (level instanceof ServerLevel serverLevel) {
                    ChunkPos chunkPos = new ChunkPos(getBlockPos().getX() >> 4, getBlockPos().getZ() >> 4);
                    PacketDistributor.sendToPlayersTrackingChunk(
                            serverLevel,
                            chunkPos,
                            new UmbrellaStandSyncPacket(getBlockPos(), this.umbrellaStack)
                    );
                }

                // 更新方块状态给客户端
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
                        Block.UPDATE_ALL | Block.UPDATE_CLIENTS);
            }
        } finally {
            updating = false;
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public int getComparatorOutput() {
        return this.umbrellaStack.isEmpty() ? 0 : 15;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return UmbrellaItem.isUmbrella(stack) && this.getItem(slot).isEmpty();
    }

    @Override
    public boolean canTakeItem(Container hopperContainer, int slot, ItemStack stack) {
        return hopperContainer.hasAnyMatching(ItemStack::isEmpty);
    }

    @Override
    public @NotNull BlockEntity getContainerBlockEntity() {
        return this;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!this.umbrellaStack.isEmpty()) {
            CompoundTag itemTag = (CompoundTag) ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, this.umbrellaStack)
                    .getOrThrow();
            tag.put("UmbrellaItem", itemTag);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        // 直接更新数据，不触发额外的同步
        this.umbrellaStack = input.read("UmbrellaItem", ItemStack.CODEC)
                .orElse(ItemStack.EMPTY);

        // 客户端同步 BlockState
        if (level != null && level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            boolean hasUmbrella = !this.umbrellaStack.isEmpty();
            if (state.getValue(UmbrellaStandBlock.HAS_UMBRELLA) != hasUmbrella) {
                level.setBlock(worldPosition, state.setValue(UmbrellaStandBlock.HAS_UMBRELLA, hasUmbrella),
                        Block.UPDATE_ALL | Block.UPDATE_CLIENTS);
            }
        }
    }
}