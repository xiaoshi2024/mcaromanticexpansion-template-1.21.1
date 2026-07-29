package com.xiaoshi2022.mcaromanticexpansion.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public abstract class BaseTrigger<T extends AbstractCriterionTriggerInstance>
        extends SimpleCriterionTrigger<T> {

    private final ResourceLocation id;

    public BaseTrigger(String name) {
        this.id = new ResourceLocation("mcaromanticexpansion", name);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }
}