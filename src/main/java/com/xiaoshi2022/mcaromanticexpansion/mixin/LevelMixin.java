package com.xiaoshi2022.mcaromanticexpansion.mixin;

import com.xiaoshi2022.mcaromanticexpansion.util.UmbrellaUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelAccessor {
    @Override
    public boolean canSeeSky(BlockPos pos) {
        if (LevelAccessor.super.canSeeSky(pos)) {
            return !UmbrellaUtils.isUnderUmbrella((Level)(Object)this, pos);
        }
        return false;
    }
}