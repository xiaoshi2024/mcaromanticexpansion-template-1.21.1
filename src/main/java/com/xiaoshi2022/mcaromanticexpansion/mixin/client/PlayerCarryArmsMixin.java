package com.xiaoshi2022.mcaromanticexpansion.mixin.client;

import com.xiaoshi2022.mcaromanticexpansion.client.CarryClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(HumanoidModel.class)
public abstract class PlayerCarryArmsMixin<T extends HumanoidRenderState> {

    @Inject(method = "setupAnim", at = @At("TAIL"), remap = false)
    private void mcae$applyCarryArmsPose(T state, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        UUID playerId = mc.player.getUUID();
        if (!CarryClientState.isCarrier(playerId)) return;

        // 将 this 转换为 HumanoidModel 以访问 public final 字段
        HumanoidModel<T> model = (HumanoidModel<T>) (Object) this;

        // 直接访问公开字段 - 不需要 @Shadow
        ModelPart rightArm = model.rightArm;
        ModelPart leftArm = model.leftArm;

        // 应用自定义手臂角度
        rightArm.xRot = (float) Math.toRadians(-40.0);
        rightArm.yRot = (float) Math.toRadians(29.0);
        rightArm.zRot = 0.0F;

        leftArm.xRot = (float) Math.toRadians(-40.0);
        leftArm.yRot = (float) Math.toRadians(-22.0);
        leftArm.zRot = 0.0F;
    }
}