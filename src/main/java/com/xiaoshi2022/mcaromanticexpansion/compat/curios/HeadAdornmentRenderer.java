package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xiaoshi2022.mcaromanticexpansion.item.HairPinItem;
import com.xiaoshi2022.mcaromanticexpansion.item.RedVeilItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class HeadAdornmentRenderer implements ICurioRenderer {

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack itemStack,
            SlotContext slotContext,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource multiBufferSource,
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

        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            @SuppressWarnings("unchecked")
            HumanoidModel<LivingEntity> playerModel = (HumanoidModel<LivingEntity>) humanoidModel;

            playerModel.head.translateAndRotate(poseStack);

            if (itemStack.getItem() instanceof RedVeilItem) {
                poseStack.translate(0.0F, -0.26F, 0.0F);
                poseStack.scale(0.6F, 0.6F, 0.6F);
                // 旋转戒指使其朝向正确
                poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            } else if (itemStack.getItem() instanceof HairPinItem) {
                poseStack.translate(0.0F, -0.25F, 0.05F);
                poseStack.scale(0.6F, 0.6F, 0.6F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    itemStack,
                    ItemDisplayContext.HEAD,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    multiBufferSource,
                    slotContext.entity().level(),
                    0
            );
        }

        poseStack.popPose();
    }
}