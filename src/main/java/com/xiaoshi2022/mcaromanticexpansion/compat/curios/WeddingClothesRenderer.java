package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesModel;
import com.xiaoshi2022.mcaromanticexpansion.item.WeddingClothesItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import java.util.HashMap;
import java.util.Map;

public class WeddingClothesRenderer implements ICurioRenderer {

    private static final Map<String, Identifier> TEXTURE_CACHE = new HashMap<>();
    private WeddingClothesModel model;

    private WeddingClothesModel getModel() {
        if (model == null) {
            model = new WeddingClothesModel(
                    Minecraft.getInstance().getEntityModels()
                            .bakeLayer(WeddingClothesModel.LAYER_LOCATION)
            );
        }
        return model;
    }

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

        if (!(stack.getItem() instanceof WeddingClothesItem)) {
            return;
        }

        WeddingClothesItem item = (WeddingClothesItem) stack.getItem();
        WeddingClothesModel clothesModel = getModel();
        Identifier texture = getTexture(item);

        RenderType renderType = RenderTypes.entityCutout(texture, true);
        int overlay = OverlayTexture.NO_OVERLAY;

        M baseModel = renderLayerParent.getModel();
        if (baseModel instanceof HumanoidModel<?> playerModel) {
            // 渲染各部分
            renderPart(submitNodeCollector, playerModel.head, clothesModel.getHead(),
                    poseStack, renderType, packedLight, 0.0F, 0.75F, 0.0F);
            renderPart(submitNodeCollector, playerModel.body, clothesModel.getBody(),
                    poseStack, renderType, packedLight, 0.0F, 0.74F, 0.0F);
            renderPart(submitNodeCollector, playerModel.rightArm, clothesModel.getRightArm(),
                    poseStack, renderType, packedLight, 0.32F, 0.6F, 0.0F);
            renderPart(submitNodeCollector, playerModel.leftArm, clothesModel.getLeftArm(),
                    poseStack, renderType, packedLight, -0.32F, 0.6F, 0.0F);
            renderPart(submitNodeCollector, playerModel.rightLeg, clothesModel.getRightLeg(),
                    poseStack, renderType, packedLight, 0.12F, -0.8F, 0.0F);
            renderPart(submitNodeCollector, playerModel.leftLeg, clothesModel.getLeftLeg(),
                    poseStack, renderType, packedLight, -0.12F, -0.8F, 0.0F);
        }
    }

    private void renderPart(
            SubmitNodeCollector collector,
            net.minecraft.client.model.geom.ModelPart sourcePart,
            net.minecraft.client.model.geom.ModelPart targetPart,
            PoseStack poseStack,
            RenderType renderType,
            int light,
            float x, float y, float z) {

        poseStack.pushPose();
        sourcePart.translateAndRotate(poseStack);
        poseStack.translate(x, y, z);
        collector.submitModelPart(
                targetPart,
                poseStack,
                renderType,
                light,
                OverlayTexture.NO_OVERLAY,
                null
        );
        poseStack.popPose();
    }

    private Identifier getTexture(WeddingClothesItem item) {
        String key = item.getCulture().getName() + "_" + item.getGender().getName();
        return TEXTURE_CACHE.computeIfAbsent(key, k ->
                Identifier.fromNamespaceAndPath(
                        MCARomanticExpansion.MODID,
                        "textures/armor/" + k + ".png"
                )
        );
    }
}