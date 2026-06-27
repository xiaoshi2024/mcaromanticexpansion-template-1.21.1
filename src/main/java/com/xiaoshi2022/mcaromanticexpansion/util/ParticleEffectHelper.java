package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.registry.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.Random;

public class ParticleEffectHelper {
    private static final Random random = new Random();

    /**
     * 生成半圆彩虹拱桥
     */
    public static void spawnRainbowArch(ServerLevel level,
                                        double centerX, double centerZ, double baseY,
                                        double distance, double directionAngle) {
        double radius = Math.min(distance * 0.5 + 1.5, 5.0);
        int particlesPerLayer = 50 + (int)(distance * 8);
        int layers = 7;

        for (int layer = 0; layer < layers; layer++) {
            double layerRadius = radius - layer * 0.15;

            for (int i = 0; i <= particlesPerLayer; i++) {
                double theta = Math.PI * i / particlesPerLayer;
                double localX = layerRadius * Math.cos(theta);
                double localY = layerRadius * Math.sin(theta);

                double x = centerX + localX * Math.cos(directionAngle);
                double z = centerZ + localX * Math.sin(directionAngle);
                double y = baseY + localY;

                double spread = 0.1 + layer * 0.03;
                x += (random.nextDouble() - 0.5) * spread;
                z += (random.nextDouble() - 0.5) * spread;
                y += (random.nextDouble() - 0.5) * spread * 0.3;

                level.sendParticles(
                        ModParticles.RAINBOW_PARTICLE.get(),
                        x, y, z,
                        1,
                        layer / 7.0, theta / Math.PI, directionAngle,
                        0.1
                );
            }
        }
    }

    /**
     * 生成环绕心形粒子
     */
    public static void spawnHeartCircles(ServerLevel level,
                                         double centerX, double centerZ, double baseY,
                                         int count, double minRadius, double maxRadius) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = minRadius + random.nextDouble() * (maxRadius - minRadius);
            level.sendParticles(
                    ParticleTypes.HEART,
                    centerX + Math.cos(angle) * r,
                    baseY + 0.5 + random.nextDouble() * 1.5,
                    centerZ + Math.sin(angle) * r,
                    1, 0, 0.05, 0, 0.1
            );
        }
    }

    /**
     * 生成拱顶星星特效
     */
    public static void spawnTopStars(ServerLevel level,
                                     double centerX, double centerY, double centerZ,
                                     int count) {
        for (int i = 0; i < count; i++) {
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    centerX + (random.nextDouble() - 0.5) * 0.6,
                    centerY + (random.nextDouble() - 0.5) * 0.4,
                    centerZ + (random.nextDouble() - 0.5) * 0.6,
                    1, 0, 0.02, 0, 0.05
            );
        }
    }

    /**
     * 播放浪漫音效
     */
    public static void playRomanticSounds(ServerLevel level,
                                          double x, double y, double z) {
        level.playSound(null, x, y, z,
                SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.PLAYERS,
                2.0f, 1.0f);
        level.playSound(null, x, y, z,
                SoundEvents.FIREWORK_ROCKET_TWINKLE,
                SoundSource.PLAYERS,
                1.5f, 1.2f);
    }
}