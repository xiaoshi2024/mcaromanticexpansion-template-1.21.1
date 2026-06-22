package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class RomanticEventManager {
    private static final Map<UUID, RomanticEventState> eventStates = new HashMap<>();
    private static final int EVENT_CHECK_INTERVAL = 100;
    private static final Random random = new Random();
    private static final double EVENT_TRIGGER_CHANCE = 0.3; // 30%概率触发事件

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
            MCARomanticExpansion.LOGGER.debug("Checking romantic event for {} at tick {}",
                    player.getName().getString(), state.ticksInSharedUmbrella);
            checkAndTriggerEvent(serverPlayer);
        }
    }

    public static void onSharedUmbrellaEstablished(ServerPlayer player, ServerPlayer partner) {
        // 共伞建立时有30%概率触发即时事件
        if (random.nextDouble() < EVENT_TRIGGER_CHANCE) {
            MCARomanticExpansion.LOGGER.debug("Triggering instant event for shared umbrella between {} and {}",
                    player.getName().getString(), partner.getName().getString());
            
            List<RomanticEvent> instantEvents = Arrays.asList(
                    RomanticEvent.HEART_TO_HEART,
                    RomanticEvent.RAINBOW_PACT,
                    RomanticEvent.TIME_STANDS_STILL,
                    RomanticEvent.SYNCHRONY_TEST
            );
            
            double totalWeight = instantEvents.stream().mapToDouble(RomanticEvent::weight).sum();
            double randomValue = random.nextDouble() * totalWeight;
            double accumulated = 0;

            for (RomanticEvent event : instantEvents) {
                accumulated += event.weight();
                if (randomValue <= accumulated) {
                    triggerEvent(event, player, partner);
                    break;
                }
            }
        }
    }

    private static void checkAndTriggerEvent(ServerPlayer player) {
        Player partner = SharedUmbrellaManager.getSharedPartner(player);
        if (!(partner instanceof ServerPlayer serverPartner)) {
            MCARomanticExpansion.LOGGER.debug("Partner not found for {}", player.getName().getString());
            return;
        }

        int affection = AffectionManager.getAffection(player, partner);
        RomanticEventState state = eventStates.get(player.getUUID());
        if (state == null) {
            MCARomanticExpansion.LOGGER.debug("Event state not found for {}", player.getName().getString());
            return;
        }

        MCARomanticExpansion.LOGGER.debug("Player {} affection with {}: {}, ticks: {}, lastEventTime: {}",
                player.getName().getString(), partner.getName().getString(),
                affection, state.ticksInSharedUmbrella, state.lastEventTime);

        List<RomanticEvent> availableEvents = getAvailableEvents(affection, state);
        
        if (availableEvents.isEmpty()) {
            MCARomanticExpansion.LOGGER.debug("No available events for {} (affection: {})",
                    player.getName().getString(), affection);
            return;
        }

        MCARomanticExpansion.LOGGER.debug("Available events for {}: {}",
                player.getName().getString(), 
                availableEvents.stream().map(RomanticEvent::id).toList());

        // 【关键修改】30%概率触发事件，70%概率什么都不发生
        double triggerChance = 0.3; // 30%概率
        if (random.nextDouble() > triggerChance) {
            MCARomanticExpansion.LOGGER.debug("Event check failed for {} (chance: {} > {})",
                    player.getName().getString(), random.nextDouble(), triggerChance);
            return;
        }

        double totalWeight = availableEvents.stream().mapToDouble(RomanticEvent::weight).sum();
        double randomValue = random.nextDouble() * totalWeight;
        double accumulated = 0;

        MCARomanticExpansion.LOGGER.debug("Total weight: {}, random: {}", totalWeight, randomValue);

        for (RomanticEvent event : availableEvents) {
            accumulated += event.weight();
            MCARomanticExpansion.LOGGER.debug("Checking event {}: accumulated={}, random={}",
                    event.id(), accumulated, randomValue);
            if (randomValue <= accumulated) {
                triggerEvent(event, player, serverPartner);
                state.lastTriggeredEvent = event.id();
                state.lastEventTime = state.ticksInSharedUmbrella;
                MCARomanticExpansion.LOGGER.debug("Triggered event {} for {}", event.id(), player.getName().getString());
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
        MCARomanticExpansion.LOGGER.debug("=== ROMANTIC EVENT TRIGGERED: {} between {} and {} ===",
                event.id(), player.getName().getString(), partner.getName().getString());
        
        // 触发成就
        triggerEventAchievement(player, event.id());
        triggerEventAchievement(partner, event.id());
        
        // 触发特殊效果
        event.triggerEffect(player, partner);
        
        // 增加好感度
        AffectionManager.addAffection(player, partner, event.affectionBonus());
        AffectionManager.addAffection(partner, player, event.affectionBonus());
        MCARomanticExpansion.LOGGER.debug("=== Added {} affection bonus for both players ===", event.affectionBonus());
    }
    
    private static void triggerEventAchievement(ServerPlayer player, String eventId) {
        com.xiaoshi2022.mcaromanticexpansion.advancement.CriterionTriggerRegister.ROMANTIC_EVENT.get()
                .trigger(player, eventId);
    }

    public enum RomanticEvent {
        SHARE_A_STORY("share_story", 1.0, 5, 
                "§d你与对方分享了一个有趣的故事...",
                "§d对方与你分享了一个有趣的故事...") {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
        },
        
        HOLD_HANDS("hold_hands", 0.8, 8,
                "§d你们的手不经意间触碰到了一起，心跳加速...",
                "§d你们的手不经意间触碰到了一起，心跳加速...") {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
        },
        
        WHISPER_LOVE("whisper_love", 0.6, 12,
                "§d你轻声说出了藏在心底的话...",
                "§d对方轻声说出了藏在心底的话...") {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
        },
        
        GENTLE_KISS("gentle_kiss", 0.4, 18,
                "§d在伞下，你们交换了一个温柔的吻...",
                "§d在伞下，你们交换了一个温柔的吻...") {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
        },
        
        CONFESSION("confession", 0.2, 25,
                "§d你鼓起勇气表白了！",
                "§d对方鼓起勇气向你表白了！") {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
        },
        
        // 共伞专属事件
        HEART_TO_HEART("heart_to_heart", 1.0, 10,
                "§d心意相通：你们获得了幸运和速度的祝福！",
                "§d心意相通：你们获得了幸运和速度的祝福！") {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                // 双方获得5分钟的幸运和速度增益
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 6000, 1));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 1));
                partner.addEffect(new MobEffectInstance(MobEffects.LUCK, 6000, 1));
                partner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 1));
            }
        },
        
        RAINBOW_PACT("rainbow_pact", 0.8, 15,
                "§d彩虹之约：一道绚丽的彩虹出现在你们身边！",
                "§d彩虹之约：一道绚丽的彩虹出现在你们身边！") {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                // 在玩家附近生成彩虹粒子效果
                ServerLevel level = player.serverLevel();
                double centerX = (player.getX() + partner.getX()) / 2;
                double centerY = Math.max(player.getY(), partner.getY()) + 5;
                double centerZ = (player.getZ() + partner.getZ()) / 2;
                
                // 生成彩色粒子
                for (int i = 0; i < 50; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = 2 + random.nextDouble() * 3;
                    double x = centerX + Math.cos(angle) * radius;
                    double z = centerZ + Math.sin(angle) * radius;
                    double y = centerY - random.nextDouble() * 8;
                    
                    // 使用彩色火焰粒子
                    level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 
                            random.nextGaussian() * 0.1, 
                            random.nextGaussian() * 0.1, 
                            random.nextGaussian() * 0.1, 0.05);
                    
                    // 添加心形粒子
                    if (i % 5 == 0) {
                        level.sendParticles(ParticleTypes.HEART, x, y + 1, z, 1, 
                                random.nextGaussian() * 0.2, 
                                random.nextDouble() * 0.5, 
                                random.nextGaussian() * 0.2, 0.1);
                    }
                }
                
                // 播放音效
                player.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
            }
        },
        
        TIME_STANDS_STILL("time_stands_still", 0.6, 12,
                "§d时光留念：雨伞下，时间仿佛变慢了...",
                "§d时光留念：雨伞下，时间仿佛变慢了...") {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
        },
        
        SYNCHRONY_TEST("synchrony_test", 0.4, 20,
                "§d默契考验：在接下来的1分钟内，同时跳起！",
                "§d默契考验：在接下来的1分钟内，同时跳起！") {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                // 记录默契考验开始时间
                RomanticEventState state = eventStates.computeIfAbsent(player.getUUID(), 
                        uuid -> new RomanticEventState());
                state.synchronyTestActive = true;
                state.synchronyTestStartTime = player.serverLevel().getGameTime();
                
                RomanticEventState partnerState = eventStates.computeIfAbsent(partner.getUUID(), 
                        uuid -> new RomanticEventState());
                partnerState.synchronyTestActive = true;
                partnerState.synchronyTestStartTime = partner.serverLevel().getGameTime();
            }
        };

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
        
        public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
    }

    private static class RomanticEventState {
        int ticksInSharedUmbrella = 0;
        String lastTriggeredEvent = "";
        int lastEventTime = 0;
        boolean synchronyTestActive = false;
        long synchronyTestStartTime = 0;
    }
}
