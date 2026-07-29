//package com.xiaoshi2022.mcaromanticexpansion.advancement;
//
//import com.mojang.serialization.Codec;
//import com.mojang.serialization.codecs.RecordCodecBuilder;
//import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
//import net.minecraft.advancements.CriterionTrigger;
//import net.minecraft.advancements.critereon.ContextAwarePredicate;
//import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
//import net.minecraft.core.Registry;
//import net.minecraft.core.registries.BuiltInRegistries;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraftforge.registries.DeferredRegister;
//import net.minecraftforge.registries.ForgeRegistries;
//import net.minecraftforge.registries.RegistryObject;
//
//import java.util.Optional;
//
//public class CriterionTriggerRegister {
//
//    // 使用 ForgeRegistries.TRIGGER_TYPES（如果存在）
//    // 或者在 1.20.1 中，CriterionTrigger 可能不在 ForgeRegistries 中，
//    // 需要直接使用 BuiltInRegistries 注册
//
//    // 方法1：如果 ForgeRegistries.TRIGGER_TYPES 存在
//    // public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
//    //         DeferredRegister.create(ForgeRegistries.TRIGGER_TYPES, MCARomanticExpansion.MODID);
//
//    // 方法2：直接使用 BuiltInRegistries 注册（推荐）
//    // 在 1.20.1 中，需要手动注册
//
//    // 由于 1.20.1 没有直接注册触发器的方式，我们使用静态初始化块手动注册
//    public static final UnveilVeilTrigger UNVEIL_VEIL_TRIGGER = new UnveilVeilTrigger();
//    public static final SharedUmbrellaTrigger SHARED_UMBRELLA_TRIGGER = new SharedUmbrellaTrigger();
//    public static final AffectionLevelTrigger AFFECTION_LEVEL_TRIGGER = new AffectionLevelTrigger();
//    public static final RomanticEventTrigger ROMANTIC_EVENT_TRIGGER = new RomanticEventTrigger();
//    public static final FirstUmbrellaGiftTrigger FIRST_UMBRELLA_GIFT_TRIGGER = new FirstUmbrellaGiftTrigger();
//    public static final RainyUmbrellaGiftTrigger RAINY_UMBRELLA_GIFT_TRIGGER = new RainyUmbrellaGiftTrigger();
//    public static final MutualUmbrellaGiftTrigger MUTUAL_UMBRELLA_GIFT_TRIGGER = new MutualUmbrellaGiftTrigger();
//    public static final LoveLetterReplyTrigger LOVE_LETTER_REPLY_TRIGGER = new LoveLetterReplyTrigger();
//
//    // 注册方法 - 在模组初始化时调用
//    public static void register() {
//        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
//                new ResourceLocation(MCARomanticExpansion.MODID, "unveil_veil"), UNVEIL_VEIL_TRIGGER);
//        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
//                new ResourceLocation(MCARomanticExpansion.MODID, "shared_umbrella"), SHARED_UMBRELLA_TRIGGER);
//        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
//                new ResourceLocation(MCARomanticExpansion.MODID, "affection_level"), AFFECTION_LEVEL_TRIGGER);
//        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
//                new ResourceLocation(MCARomanticExpansion.MODID, "romantic_event"), ROMANTIC_EVENT_TRIGGER);
//        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
//                new ResourceLocation(MCARomanticExpansion.MODID, "first_umbrella_gift"), FIRST_UMBRELLA_GIFT_TRIGGER);
//        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
//                new ResourceLocation(MCARomanticExpansion.MODID, "rainy_umbrella_gift"), RAINY_UMBRELLA_GIFT_TRIGGER);
//        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
//                new ResourceLocation(MCARomanticExpansion.MODID, "mutual_umbrella_gift"), MUTUAL_UMBRELLA_GIFT_TRIGGER);
//        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
//                new ResourceLocation(MCARomanticExpansion.MODID, "love_letter_reply"), LOVE_LETTER_REPLY_TRIGGER);
//    }
//
//    // 获取触发器实例的便捷方法
//    public static UnveilVeilTrigger getUnveilVeilTrigger() { return UNVEIL_VEIL_TRIGGER; }
//    public static SharedUmbrellaTrigger getSharedUmbrellaTrigger() { return SHARED_UMBRELLA_TRIGGER; }
//    public static AffectionLevelTrigger getAffectionLevelTrigger() { return AFFECTION_LEVEL_TRIGGER; }
//    public static RomanticEventTrigger getRomanticEventTrigger() { return ROMANTIC_EVENT_TRIGGER; }
//    public static FirstUmbrellaGiftTrigger getFirstUmbrellaGiftTrigger() { return FIRST_UMBRELLA_GIFT_TRIGGER; }
//    public static RainyUmbrellaGiftTrigger getRainyUmbrellaGiftTrigger() { return RAINY_UMBRELLA_GIFT_TRIGGER; }
//    public static MutualUmbrellaGiftTrigger getMutualUmbrellaGiftTrigger() { return MUTUAL_UMBRELLA_GIFT_TRIGGER; }
//    public static LoveLetterReplyTrigger getLoveLetterReplyTrigger() { return LOVE_LETTER_REPLY_TRIGGER; }
//
//    // ========== 触发器类定义 ==========
//
//    public static class UnveilVeilTrigger extends SimpleCriterionTrigger<UnveilVeilTrigger.Instance> {
//        @Override
//        public Codec<Instance> codec() {
//            return Instance.CODEC;
//        }
//
//        public void trigger(ServerPlayer player) {
//            this.trigger(player, instance -> true);
//        }
//
//        public record Instance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
//            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
//                    instance.group(
//                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
//                    ).apply(instance, Instance::new)
//            );
//        }
//    }
//
//    public static class SharedUmbrellaTrigger extends SimpleCriterionTrigger<SharedUmbrellaTrigger.Instance> {
//        @Override
//        public Codec<Instance> codec() {
//            return Instance.CODEC;
//        }
//
//        public void trigger(ServerPlayer player, int duration) {
//            this.trigger(player, instance -> duration >= instance.minDuration());
//        }
//
//        public record Instance(Optional<ContextAwarePredicate> player, int minDuration) implements SimpleInstance {
//            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
//                    instance.group(
//                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
//                            Codec.INT.fieldOf("min_duration").forGetter(Instance::minDuration)
//                    ).apply(instance, Instance::new)
//            );
//        }
//    }
//
//    public static class AffectionLevelTrigger extends SimpleCriterionTrigger<AffectionLevelTrigger.Instance> {
//        @Override
//        public Codec<Instance> codec() {
//            return Instance.CODEC;
//        }
//
//        public void trigger(ServerPlayer player, int level) {
//            this.trigger(player, instance -> level >= instance.minLevel());
//        }
//
//        public record Instance(Optional<ContextAwarePredicate> player, int minLevel) implements SimpleInstance {
//            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
//                    instance.group(
//                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
//                            Codec.INT.fieldOf("min_level").forGetter(Instance::minLevel)
//                    ).apply(instance, Instance::new)
//            );
//        }
//    }
//
//    public static class RomanticEventTrigger extends SimpleCriterionTrigger<RomanticEventTrigger.Instance> {
//        @Override
//        public Codec<Instance> codec() {
//            return Instance.CODEC;
//        }
//
//        public void trigger(ServerPlayer player, String eventId) {
//            this.trigger(player, instance -> instance.eventId().equals(eventId) || instance.eventId().equals("any"));
//        }
//
//        public record Instance(Optional<ContextAwarePredicate> player, String eventId) implements SimpleInstance {
//            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
//                    instance.group(
//                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
//                            Codec.STRING.fieldOf("event_id").forGetter(Instance::eventId)
//                    ).apply(instance, Instance::new)
//            );
//        }
//    }
//
//    public static class FirstUmbrellaGiftTrigger extends SimpleCriterionTrigger<FirstUmbrellaGiftTrigger.Instance> {
//        @Override
//        public Codec<Instance> codec() {
//            return Instance.CODEC;
//        }
//
//        public void trigger(ServerPlayer player) {
//            this.trigger(player, instance -> true);
//        }
//
//        public record Instance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
//            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
//                    instance.group(
//                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
//                    ).apply(instance, Instance::new)
//            );
//        }
//    }
//
//    public static class RainyUmbrellaGiftTrigger extends SimpleCriterionTrigger<RainyUmbrellaGiftTrigger.Instance> {
//        @Override
//        public Codec<Instance> codec() {
//            return Instance.CODEC;
//        }
//
//        public void trigger(ServerPlayer player, int count) {
//            this.trigger(player, instance -> count >= instance.minCount());
//        }
//
//        public record Instance(Optional<ContextAwarePredicate> player, int minCount) implements SimpleInstance {
//            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
//                    instance.group(
//                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
//                            Codec.INT.fieldOf("min_count").forGetter(Instance::minCount)
//                    ).apply(instance, Instance::new)
//            );
//        }
//    }
//
//    public static class MutualUmbrellaGiftTrigger extends SimpleCriterionTrigger<MutualUmbrellaGiftTrigger.Instance> {
//        @Override
//        public Codec<Instance> codec() {
//            return Instance.CODEC;
//        }
//
//        public void trigger(ServerPlayer player, int count) {
//            this.trigger(player, instance -> count >= instance.minCount());
//        }
//
//        public record Instance(Optional<ContextAwarePredicate> player, int minCount) implements SimpleInstance {
//            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
//                    instance.group(
//                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
//                            Codec.INT.fieldOf("min_count").forGetter(Instance::minCount)
//                    ).apply(instance, Instance::new)
//            );
//        }
//    }
//
//    public static class LoveLetterReplyTrigger extends SimpleCriterionTrigger<LoveLetterReplyTrigger.Instance> {
//        @Override
//        public Codec<Instance> codec() {
//            return Instance.CODEC;
//        }
//
//        public void trigger(ServerPlayer player) {
//            this.trigger(player, instance -> true);
//        }
//
//        public record Instance(Optional<ContextAwarePredicate> player) implements SimpleInstance {
//            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
//                    instance.group(
//                            ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
//                    ).apply(instance, Instance::new)
//            );
//        }
//    }
//}