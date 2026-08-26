package com.xiaoshi2022.mcaromanticexpansion.client.model;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public abstract class WeddingClothesModel extends EntityModel<EntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION_MALE = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "wedding_clothes_male"), "main"
    );
    public static final ModelLayerLocation LAYER_LOCATION_FEMALE = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "wedding_clothes_female"), "main"
    );

    protected final ModelPart Waist;
    protected final ModelPart Head;
    protected final ModelPart Body;
    protected final ModelPart RightArm;
    protected final ModelPart LeftArm;
    protected final ModelPart RightLeg;
    protected final ModelPart LeftLeg;

    public WeddingClothesModel(ModelPart root) {
        super(root);
        this.Waist = root.getChild("Waist");
        this.Head = this.Waist.getChild("Head");
        this.Body = this.Waist.getChild("Body");
        this.RightArm = this.Waist.getChild("RightArm");
        this.LeftArm = this.Waist.getChild("LeftArm");
        this.RightLeg = root.getChild("RightLeg");
        this.LeftLeg = root.getChild("LeftLeg");
    }

    // Getter 方法
    public ModelPart getHead() { return Head; }
    public ModelPart getBody() { return Body; }
    public ModelPart getRightArm() { return RightArm; }
    public ModelPart getLeftArm() { return LeftArm; }
    public ModelPart getRightLeg() { return RightLeg; }
    public ModelPart getLeftLeg() { return LeftLeg; }

    // 创建基础模型（不含手臂和腿，由子类各自定义）
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
    public void setupAnim(EntityRenderState state) {
        // 子类可以覆盖此方法实现自定义动画
    }
}