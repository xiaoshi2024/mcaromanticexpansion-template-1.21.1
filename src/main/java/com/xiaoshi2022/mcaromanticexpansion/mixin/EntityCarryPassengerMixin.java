package com.xiaoshi2022.mcaromanticexpansion.mixin;

import com.xiaoshi2022.mcaromanticexpansion.util.CarryRuntime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityCarryPassengerMixin {

    /**
     * 方法可能已被移除/重命名，使用 require = 0 避免编译失败
     * remap = false 使用当前IDE中的方法名
     */
    @Inject(method = "getPassengerRidingPosition",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private void friendship$getCarryPassengerRidingPosition(Entity passenger,
                                                            CallbackInfoReturnable<Vec3> cir) {
        Entity carrier = (Entity) (Object) this;
        if (!(carrier instanceof Player) || !(passenger instanceof Player)) {
            return;
        }
        if (!CarryRuntime.isCarryPair(carrier.getUUID(), passenger.getUUID())) {
            return;
        }
        double yawRad = Math.toRadians(carrier.getYRot());
        double backX = Math.sin(yawRad) * 0.25;
        double backZ = -Math.cos(yawRad) * 0.25;
        Vec3 pos = new Vec3(
                carrier.getX() + backX,
                carrier.getY() + CarryRuntime.carryHeight(),
                carrier.getZ() + backZ
        );
        cir.setReturnValue(pos);
    }

    @Inject(method = "getVehicleAttachmentPoint",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private void friendship$getCarryVehicleAttachmentPoint(Entity vehicle,
                                                           CallbackInfoReturnable<Vec3> cir) {
        Entity passenger = (Entity) (Object) this;
        if (!(vehicle instanceof Player) || !(passenger instanceof Player)) {
            return;
        }
        if (!CarryRuntime.isCarryPair(vehicle.getUUID(), passenger.getUUID())) {
            return;
        }
        cir.setReturnValue(Vec3.ZERO);
    }
}