package com.xiaoshi2022.mcaromanticexpansion.mixin.client;

import com.xiaoshi2022.mcaromanticexpansion.client.CarryClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerRenderer.class)
public abstract class PlayerCarryPoseRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "HEAD")
    )
    private void mcae$tiltCarriedPlayer(AbstractClientPlayer player, float entityYaw, float partialTick,
                                        PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                        CallbackInfo ci) {
        UUID playerId = player.getUUID();
        if (!CarryClientState.isCarried(playerId)) {
            return;
        }

        UUID carrierId = CarryClientState.carrierOf(playerId);
        if (carrierId == null) return;

        Player carrier = CarryClientState.getPlayerByUUID(carrierId);

        // 获取载体的偏航角（用于对齐被抱者的朝向）
        float carrierYaw;
        if (carrier != null) {
            float yawPrev = carrier.yBodyRotO;
            float yawNow = carrier.yBodyRot;
            carrierYaw = yawPrev + (yawNow - yawPrev) * partialTick;
        } else {
            // 保底：如果没有载体，使用玩家自身旋转
            float yawPrev = player.yBodyRotO;
            float yawNow = player.yBodyRot;
            carrierYaw = yawPrev + (yawNow - yawPrev) * partialTick;
        }

        // ========== 核心修复：正确的变换顺序 ==========
        // 注意：此时 PoseStack 已经在世界坐标的玩家位置

        // 1. 重置所有本地变换（非常重要！）
        // 因为 PlayerRenderer 可能已经应用了一些变换，我们需要清除它们
        // 但我们不能使用 setIdentity()，因为会清除世界坐标
        // 正确的做法：使用 push/pop 或者直接覆盖

        // 但由于我们在 HEAD 注入，此时 PoseStack 还没有应用模型的本地变换
        // 所以我们可以安全地从头开始构建本地变换

        // 2. 应用载体的偏航旋转（让被抱者朝向载体面向的方向）
        // 注意：取反是因为 Minecraft 的坐标系统
        Quaternionf yawRotation = new Quaternionf().rotationY((float) Math.toRadians(-carrierYaw));
        poseStack.mulPose(yawRotation);

        // 3. 让玩家躺下（绕 X 轴旋转 90 度）
        Quaternionf layDown = new Quaternionf().rotationX((float) Math.toRadians(90.0F));
        poseStack.mulPose(layDown);

        // 4. 调整身体方向（绕 Z 轴旋转 90 度，让身体垂直于载体）
        Quaternionf bodyAlign = new Quaternionf().rotationZ((float) Math.toRadians(90.0F));
        poseStack.mulPose(bodyAlign);

        // 5. 轻微的倾斜让姿势更自然（绕 Z 轴旋转 -5 度）
        Quaternionf naturalTilt = new Quaternionf().rotationZ((float) Math.toRadians(-5.0F));
        poseStack.mulPose(naturalTilt);

        // 6. 最后进行本地平移：将模型从脚底(0,0,0)抬高到手臂位置
        // 注意：这里的所有平移都是基于"旋转后"的本地坐标系
        // 参数说明：
        //   X轴: 左右偏移 (正=右, 负=左)
        //   Y轴: 上下偏移 (正=上)
        //   Z轴: 前后偏移 (正=前, 负=后)
        poseStack.translate(0.2D, -1.2D, 0.3D);

        // 7. 可选：进一步微调位置适应不同玩家模型
        // 如果是抱在左肩，可以这样调整：
        // poseStack.translate(-0.3D, 0.85D, 0.3D);

        // 如果是抱在右肩，可以这样调整：
        // poseStack.translate(0.3D, 0.85D, 0.3D);
    }
}