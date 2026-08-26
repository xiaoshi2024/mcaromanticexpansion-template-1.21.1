package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class RingCuriosRenderer implements ICurioRenderer {

    @Override
    public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            S renderState,
            RenderLayerParent<S, M> renderLayerParent,
            EntityRendererProvider.Context context,
            float yRotation,
            float xRotation) {

        poseStack.pushPose();

        M model = renderLayerParent.getModel();
        if (model instanceof HumanoidModel<?> humanoidModel) {
            humanoidModel.leftArm.translateAndRotate(poseStack);

            poseStack.scale(0.2F, 0.2F, 0.2F);
            poseStack.translate(0.8F, 2.9F, 0.2F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));

            // 从 SlotContext 获取实体
            LivingEntity entity = slotContext.entity();

            Minecraft.getInstance().getEntityRenderDispatcher()
                    .getItemInHandRenderer().renderItem(
                            entity,
                            stack,
                            ItemDisplayContext.FIXED,
                            poseStack,
                            submitNodeCollector,
                            packedLight
                    );
        }

        poseStack.popPose();
    }
}