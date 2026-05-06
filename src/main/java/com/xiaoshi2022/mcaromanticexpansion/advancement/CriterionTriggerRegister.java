package com.xiaoshi2022.mcaromanticexpansion.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public class CriterionTriggerRegister {

    public static final DeferredRegister<net.minecraft.advancements.CriterionTrigger<?>> TRIGGER_TYPES =
            DeferredRegister.create(Registries.TRIGGER_TYPE, MCARomanticExpansion.MODID);

    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, UnveilVeilTrigger> UNVEIL_VEIL =
            TRIGGER_TYPES.register("unveil_veil", UnveilVeilTrigger::new);

    /**
     * 揭下红盖头触发器 - 完全模仿 CorpseOrigin 的实现
     */
    public static class UnveilVeilTrigger extends SimpleCriterionTrigger<UnveilVeilTrigger.Instance> {

        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        // 注意：这里没有添加任何额外的方法，完全按照 CorpseOrigin 的模式
        public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
                    ).apply(instance, Instance::new)
            );
        }
    }
}