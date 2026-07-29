package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.RomanticEventManager;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

public class RomanticAdvancementListener {

    // 成就ID -> 对应的事件ID 映射表
    private static final Map<ResourceLocation, String> ADVANCEMENT_EVENT_MAP = new HashMap<>();

    static {
        // 1.20.1 中使用 new ResourceLocation 或 new ResourceLocation
        // 浪漫成就映射
        ADVANCEMENT_EVENT_MAP.put(
                new ResourceLocation(MCARomanticExpansion.MODID, "rainbow_pact"),
                "rainbow_pact"
        );
        ADVANCEMENT_EVENT_MAP.put(
                new ResourceLocation(MCARomanticExpansion.MODID, "time_stands_still"),
                "time_stands_still"
        );
        ADVANCEMENT_EVENT_MAP.put(
                new ResourceLocation(MCARomanticExpansion.MODID, "heart_to_heart"),
                "heart_to_heart"
        );
        ADVANCEMENT_EVENT_MAP.put(
                new ResourceLocation(MCARomanticExpansion.MODID, "synchrony_test"),
                "synchrony_test"
        );
        ADVANCEMENT_EVENT_MAP.put(
                new ResourceLocation(MCARomanticExpansion.MODID, "share_story"),
                "share_story"
        );
        ADVANCEMENT_EVENT_MAP.put(
                new ResourceLocation(MCARomanticExpansion.MODID, "hold_hands"),
                "hold_hands"
        );
        ADVANCEMENT_EVENT_MAP.put(
                new ResourceLocation(MCARomanticExpansion.MODID, "whisper_love"),
                "whisper_love"
        );
        ADVANCEMENT_EVENT_MAP.put(
                new ResourceLocation(MCARomanticExpansion.MODID, "gentle_kiss"),
                "gentle_kiss"
        );
        ADVANCEMENT_EVENT_MAP.put(
                new ResourceLocation(MCARomanticExpansion.MODID, "confession"),
                "confession"
        );
        ADVANCEMENT_EVENT_MAP.put(
                new ResourceLocation(MCARomanticExpansion.MODID, "hero_resque"),
                "hero_resque"
        );
        ADVANCEMENT_EVENT_MAP.put(
                new ResourceLocation(MCARomanticExpansion.MODID, "moonlight_serenade"),
                "moonlight_serenade"
        );
        ADVANCEMENT_EVENT_MAP.put(
                new ResourceLocation(MCARomanticExpansion.MODID, "starfall"),
                "starfall"
        );
    }

    // 1.20.1 中使用 AdvancementEvent 事件
    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 在 1.20.1 中，AdvancementEvent 使用 getAdvancement() 获取 Advancement
        Advancement advancement = event.getAdvancement();
        ResourceLocation advId = advancement.getId();

        MCARomanticExpansion.LOGGER.debug("Player {} earned advancement: {}",
                player.getName().getString(), advId);

        String eventId = ADVANCEMENT_EVENT_MAP.get(advId);
        if (eventId == null) {
            return;
        }

        MCARomanticExpansion.LOGGER.debug("🎉 Romantic advancement detected: {}, triggering event: {}",
                advId, eventId);

        ServerPlayer partner = findNearestPlayer(player);
        if (partner == null) {
            MCARomanticExpansion.LOGGER.warn("No partner found, using self as partner for testing");
            partner = player;
        }

        RomanticEventManager.triggerEventById(player, partner, eventId);

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§d§l💕 通过成就解锁了浪漫事件: " + eventId + "!"
                )
        );
    }

    private static ServerPlayer findNearestPlayer(ServerPlayer player) {
        return player.serverLevel().getPlayers(
                        p -> p != player && p.distanceTo(player) < 20
                ).stream()
                .min((p1, p2) -> Double.compare(p1.distanceTo(player), p2.distanceTo(player)))
                .orElse(null);
    }
}