package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesModel;
import com.xiaoshi2022.mcaromanticexpansion.item.WeddingClothesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class WeddingClothesRenderer {

    // 纹理常量
    private static final Identifier TEXTURE_CHINESE_MALE = Identifier.fromNamespaceAndPath(
            MCARomanticExpansion.MODID, "textures/armor/chinese_male.png");
    private static final Identifier TEXTURE_CHINESE_FEMALE = Identifier.fromNamespaceAndPath(
            MCARomanticExpansion.MODID, "textures/armor/chinese_female.png");
    private static final Identifier TEXTURE_WESTERN_MALE = Identifier.fromNamespaceAndPath(
            MCARomanticExpansion.MODID, "textures/armor/western_male.png");
    private static final Identifier TEXTURE_WESTERN_FEMALE = Identifier.fromNamespaceAndPath(
            MCARomanticExpansion.MODID, "textures/armor/western_female.png");

    private static final Map<String, Identifier> CULTURE_TEXTURE_CACHE = new HashMap<>();

    private WeddingClothesModel model;

    private WeddingClothesModel getModel() {
        if (model == null) {
            model = new WeddingClothesModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(WeddingClothesModel.LAYER_LOCATION)
            );
        }
        return model;
    }

    public void render(
            ItemStack stack,
            LivingEntity entity,
            HumanoidModel playerModel,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
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
        WeddingClothesModel clothesModel = getModel();
        Identifier texture = getTextureForItem(item);

        int overlay = OverlayTexture.NO_OVERLAY;

        // 获取 OrderedSubmitNodeCollector
        OrderedSubmitNodeCollector orderedCollector = submitNodeCollector.order(0);

        // 使用 entityCutout 替代 entityCutoutNoCull
        var renderType = RenderTypes.entityCutout(texture, true);

        // ========== 1. 渲染头部 ==========
        poseStack.pushPose();
        playerModel.head.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 0.75F, 0.0F);
        orderedCollector.submitModelPart(
                clothesModel.getHead(),
                poseStack,
                renderType,
                light,
                overlay,
                null  // sprite
        );
        poseStack.popPose();

        // ========== 2. 渲染身体 ==========
        poseStack.pushPose();
        playerModel.body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 0.74F, 0.0F);
        orderedCollector.submitModelPart(
                clothesModel.getBody(),
                poseStack,
                renderType,
                light,
                overlay,
                null
        );
        poseStack.popPose();

        // ========== 3. 渲染右臂 ==========
        poseStack.pushPose();
        playerModel.rightArm.translateAndRotate(poseStack);
        poseStack.translate(0.32F, 0.6F, 0.0F);
        orderedCollector.submitModelPart(
                clothesModel.getRightArm(),
                poseStack,
                renderType,
                light,
                overlay,
                null
        );
        poseStack.popPose();

        // ========== 4. 渲染左臂 ==========
        poseStack.pushPose();
        playerModel.leftArm.translateAndRotate(poseStack);
        poseStack.translate(-0.32F, 0.6F, 0.0F);
        orderedCollector.submitModelPart(
                clothesModel.getLeftArm(),
                poseStack,
                renderType,
                light,
                overlay,
                null
        );
        poseStack.popPose();

        // ========== 5. 渲染右腿 ==========
        poseStack.pushPose();
        playerModel.rightLeg.translateAndRotate(poseStack);
        poseStack.translate(0.12F, -0.8F, 0.0F);
        orderedCollector.submitModelPart(
                clothesModel.getRightLeg(),
                poseStack,
                renderType,
                light,
                overlay,
                null
        );
        poseStack.popPose();

        // ========== 6. 渲染左腿 ==========
        poseStack.pushPose();
        playerModel.leftLeg.translateAndRotate(poseStack);
        poseStack.translate(-0.12F, -0.8F, 0.0F);
        orderedCollector.submitModelPart(
                clothesModel.getLeftLeg(),
                poseStack,
                renderType,
                light,
                overlay,
                null
        );
        poseStack.popPose();
    }

    private Identifier getTextureForItem(WeddingClothesItem item) {
        WeddingClothesItem.WeddingCulture culture = item.getCulture();
        WeddingClothesItem.Gender gender = item.getGender();

        if (culture == WeddingClothesItem.WeddingCulture.CHINESE) {
            return gender == WeddingClothesItem.Gender.MALE ? TEXTURE_CHINESE_MALE : TEXTURE_CHINESE_FEMALE;
        }
        if (culture == WeddingClothesItem.WeddingCulture.WESTERN) {
            return gender == WeddingClothesItem.Gender.MALE ? TEXTURE_WESTERN_MALE : TEXTURE_WESTERN_FEMALE;
        }

        String key = culture.getName() + "_" + gender.getName();
        return CULTURE_TEXTURE_CACHE.computeIfAbsent(key, k ->
                Identifier.fromNamespaceAndPath(
                        MCARomanticExpansion.MODID,
                        "textures/armor/" + culture.getName() + "_" + gender.getName() + ".png"
                )
        );
    }

    @Deprecated
    private Identifier getTexture(ItemStack stack, LivingEntity entity) {
        if (stack.getItem() instanceof WeddingClothesItem item) {
            return getTextureForItem(item);
        }
        return TEXTURE_CHINESE_MALE;
    }
}