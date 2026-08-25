//package com.xiaoshi2022.mcaromanticexpansion.content.block;
//
//import com.mojang.serialization.MapCodec;
//import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.world.Containers;
//import net.minecraft.world.InteractionHand;
//import net.minecraft.world.InteractionResult;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.context.BlockPlaceContext;
//import net.minecraft.world.level.BlockGetter;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.LevelAccessor;
//import net.minecraft.world.level.LevelReader;
//import net.minecraft.world.level.block.BaseEntityBlock;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.RenderShape;
//import net.minecraft.world.level.block.Rotation;
//import net.minecraft.world.level.block.Mirror;
//import net.minecraft.world.level.block.SimpleWaterloggedBlock;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.entity.BlockEntityTicker;
//import net.minecraft.world.level.block.entity.BlockEntityType;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.block.state.StateDefinition;
//import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//import net.minecraft.world.level.block.state.properties.BooleanProperty;
//import net.minecraft.world.level.block.state.properties.EnumProperty;
//import net.minecraft.world.level.gameevent.GameEvent;
//import net.minecraft.world.level.material.FluidState;
//import net.minecraft.world.level.material.Fluids;
//import net.minecraft.world.phys.BlockHitResult;
//import net.minecraft.world.phys.shapes.CollisionContext;
//import net.minecraft.world.phys.shapes.Shapes;
//import net.minecraft.world.phys.shapes.VoxelShape;
//import net.minecraft.util.RandomSource;
//import net.minecraft.world.level.ScheduledTickAccess;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Objects;
//
//public class UmbrellaStandBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
//    public static final MapCodec<UmbrellaStandBlock> CODEC = simpleCodec(UmbrellaStandBlock::new);
//
//    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
//    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
//
//    // 基础碰撞箱（朝向 NORTH）
//    private static final VoxelShape BASE_SHAPE = Shapes.or(
//            Block.box(12, 0, 0, 14, 16, 2),
//            Block.box(12, 0, 14, 14, 16, 16),
//            Block.box(12, 10, 2, 14, 14, 14),
//            Block.box(9, 0, 2, 14, 4, 14)
//    );
//
//    private static final VoxelShape SHAPE_NORTH = BASE_SHAPE;
//    private static final VoxelShape SHAPE_SOUTH = rotateY180(BASE_SHAPE);
//    private static final VoxelShape SHAPE_WEST = rotateY90(BASE_SHAPE);
//    private static final VoxelShape SHAPE_EAST = rotateY270(BASE_SHAPE);
//
//    public UmbrellaStandBlock(Properties properties) {
//        super(properties);
//        // 不需要在这里调用 registerDefaultState，因为可能还没注册完成
//        // 但在 createBlockStateDefinition 中定义属性
//    }
//
//    @Override
//    protected MapCodec<? extends BaseEntityBlock> codec() {
//        return CODEC;
//    }
//
//    @Override
//    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
//        builder.add(WATERLOGGED, FACING);
////        // 设置默认状态
////        this.registerDefaultState(this.stateDefinition.any()
////                .setValue(WATERLOGGED, false)
////                .setValue(FACING, Direction.NORTH)
////        );
//    }
//
//    // 右键空手取伞
//    @Override
//    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
//        if (!(level.getBlockEntity(pos) instanceof UmbrellaStandBlockEntity blockEntity)) {
//            return InteractionResult.PASS;
//        }
//
//        if (!blockEntity.hasStack()) {
//            return InteractionResult.PASS;
//        }
//
//        if (!level.isClientSide()) {
//            ItemStack stack = blockEntity.removeTheItem();
//            if (!stack.isEmpty()) {
//                if (!player.addItem(stack)) {
//                    player.drop(stack, false);
//                }
//                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
//            }
//        }
//
//        return InteractionResult.SUCCESS;
//    }
//
//    // 手持物品右键放伞
//    @Override
//    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
//                                          Player player, InteractionHand hand, BlockHitResult hit) {
//        if (!(level.getBlockEntity(pos) instanceof UmbrellaStandBlockEntity blockEntity)) {
//            return InteractionResult.PASS;
//        }
//
//        if (blockEntity.hasStack()) {
//            return InteractionResult.PASS;
//        }
//
//        if (!stack.is(ModItems.UMBRELLA.get())) {
//            return InteractionResult.PASS;
//        }
//
//        if (level.getBlockState(pos.above()).isAir() && level.getBlockState(pos.above(2)).isAir()) {
//            if (!level.isClientSide()) {
//                blockEntity.setTheItem(stack.copyWithCount(1));
//                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
//
//                if (!player.isCreative()) {
//                    stack.shrink(1);
//                }
//            }
//            return InteractionResult.SUCCESS;
//        }
//        return InteractionResult.FAIL;
//    }
//
//    // 使用 onDestroyedByPlayer 方法（NeoForge 26.2）
//    @Override
//    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
//        if (!level.isClientSide()) {
//            if (level.getBlockEntity(pos) instanceof UmbrellaStandBlockEntity be) {
//                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.getTheItem());
//            }
//        }
//        return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
//    }
//
//    @Override
//    protected boolean hasAnalogOutputSignal(BlockState state) {
//        return true;
//    }
//
//    @Override
//    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
//        return level.getBlockEntity(pos) instanceof UmbrellaStandBlockEntity blockEntity ?
//                blockEntity.getComparatorOutput() : 0;
//    }
//
//    @Nullable
//    @Override
//    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
//        return new UmbrellaStandBlockEntity(pos, state);
//    }
//
//    @Nullable
//    @Override
//    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
//        return null;
//    }
//
//    @Override
//    public RenderShape getRenderShape(BlockState state) {
//        return RenderShape.MODEL;
//    }
//
//    @Override
//    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
//        Direction facing = state.getValue(FACING);
//        return switch (facing) {
//            case NORTH -> SHAPE_NORTH;
//            case SOUTH -> SHAPE_SOUTH;
//            case WEST -> SHAPE_WEST;
//            case EAST -> SHAPE_EAST;
//            default -> SHAPE_NORTH;
//        };
//    }
//
//    @Nullable
//    @Override
//    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
//        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
//        // 修复：使用 getOpposite() 确保朝向玩家
//        return this.defaultBlockState()
//                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER)
//                .setValue(FACING, ctx.getHorizontalDirection().getOpposite());
//    }
//
//    @Override
//    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos,
//                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
//        if (state.getValue(WATERLOGGED)) {
//            tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
//        }
//        return super.updateShape(state, level, tickAccess, pos, direction, neighborPos, neighborState, random);
//    }
//
//    @Override
//    protected FluidState getFluidState(BlockState state) {
//        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
//    }
//
//    @Override
//    protected BlockState rotate(BlockState state, Rotation rotation) {
//        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
//    }
//
//    @Override
//    protected BlockState mirror(BlockState state, Mirror mirror) {
//        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
//    }
//
//    // ==================== 碰撞箱旋转辅助方法 ====================
//
//    private static VoxelShape rotateY180(VoxelShape shape) {
//        VoxelShape[] buffer = new VoxelShape[]{Shapes.empty()};
//        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
//            buffer[0] = Shapes.or(buffer[0], Shapes.box(
//                    1 - maxX, minY, 1 - maxZ,
//                    1 - minX, maxY, 1 - minZ
//            ));
//        });
//        return buffer[0];
//    }
//
//    private static VoxelShape rotateY90(VoxelShape shape) {
//        VoxelShape[] buffer = new VoxelShape[]{Shapes.empty()};
//        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
//            buffer[0] = Shapes.or(buffer[0], Shapes.box(
//                    minZ, minY, 1 - maxX,
//                    maxZ, maxY, 1 - minX
//            ));
//        });
//        return buffer[0];
//    }
//
//    private static VoxelShape rotateY270(VoxelShape shape) {
//        VoxelShape[] buffer = new VoxelShape[]{Shapes.empty()};
//        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
//            buffer[0] = Shapes.or(buffer[0], Shapes.box(
//                    1 - maxZ, minY, minX,
//                    1 - minZ, maxY, maxX
//            ));
//        });
//        return buffer[0];
//    }
//}