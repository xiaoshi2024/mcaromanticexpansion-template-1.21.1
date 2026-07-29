package com.xiaoshi2022.mcaromanticexpansion.mixin;

import com.xiaoshi2022.mcaromanticexpansion.util.CarryRuntime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityCarryPassengerMixin {

    /**
     * 拦截 rideTick 方法，在乘客更新位置时重新定位
     */
    @Inject(
            method = "rideTick",
            at = @At("RETURN")
    )
    private void friendship$rideTick(CallbackInfo ci) {
        Entity passenger = (Entity) (Object) this;

        if (!(passenger instanceof Player)) {
            return;
        }

        Entity carrier = passenger.getVehicle();
        if (!(carrier instanceof Player)) {
            return;
        }

        if (!CarryRuntime.isCarryPair(carrier.getUUID(), passenger.getUUID())) {
            return;
        }

        // 计算乘客位置
        double yawRad = Math.toRadians(carrier.getYRot());
        double backX = Math.sin(yawRad) * 0.25d;
        double backZ = -Math.cos(yawRad) * 0.25d;

        double x = carrier.getX() + backX;
        double y = carrier.getY() + CarryRuntime.carryHeight();
        double z = carrier.getZ() + backZ;

        passenger.setPos(x, y, z);
    }
}