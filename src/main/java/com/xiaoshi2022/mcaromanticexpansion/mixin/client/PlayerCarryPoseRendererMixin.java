package com.xiaoshi2022.mcaromanticexpansion.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaoshi2022.mcaromanticexpansion.client.CarryClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
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

    @SuppressWarnings({"InvalidInjectorMethodSignature", "UnresolvedMixinReference"})
    @Inject(
            method = "submit",
            at = @At(value = "HEAD"),
            remap = false
    )
    private void mcae$tiltCarriedPlayer(AvatarRenderState state, PoseStack poseStack,
                                        SubmitNodeCollector submitNodeCollector,
                                        CameraRenderState camera,
                                        CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        AbstractClientPlayer player = mc.player;
        UUID playerId = player.getUUID();

        if (!CarryClientState.isCarried(playerId)) {
            return;
        }

        UUID carrierId = CarryClientState.carrierOf(playerId);
        if (carrierId == null) return;

        Player carrier = CarryClientState.getPlayerByUUID(carrierId);

        float carrierYaw;
        if (carrier != null) {
            float yawPrev = carrier.yBodyRotO;
            float yawNow = carrier.yBodyRot;
            carrierYaw = yawPrev + (yawNow - yawPrev) * state.partialTick;
        } else {
            float yawPrev = player.yBodyRotO;
            float yawNow = player.yBodyRot;
            carrierYaw = yawPrev + (yawNow - yawPrev) * state.partialTick;
        }

        Quaternionf yawRotation = new Quaternionf().rotationY((float) Math.toRadians(-carrierYaw));
        poseStack.mulPose(yawRotation);

        Quaternionf layDown = new Quaternionf().rotationX((float) Math.toRadians(90.0F));
        poseStack.mulPose(layDown);

        Quaternionf bodyAlign = new Quaternionf().rotationZ((float) Math.toRadians(90.0F));
        poseStack.mulPose(bodyAlign);

        Quaternionf naturalTilt = new Quaternionf().rotationZ((float) Math.toRadians(-5.0F));
        poseStack.mulPose(naturalTilt);

        poseStack.translate(0.2D, -1.2D, 0.3D);
    }
}