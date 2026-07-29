package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MCARomanticExpansion.MODID);

    public static final RegistryObject<SimpleParticleType> RAINBOW_PARTICLE =
            PARTICLES.register("rainbow_particle", () -> new SimpleParticleType(false));
}