package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesModel;
import com.xiaoshi2022.mcaromanticexpansion.item.WeddingClothesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class WeddingClothesRenderer implements ICurioRenderer {

    private static final ResourceLocation TEXTURE_CHINESE_MALE = ResourceLocation.fromNamespaceAndPath(
            MCARomanticExpansion.MODID, "textures/armor/chinese_male.png");
    private static final ResourceLocation TEXTURE_CHINESE_FEMALE = ResourceLocation.fromNamespaceAndPath(
            MCARomanticExpansion.MODID, "textures/armor/chinese_female.png");
    private static final ResourceLocation TEXTURE_WESTERN_MALE = ResourceLocation.fromNamespaceAndPath(
            MCARomanticExpansion.MODID, "textures/armor/western_male.png");
    private static final ResourceLocation TEXTURE_WESTERN_FEMALE = ResourceLocation.fromNamespaceAndPath(
            MCARomanticExpansion.MODID, "textures/armor/western_female.png");

    private WeddingClothesModel<LivingEntity> model;

    private WeddingClothesModel<LivingEntity> getModel() {
        if (model == null) {
            model = new WeddingClothesModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(WeddingClothesModel.LAYER_LOCATION)
            );
        }
        return model;
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffer,
            int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {

        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        @SuppressWarnings("unchecked")
        HumanoidModel<LivingEntity> playerModel = (HumanoidModel<LivingEntity>) humanoidModel;

        WeddingClothesModel<LivingEntity> model = getModel();
        ResourceLocation texture = getTexture(stack, slotContext.entity());
        RenderType renderType = RenderType.entityCutoutNoCull(texture);
        VertexConsumer consumer = buffer.getBuffer(renderType);
        int color = 0xFFFFFFFF;
        int overlay = OverlayTexture.NO_OVERLAY;

        // 注意：每个部件的默认位置已经在模型定义中设置了（PartPose.offset）
        // 我们需要先应用玩家骨骼变换，然后渲染模型部件

        // ========== 1. 渲染头部 - 跟随玩家头部 ==========
        poseStack.pushPose();
        // 先应用玩家头部的变换
        playerModel.head.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 0.75F, 0.0F);  // 如果需要微调
        // 头部模型本身的偏移已经在模型中，直接渲染
        model.getHead().render(poseStack, consumer, light, overlay, color);
        poseStack.popPose();

        // ========== 2. 渲染身体 - 跟随玩家身体 ==========
        poseStack.pushPose();
        playerModel.body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 0.7F, 0.0F);  // 如果需要微调
        model.getBody().render(poseStack, consumer, light, overlay, color);
        poseStack.popPose();

        // ========== 3. 渲染右臂 - 跟随玩家右臂 ==========
        poseStack.pushPose();
        playerModel.rightArm.translateAndRotate(poseStack);
        // 右臂模型需要额外的偏移来对齐
        poseStack.translate(0.32F, 0.6F, 0.0F);  // 如果需要微调
        model.getRightArm().render(poseStack, consumer, light, overlay, color);
        poseStack.popPose();

        // ========== 4. 渲染左臂 - 跟随玩家左臂 ==========
        poseStack.pushPose();
        playerModel.leftArm.translateAndRotate(poseStack);
        poseStack.translate(-0.32F, 0.6F, 0.0F);  // 如果需要微调
        model.getLeftArm().render(poseStack, consumer, light, overlay, color);
        poseStack.popPose();

        // ========== 5. 渲染右腿 - 跟随玩家右腿 ==========
        poseStack.pushPose();
        playerModel.rightLeg.translateAndRotate(poseStack);
        poseStack.translate(0.12F, -0.8F, 0.0F);  // 如果需要微调
        model.getRightLeg().render(poseStack, consumer, light, overlay, color);
        poseStack.popPose();

        // ========== 6. 渲染左腿 - 跟随玩家左腿 ==========
        poseStack.pushPose();
        playerModel.leftLeg.translateAndRotate(poseStack);
        poseStack.translate(-0.12F, -0.8F, 0.0F);  // 如果需要微调
        model.getLeftLeg().render(poseStack, consumer, light, overlay, color);
        poseStack.popPose();
    }

    private ResourceLocation getTexture(ItemStack stack, LivingEntity entity) {
        if (stack.getItem() instanceof WeddingClothesItem weddingClothes) {
            WeddingClothesItem.WeddingType type = weddingClothes.getType();
            WeddingClothesItem.Gender gender = weddingClothes.getGender();

            if (type == WeddingClothesItem.WeddingType.CHINESE) {
                return gender == WeddingClothesItem.Gender.MALE ? TEXTURE_CHINESE_MALE : TEXTURE_CHINESE_FEMALE;
            } else {
                return gender == WeddingClothesItem.Gender.MALE ? TEXTURE_WESTERN_MALE : TEXTURE_WESTERN_FEMALE;
            }
        }
        return TEXTURE_CHINESE_MALE;
    }
}