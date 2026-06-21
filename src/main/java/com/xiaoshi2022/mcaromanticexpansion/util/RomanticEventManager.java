package com.xiaoshi2022.mcaromanticexpansion.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class RomanticEventManager {
    private static final Map<UUID, RomanticEventState> eventStates = new HashMap<>();
    private static final int EVENT_CHECK_INTERVAL = 100;
    private static final Random random = new Random();

    public static void onPlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        if (!SharedUmbrellaManager.isInSharedUmbrella(player)) {
            eventStates.remove(player.getUUID());
            return;
        }

        RomanticEventState state = eventStates.computeIfAbsent(player.getUUID(), 
                uuid -> new RomanticEventState());

        state.ticksInSharedUmbrella++;

        if (state.ticksInSharedUmbrella % EVENT_CHECK_INTERVAL == 0) {
            checkAndTriggerEvent(serverPlayer);
        }
    }

    private static void checkAndTriggerEvent(ServerPlayer player) {
        Player partner = SharedUmbrellaManager.getSharedPartner(player);
        if (!(partner instanceof ServerPlayer serverPartner)) return;

        int affection = AffectionManager.getAffection(player, partner);
        RomanticEventState state = eventStates.get(player.getUUID());
        if (state == null) return;

        List<RomanticEvent> availableEvents = getAvailableEvents(affection, state);
        
        if (availableEvents.isEmpty()) return;

        double totalWeight = availableEvents.stream().mapToDouble(RomanticEvent::weight).sum();
        double randomValue = random.nextDouble() * totalWeight;
        double accumulated = 0;

        for (RomanticEvent event : availableEvents) {
            accumulated += event.weight();
            if (randomValue <= accumulated) {
                triggerEvent(event, player, serverPartner);
                state.lastTriggeredEvent = event.id();
                state.lastEventTime = state.ticksInSharedUmbrella;
                break;
            }
        }
    }

    private static List<RomanticEvent> getAvailableEvents(int affection, RomanticEventState state) {
        List<RomanticEvent> events = new ArrayList<>();

        if (affection >= 10 && state.ticksInSharedUmbrella - state.lastEventTime > 200) {
            events.add(RomanticEvent.SHARE_A_STORY);
        }
        if (affection >= 25 && state.ticksInSharedUmbrella - state.lastEventTime > 300) {
            events.add(RomanticEvent.HOLD_HANDS);
        }
        if (affection >= 40 && state.ticksInSharedUmbrella - state.lastEventTime > 400) {
            events.add(RomanticEvent.WHISPER_LOVE);
        }
        if (affection >= 60 && state.ticksInSharedUmbrella - state.lastEventTime > 500) {
            events.add(RomanticEvent.GENTLE_KISS);
        }
        if (affection >= 80 && state.ticksInSharedUmbrella - state.lastEventTime > 600) {
            events.add(RomanticEvent.CONFESSION);
        }

        return events;
    }

    private static void triggerEvent(RomanticEvent event, ServerPlayer player, ServerPlayer partner) {
        player.sendSystemMessage(Component.literal(event.playerMessage()).withStyle(ChatFormatting.LIGHT_PURPLE));
        partner.sendSystemMessage(Component.literal(event.partnerMessage()).withStyle(ChatFormatting.LIGHT_PURPLE));
        
        AffectionManager.addAffection(player, partner, event.affectionBonus());
        AffectionManager.addAffection(partner, player, event.affectionBonus());
    }

    public enum RomanticEvent {
        SHARE_A_STORY("share_story", 1.0, 5, 
                "§d你与对方分享了一个有趣的故事...",
                "§d对方与你分享了一个有趣的故事..."),
        
        HOLD_HANDS("hold_hands", 0.8, 8,
                "§d你们的手不经意间触碰到了一起，心跳加速...",
                "§d你们的手不经意间触碰到了一起，心跳加速..."),
        
        WHISPER_LOVE("whisper_love", 0.6, 12,
                "§d你轻声说出了藏在心底的话...",
                "§d对方轻声说出了藏在心底的话..."),
        
        GENTLE_KISS("gentle_kiss", 0.4, 18,
                "§d在伞下，你们交换了一个温柔的吻...",
                "§d在伞下，你们交换了一个温柔的吻..."),
        
        CONFESSION("confession", 0.2, 25,
                "§d你鼓起勇气表白了！",
                "§d对方鼓起勇气向你表白了！");

        private final String id;
        private final double weight;
        private final int affectionBonus;
        private final String playerMessage;
        private final String partnerMessage;

        RomanticEvent(String id, double weight, int affectionBonus, String playerMessage, String partnerMessage) {
            this.id = id;
            this.weight = weight;
            this.affectionBonus = affectionBonus;
            this.playerMessage = playerMessage;
            this.partnerMessage = partnerMessage;
        }

        public String id() { return id; }
        public double weight() { return weight; }
        public int affectionBonus() { return affectionBonus; }
        public String playerMessage() { return playerMessage; }
        public String partnerMessage() { return partnerMessage; }
    }

    private static class RomanticEventState {
        int ticksInSharedUmbrella = 0;
        String lastTriggeredEvent = "";
        int lastEventTime = 0;
    }
}
