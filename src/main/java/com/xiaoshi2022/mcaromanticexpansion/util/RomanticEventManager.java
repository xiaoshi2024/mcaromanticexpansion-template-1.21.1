package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.advancement.ModAdvancements;
import com.xiaoshi2022.mcaromanticexpansion.api.IRomanticEvent;
import com.xiaoshi2022.mcaromanticexpansion.api.RomanticExpansionAPI;
import com.xiaoshi2022.mcaromanticexpansion.api.event.AffectionChangedEvent;
import com.xiaoshi2022.mcaromanticexpansion.api.event.RomanticEventTriggeredEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;

public class RomanticEventManager {
    private static final Map<UUID, RomanticEventState> eventStates = new HashMap<>();
    private static final int EVENT_CHECK_INTERVAL = 100;
    private static final Random random = new Random();
    private static final double EVENT_TRIGGER_CHANCE = 0.3;

    private static final Map<String, UmbrellaEventState> umbrellaEventStates = new HashMap<>();

    // 存储被英雄救美事件影响的僵尸
    private static final Map<UUID, List<Zombie>> heroZombies = new HashMap<>();

    public static void onPlayerTick(Player player) {
        // 每 tick 检查英雄救美事件是否应该清除僵尸
        if (player instanceof ServerPlayer serverPlayer) {
            UUID playerId = serverPlayer.getUUID();
            if (heroZombies.containsKey(playerId)) {
                List<Zombie> zombies = heroZombies.get(playerId);
                // 检查是否所有僵尸都死了
                boolean allDead = zombies.stream().allMatch(zombie -> !zombie.isAlive());
                if (allDead) {
                    heroZombies.remove(playerId);
                    // 英雄救美成功！给予奖励
                    if (!zombies.isEmpty()) {
                        applyHeroReward(serverPlayer);
                    }
                }
            }
        }
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

            List<RomanticEvent> builtinEvents = Arrays.asList(
                    RomanticEvent.HEART_TO_HEART,
                    RomanticEvent.RAINBOW_PACT,
                    RomanticEvent.TIME_STANDS_STILL,
                    RomanticEvent.SYNCHRONY_TEST,
                    RomanticEvent.HERO_RESQUE,
                    RomanticEvent.MOONLIGHT_SERENADE,
                    RomanticEvent.STARFALL
            );

            Collection<IRomanticEvent> customEvents = RomanticExpansionAPI.getCustomEvents();
            double builtinWeight = builtinEvents.stream().mapToDouble(RomanticEvent::weight).sum();
            double customWeight = customEvents.stream().mapToDouble(IRomanticEvent::weight).sum();

            double totalWeight = builtinWeight + customWeight;
            double randomValue = random.nextDouble() * totalWeight;
            double accumulated = 0;

            boolean triggered = false;

            for (RomanticEvent event : builtinEvents) {
                accumulated += event.weight();
                if (randomValue <= accumulated) {
                    triggerEvent(event, player, partner);
                    triggered = true;
                    break;
                }
            }

            if (!triggered && !customEvents.isEmpty()) {
                double remaining = randomValue - builtinWeight;
                double acc = 0;
                for (IRomanticEvent ev : customEvents) {
                    acc += ev.weight();
                    if (remaining <= acc) {
                        RomanticExpansionAPI.triggerRomanticEvent(player, partner, ev.id());
                        triggered = true;
                        break;
                    }
                }
            }

            if (triggered) {
                umbrellaState.eventTriggered = true;
                umbrellaEventStates.put(pairKey, umbrellaState);

                RomanticEventState playerState = new RomanticEventState();
                playerState.lastEventTime = EVENT_CHECK_INTERVAL;
                eventStates.put(player.getUUID(), playerState);

                RomanticEventState partnerState = new RomanticEventState();
                partnerState.lastEventTime = EVENT_CHECK_INTERVAL;
                eventStates.put(partner.getUUID(), partnerState);
            }
        }
    }

    public static void triggerEventById(ServerPlayer player, ServerPlayer partner, String eventId) {
        for (RomanticEvent event : RomanticEvent.values()) {
            if (event.id().equals(eventId)) {
                triggerEvent(event, player, partner);
                MCARomanticExpansion.LOGGER.debug("✅ Manually triggered event: {} for {} and {}",
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

        RomanticEventTriggeredEvent forgeEvent = new RomanticEventTriggeredEvent(
                player, partner, event.id(), false, event.affectionBonus()
        );

        // 【Forge 移植】使用 MinecraftForge.EVENT_BUS.post(event)
        // Forge 中 post 返回 true 表示事件被取消
        if (MinecraftForge.EVENT_BUS.post(forgeEvent)) {
            MCARomanticExpansion.LOGGER.debug("Romantic event {} canceled by event listener", event.id());
            return;
        }

        triggerEventAchievement(player, event.id());
        triggerEventAchievement(partner, event.id());

        event.triggerEffect(player, partner);

        if (event.getPlayerMessage() != null) {
            player.sendSystemMessage(event.getPlayerMessage());
        }
        if (event.getPartnerMessage() != null) {
            partner.sendSystemMessage(event.getPartnerMessage());
        }

        addRomanticEventAffection(player, partner, event.affectionBonus());
        addRomanticEventAffection(partner, player, event.affectionBonus());
        MCARomanticExpansion.LOGGER.debug("=== Added {} affection bonus for both players ===", event.affectionBonus());
    }

    private static void addRomanticEventAffection(ServerPlayer player, ServerPlayer target, int amount) {
        CompoundTag persistentData = player.getPersistentData();
        int current = getAffectionFromNBTStatic(persistentData, target.getUUID());
        int newValue = current + amount;
        if (newValue < -100) newValue = -100;

        AffectionChangedEvent event = new AffectionChangedEvent(
                player, target, current, newValue, AffectionChangedEvent.ChangeReason.ROMANTIC_EVENT
        );

        if (MinecraftForge.EVENT_BUS.post(event)) {
            return;
        }
        int finalValue = event.getNewValue();
        setAffectionToNBTStatic(persistentData, target.getUUID(), finalValue);
        sendAffectionSyncStatic(player, target.getUUID(), finalValue);
    }

    private static int getAffectionFromNBTStatic(CompoundTag tag, UUID targetUUID) {
        String AFFECTION_TAG = "RomanticAffection";
        String TARGET_UUID_TAG = "TargetUUID";
        String AFFECTION_VALUE_TAG = "AffectionValue";
        ListTag affectionList = tag.getList(AFFECTION_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < affectionList.size(); i++) {
            CompoundTag entry = affectionList.getCompound(i);
            if (entry.getString(TARGET_UUID_TAG).equals(targetUUID.toString())) {
                return entry.getInt(AFFECTION_VALUE_TAG);
            }
        }
        return 0;
    }

    private static void setAffectionToNBTStatic(CompoundTag tag, UUID targetUUID, int value) {
        String AFFECTION_TAG = "RomanticAffection";
        String TARGET_UUID_TAG = "TargetUUID";
        String AFFECTION_VALUE_TAG = "AffectionValue";
        String LAST_INTERACTION_TAG = "LastInteractionTime";
        ListTag affectionList = tag.getList(AFFECTION_TAG, Tag.TAG_COMPOUND);
        boolean found = false;
        for (int i = 0; i < affectionList.size(); i++) {
            CompoundTag entry = affectionList.getCompound(i);
            if (entry.getString(TARGET_UUID_TAG).equals(targetUUID.toString())) {
                entry.putInt(AFFECTION_VALUE_TAG, value);
                entry.putLong(LAST_INTERACTION_TAG, System.currentTimeMillis());
                found = true;
                break;
            }
        }
        if (!found) {
            CompoundTag newEntry = new CompoundTag();
            newEntry.putString(TARGET_UUID_TAG, targetUUID.toString());
            newEntry.putInt(AFFECTION_VALUE_TAG, value);
            newEntry.putLong(LAST_INTERACTION_TAG, System.currentTimeMillis());
            affectionList.add(newEntry);
        }
        tag.put(AFFECTION_TAG, affectionList);
    }

    private static void sendAffectionSyncStatic(ServerPlayer player, UUID targetUUID, int affection) {
        try {
            Class<?> packetClass = Class.forName("com.xiaoshi2022.mcaromanticexpansion.network.AffectionSyncPacket");
            java.lang.reflect.Method sendMethod = packetClass.getMethod("sendToClient", ServerPlayer.class, UUID.class, int.class);
            sendMethod.invoke(null, player, targetUUID, affection);
        } catch (Exception ignored) {
        }
    }

    // 【修复】使用 ModAdvancements 替代 CriterionTriggerRegister
    private static void triggerEventAchievement(ServerPlayer player, String eventId) {
        ModAdvancements.triggerRomanticEvent(player, eventId);
    }

    // ========== 英雄救美奖励 ==========
    private static void applyHeroReward(ServerPlayer player) {
        // 给予生命恢复和饱和效果
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2)); // 30秒生命恢复II
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1)); // 30秒抗性提升
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 600, 1)); // 30秒饱和

        // 播放胜利音效
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);

        MCARomanticExpansion.LOGGER.info("🎉 Hero reward applied to {}", player.getName().getString());
    }

    public enum RomanticEvent {
        // ===== 原有事件 =====
        SHARE_A_STORY("share_story", 1.0, 5) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                ServerLevel level = player.serverLevel();
                double centerX = (player.getX() + partner.getX()) / 2;
                double centerZ = (player.getZ() + partner.getZ()) / 2;
                double centerY = Math.max(player.getY(), partner.getY()) + 1;

                for (int i = 0; i < 30; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = 1.0 + random.nextDouble() * 2;
                    level.sendParticles(ParticleTypes.ENCHANT,
                            centerX + Math.cos(angle) * radius,
                            centerY + random.nextDouble() * 2,
                            centerZ + Math.sin(angle) * radius,
                            1, 0, 0.1, 0, 0.1);
                }
            }
        },

        HOLD_HANDS("hold_hands", 0.8, 8) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                ServerLevel level = player.serverLevel();
                for (int i = 0; i < 20; i++) {
                    double t = (double) i / 20;
                    double x = player.getX() + (partner.getX() - player.getX()) * t;
                    double y = player.getY() + 1.5 + (partner.getY() - player.getY()) * t;
                    double z = player.getZ() + (partner.getZ() - player.getZ()) * t;
                    level.sendParticles(ParticleTypes.HEART, x, y + random.nextDouble() * 0.5, z, 1, 0, 0.05, 0, 0);
                }
            }
        },

        WHISPER_LOVE("whisper_love", 0.6, 12) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                ServerLevel level = player.serverLevel();
                for (int i = 0; i < 15; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = 0.5 + random.nextDouble() * 1.5;
                    double x = player.getX() + Math.cos(angle) * radius;
                    double z = player.getZ() + Math.sin(angle) * radius;
                    level.sendParticles(ParticleTypes.NOTE,
                            x, player.getY() + 1.5 + random.nextDouble() * 0.5, z,
                            1, random.nextDouble() * 2, 0, 0, 1.0);
                }
            }
        },

        GENTLE_KISS("gentle_kiss", 0.4, 18) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                ServerLevel level = player.serverLevel();
                double centerX = (player.getX() + partner.getX()) / 2;
                double centerZ = (player.getZ() + partner.getZ()) / 2;
                double centerY = Math.max(player.getY(), partner.getY()) + 1.5;

                for (int i = 0; i < 40; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = 0.5 + random.nextDouble() * 3;
                    level.sendParticles(ParticleTypes.HEART,
                            centerX + Math.cos(angle) * radius,
                            centerY + random.nextDouble() * 2 - 1,
                            centerZ + Math.sin(angle) * radius,
                            1, 0, 0.1, 0, 0.1);
                }
                level.playSound(null, centerX, centerY, centerZ,
                        SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.5f, 1.5f);
            }
        },

        CONFESSION("confession", 0.2, 25) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                ServerLevel level = player.serverLevel();
                double centerX = (player.getX() + partner.getX()) / 2;
                double centerZ = (player.getZ() + partner.getZ()) / 2;
                double centerY = Math.max(player.getY(), partner.getY()) + 1;

                for (int i = 0; i < 80; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = 0.5 + random.nextDouble() * 4;
                    level.sendParticles(ParticleTypes.HEART,
                            centerX + Math.cos(angle) * radius,
                            centerY + random.nextDouble() * 3 - 1.5,
                            centerZ + Math.sin(angle) * radius,
                            1, 0, 0.2, 0, 0.1);
                }

                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 6000, 1));
                partner.addEffect(new MobEffectInstance(MobEffects.LUCK, 6000, 1));

                level.playSound(null, centerX, centerY, centerZ,
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
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
                boolean isSinglePlayer = (partner == null || partner == player);

                double centerX, centerZ, baseY;
                double distance = 3.0;

                if (isSinglePlayer) {
                    centerX = player.getX();
                    centerZ = player.getZ();
                    baseY = player.getY();
                    double randomAngle = random.nextDouble() * Math.PI * 2;
                    ParticleEffectHelper.spawnRainbowArch(level, centerX, centerZ, baseY, distance, randomAngle);
                    ParticleEffectHelper.spawnHeartCircles(level, centerX, centerZ, baseY, 50, 1.5, 3.5);
                } else {
                    centerX = (player.getX() + partner.getX()) / 2;
                    centerZ = (player.getZ() + partner.getZ()) / 2;
                    baseY = Math.max(player.getY(), partner.getY());
                    distance = player.distanceTo(partner);
                    double directionAngle = Math.atan2(
                            partner.getZ() - player.getZ(),
                            partner.getX() - player.getX()
                    );

                    ParticleEffectHelper.spawnRainbowArch(level, centerX, centerZ, baseY, distance, directionAngle);
                    ParticleEffectHelper.spawnHeartCircles(level, centerX, centerZ, baseY, 50, 1.5, 3.5);

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

                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 400, 1));
                partner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
                partner.addEffect(new MobEffectInstance(MobEffects.SATURATION, 400, 1));

                ParticleEffectHelper.playRomanticSounds(level, centerX, baseY + 2, centerZ);
                MCARomanticExpansion.LOGGER.debug("✅ Rainbow arch + Regen/Saturation buffs applied!");
            }
        },

        TIME_STANDS_STILL("time_stands_still", 0.6, 12) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                ServerLevel level = player.serverLevel();
                double centerX = (player.getX() + partner.getX()) / 2;
                double centerZ = (player.getZ() + partner.getZ()) / 2;
                double centerY = Math.max(player.getY(), partner.getY()) + 2;

                for (int i = 0; i < 60; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = 1.0 + random.nextDouble() * 3;
                    double x = centerX + Math.cos(angle) * radius;
                    double z = centerZ + Math.sin(angle) * radius;
                    level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                            x, centerY + random.nextDouble() * 2 - 1, z,
                            1, 0, 0.1, 0, 0.1);
                }
                level.playSound(null, centerX, centerY, centerZ,
                        SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
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

                ServerLevel level = player.serverLevel();
                for (int i = 0; i < 30; i++) {
                    double t = (double) i / 30;
                    double x = player.getX() + (partner.getX() - player.getX()) * t;
                    double y = player.getY() + 1.5 + (partner.getY() - player.getY()) * t;
                    double z = player.getZ() + (partner.getZ() - player.getZ()) * t;
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            x, y + random.nextDouble() * 0.5, z,
                            1, 0, 0.1, 0, 0.1);
                }
            }
        },

        HERO_RESQUE("hero_resque", 0.3, 30) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                ServerLevel level = player.serverLevel();
                UUID playerId = player.getUUID();

                if (heroZombies.containsKey(playerId)) {
                    heroZombies.get(playerId).forEach(zombie -> zombie.remove(Entity.RemovalReason.DISCARDED));
                    heroZombies.remove(playerId);
                }

                List<Zombie> spawnedZombies = new ArrayList<>();
                int zombieCount = 5 + random.nextInt(3);
                double radius = 8 + random.nextDouble() * 4;

                player.sendSystemMessage(Component.translatable("event.mcaromanticexpansion.hero_resque.player"));
                partner.sendSystemMessage(Component.translatable("event.mcaromanticexpansion.hero_resque.partner"));

                level.playSound(null, player.blockPosition(),
                        SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 0.5f, 0.5f);

                for (int i = 0; i < zombieCount; i++) {
                    double angle = (i / (double) zombieCount) * Math.PI * 2 + random.nextDouble() * 0.5;
                    double x = player.getX() + Math.cos(angle) * radius;
                    double z = player.getZ() + Math.sin(angle) * radius;

                    double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, (int)x, (int)z);

                    Zombie zombie = new Zombie(EntityType.ZOMBIE, level);
                    zombie.setPos(x, y, z);
                    zombie.setTarget(player);
                    zombie.setAggressive(true);
                    zombie.setBaby(false);
                    zombie.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));

                    level.addFreshEntity(zombie);
                    spawnedZombies.add(zombie);

                    level.sendParticles(ParticleTypes.POOF, x, y + 1, z, 10, 0.5, 0.5, 0.5, 0.1);
                }

                heroZombies.put(playerId, spawnedZombies);

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (heroZombies.containsKey(playerId)) {
                            List<Zombie> remaining = heroZombies.get(playerId);
                            if (!remaining.isEmpty()) {
                                remaining.forEach(zombie -> {
                                    if (zombie.isAlive()) {
                                        zombie.remove(Entity.RemovalReason.DISCARDED);
                                    }
                                });
                                heroZombies.remove(playerId);
                            }
                        }
                    }
                }, 30000);

                MCARomanticExpansion.LOGGER.info("⚔️ Hero Resque triggered! {} zombies spawned around {}", zombieCount, player.getName().getString());
            }
        },

        MOONLIGHT_SERENADE("moonlight_serenade", 0.3, 20) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                ServerLevel level = player.serverLevel();
                double centerX = (player.getX() + partner.getX()) / 2;
                double centerZ = (player.getZ() + partner.getZ()) / 2;
                double centerY = Math.max(player.getY(), partner.getY());

                if (level.isDay()) {
                    level.setDayTime(14000);
                }

                for (int i = 0; i < 100; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = 2 + random.nextDouble() * 5;
                    double x = centerX + Math.cos(angle) * radius;
                    double z = centerZ + Math.sin(angle) * radius;
                    double y = centerY + random.nextDouble() * 4;
                    level.sendParticles(ParticleTypes.END_ROD,
                            x, y, z, 1, 0, 0.01, 0, 0.05);
                }

                for (int i = 0; i < 20; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = 1.0 + random.nextDouble() * 3;
                    double x = centerX + Math.cos(angle) * radius;
                    double z = centerZ + Math.sin(angle) * radius;
                    double y = centerY + 0.5 + random.nextDouble() * 3;
                    level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                            x, y, z, 1, 0, 0.02, 0, 0.02);
                }

                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0));
                partner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0));

                MCARomanticExpansion.LOGGER.info("🌙 Moonlight Serenade triggered!");
            }
        },

        STARFALL("starfall", 0.25, 25) {
            @Override
            public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
                ServerLevel level = player.serverLevel();
                double centerX = (player.getX() + partner.getX()) / 2;
                double centerZ = (player.getZ() + partner.getZ()) / 2;
                double centerY = Math.max(player.getY(), partner.getY()) + 10;

                for (int i = 0; i < 20; i++) {
                    double startX = centerX + (random.nextDouble() - 0.5) * 20;
                    double startZ = centerZ + (random.nextDouble() - 0.5) * 20;
                    double endX = startX + (random.nextDouble() - 0.5) * 6;
                    double endZ = startZ + (random.nextDouble() - 0.5) * 6;

                    for (int step = 0; step < 30; step++) {
                        double t = (double) step / 30;
                        double x = startX + (endX - startX) * t;
                        double z = startZ + (endZ - startZ) * t;
                        double y = centerY - t * 8;

                        level.sendParticles(ParticleTypes.FIREWORK,
                                x, y, z, 0, 0, 0, 0, 0);
                        level.sendParticles(ParticleTypes.GLOW,
                                x, y, z, 1, 0, 0, 0, 0.1);
                    }

                    level.sendParticles(ParticleTypes.END_ROD,
                            endX, centerY - 8, endZ, 5, 0.5, 0.5, 0.5, 0.1);
                }

                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 1));
                partner.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 1));

                MCARomanticExpansion.LOGGER.info("🌟 Starfall triggered!");
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