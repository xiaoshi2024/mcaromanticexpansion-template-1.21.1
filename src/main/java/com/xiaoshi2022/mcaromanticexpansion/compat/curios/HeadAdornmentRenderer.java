package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xiaoshi2022.mcaromanticexpansion.item.HairPinItem;
import com.xiaoshi2022.mcaromanticexpansion.item.RedVeilItem;
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

public class HeadAdornmentRenderer implements ICurioRenderer {

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

        if (!(stack.getItem() instanceof RedVeilItem) && !(stack.getItem() instanceof HairPinItem)) {
            return;
        }

        poseStack.pushPose();

        M model = renderLayerParent.getModel();
        if (model instanceof HumanoidModel<?> humanoidModel) {
            humanoidModel.head.translateAndRotate(poseStack);

            if (stack.getItem() instanceof RedVeilItem) {
                poseStack.translate(0.0F, -0.26F, 0.0F);
                poseStack.scale(0.6F, 0.6F, 0.6F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            } else if (stack.getItem() instanceof HairPinItem) {
                poseStack.translate(0.0F, -0.25F, 0.05F);
                poseStack.scale(0.6F, 0.6F, 0.6F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }

            // 从 SlotContext 获取实体
            LivingEntity entity = slotContext.entity();

            Minecraft.getInstance().getEntityRenderDispatcher()
                    .getItemInHandRenderer().renderItem(
                            entity,
                            stack,
                            ItemDisplayContext.HEAD,
                            poseStack,
                            submitNodeCollector,
                            packedLight
                    );
        }

        poseStack.popPose();
    }
}