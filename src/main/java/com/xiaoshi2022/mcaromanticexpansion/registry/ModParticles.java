package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE, MCARomanticExpansion.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAINBOW_PARTICLE =
            PARTICLES.register("rainbow_particle", () -> new SimpleParticleType(false));
}