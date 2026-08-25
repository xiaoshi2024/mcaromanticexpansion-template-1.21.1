package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xiaoshi2022.mcaromanticexpansion.item.HairPinItem;
import com.xiaoshi2022.mcaromanticexpansion.item.RedVeilItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class HeadAdornmentRenderer {

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

        if (!(itemStack.getItem() instanceof RedVeilItem) && !(itemStack.getItem() instanceof HairPinItem)) {
            return;
        }

        poseStack.pushPose();

        // 获取模型
        EntityModel<?> model = renderLayerParent.getModel();
        if (model instanceof HumanoidModel<?> humanoidModel) {
            // 使用 HumanoidModel 的头部变换
            humanoidModel.head.translateAndRotate(poseStack);

            if (itemStack.getItem() instanceof RedVeilItem) {
                poseStack.translate(0.0F, -0.26F, 0.0F);
                poseStack.scale(0.6F, 0.6F, 0.6F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            } else if (itemStack.getItem() instanceof HairPinItem) {
                poseStack.translate(0.0F, -0.25F, 0.05F);
                poseStack.scale(0.6F, 0.6F, 0.6F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }

            // 修复: 使用 gameRenderer.itemInHandRenderer.renderItem 替代 getItemRenderer().renderStatic
            Minecraft.getInstance().gameRenderer.itemInHandRenderer.renderItem(
                    null,
                    itemStack,
                    ItemDisplayContext.HEAD,
                    poseStack,
                    submitNodeCollector,
                    light
            );
        }

        poseStack.popPose();
    }
}