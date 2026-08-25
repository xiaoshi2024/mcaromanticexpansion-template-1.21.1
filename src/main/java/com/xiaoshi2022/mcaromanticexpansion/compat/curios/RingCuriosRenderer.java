package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
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
            RenderLayerParent<HumanoidRenderState, ? extends EntityModel<?>> renderLayerParent,
            SubmitNodeCollector submitNodeCollector,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        poseStack.pushPose();

        EntityModel<?> model = renderLayerParent.getModel();
        if (model instanceof HumanoidModel<?> humanoidModel) {
            // 在 NeoForge 26.2 中，translateToHand 方法可能已变更
            // 使用 body 或 arm 的变换
            humanoidModel.leftArm.translateAndRotate(poseStack);

            // ========== 调整为手掌/手指位置 ==========
            // 调整戒指的大小
            poseStack.scale(0.2F, 0.2F, 0.2F);

            // 位置调整（往下移，往手指尖方向）
            poseStack.translate(0.8F, 2.9F, 0.2F);

            // 旋转戒指使其朝向正确
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));

            // 修复: 使用 gameRenderer.itemInHandRenderer.renderItem
            Minecraft.getInstance().gameRenderer.itemInHandRenderer.renderItem(
                    null,
                    itemStack,
                    ItemDisplayContext.FIXED,
                    poseStack,
                    submitNodeCollector,
                    light
            );
        }

        poseStack.popPose();
    }
}