package com.xiaoshi2022.mcaromanticexpansion.mixin.client;

import com.xiaoshi2022.mcaromanticexpansion.client.CarryClientState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class PlayerCarryArmsMixin<T extends LivingEntity> {

    @Shadow
    @Final
    public ModelPart rightArm;

    @Shadow
    @Final
    public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("HEAD"),
            cancellable = false)
    private void mcae$raiseCarryArmsPre(T entity, float limbSwing, float limbSwingAmount,
                                        float ageInTicks, float netHeadYaw, float headPitch,
                                        CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!CarryClientState.isCarrier(player.getUUID())) {
            return;
        }

        // 手臂角度：-40度，略微内收（30度）
        this.rightArm.xRot = (float) Math.toRadians(-40.0d);
        this.rightArm.yRot = (float) Math.toRadians(29.0d);   // 右臂略微内收
        this.rightArm.zRot = (float) Math.toRadians(0.0d);

        this.leftArm.xRot = (float) Math.toRadians(-40.0d);
        this.leftArm.yRot = (float) Math.toRadians(-22.0d);  // 左臂略微内收
        this.leftArm.zRot = (float) Math.toRadians(0.0d);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL"))
    private void mcae$raiseCarryArmsPost(T entity, float limbSwing, float limbSwingAmount,
                                         float ageInTicks, float netHeadYaw, float headPitch,
                                         CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!CarryClientState.isCarrier(player.getUUID())) {
            return;
        }

        // 再次确认
        this.rightArm.xRot = (float) Math.toRadians(-40.0d);
        this.rightArm.yRot = (float) Math.toRadians(29.0d);
        this.rightArm.zRot = (float) Math.toRadians(0.0d);

        this.leftArm.xRot = (float) Math.toRadians(-40.0d);
        this.leftArm.yRot = (float) Math.toRadians(-22.0d);
        this.leftArm.zRot = (float) Math.toRadians(0.0d);
    }
}