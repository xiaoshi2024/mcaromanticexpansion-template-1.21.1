package com.xiaoshi2022.mcaromanticexpansion.mixin.client;

import com.xiaoshi2022.mcaromanticexpansion.client.CarryClientState;
import com.xiaoshi2022.mcaromanticexpansion.util.CarryRuntime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class ClientEntityCarryPassengerMixin {

    @Inject(method = "getPassengerRidingPosition", at = @At("HEAD"), cancellable = true)
    private void mcae$getCarryPassengerRidingPosition(Entity passenger,
                                                      CallbackInfoReturnable<Vec3> cir) {
        Entity carrier = (Entity) (Object) this;
        if (!(carrier instanceof Player) || !(passenger instanceof Player)) {
            return;
        }
        UUID carrierId = carrier.getUUID();
        UUID passengerId = passenger.getUUID();
        UUID storedCarrierId = CarryClientState.carrierOf(passengerId);
        if (storedCarrierId == null || !storedCarrierId.equals(carrierId)) {
            return;
        }

        // 被抱者的位置：在载体前方稍微偏上，靠近手臂位置
        double yawRad = Math.toRadians(carrier.getYRot());
        double forwardX = Math.sin(yawRad) * 0.35d;   // 稍微向前
        double forwardZ = -Math.cos(yawRad) * 0.35d;

        Vec3 pos = new Vec3(
                carrier.getX() + forwardX,
                carrier.getY() + CarryRuntime.carryHeight() - 0.5d,  // 调整高度到手臂位置
                carrier.getZ() + forwardZ
        );
        cir.setReturnValue(pos);
    }

    @Inject(method = "getVehicleAttachmentPoint", at = @At("HEAD"), cancellable = true)
    private void mcae$getCarryVehicleAttachmentPoint(Entity vehicle,
                                                     CallbackInfoReturnable<Vec3> cir) {
        Entity passenger = (Entity) (Object) this;
        if (!(vehicle instanceof Player) || !(passenger instanceof Player)) {
            return;
        }
        UUID passengerId = passenger.getUUID();
        UUID vehicleId = vehicle.getUUID();
        UUID storedCarrierId = CarryClientState.carrierOf(passengerId);
        if (storedCarrierId == null || !storedCarrierId.equals(vehicleId)) {
            return;
        }
        // 被抱者与载体的连接点
        cir.setReturnValue(new Vec3(0, 0.5d, 0.3d));
    }
}