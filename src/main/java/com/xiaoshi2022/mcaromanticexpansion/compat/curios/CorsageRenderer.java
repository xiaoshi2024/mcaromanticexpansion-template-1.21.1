package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xiaoshi2022.mcaromanticexpansion.item.CorsageItem;
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

public class CorsageRenderer {

    public void render(
            ItemStack itemStack,
            Object slotContext,
            PoseStack poseStack,
            RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> renderLayerParent,
            SubmitNodeCollector submitNodeCollector,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        if (!(itemStack.getItem() instanceof CorsageItem)) {
            return;
        }

        poseStack.pushPose();

        HumanoidModel<HumanoidRenderState> humanoidModel = renderLayerParent.getModel();
        if (humanoidModel != null) {
            // 在 NeoForge 26.2 中，translateAndRotate 可能已被移除或签名改变
            // 使用 body 的变换
            humanoidModel.body.translateAndRotate(poseStack);

            poseStack.translate(0.16F, 0.25F, -0.15F);
            poseStack.scale(0.3F, 0.3F, 0.3F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));

            // 使用 ItemInHandRenderer 渲染
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