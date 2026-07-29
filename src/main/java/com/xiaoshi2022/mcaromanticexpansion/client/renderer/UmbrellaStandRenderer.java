package com.xiaoshi2022.mcaromanticexpansion.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xiaoshi2022.mcaromanticexpansion.content.block.UmbrellaStandBlock;
import com.xiaoshi2022.mcaromanticexpansion.content.block.UmbrellaStandBlockEntity;
import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class UmbrellaStandRenderer implements BlockEntityRenderer<UmbrellaStandBlockEntity> {

    public UmbrellaStandRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(UmbrellaStandBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        // 获取伞架中的伞
        ItemStack umbrellaStack = blockEntity.getTheItem();
        if (umbrellaStack.isEmpty()) {
            return;
        }

        // 获取方块状态和朝向
        Direction facing = blockEntity.getBlockState().getValue(UmbrellaStandBlock.FACING);
        
        // 获取物品渲染器
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(umbrellaStack, blockEntity.getLevel(), null, 0);

        poseStack.pushPose();

        // 将渲染位置移动到方块中心偏下
        poseStack.translate(0.5D, 0.5D, 0.5D);

        // 根据朝向旋转
        float rotationY = switch (facing) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));

        // 伞的位置：从伞架顶部伸出
        poseStack.translate(0.0D, 0.5D, 0.0D);

        // 获取伞的开关状态并应用旋转
        float state = UmbrellaItem.getUmbrellaState(umbrellaStack);
        // 伞打开时稍微倾斜（模拟撑开效果）
        float tiltAngle = state * 15.0F; // 0°（关闭）到 15°（完全打开）
        poseStack.mulPose(Axis.XP.rotationDegrees(-tiltAngle));

        // 调整缩放 - 让伞看起来大小合适
        float scale = 1.0F;
        poseStack.scale(scale, scale, scale);

        // 渲染物品
        int light = LightTexture.FULL_BRIGHT; // 或者使用 packedLight
        itemRenderer.render(
                umbrellaStack,
                ItemDisplayContext.FIXED,
                false,
                poseStack,
                bufferSource,
                light,
                OverlayTexture.NO_OVERLAY,
                model
        );

        poseStack.popPose();
    }
}