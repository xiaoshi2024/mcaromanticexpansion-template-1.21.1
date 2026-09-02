package com.xiaoshi2022.mcaromanticexpansion.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public class WeddingClothesFemaleModel extends WeddingClothesModel {

    public WeddingClothesFemaleModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        var Waist = createBaseParts(meshdefinition);

        // 女性手臂：3格宽，标准对齐
        Waist.addOrReplaceChild("RightArm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(-5.0F, -10.0F, 0.0F));

        Waist.addOrReplaceChild("LeftArm",
                CubeListBuilder.create()
                        .texOffs(32, 48)
                        .addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(5.0F, -10.0F, 0.0F));

        meshdefinition.getRoot().addOrReplaceChild("RightLeg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        meshdefinition.getRoot().addOrReplaceChild("LeftLeg",
                CubeListBuilder.create()
                        .texOffs(16, 48)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.2F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}