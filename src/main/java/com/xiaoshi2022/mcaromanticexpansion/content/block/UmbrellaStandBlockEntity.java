//package com.xiaoshi2022.mcaromanticexpansion.content.block;
//
//import com.xiaoshi2022.mcaromanticexpansion.network.UmbrellaStandSyncPacket;
//import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlockEntities;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.HolderLookup;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.nbt.NbtOps;
//import net.minecraft.network.protocol.Packet;
//import net.minecraft.network.protocol.game.ClientGamePacketListener;
//import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.Container;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.ChunkPos;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.gameevent.GameEvent;
//import net.minecraft.world.level.storage.ValueInput;
//import net.minecraft.world.level.storage.ValueOutput;
//import net.minecraft.world.ticks.ContainerSingleItem;
//import net.neoforged.neoforge.network.PacketDistributor;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//public class UmbrellaStandBlockEntity extends BlockEntity implements ContainerSingleItem.BlockContainerSingleItem {
//    private ItemStack umbrellaStack = ItemStack.EMPTY;
//
//    public UmbrellaStandBlockEntity(BlockPos pos, BlockState state) {
//        super(ModBlockEntities.UMBRELLA_STAND_BLOCK_ENTITY.get(), pos, state);
//    }
//
//    @Override
//    protected void loadAdditional(ValueInput input) {
//        super.loadAdditional(input);
//        this.umbrellaStack = input.read("UmbrellaItem", ItemStack.CODEC)
//                .orElse(ItemStack.EMPTY);
//    }
//
//    @Override
//    protected void saveAdditional(ValueOutput output) {
//        super.saveAdditional(output);
//        if (!this.umbrellaStack.isEmpty()) {
//            output.store("UmbrellaItem", ItemStack.CODEC, this.umbrellaStack);
//        }
//    }
//
//    @Override
//    public @NotNull ItemStack getTheItem() {
//        return this.umbrellaStack;
//    }
//
//    @Override
//    public void setTheItem(ItemStack stack) {
//        this.umbrellaStack = stack.copy();
//        this.setChanged();
//        this.syncToClient();
//        System.out.println("setTheItem: set umbrella");
//    }
//
//    public boolean hasStack() {
//        return !this.umbrellaStack.isEmpty();
//    }
//
//    @Override
//    public @NotNull ItemStack removeTheItem() {
//        ItemStack stack = this.umbrellaStack.copy();
//        this.umbrellaStack = ItemStack.EMPTY;
//        this.setChanged();
//        this.syncToClient();
//        System.out.println("removeTheItem: removed umbrella");
//        return stack;
//    }
//
//    @Override
//    public @NotNull ItemStack splitTheItem(int count) {
//        ItemStack stack = ContainerSingleItem.BlockContainerSingleItem.super.splitTheItem(count);
//        syncToClient();
//        return stack;
//    }
//
//    private void syncToClient() {
//        Level level = this.getLevel();
//        if (level != null && !level.isClientSide()) {
//            this.setChanged();
//
//            if (level instanceof ServerLevel serverLevel) {
//                ChunkPos chunkPos = new ChunkPos(getBlockPos().getX() >> 4, getBlockPos().getZ() >> 4);
//                PacketDistributor.sendToPlayersTrackingChunk(
//                        serverLevel,
//                        chunkPos,
//                        new UmbrellaStandSyncPacket(getBlockPos(), this.umbrellaStack)
//                );
//            }
//
//            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
//                    Block.UPDATE_ALL | Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
//
//            level.gameEvent(GameEvent.BLOCK_CHANGE, getBlockPos(), GameEvent.Context.of(getBlockState()));
//        }
//    }
//
//    @Override
//    public int getMaxStackSize() {
//        return 1;
//    }
//
//    public int getComparatorOutput() {
//        return this.umbrellaStack.isEmpty() ? 0 : 15;
//    }
//
//    @Override
//    public boolean canPlaceItem(int slot, ItemStack stack) {
//        return stack.is(com.xiaoshi2022.mcaromanticexpansion.registry.ModItems.UMBRELLA.get())
//                && this.getItem(slot).isEmpty();
//    }
//
//    @Override
//    public boolean canTakeItem(Container hopperContainer, int slot, ItemStack stack) {
//        return hopperContainer.hasAnyMatching(ItemStack::isEmpty);
//    }
//
//    @Override
//    public @NotNull BlockEntity getContainerBlockEntity() {
//        return this;
//    }
//
//    @Nullable
//    @Override
//    public Packet<ClientGamePacketListener> getUpdatePacket() {
//        return ClientboundBlockEntityDataPacket.create(this);
//    }
//
//    @Override
//    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
//        CompoundTag tag = new CompoundTag();
//        if (!this.umbrellaStack.isEmpty()) {
//            // 使用 CODEC 编码到 NBT
//            CompoundTag itemTag = (CompoundTag) ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, this.umbrellaStack)
//                    .getOrThrow();
//            tag.put("UmbrellaItem", itemTag);
//        }
//        return tag;
//    }
//
//    @Override
//    public void handleUpdateTag(ValueInput input) {
//        // 从 ValueInput 中读取 UmbrellaItem
//        this.umbrellaStack = input.read("UmbrellaItem", ItemStack.CODEC)
//                .orElse(ItemStack.EMPTY);
//
//        Level level = this.getLevel();
//        if (level != null && level.isClientSide()) {
//            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
//                    Block.UPDATE_ALL | Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
//        }
//    }
//
//}