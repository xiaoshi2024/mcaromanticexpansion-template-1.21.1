package com.xiaoshi2022.mcaromanticexpansion.client.model;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public class WeddingClothesModel extends EntityModel<EntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "wedding_clothes"), "main"
    );

    private final ModelPart Waist;
    private final ModelPart Head;
    private final ModelPart Body;
    private final ModelPart RightArm;
    private final ModelPart LeftArm;
    private final ModelPart RightLeg;
    private final ModelPart LeftLeg;

    // Getter 方法（如果需要外部访问）
    public ModelPart getHead() { return Head; }
    public ModelPart getBody() { return Body; }
    public ModelPart getRightArm() { return RightArm; }
    public ModelPart getLeftArm() { return LeftArm; }
    public ModelPart getRightLeg() { return RightLeg; }
    public ModelPart getLeftLeg() { return LeftLeg; }

    public WeddingClothesModel(ModelPart root) {
        super(root); // 调用父类构造函数，传入 root
        this.Waist = root.getChild("Waist");
        this.Head = this.Waist.getChild("Head");
        this.Body = this.Waist.getChild("Body");
        this.RightArm = this.Waist.getChild("RightArm");
        this.LeftArm = this.Waist.getChild("LeftArm");
        this.RightLeg = root.getChild("RightLeg");
        this.LeftLeg = root.getChild("LeftLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
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

        PartDefinition RightArm = Waist.addOrReplaceChild("RightArm",
                CubeListBuilder.create()
                        .texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(-5.0F, -10.0F, 0.0F));

        PartDefinition LeftArm = Waist.addOrReplaceChild("LeftArm",
                CubeListBuilder.create()
                        .texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(5.0F, -10.0F, 0.0F));

        PartDefinition RightLeg = partdefinition.addOrReplaceChild("RightLeg",
                CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        PartDefinition LeftLeg = partdefinition.addOrReplaceChild("LeftLeg",
                CubeListBuilder.create()
                        .texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(EntityRenderState state) {
        // 在这里设置动画，使用 state 中的数据
        // 如果需要访问实体数据，可以从 state 中获取
        // 例如：
        // float ageInTicks = state.ageInTicks;
        // float partialTick = state.partialTick;
        // 根据状态设置模型部件的旋转
    }
}