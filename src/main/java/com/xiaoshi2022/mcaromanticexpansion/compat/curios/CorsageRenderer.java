package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xiaoshi2022.mcaromanticexpansion.item.CorsageItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CorsageRenderer {

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

        if (!(itemStack.getItem() instanceof CorsageItem)) {
            return;
        }

        poseStack.pushPose();

        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            @SuppressWarnings("unchecked")
            HumanoidModel<LivingEntity> armedModel = (HumanoidModel<LivingEntity>) humanoidModel;

            armedModel.body.translateAndRotate(poseStack);

            poseStack.translate(0.16F, 0.25F, -0.15F);
            poseStack.scale(0.3F, 0.3F, 0.3F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));

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