package com.xiaoshi2022.mcaromanticexpansion.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class RainbowParticle extends SingleQuadParticle {

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
    private final int layerIndex;       // 0-6，彩虹层
    private final double angle;         // 0-π，在半圆上的角度位置
    private final double directionAngle; // 拱桥朝向（弧度）
    private final double centerX, centerY, centerZ; // 圆心位置
    private final double radius;        // 半圆半径
    private final double layerOffset;   // 每层半径偏移（形成厚度）

    // 动画参数
    private final float floatPhase;
    private final float floatSpeed;

    // 存储原始颜色
    private float colorR, colorG, colorB;

    protected RainbowParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz, SpriteSet spriteSet) {
        super(level, x, y, z, spriteSet.get(level.getRandom()));

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
        this.lifetime = 120 + level.getRandom().nextInt(40);
        this.gravity = 0.0f;
        this.hasPhysics = false;

        // 颜色
        float[] color = RAINBOW_COLORS[this.layerIndex];
        this.colorR = color[0];
        this.colorG = color[1];
        this.colorB = color[2];
        this.setColor(this.colorR, this.colorG, this.colorB);

        // 外层粒子更大
        this.quadSize = 0.12f + (1.0f - this.layerIndex / 7.0f) * 0.12f;

        // 随机动画相位
        this.floatPhase = level.getRandom().nextFloat() * (float)Math.PI * 2;
        this.floatSpeed = 0.04f + level.getRandom().nextFloat() * 0.02f;

        // 选择精灵
        this.sprite = spriteSet.get(level.getRandom());
        updatePosition();

        // 设置初始位置
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
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
        double newX = this.centerX + localX * Math.cos(this.directionAngle);
        double newZ = this.centerZ + localX * Math.sin(this.directionAngle);
        double newY = this.centerY + localY;

        this.setPos(newX, newY, newZ);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

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
        double newX = this.centerX + localX * Math.cos(this.directionAngle)
                + floatX * Math.cos(this.directionAngle) - floatZ * Math.sin(this.directionAngle);
        double newZ = this.centerZ + localX * Math.sin(this.directionAngle)
                + floatX * Math.sin(this.directionAngle) + floatZ * Math.cos(this.directionAngle);
        double newY = this.centerY + localY + floatY;

        this.setPos(newX, newY, newZ);

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
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {
        // 更新颜色（保持透明度变化）
        this.setColor(this.colorR, this.colorG, this.colorB);

        // 调用父类方法进行渲染
        super.extract(particleTypeRenderState, camera, partialTickTime);
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    @Override
    protected Layer getLayer() {
        // 使用透明粒子层
        return Layer.TRANSLUCENT;
    }

    // 重写获取光照的方法
    @Override
    protected int getLightCoords(float partialTick) {
        // 使用全亮光照
        return 15728880; // 15, 15 亮度
    }

    // ========== 工厂 ==========
    @OnlyIn(Dist.CLIENT)
    public static class Provider implements net.minecraft.client.particle.ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz,
                                       RandomSource random) {
            return new RainbowParticle(level, x, y, z, vx, vy, vz, spriteSet);
        }
    }
}