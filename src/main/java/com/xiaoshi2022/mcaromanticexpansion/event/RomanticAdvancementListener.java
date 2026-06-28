package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.RomanticEventManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

import java.util.HashMap;
import java.util.Map;

public class RomanticAdvancementListener {

    // 成就ID -> 对应的事件ID 映射表
    private static final Map<ResourceLocation, String> ADVANCEMENT_EVENT_MAP = new HashMap<>();

    static {
        // 浪漫成就映射
        ADVANCEMENT_EVENT_MAP.put(
                ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "rainbow_pact"),
                "rainbow_pact"
        );
        ADVANCEMENT_EVENT_MAP.put(
                ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "time_stands_still"),
                "time_stands_still"
        );
        ADVANCEMENT_EVENT_MAP.put(
                ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "heart_to_heart"),
                "heart_to_heart"
        );
        ADVANCEMENT_EVENT_MAP.put(
                ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "synchrony_test"),
                "synchrony_test"
        );
        ADVANCEMENT_EVENT_MAP.put(
                ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "share_story"),
                "share_story"
        );
        ADVANCEMENT_EVENT_MAP.put(
                ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "hold_hands"),
                "hold_hands"
        );
        ADVANCEMENT_EVENT_MAP.put(
                ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "whisper_love"),
                "whisper_love"
        );
        ADVANCEMENT_EVENT_MAP.put(
                ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "gentle_kiss"),
                "gentle_kiss"
        );
        ADVANCEMENT_EVENT_MAP.put(
                ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "confession"),
                "confession"
        );

        // ========== 新增强化事件 ==========
        ADVANCEMENT_EVENT_MAP.put(
                ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "hero_resque"),
                "hero_resque"
        );
        ADVANCEMENT_EVENT_MAP.put(
                ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "moonlight_serenade"),
                "moonlight_serenade"
        );
        ADVANCEMENT_EVENT_MAP.put(
                ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "starfall"),
                "starfall"
        );
    }

    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        AdvancementHolder advancementHolder = event.getAdvancement();
        ResourceLocation advId = advancementHolder.id();

        MCARomanticExpansion.LOGGER.debug("Player {} earned advancement: {}",
                player.getName().getString(), advId);

        String eventId = ADVANCEMENT_EVENT_MAP.get(advId);
        if (eventId == null) {
            return;
        }

        MCARomanticExpansion.LOGGER.debug("🎉 Romantic advancement detected: {}, triggering event: {}",
                advId, eventId);

        // ========== 修改：找不到伴侣时，用自己作为伴侣 ==========
        ServerPlayer partner = findNearestPlayer(player);
        if (partner == null) {
            MCARomanticExpansion.LOGGER.warn("No partner found, using self as partner for testing");
            partner = player;  // 自己作为伴侣（仅测试用）
        }

        RomanticEventManager.triggerEventById(player, partner, eventId);

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "§d§l💕 通过成就解锁了浪漫事件: " + eventId + "!"
                )
        );
    }

    private static ServerPlayer findNearestPlayer(ServerPlayer player) {
        // 修复3: 使用 getPlayers 的 Predicate 版本
        return player.serverLevel().getPlayers(
                        p -> p != player && p.distanceTo(player) < 20
                ).stream()
                .min((p1, p2) -> Double.compare(p1.distanceTo(player), p2.distanceTo(player)))
                .orElse(null);
    }
}