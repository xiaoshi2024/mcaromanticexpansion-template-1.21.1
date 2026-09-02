package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesFemaleModel;
import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesMaleModel;
import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesModel;
import com.xiaoshi2022.mcaromanticexpansion.item.WeddingClothesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

// ❌ 移除 implements ICurioRenderer
public class WeddingClothesRenderer {

    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();
    private WeddingClothesModel maleModel;
    private WeddingClothesModel femaleModel;

    private WeddingClothesModel getMaleModel() {
        if (maleModel == null) {
            maleModel = new WeddingClothesMaleModel(
                    Minecraft.getInstance().getEntityModels()
                            .bakeLayer(WeddingClothesModel.LAYER_LOCATION_MALE)
            );
        }
        return maleModel;
    }

    private WeddingClothesModel getFemaleModel() {
        if (femaleModel == null) {
            femaleModel = new WeddingClothesFemaleModel(
                    Minecraft.getInstance().getEntityModels()
                            .bakeLayer(WeddingClothesModel.LAYER_LOCATION_FEMALE)
            );
        }
        return femaleModel;
    }

    /**
     * 主渲染方法 - 供 CuriosIntegration 通过反射调用
     */
    public void render(
            ItemStack stack,
            Object slotContext,  // 使用 Object 避免直接依赖 SlotContext
            PoseStack poseStack,
            RenderLayerParent<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> renderLayerParent,
            MultiBufferSource bufferSource,
            int packedLight,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        if (!(stack.getItem() instanceof WeddingClothesItem item)) {
            return;
        }

        WeddingClothesModel clothesModel = item.getGender() == WeddingClothesItem.Gender.FEMALE
                ? getFemaleModel() : getMaleModel();

        ResourceLocation texture = getTexture(item);
        RenderType renderType = RenderType.entityCutout(texture);

        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
            @SuppressWarnings("unchecked")
            HumanoidModel<LivingEntity> playerModel = (HumanoidModel<LivingEntity>) humanoidModel;

            copyPose(playerModel.head, clothesModel.getHead());
            copyPose(playerModel.body, clothesModel.getBody());
            copyPose(playerModel.rightArm, clothesModel.getRightArm());
            copyPose(playerModel.leftArm, clothesModel.getLeftArm());
            copyPose(playerModel.rightLeg, clothesModel.getRightLeg());
            copyPose(playerModel.leftLeg, clothesModel.getLeftLeg());

            poseStack.pushPose();
            clothesModel.renderToBuffer(poseStack, bufferSource.getBuffer(renderType),
                    packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            poseStack.popPose();
        }
    }

    /**
     * 辅助方法：支持直接传入 HumanoidModel
     */
    public void render(
            ItemStack stack,
            LivingEntity entity,
            HumanoidModel<LivingEntity> playerModel,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        if (!(stack.getItem() instanceof WeddingClothesItem item)) {
            return;
        }

        WeddingClothesModel clothesModel = item.getGender() == WeddingClothesItem.Gender.FEMALE
                ? getFemaleModel() : getMaleModel();

        ResourceLocation texture = getTexture(item);
        RenderType renderType = RenderType.entityCutout(texture);

        copyPose(playerModel.head, clothesModel.getHead());
        copyPose(playerModel.body, clothesModel.getBody());
        copyPose(playerModel.rightArm, clothesModel.getRightArm());
        copyPose(playerModel.leftArm, clothesModel.getLeftArm());
        copyPose(playerModel.rightLeg, clothesModel.getRightLeg());
        copyPose(playerModel.leftLeg, clothesModel.getLeftLeg());

        poseStack.pushPose();
        clothesModel.renderToBuffer(poseStack, bufferSource.getBuffer(renderType),
                packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }

    private void copyPose(ModelPart source, ModelPart target) {
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
        target.x = source.x;
        target.y = source.y;
        target.z = source.z;
    }

    private ResourceLocation getTexture(WeddingClothesItem item) {
        String key = item.getCulture().getName() + "_" + item.getGender().getName();
        return TEXTURE_CACHE.computeIfAbsent(key, k ->
                ResourceLocation.fromNamespaceAndPath(
                        MCARomanticExpansion.MODID,
                        "textures/armor/" + k + ".png"
                )
        );
    }
}