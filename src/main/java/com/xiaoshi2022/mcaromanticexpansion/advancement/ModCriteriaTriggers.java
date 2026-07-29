package com.xiaoshi2022.mcaromanticexpansion.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ModCriteriaTriggers {

    // ========== 注册所有触发器 ==========
    public static final FirstUmbrellaGiftTrigger FIRST_UMBRELLA_GIFT =
            CriteriaTriggers.register(new FirstUmbrellaGiftTrigger());

    public static final RainyUmbrellaGiftTrigger RAINY_UMBRELLA_GIFT =
            CriteriaTriggers.register(new RainyUmbrellaGiftTrigger());

    public static final MutualUmbrellaGiftTrigger MUTUAL_UMBRELLA_GIFT =
            CriteriaTriggers.register(new MutualUmbrellaGiftTrigger());

    public static final LoveLetterReplyTrigger LOVE_LETTER_REPLY =
            CriteriaTriggers.register(new LoveLetterReplyTrigger());

    public static final UnveilVeilTrigger UNVEIL_VEIL =
            CriteriaTriggers.register(new UnveilVeilTrigger());

    public static final RomanticEventTrigger ROMANTIC_EVENT =
            CriteriaTriggers.register(new RomanticEventTrigger());

    // ========== 确保类加载 ==========
    public static void register() {
        // 静态初始化已经完成，这个方法只是为了确保类被加载
    }

    // ============================================================
    // 1. 初次共伞触发器
    // ============================================================
    public static class FirstUmbrellaGiftTrigger extends SimpleCriterionTrigger<FirstUmbrellaGiftTrigger.Instance> {
        public static final ResourceLocation ID = new ResourceLocation("mcaromanticexpansion", "first_umbrella_gift");

        @Override
        public ResourceLocation getId() { return ID; }

        @Override
        public Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext ctx) {
            return new Instance(player);
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            public Instance(ContextAwarePredicate player) {
                super(ID, player);
            }
        }
    }

    // ============================================================
    // 2. 雨中共伞触发器
    // ============================================================
    public static class RainyUmbrellaGiftTrigger extends SimpleCriterionTrigger<RainyUmbrellaGiftTrigger.Instance> {
        public static final ResourceLocation ID = new ResourceLocation("mcaromanticexpansion", "rainy_umbrella_gift");

        @Override
        public ResourceLocation getId() { return ID; }

        @Override
        public Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext ctx) {
            return new Instance(player);
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            public Instance(ContextAwarePredicate player) {
                super(ID, player);
            }
        }
    }

    // ============================================================
    // 3. 多次共伞触发器（20次）
    // ============================================================
    public static class MutualUmbrellaGiftTrigger extends SimpleCriterionTrigger<MutualUmbrellaGiftTrigger.Instance> {
        public static final ResourceLocation ID = new ResourceLocation("mcaromanticexpansion", "mutual_umbrella_gift");

        @Override
        public ResourceLocation getId() { return ID; }

        @Override
        public Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext ctx) {
            return new Instance(player);
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            public Instance(ContextAwarePredicate player) {
                super(ID, player);
            }
        }
    }

    // ============================================================
    // 4. 情书回信触发器
    // ============================================================
    public static class LoveLetterReplyTrigger extends SimpleCriterionTrigger<LoveLetterReplyTrigger.Instance> {
        public static final ResourceLocation ID = new ResourceLocation("mcaromanticexpansion", "love_letter_reply");

        @Override
        public ResourceLocation getId() { return ID; }

        @Override
        public Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext ctx) {
            return new Instance(player);
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            public Instance(ContextAwarePredicate player) {
                super(ID, player);
            }
        }
    }

    // ============================================================
    // 5. 揭盖头触发器
    // ============================================================
    public static class UnveilVeilTrigger extends SimpleCriterionTrigger<UnveilVeilTrigger.Instance> {
        public static final ResourceLocation ID = new ResourceLocation("mcaromanticexpansion", "unveil_veil");

        @Override
        public ResourceLocation getId() { return ID; }

        @Override
        public Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext ctx) {
            return new Instance(player);
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            public Instance(ContextAwarePredicate player) {
                super(ID, player);
            }
        }
    }

    // ============================================================
    // 6. 浪漫事件触发器
    // ============================================================
    public static class RomanticEventTrigger extends SimpleCriterionTrigger<RomanticEventTrigger.Instance> {
        public static final ResourceLocation ID = new ResourceLocation("mcaromanticexpansion", "romantic_event");

        @Override
        public ResourceLocation getId() { return ID; }

        @Override
        public Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext ctx) {
            return new Instance(player);
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public static class Instance extends AbstractCriterionTriggerInstance {
            public Instance(ContextAwarePredicate player) {
                super(ID, player);
            }
        }
    }
}