package com.xiaoshi2022.mcaromanticexpansion.content.block;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.network.UmbrellaStandSyncPacket;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.ticks.ContainerSingleItem;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UmbrellaStandBlockEntity extends BlockEntity implements ContainerSingleItem.BlockContainerSingleItem {
    private ItemStack umbrellaStack = ItemStack.EMPTY;

    public UmbrellaStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UMBRELLA_STAND_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        this.umbrellaStack = ItemStack.EMPTY;
        if (compoundTag.contains("UmbrellaItem")) {
            this.umbrellaStack = ItemStack.parse(provider, compoundTag.getCompound("UmbrellaItem"))
                    .orElse(ItemStack.EMPTY);
        }
        System.out.println("loadAdditional: umbrellaStack = " + (this.umbrellaStack.isEmpty() ? "EMPTY" : "HAS ITEM"));
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        if (!this.umbrellaStack.isEmpty()) {
            compoundTag.put("UmbrellaItem", this.umbrellaStack.save(provider));
        }
        System.out.println("saveAdditional: umbrellaStack = " + (this.umbrellaStack.isEmpty() ? "EMPTY" : "HAS ITEM"));
    }

    @Override
    public @NotNull ItemStack getTheItem() {
        return this.umbrellaStack;
    }

    @Override
    public void setTheItem(ItemStack stack) {
        this.umbrellaStack = stack.copy();
        this.setChanged();
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
        this.syncToClient();
        System.out.println("removeTheItem: removed umbrella");
        return stack;
    }

    @Override
    public @NotNull ItemStack splitTheItem(int count) {
        ItemStack stack = ContainerSingleItem.BlockContainerSingleItem.super.splitTheItem(count);
        syncToClient();
        return stack;
    }

    private void syncToClient() {
        Level level = this.getLevel();
        if (level != null && !level.isClientSide()) {
            this.setChanged();

            // 发送自定义网络包给追踪这个区块的所有玩家
            if (level instanceof ServerLevel serverLevel) {
                ChunkPos chunkPos = new ChunkPos(getBlockPos());
                PacketDistributor.sendToPlayersTrackingChunk(
                        serverLevel,
                        chunkPos,
                        new UmbrellaStandSyncPacket(getBlockPos(), this.umbrellaStack)
                );
                System.out.println("syncToClient: sent custom packet to tracking players, hasStack = " + hasStack());
            }

            // 同时使用标准方块更新作为备份
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
                    Block.UPDATE_ALL | Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);

            level.gameEvent(GameEvent.BLOCK_CHANGE, getBlockPos(), GameEvent.Context.of(getBlockState()));
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
        return stack.is(com.xiaoshi2022.mcaromanticexpansion.registry.ModItems.UMBRELLA.get())
                && this.getItem(slot).isEmpty();
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
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, provider);
        System.out.println("getUpdateTag: " + (tag.contains("UmbrellaItem") ? "HAS ITEM" : "EMPTY"));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        System.out.println("handleUpdateTag called on client");
        this.loadAdditional(tag, provider);
        Level level = this.getLevel();
        if (level != null && level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
                    Block.UPDATE_ALL | Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        }
    }
}