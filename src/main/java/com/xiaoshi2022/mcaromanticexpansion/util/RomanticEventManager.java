package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class RomanticEventManager {
    private static final Map<UUID, RomanticEventState> eventStates = new HashMap<>();
    private static final int EVENT_CHECK_INTERVAL = 100;
    private static final Random random = new Random();
    private static final double EVENT_TRIGGER_CHANCE = 0.3;

    private static final Map<String, UmbrellaEventState> umbrellaEventStates = new HashMap<>();

    public static void onPlayerTick(Player player) {
    }

    public static void onSharedUmbrellaEstablished(ServerPlayer player, ServerPlayer partner) {
        String pairKey = getPlayerPairKey(player, partner);

        UmbrellaEventState umbrellaState = umbrellaEventStates.computeIfAbsent(pairKey,
                k -> new UmbrellaEventState());

        boolean isRaining = player.level().isRaining();

        if (umbrellaState.eventTriggered && !isRaining) {
            MCARomanticExpansion.LOGGER.debug("Event already triggered for pair {}, not raining, skipping",
                    player.getName().getString());
            return;
        }

        if (isRaining) {
            umbrellaState.eventTriggered = false;
        }

        if (random.nextDouble() < EVENT_TRIGGER_CHANCE) {
            MCARomanticExpansion.LOGGER.debug("Triggering instant event for shared umbrella between {} and {}",
                    player.getName().getString(), partner.getName().getString());

            List<RomanticEvent> instantEvents = Arrays.asList(
                    RomanticEvent.HEART_TO_HEART,
                    RomanticEvent.RAINBOW_PACTRAINBOW_PACT,
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
                    umbrellaState.eventTriggered = true;
                    umbrellaEventStates.put(pairKey, umbrellaState);

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

    public static void triggerEventById(ServerPlayer player, ServerPlayer partner, String eventId) {
        for (RomanticEvent event : RomanticEvent.values()) {
            if (event.id().equals(eventId)) {
                triggerEvent(event, player, partner);
                MCARomanticExpansion.LOGGER.info("✅ Manually triggered event: {} for {} and {}",
                        eventId, player.getName().getString(), partner.getName().getString());
                return;
            }
        }
        MCARomanticExpansion.LOGGER.warn("Unknown event ID: {}", eventId);
    }

    private static String getPlayerPairKey(Player p1, Player p2) {
        UUID uuid1 = p1.getUUID();
        UUID uuid2 = p2.getUUID();
        if (uuid1.compareTo(uuid2) < 0) {
            return uuid1.toString() + ":" + uuid2.toString();
        } else {
            return uuid2.toString() + ":" + uuid1.toString();
        }
    }

    public static void triggerEvent(RomanticEvent event, ServerPlayer player, ServerPlayer partner) {
        MCARomanticExpansion.LOGGER.debug("=== ROMANTIC EVENT TRIGGERED: {} between {} and {} ===",
                event.id(), player.getName().getString(), partner.getName().getString());

        triggerEventAchievement(player, event.id());
        triggerEventAchievement(partner, event.id());

        event.triggerEffect(player, partner);

        player.sendSystemMessage(event.getPlayerMessage());
        partner.sendSystemMessage(event.getPartnerMessage());

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

        RAINBOW_PACTRAINBOW_PACT("rainbow_pact", 0.8, 15) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                ServerLevel level = player.serverLevel();
                boolean isSinglePlayer = (partner == null || partner == player);

                double centerX, centerZ, baseY;
                double distance = 3.0;

                if (isSinglePlayer) {
                    // 单机模式
                    centerX = player.getX();
                    centerZ = player.getZ();
                    baseY = player.getY();
                    double randomAngle = random.nextDouble() * Math.PI * 2;

                    ParticleEffectHelper.spawnRainbowArch(level, centerX, centerZ, baseY, distance, randomAngle);
                    ParticleEffectHelper.spawnHeartCircles(level, centerX, centerZ, baseY, 50, 1.5, 3.5);

                } else {
                    // 多人模式
                    centerX = (player.getX() + partner.getX()) / 2;
                    centerZ = (player.getZ() + partner.getZ()) / 2;
                    baseY = Math.max(player.getY(), partner.getY());
                    distance = player.distanceTo(partner);
                    double directionAngle = Math.atan2(
                            partner.getZ() - player.getZ(),
                            partner.getX() - player.getX()
                    );

                    ParticleEffectHelper.spawnRainbowArch(level, centerX, centerZ, baseY, distance, directionAngle);

                    // 两端心形
                    double radius = Math.min(distance * 0.5 + 1.5, 5.0);
                    for (int side = 0; side < 2; side++) {
                        double theta = side == 0 ? 0 : Math.PI;
                        double localX = radius * Math.cos(theta);
                        double localY = radius * Math.sin(theta);

                        double x = centerX + localX * Math.cos(directionAngle);
                        double z = centerZ + localX * Math.sin(directionAngle);
                        double y = baseY + localY;

                        ParticleEffectHelper.spawnHeartCircles(level, x, z, y, 30, 0.3, 1.1);
                    }
                }

                // 播放音效
                ParticleEffectHelper.playRomanticSounds(level, centerX, baseY + 2, centerZ);

                MCARomanticExpansion.LOGGER.info("✅ Semicircle rainbow arch generated! (Single player mode: {})", isSinglePlayer);
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

    private static class UmbrellaEventState {
        boolean eventTriggered = false;
    }
}