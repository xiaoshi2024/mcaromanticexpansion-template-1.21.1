package com.xiaoshi2022.mcaromanticexpansion.content.block;

import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class UmbrellaStandBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    // 【删除 CODEC - 不需要序列化】

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape BASE_SHAPE = Shapes.or(
            Block.box(12, 0, 0, 14, 16, 2),
            Block.box(12, 0, 14, 14, 16, 16),
            Block.box(12, 10, 2, 14, 14, 14),
            Block.box(9, 0, 2, 14, 4, 14)
    );

    private static final VoxelShape SHAPE_NORTH = BASE_SHAPE;
    private static final VoxelShape SHAPE_SOUTH = rotateY180(BASE_SHAPE);
    private static final VoxelShape SHAPE_WEST = rotateY90(BASE_SHAPE);
    private static final VoxelShape SHAPE_EAST = rotateY270(BASE_SHAPE);

    public UmbrellaStandBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(FACING, Direction.NORTH)
        );
    }

    // 【删除 codec() 方法 - 使用父类默认实现】

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, FACING);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof UmbrellaStandBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);

        // 手里空手：取出伞
        if (heldItem.isEmpty()) {
            if (!blockEntity.hasStack()) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                ItemStack stack = blockEntity.removeTheItem();
                if (!stack.isEmpty()) {
                    if (!player.addItem(stack)) {
                        player.drop(stack, false);
                    }
                    level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // 手里拿着伞：放置进去
        if (heldItem.is(ModItems.UMBRELLA.get())) {
            if (blockEntity.hasStack()) {
                return InteractionResult.PASS;
            }
            if (level.getBlockState(pos.above()).isAir() && level.getBlockState(pos.above(2)).isAir()) {
                if (!level.isClientSide()) {
                    blockEntity.setTheItem(heldItem.copyWithCount(1));
                    level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        if (level.getBlockEntity(blockPos) instanceof UmbrellaStandBlockEntity be) {
            Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), be.getTheItem());
        }
        super.onRemove(blockState, level, blockPos, blockState2, bl);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof UmbrellaStandBlockEntity blockEntity ?
                blockEntity.getComparatorOutput() : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UmbrellaStandBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx))
                .setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER)
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    // ==================== 碰撞箱旋转辅助方法 ====================
    private static VoxelShape rotateY180(VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            buffer[0] = Shapes.or(buffer[0], Shapes.box(
                    1 - maxX, minY, 1 - maxZ,
                    1 - minX, maxY, 1 - minZ
            ));
        });
        return buffer[0];
    }

    private static VoxelShape rotateY90(VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            buffer[0] = Shapes.or(buffer[0], Shapes.box(
                    minZ, minY, 1 - maxX,
                    maxZ, maxY, 1 - minX
            ));
        });
        return buffer[0];
    }

    private static VoxelShape rotateY270(VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            buffer[0] = Shapes.or(buffer[0], Shapes.box(
                    1 - maxZ, minY, minX,
                    1 - minZ, maxY, maxX
            ));
        });
        return buffer[0];
    }
}