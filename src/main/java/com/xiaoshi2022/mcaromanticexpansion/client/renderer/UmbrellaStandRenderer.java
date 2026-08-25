//package com.xiaoshi2022.mcaromanticexpansion.client.renderer;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import com.mojang.math.Axis;
//import com.xiaoshi2022.mcaromanticexpansion.content.block.UmbrellaStandBlock;
//import com.xiaoshi2022.mcaromanticexpansion.content.block.UmbrellaStandBlockEntity;
//import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.renderer.ItemInHandRenderer;
//import net.minecraft.client.renderer.SubmitNodeCollector;
//import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
//import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
//import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
//import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//import net.minecraft.client.renderer.state.level.CameraRenderState;
//import net.minecraft.core.Direction;
//import net.minecraft.world.item.ItemDisplayContext;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.phys.Vec3;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.api.distmarker.OnlyIn;
//
//@OnlyIn(Dist.CLIENT)
//public class UmbrellaStandRenderer implements BlockEntityRenderer<UmbrellaStandBlockEntity, UmbrellaStandRenderer.UmbrellaStandRenderState> {
//
//    private final BlockEntityRendererProvider.Context context;
//    private final ItemInHandRenderer itemInHandRenderer;
//
//    public UmbrellaStandRenderer(BlockEntityRendererProvider.Context context) {
//        this.context = context;
//        // 从 Minecraft 获取 ItemInHandRenderer
//        this.itemInHandRenderer = Minecraft.getInstance().gameRenderer.itemInHandRenderer;
//    }
//
//    @Override
//    public UmbrellaStandRenderState createRenderState() {
//        return new UmbrellaStandRenderState();
//    }
//
//    @Override
//    public void extractRenderState(UmbrellaStandBlockEntity blockEntity, UmbrellaStandRenderState state,
//                                   float partialTicks, Vec3 cameraPosition,
//                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
//        state.item = blockEntity.getTheItem();
//        state.facing = blockEntity.getBlockState().getValue(UmbrellaStandBlock.FACING);
//        state.umbrellaState = UmbrellaItem.getUmbrellaState(state.item);
//        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
//    }
//
//    @Override
//    public void submit(UmbrellaStandRenderState state, PoseStack poseStack,
//                       SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
//
//        ItemStack umbrellaStack = state.item;
//        if (umbrellaStack == null || umbrellaStack.isEmpty()) {
//            return;
//        }
//
//        Direction facing = state.facing;
//
//        poseStack.pushPose();
//
//        // 将渲染位置移动到方块中心偏下
//        poseStack.translate(0.5D, 0.5D, 0.5D);
//
//        // 根据朝向旋转
//        float rotationY = switch (facing) {
//            case NORTH -> 180.0F;
//            case SOUTH -> 0.0F;
//            case WEST -> 90.0F;
//            case EAST -> -90.0F;
//            default -> 0.0F;
//        };
//        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
//
//        // 伞的位置：从伞架顶部伸出
//        poseStack.translate(0.0D, 0.5D, 0.0D);
//
//        // 获取伞的开关状态并应用旋转
//        float tiltAngle = state.umbrellaState * 15.0F;
//        poseStack.mulPose(Axis.XP.rotationDegrees(-tiltAngle));
//
//        // 调整缩放
//        float scale = 1.0F;
//        poseStack.scale(scale, scale, scale);
//
//        // 使用 ItemInHandRenderer.renderItem 渲染物品
//        // 参数: LivingEntity, ItemStack, ItemDisplayContext, PoseStack, SubmitNodeCollector, int
//        itemInHandRenderer.renderItem(
//                null,  // 不需要 LivingEntity
//                umbrellaStack,
//                ItemDisplayContext.FIXED,
//                poseStack,
//                submitNodeCollector,
//                15728880  // FULL_BRIGHT
//        );
//
//        poseStack.popPose();
//    }
//
//    @Override
//    public boolean shouldRenderOffScreen() {
//        return false;
//    }
//
//    @Override
//    public int getViewDistance() {
//        return 64;
//    }
//
//    @Override
//    public boolean shouldRender(UmbrellaStandBlockEntity blockEntity, Vec3 cameraPosition) {
//        return Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPosition, (double) this.getViewDistance());
//    }
//
//    // ========== 自定义 RenderState ==========
//    public static class UmbrellaStandRenderState extends BlockEntityRenderState {
//        public ItemStack item = ItemStack.EMPTY;
//        public Direction facing = Direction.NORTH;
//        public float umbrellaState = 0.0F;
//    }
//}