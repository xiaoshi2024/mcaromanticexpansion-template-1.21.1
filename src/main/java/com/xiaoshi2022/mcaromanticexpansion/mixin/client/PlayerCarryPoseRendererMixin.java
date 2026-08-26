package com.xiaoshi2022.mcaromanticexpansion.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaoshi2022.mcaromanticexpansion.client.CarryClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(AvatarRenderer.class)
public abstract class PlayerCarryPoseRendererMixin {

    @Inject(method = "submit", at = @At("HEAD"), remap = false)
    private void mcae$tiltCarriedPlayer(AvatarRenderState state, PoseStack poseStack,
                                        SubmitNodeCollector submitNodeCollector,
                                        CameraRenderState camera,
                                        CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        UUID playerId = mc.player.getUUID();

        // 检查当前渲染的玩家是否是被抱者
        // 注意：由于状态对象不直接关联UUID，需要通过其他方式判断
        // 可能需要从state中获取玩家ID，或使用渲染上下文

        // 方案：如果state没有playerId字段，建议在CarryClientState中维护
        // 当前渲染的实体ID和状态映射

        // 临时方案：仅当玩家本身是被抱者时处理
        if (!CarryClientState.isCarried(playerId)) return;

        UUID carrierId = CarryClientState.carrierOf(playerId);
        if (carrierId == null) return;

        Player carrier = CarryClientState.getPlayerByUUID(carrierId);
        float carrierYaw;
        if (carrier != null) {
            carrierYaw = carrier.yBodyRot; // 使用当前值
        } else {
            carrierYaw = state.yRot; // 回退
        }

        // 应用旋转 - 使用新API
        Quaternionf yawRotation = new Quaternionf().rotationY((float) Math.toRadians(-carrierYaw));
        poseStack.mulPose(yawRotation);

        Quaternionf layDown = new Quaternionf().rotationX((float) Math.toRadians(90.0F));
        poseStack.mulPose(layDown);

        Quaternionf bodyAlign = new Quaternionf().rotationZ((float) Math.toRadians(90.0F));
        poseStack.mulPose(bodyAlign);

        Quaternionf naturalTilt = new Quaternionf().rotationZ((float) Math.toRadians(-5.0F));
        poseStack.mulPose(naturalTilt);

        poseStack.translate(0.2, -1.2, 0.3);
    }
}