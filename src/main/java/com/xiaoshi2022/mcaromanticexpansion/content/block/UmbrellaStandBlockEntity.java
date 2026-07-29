package com.xiaoshi2022.mcaromanticexpansion.content.block;

import com.xiaoshi2022.mcaromanticexpansion.network.ModNetwork;
import com.xiaoshi2022.mcaromanticexpansion.network.UmbrellaStandSyncPacket;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UmbrellaStandBlockEntity extends BlockEntity implements Container {
    private ItemStack umbrellaStack = ItemStack.EMPTY;

    public UmbrellaStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UMBRELLA_STAND_BLOCK_ENTITY.get(), pos, state);
    }

    // 【修复】1.20.1 中使用 load 方法
    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        this.umbrellaStack = ItemStack.EMPTY;
        if (compoundTag.contains("UmbrellaItem")) {
            this.umbrellaStack = ItemStack.of(compoundTag.getCompound("UmbrellaItem"));
        }
    }

    // 【修复】1.20.1 中使用 saveAdditional 方法
    @Override
    public void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        if (!this.umbrellaStack.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            this.umbrellaStack.save(itemTag);
            compoundTag.put("UmbrellaItem", itemTag);
        }
    }

    public ItemStack getTheItem() {
        return this.umbrellaStack;
    }

    public void setTheItem(ItemStack stack) {
        this.umbrellaStack = stack.copy();
        this.setChanged();
        this.syncToClient();
    }

    public boolean hasStack() {
        return !this.umbrellaStack.isEmpty();
    }

    public ItemStack removeTheItem() {
        ItemStack stack = this.umbrellaStack.copy();
        this.umbrellaStack = ItemStack.EMPTY;
        this.setChanged();
        this.syncToClient();
        return stack;
    }

    private void syncToClient() {
        Level level = this.getLevel();
        if (level != null && !level.isClientSide()) {
            this.setChanged();

            if (level instanceof ServerLevel serverLevel) {
                UmbrellaStandSyncPacket packet = new UmbrellaStandSyncPacket(getBlockPos(), this.umbrellaStack);
                ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> serverLevel.getChunkAt(getBlockPos())), packet);
            }

            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
                    Block.UPDATE_ALL | Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);

            level.gameEvent(GameEvent.BLOCK_CHANGE, getBlockPos(), GameEvent.Context.of(getBlockState()));
        }
    }

    public int getComparatorOutput() {
        return this.umbrellaStack.isEmpty() ? 0 : 15;
    }

    // ========== Container 接口实现 ==========

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.umbrellaStack.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? this.umbrellaStack : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == 0 && !this.umbrellaStack.isEmpty()) {
            ItemStack result = this.umbrellaStack.split(amount);
            if (this.umbrellaStack.isEmpty()) {
                this.setChanged();
                this.syncToClient();
            }
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == 0) {
            ItemStack result = this.umbrellaStack;
            this.umbrellaStack = ItemStack.EMPTY;
            this.setChanged();
            this.syncToClient();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            this.umbrellaStack = stack.copy();
            this.setChanged();
            this.syncToClient();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.umbrellaStack = ItemStack.EMPTY;
        this.setChanged();
        this.syncToClient();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0
                && stack.is(com.xiaoshi2022.mcaromanticexpansion.registry.ModItems.UMBRELLA.get())
                && this.umbrellaStack.isEmpty();
    }

    public boolean canTakeItem(Container hopperContainer, int slot, ItemStack stack) {
        return hopperContainer.hasAnyMatching(ItemStack::isEmpty);
    }

    // ========== 网络同步 ==========

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // 【修复】getUpdateTag 不需要参数
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        return tag;
    }

    // 【修复】handleUpdateTag 只接受 CompoundTag
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
        Level level = this.getLevel();
        if (level != null && level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
                    Block.UPDATE_ALL | Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        }
    }
}