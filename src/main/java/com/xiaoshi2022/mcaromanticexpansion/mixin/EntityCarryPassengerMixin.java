package com.xiaoshi2022.mcaromanticexpansion.mixin;

import com.xiaoshi2022.mcaromanticexpansion.util.CarryRuntime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityCarryPassengerMixin {

    @Inject(
            method = "positionRider(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void friendship$positionRider(Entity passenger, CallbackInfo ci) {
        Entity carrier = (Entity) (Object) this;

        if (!(carrier instanceof Player) || !(passenger instanceof Player)) {
            return;
        }

        if (!CarryRuntime.isCarryPair(carrier.getUUID(), passenger.getUUID())) {
            return;
        }

        double yawRad = Math.toRadians(carrier.getYRot());
        double backX = Math.sin(yawRad) * 0.25d;
        double backZ = -Math.cos(yawRad) * 0.25d;

        double x = carrier.getX() + backX;
        double y = carrier.getY() + CarryRuntime.carryHeight();
        double z = carrier.getZ() + backZ;

        passenger.setPos(x, y, z);
        ci.cancel();
    }
}
