package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class RingCuriosRenderer {

    public void render(
            ItemStack itemStack,
            Object slotContext,
            PoseStack poseStack,
            RenderLayerParent<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> renderLayerParent,
            MultiBufferSource multiBufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        poseStack.pushPose();

        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            @SuppressWarnings("unchecked")
            HumanoidModel<LivingEntity> armedModel = (HumanoidModel<LivingEntity>) humanoidModel;

            armedModel.translateToHand(HumanoidArm.LEFT, poseStack);

            // ========== 调整为手掌/手指位置 ==========
            // 调整戒指的大小
            poseStack.scale(0.2F, 0.2F, 0.2F);

            // 位置调整（往下移，往手指尖方向）
            // translate(x, y, z)
            // x: 左右偏移 (正=向外，负=向内)
            // y: 上下偏移 (正=向上，负=向下)
            // z: 前后偏移 (正=向前，负=向后)
            poseStack.translate(0.8F, 2.9F, 0.2F);

            // 旋转戒指使其朝向正确
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));

            // 使用 ItemInHandRenderer 渲染戒指
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    itemStack,
                    ItemDisplayContext.FIXED,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    multiBufferSource,
                    null,
                    0
            );
        }

        poseStack.popPose();
    }
}