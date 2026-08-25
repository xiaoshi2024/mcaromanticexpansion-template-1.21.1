package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.RomanticEventManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

import java.util.HashMap;
import java.util.Map;

public class RomanticAdvancementListener {

    private static final Map<Identifier, String> ADVANCEMENT_EVENT_MAP = new HashMap<>();

    static {
        ADVANCEMENT_EVENT_MAP.put(
                Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "rainbow_pact"),
                "rainbow_pact"
        );
        ADVANCEMENT_EVENT_MAP.put(
                Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "time_stands_still"),
                "time_stands_still"
        );
        ADVANCEMENT_EVENT_MAP.put(
                Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "heart_to_heart"),
                "heart_to_heart"
        );
        ADVANCEMENT_EVENT_MAP.put(
                Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "synchrony_test"),
                "synchrony_test"
        );
        ADVANCEMENT_EVENT_MAP.put(
                Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "share_story"),
                "share_story"
        );
        ADVANCEMENT_EVENT_MAP.put(
                Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "hold_hands"),
                "hold_hands"
        );
        ADVANCEMENT_EVENT_MAP.put(
                Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "whisper_love"),
                "whisper_love"
        );
        ADVANCEMENT_EVENT_MAP.put(
                Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "gentle_kiss"),
                "gentle_kiss"
        );
        ADVANCEMENT_EVENT_MAP.put(
                Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "confession"),
                "confession"
        );
        ADVANCEMENT_EVENT_MAP.put(
                Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "hero_resque"),
                "hero_resque"
        );
        ADVANCEMENT_EVENT_MAP.put(
                Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "moonlight_serenade"),
                "moonlight_serenade"
        );
        ADVANCEMENT_EVENT_MAP.put(
                Identifier.fromNamespaceAndPath(MCARomanticExpansion.MODID, "starfall"),
                "starfall"
        );
    }

    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AdvancementHolder advancementHolder = event.getAdvancement();
        Identifier advId = advancementHolder.id();

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

        // 使用 sendSystemMessage 替代 displayClientMessage
        player.sendSystemMessage(
                net.minecraft.network.chat.Component.translatable(
                        "message.mcaromanticexpansion.advancement.unlocked_event", eventId
                )
        );
    }

    private static ServerPlayer findNearestPlayer(ServerPlayer player) {
        // serverLevel() 改为 level()
        return player.level().getPlayers(
                        p -> p != player && p.distanceTo(player) < 20
                ).stream()
                .min((p1, p2) -> Double.compare(p1.distanceTo(player), p2.distanceTo(player)))
                .orElse(null);
    }
}