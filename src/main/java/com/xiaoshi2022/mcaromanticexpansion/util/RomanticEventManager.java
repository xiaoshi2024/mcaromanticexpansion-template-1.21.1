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
    
    // 记录玩家对之间的共伞事件触发状态
    private static final Map<String, UmbrellaEventState> umbrellaEventStates = new HashMap<>();

    public static void onPlayerTick(Player player) {
        // 移除共伞期间的定时事件检查，只保留共伞建立时的单次触发
        // 好感度只在触发成就时增加一次，不再持续增加
    }

    public static void onSharedUmbrellaEstablished(ServerPlayer player, ServerPlayer partner) {
        // 生成唯一的玩家对标识符
        String pairKey = getPlayerPairKey(player, partner);
        
        // 检查是否已经触发过事件，或者是否在下雨（允许重新触发）
        UmbrellaEventState umbrellaState = umbrellaEventStates.computeIfAbsent(pairKey, 
                k -> new UmbrellaEventState());
        
        boolean isRaining = player.level().isRaining();
        
        // 如果之前已经触发过事件且现在不在下雨，则不触发
        if (umbrellaState.eventTriggered && !isRaining) {
            MCARomanticExpansion.LOGGER.debug("Event already triggered for pair {} and {}, not raining, skipping",
                    player.getName().getString(), partner.getName().getString());
            return;
        }
        
        // 如果下雨，重置触发状态（允许重新触发）
        if (isRaining) {
            umbrellaState.eventTriggered = false;
        }
        
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
                    // 标记已经触发过事件
                    umbrellaState.eventTriggered = true;
                    umbrellaEventStates.put(pairKey, umbrellaState);
                    
                    // 设置冷却时间，防止立即触发下一个事件
                    RomanticEventState playerState = new RomanticEventState();
                    playerState.lastEventTime = EVENT_CHECK_INTERVAL;
                    eventStates.put(player.getUUID(), playerState);
                    
                    RomanticEventState partnerState = new RomanticEventState();
                    partnerState.lastEventTime = EVENT_CHECK_INTERVAL;
                    eventStates.put(partner.getUUID(), partnerState);
                    break;
                }
            }
        }
    }
    
    /**
     * 生成玩家对的唯一标识符（排序保证一致性）
     */
    private static String getPlayerPairKey(Player p1, Player p2) {
        UUID uuid1 = p1.getUUID();
        UUID uuid2 = p2.getUUID();
        if (uuid1.compareTo(uuid2) < 0) {
            return uuid1.toString() + ":" + uuid2.toString();
        } else {
            return uuid2.toString() + ":" + uuid1.toString();
        }
    }

    private static void checkAndTriggerEvent(ServerPlayer player) {
        Player partner = SharedUmbrellaManager.getSharedPartner(player);
        if (!(partner instanceof ServerPlayer serverPartner)) {
            MCARomanticExpansion.LOGGER.debug("Partner not found for {}", player.getName().getString());
            return;
        }

        // 确保同一时刻只有一方能触发事件（按UUID排序，只有UUID较小的玩家才能触发）
        if (player.getUUID().compareTo(serverPartner.getUUID()) > 0) {
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
        double randomValue = random.nextDouble(); // 只调用一次！
        if (randomValue > triggerChance) {
            MCARomanticExpansion.LOGGER.debug("Event check failed for {} (chance: {} > {})",
                    player.getName().getString(), randomValue, triggerChance);
            return;
        }

        double totalWeight = availableEvents.stream().mapToDouble(RomanticEvent::weight).sum();
        double eventRandomValue = random.nextDouble() * totalWeight; // 修改变量名避免冲突
        double accumulated = 0;

        MCARomanticExpansion.LOGGER.debug("Total weight: {}, random: {}", totalWeight, eventRandomValue);

        for (RomanticEvent event : availableEvents) {
            accumulated += event.weight();
            MCARomanticExpansion.LOGGER.debug("Checking event {}: accumulated={}, random={}",
                    event.id(), accumulated, eventRandomValue);
            if (eventRandomValue <= accumulated) {
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
        int timeSinceLastEvent = state.ticksInSharedUmbrella - state.lastEventTime;

        // 根据好感度等级开放不同事件
        if (affection >= 10 && timeSinceLastEvent >= 150) {
            events.add(RomanticEvent.SHARE_A_STORY);
        }
        if (affection >= 25 && timeSinceLastEvent >= 250) {
            events.add(RomanticEvent.HOLD_HANDS);
        }
        if (affection >= 40 && timeSinceLastEvent >= 350) {
            events.add(RomanticEvent.WHISPER_LOVE);
        }
        if (affection >= 60 && timeSinceLastEvent >= 450) {
            events.add(RomanticEvent.GENTLE_KISS);
        }
        if (affection >= 80 && timeSinceLastEvent >= 550) {
            events.add(RomanticEvent.CONFESSION);
        }
        
        // 满好感度时（100），所有事件都可用
        if (affection >= 100) {
            events.add(RomanticEvent.CONFESSION);
            if (!events.contains(RomanticEvent.GENTLE_KISS)) events.add(RomanticEvent.GENTLE_KISS);
            if (!events.contains(RomanticEvent.WHISPER_LOVE)) events.add(RomanticEvent.WHISPER_LOVE);
            if (!events.contains(RomanticEvent.HOLD_HANDS)) events.add(RomanticEvent.HOLD_HANDS);
            if (!events.contains(RomanticEvent.SHARE_A_STORY)) events.add(RomanticEvent.SHARE_A_STORY);
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
        
        // 发送翻译后的消息给玩家
        player.sendSystemMessage(event.getPlayerMessage());
        partner.sendSystemMessage(event.getPartnerMessage());
        
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
        SHARE_A_STORY("share_story", 1.0, 5) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
        },
        
        HOLD_HANDS("hold_hands", 0.8, 8) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
        },
        
        WHISPER_LOVE("whisper_love", 0.6, 12) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
        },
        
        GENTLE_KISS("gentle_kiss", 0.4, 18) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
        },
        
        CONFESSION("confession", 0.2, 25) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
        },
        
        HEART_TO_HEART("heart_to_heart", 1.0, 10) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 6000, 1));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 1));
                partner.addEffect(new MobEffectInstance(MobEffects.LUCK, 6000, 1));
                partner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 1));
            }
        },
        
        RAINBOW_PACT("rainbow_pact", 0.8, 15) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                ServerLevel level = player.serverLevel();
                double centerX = (player.getX() + partner.getX()) / 2;
                double centerY = Math.max(player.getY(), partner.getY()) + 5;
                double centerZ = (player.getZ() + partner.getZ()) / 2;
                
                for (int i = 0; i < 50; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = 2 + random.nextDouble() * 3;
                    double x = centerX + Math.cos(angle) * radius;
                    double z = centerZ + Math.sin(angle) * radius;
                    double y = centerY - random.nextDouble() * 8;
                    
                    level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 
                            random.nextGaussian() * 0.1, 
                            random.nextGaussian() * 0.1, 
                            random.nextGaussian() * 0.1, 0.05);
                    
                    if (i % 5 == 0) {
                        level.sendParticles(ParticleTypes.HEART, x, y + 1, z, 1, 
                                random.nextGaussian() * 0.2, 
                                random.nextDouble() * 0.5, 
                                random.nextGaussian() * 0.2, 0.1);
                    }
                }
                
                player.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
            }
        },
        
        TIME_STANDS_STILL("time_stands_still", 0.6, 12) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
        },
        
        SYNCHRONY_TEST("synchrony_test", 0.4, 20) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
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

        RomanticEvent(String id, double weight, int affectionBonus) {
            this.id = id;
            this.weight = weight;
            this.affectionBonus = affectionBonus;
        }

        public String id() { return id; }
        public double weight() { return weight; }
        public int affectionBonus() { return affectionBonus; }
        
        public Component getPlayerMessage() {
            return Component.translatable("event.mcaromanticexpansion." + id + ".player");
        }
        
        public Component getPartnerMessage() {
            return Component.translatable("event.mcaromanticexpansion." + id + ".partner");
        }
        
        public void triggerEffect(ServerPlayer player, ServerPlayer partner) {}
    }

    private static class RomanticEventState {
        int ticksInSharedUmbrella = 0;
        String lastTriggeredEvent = "";
        int lastEventTime = 0;
        boolean synchronyTestActive = false;
        long synchronyTestStartTime = 0;
    }
    
    /**
     * 共伞事件状态类，记录玩家对之间的事件触发状态
     */
    private static class UmbrellaEventState {
        boolean eventTriggered = false;
    }
}
