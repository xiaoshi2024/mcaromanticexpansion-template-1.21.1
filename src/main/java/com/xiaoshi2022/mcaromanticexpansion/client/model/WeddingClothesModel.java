package com.xiaoshi2022.mcaromanticexpansion.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public abstract class WeddingClothesModel<T extends LivingEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION_MALE = new ModelLayerLocation(
            new ResourceLocation(MCARomanticExpansion.MODID, "wedding_clothes_male"), "main"
    );
    public static final ModelLayerLocation LAYER_LOCATION_FEMALE = new ModelLayerLocation(
            new ResourceLocation(MCARomanticExpansion.MODID, "wedding_clothes_female"), "main"
    );

    protected final ModelPart Waist;
    protected final ModelPart Head;
    protected final ModelPart Body;
    protected final ModelPart RightArm;
    protected final ModelPart LeftArm;
    protected final ModelPart RightLeg;
    protected final ModelPart LeftLeg;

    public WeddingClothesModel(ModelPart root) {
        this.Waist = root.getChild("Waist");
        this.Head = this.Waist.getChild("Head");
        this.Body = this.Waist.getChild("Body");
        this.RightArm = this.Waist.getChild("RightArm");
        this.LeftArm = this.Waist.getChild("LeftArm");
        this.RightLeg = root.getChild("RightLeg");
        this.LeftLeg = root.getChild("LeftLeg");
    }

    public ModelPart getHead() { return Head; }
    public ModelPart getBody() { return Body; }
    public ModelPart getRightArm() { return RightArm; }
    public ModelPart getLeftArm() { return LeftArm; }
    public ModelPart getRightLeg() { return RightLeg; }
    public ModelPart getLeftLeg() { return LeftLeg; }

    protected static PartDefinition createBaseParts(MeshDefinition meshdefinition) {
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Waist = partdefinition.addOrReplaceChild("Waist",
                CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

        PartDefinition Head = Waist.addOrReplaceChild("Head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.2F)),
                PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition Body = Waist.addOrReplaceChild("Body",
                CubeListBuilder.create()
                        .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(0.0F, -12.0F, 0.0F));

        return Waist;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        Waist.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        RightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        LeftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}