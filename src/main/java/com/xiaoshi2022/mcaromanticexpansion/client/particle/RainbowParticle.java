package com.xiaoshi2022.mcaromanticexpansion.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class RainbowParticle extends TextureSheetParticle {

    // 彩虹7色 (RGB)
    private static final float[][] RAINBOW_COLORS = {
            {1.0f, 0.0f, 0.0f},     // 红
            {1.0f, 0.5f, 0.0f},     // 橙
            {1.0f, 1.0f, 0.0f},     // 黄
            {0.0f, 1.0f, 0.0f},     // 绿
            {0.0f, 0.5f, 1.0f},     // 青
            {0.29f, 0.0f, 0.51f},   // 靛
            {0.56f, 0.0f, 1.0f}     // 紫
    };

    // ========== 半圆参数 ==========
    private final SpriteSet spriteSet;
    private final int layerIndex;       // 0-6，彩虹层
    private final double angle;         // 0-π，在半圆上的角度位置
    private final double directionAngle; // 拱桥朝向（弧度）
    private final double centerX, centerY, centerZ; // 圆心位置
    private final double radius;        // 半圆半径
    private final double layerOffset;   // 每层半径偏移（形成厚度）

    // 动画参数
    private final float floatPhase;
    private final float floatSpeed;

    protected RainbowParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz, SpriteSet spriteSet) {
        super(level, x, y, z, 0, 0, 0);
        this.spriteSet = spriteSet;

        // 解析参数
        this.layerIndex = Mth.clamp((int)(vx * 7), 0, 6);
        this.angle = vy * Math.PI;           // 0~1 映射到 0~π（半圆）
        this.directionAngle = vz;

        // 圆心在传入位置下方（拱脚落地）
        this.radius = 4.0;
        this.layerOffset = this.layerIndex * 0.12;
        this.centerX = x;
        this.centerZ = z;
        this.centerY = y; // 圆心Y（拱脚高度）

        // ========== 粒子属性 ==========
        this.setSize(0.18f, 0.18f);
        this.lifetime = 120 + this.random.nextInt(40);
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.friction = 1.0f;

        // 颜色
        float[] color = RAINBOW_COLORS[this.layerIndex];
        this.rCol = color[0];
        this.gCol = color[1];
        this.bCol = color[2];

        // 外层粒子更大
        this.quadSize = 0.12f + (1.0f - this.layerIndex / 7.0f) * 0.12f;

        // 随机动画相位
        this.floatPhase = this.random.nextFloat() * (float)Math.PI * 2;
        this.floatSpeed = 0.04f + this.random.nextFloat() * 0.02f;

        this.pickSprite(spriteSet);
        updatePosition();
    }

    /**
     * 半圆公式：
     * x = r * cos(θ)
     * y = r * sin(θ)   （只取上半圆，θ: 0~π）
     */
    private void updatePosition() {
        // 当前层的实际半径
        double r = this.radius - this.layerOffset;

        // 半圆参数方程
        double localX = r * Math.cos(this.angle);   // 水平偏移
        double localY = r * Math.sin(this.angle);   // 高度（上半圆）

        // 应用方向旋转 + 偏移到圆心
        this.x = this.centerX + localX * Math.cos(this.directionAngle);
        this.z = this.centerZ + localX * Math.sin(this.directionAngle);
        this.y = this.centerY + localY;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive()) return;

        float progress = (float) this.age / this.lifetime;

        // ========== 呼吸浮动 ==========
        float time = this.age * this.floatSpeed + this.floatPhase;
        double floatX = Math.sin(time) * 0.012;
        double floatY = Math.sin(time * 1.2) * 0.008;
        double floatZ = Math.cos(time * 0.9) * 0.012;

        // 重新计算基础位置
        double r = this.radius - this.layerOffset;
        double localX = r * Math.cos(this.angle);
        double localY = r * Math.sin(this.angle);

        // 应用浮动偏移
        this.x = this.centerX + localX * Math.cos(this.directionAngle)
                + floatX * Math.cos(this.directionAngle) - floatZ * Math.sin(this.directionAngle);
        this.z = this.centerZ + localX * Math.sin(this.directionAngle)
                + floatX * Math.sin(this.directionAngle) + floatZ * Math.cos(this.directionAngle);
        this.y = this.centerY + localY + floatY;

        // ========== 透明度渐入渐出 ==========
        if (progress < 0.12f) {
            this.alpha = progress / 0.12f * 0.9f;
        } else if (progress > 0.85f) {
            this.alpha = 0.9f * (1.0f - (progress - 0.85f) / 0.15f);
        } else {
            this.alpha = 0.9f;
        }

        // ========== 大小脉动 ==========
        float pulse = 1.0f + 0.06f * Mth.sin(this.age * 0.08f + this.floatPhase);
        this.quadSize = (0.12f + (1.0f - this.layerIndex / 7.0f) * 0.12f) * pulse;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // ========== 工厂 ==========
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new RainbowParticle(level, x, y, z, vx, vy, vz, spriteSet);
        }
    }
}