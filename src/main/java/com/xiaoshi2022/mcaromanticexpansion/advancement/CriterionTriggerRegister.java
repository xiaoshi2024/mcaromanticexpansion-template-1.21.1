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
    
    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, SharedUmbrellaTrigger> SHARED_UMBRELLA =
            TRIGGER_TYPES.register("shared_umbrella", SharedUmbrellaTrigger::new);
    
    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, AffectionLevelTrigger> AFFECTION_LEVEL =
            TRIGGER_TYPES.register("affection_level", AffectionLevelTrigger::new);
    
    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, RomanticEventTrigger> ROMANTIC_EVENT =
            TRIGGER_TYPES.register("romantic_event", RomanticEventTrigger::new);
    
    // 新成就触发器
    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, FirstUmbrellaGiftTrigger> FIRST_UMBRELLA_GIFT =
            TRIGGER_TYPES.register("first_umbrella_gift", FirstUmbrellaGiftTrigger::new);
    
    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, RainyUmbrellaGiftTrigger> RAINY_UMBRELLA_GIFT =
            TRIGGER_TYPES.register("rainy_umbrella_gift", RainyUmbrellaGiftTrigger::new);
    
    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, MutualUmbrellaGiftTrigger> MUTUAL_UMBRELLA_GIFT =
            TRIGGER_TYPES.register("mutual_umbrella_gift", MutualUmbrellaGiftTrigger::new);

    public static class UnveilVeilTrigger extends SimpleCriterionTrigger<UnveilVeilTrigger.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
                    ).apply(instance, Instance::new)
            );
        }
    }

    public static class SharedUmbrellaTrigger extends SimpleCriterionTrigger<SharedUmbrellaTrigger.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player, int duration) {
            this.trigger(player, instance -> duration >= instance.minDuration());
        }

        public record Instance(Optional<ContextAwarePredicate> player, int minDuration) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                            Codec.INT.fieldOf("min_duration").forGetter(Instance::minDuration)
                    ).apply(instance, Instance::new)
            );
        }
    }

    public static class AffectionLevelTrigger extends SimpleCriterionTrigger<AffectionLevelTrigger.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player, int level) {
            this.trigger(player, instance -> level >= instance.minLevel());
        }

        public record Instance(Optional<ContextAwarePredicate> player, int minLevel) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                            Codec.INT.fieldOf("min_level").forGetter(Instance::minLevel)
                    ).apply(instance, Instance::new)
            );
        }
    }

    public static class RomanticEventTrigger extends SimpleCriterionTrigger<RomanticEventTrigger.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player, String eventId) {
            this.trigger(player, instance -> instance.eventId().equals(eventId) || instance.eventId().equals("any"));
        }

        public record Instance(Optional<ContextAwarePredicate> player, String eventId) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                            Codec.STRING.fieldOf("event_id").forGetter(Instance::eventId)
                    ).apply(instance, Instance::new)
            );
        }
    }

    // 首次送伞给玩家
    public static class FirstUmbrellaGiftTrigger extends SimpleCriterionTrigger<FirstUmbrellaGiftTrigger.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public record Instance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
                    ).apply(instance, Instance::new)
            );
        }
    }

    // 雨天送伞累计次数
    public static class RainyUmbrellaGiftTrigger extends SimpleCriterionTrigger<RainyUmbrellaGiftTrigger.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player, int count) {
            this.trigger(player, instance -> count >= instance.minCount());
        }

        public record Instance(Optional<ContextAwarePredicate> player, int minCount) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                            Codec.INT.fieldOf("min_count").forGetter(Instance::minCount)
                    ).apply(instance, Instance::new)
            );
        }
    }

    // 互赠伞累计次数
    public static class MutualUmbrellaGiftTrigger extends SimpleCriterionTrigger<MutualUmbrellaGiftTrigger.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player, int count) {
            this.trigger(player, instance -> count >= instance.minCount());
        }

        public record Instance(Optional<ContextAwarePredicate> player, int minCount) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                            Codec.INT.fieldOf("min_count").forGetter(Instance::minCount)
                    ).apply(instance, Instance::new)
            );
        }
    }
}