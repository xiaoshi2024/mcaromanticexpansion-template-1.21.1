package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesModel;
import com.xiaoshi2022.mcaromanticexpansion.item.WeddingClothesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class WeddingClothesRenderer {

    private static final ResourceLocation TEXTURE_CHINESE_MALE = new ResourceLocation(
            MCARomanticExpansion.MODID, "textures/armor/chinese_male.png");
    private static final ResourceLocation TEXTURE_CHINESE_FEMALE = new ResourceLocation(
            MCARomanticExpansion.MODID, "textures/armor/chinese_female.png");
    private static final ResourceLocation TEXTURE_WESTERN_MALE = new ResourceLocation(
            MCARomanticExpansion.MODID, "textures/armor/western_male.png");
    private static final ResourceLocation TEXTURE_WESTERN_FEMALE = new ResourceLocation(
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

    public void render(
            ItemStack stack,
            LivingEntity entity,
            HumanoidModel<LivingEntity> playerModel,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        if (!(stack.getItem() instanceof WeddingClothesItem)) {
            return;
        }

        WeddingClothesModel<LivingEntity> clothesModel = getModel();
        ResourceLocation texture = getTexture(stack, entity);
        RenderType renderType = RenderType.entityCutoutNoCull(texture);
        VertexConsumer consumer = buffer.getBuffer(renderType);
        int overlay = OverlayTexture.NO_OVERLAY;

        // 【修复】使用颜色分量 (1.0f, 1.0f, 1.0f, 1.0f) 表示白色不透明
        float red = 1.0f;
        float green = 1.0f;
        float blue = 1.0f;
        float alpha = 1.0f;

        // 1. 渲染头部 - 跟随玩家头部
        poseStack.pushPose();
        playerModel.head.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 0.75F, 0.0F);
        clothesModel.getHead().render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        poseStack.popPose();

        // 2. 渲染身体 - 跟随玩家身体
        poseStack.pushPose();
        playerModel.body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 0.74F, 0.0F);
        clothesModel.getBody().render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        poseStack.popPose();

        // 3. 渲染右臂 - 跟随玩家右臂
        poseStack.pushPose();
        playerModel.rightArm.translateAndRotate(poseStack);
        poseStack.translate(0.32F, 0.6F, 0.0F);
        clothesModel.getRightArm().render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        poseStack.popPose();

        // 4. 渲染左臂 - 跟随玩家左臂
        poseStack.pushPose();
        playerModel.leftArm.translateAndRotate(poseStack);
        poseStack.translate(-0.32F, 0.6F, 0.0F);
        clothesModel.getLeftArm().render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        poseStack.popPose();

        // 5. 渲染右腿 - 跟随玩家右腿
        poseStack.pushPose();
        playerModel.rightLeg.translateAndRotate(poseStack);
        poseStack.translate(0.12F, -0.8F, 0.0F);
        clothesModel.getRightLeg().render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        poseStack.popPose();

        // 6. 渲染左腿 - 跟随玩家左腿
        poseStack.pushPose();
        playerModel.leftLeg.translateAndRotate(poseStack);
        poseStack.translate(-0.12F, -0.8F, 0.0F);
        clothesModel.getLeftLeg().render(poseStack, consumer, light, overlay, red, green, blue, alpha);
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