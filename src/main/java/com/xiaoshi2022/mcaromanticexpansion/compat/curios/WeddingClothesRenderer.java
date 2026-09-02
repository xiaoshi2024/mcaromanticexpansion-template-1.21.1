package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesFemaleModel;
import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesMaleModel;
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

import java.util.HashMap;
import java.util.Map;

public class WeddingClothesRenderer {

    private static final ResourceLocation TEXTURE_CHINESE_MALE = new ResourceLocation(
            MCARomanticExpansion.MODID, "textures/armor/chinese_male.png");
    private static final ResourceLocation TEXTURE_CHINESE_FEMALE = new ResourceLocation(
            MCARomanticExpansion.MODID, "textures/armor/chinese_female.png");
    private static final ResourceLocation TEXTURE_WESTERN_MALE = new ResourceLocation(
            MCARomanticExpansion.MODID, "textures/armor/western_male.png");
    private static final ResourceLocation TEXTURE_WESTERN_FEMALE = new ResourceLocation(
            MCARomanticExpansion.MODID, "textures/armor/western_female.png");

    private static final Map<String, ResourceLocation> CULTURE_TEXTURE_CACHE = new HashMap<>();

    private WeddingClothesModel<LivingEntity> maleModel;
    private WeddingClothesModel<LivingEntity> femaleModel;

    private WeddingClothesModel<LivingEntity> getMaleModel() {
        if (maleModel == null) {
            maleModel = new WeddingClothesMaleModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(WeddingClothesModel.LAYER_LOCATION_MALE)
            );
        }
        return maleModel;
    }

    private WeddingClothesModel<LivingEntity> getFemaleModel() {
        if (femaleModel == null) {
            femaleModel = new WeddingClothesFemaleModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(WeddingClothesModel.LAYER_LOCATION_FEMALE)
            );
        }
        return femaleModel;
    }

    // ✅ 根据性别选择模型
    private WeddingClothesModel<LivingEntity> getModel(WeddingClothesItem.Gender gender) {
        if (gender == WeddingClothesItem.Gender.FEMALE) {
            return getFemaleModel();
        } else {
            return getMaleModel();
        }
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

        WeddingClothesItem item = (WeddingClothesItem) stack.getItem();
        WeddingClothesModel<LivingEntity> clothesModel = getModel(item.getGender());
        ResourceLocation texture = getTextureForItem(item);

        RenderType renderType = RenderType.entityCutoutNoCull(texture);
        VertexConsumer consumer = buffer.getBuffer(renderType);
        int overlay = OverlayTexture.NO_OVERLAY;

        float red = 1.0f;
        float green = 1.0f;
        float blue = 1.0f;
        float alpha = 1.0f;

        // 渲染头部
        poseStack.pushPose();
        playerModel.head.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 0.75F, 0.0F);
        clothesModel.getHead().render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        poseStack.popPose();

        // 渲染身体
        poseStack.pushPose();
        playerModel.body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 0.74F, 0.0F);
        clothesModel.getBody().render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        poseStack.popPose();

        // 渲染右臂
        poseStack.pushPose();
        playerModel.rightArm.translateAndRotate(poseStack);
        poseStack.translate(0.32F, 0.6F, 0.0F);
        clothesModel.getRightArm().render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        poseStack.popPose();

        // 渲染左臂
        poseStack.pushPose();
        playerModel.leftArm.translateAndRotate(poseStack);
        poseStack.translate(-0.32F, 0.6F, 0.0F);
        clothesModel.getLeftArm().render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        poseStack.popPose();

        // 渲染右腿
        poseStack.pushPose();
        playerModel.rightLeg.translateAndRotate(poseStack);
        poseStack.translate(0.12F, -0.8F, 0.0F);
        clothesModel.getRightLeg().render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        poseStack.popPose();

        // 渲染左腿
        poseStack.pushPose();
        playerModel.leftLeg.translateAndRotate(poseStack);
        poseStack.translate(-0.12F, -0.8F, 0.0F);
        clothesModel.getLeftLeg().render(poseStack, consumer, light, overlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    private ResourceLocation getTextureForItem(WeddingClothesItem item) {
        WeddingClothesItem.WeddingCulture culture = item.getCulture();
        WeddingClothesItem.Gender gender = item.getGender();

        if (culture == null || gender == null) {
            return TEXTURE_CHINESE_MALE;
        }

        if (culture == WeddingClothesItem.WeddingCulture.CHINESE) {
            return gender == WeddingClothesItem.Gender.MALE ? TEXTURE_CHINESE_MALE : TEXTURE_CHINESE_FEMALE;
        }
        if (culture == WeddingClothesItem.WeddingCulture.WESTERN) {
            return gender == WeddingClothesItem.Gender.MALE ? TEXTURE_WESTERN_MALE : TEXTURE_WESTERN_FEMALE;
        }

        String key = culture.getName() + "_" + gender.getName();
        return CULTURE_TEXTURE_CACHE.computeIfAbsent(key, k ->
                new ResourceLocation(
                        MCARomanticExpansion.MODID,
                        "textures/armor/" + culture.getName() + "_" + gender.getName() + ".png"
                )
        );
    }
}