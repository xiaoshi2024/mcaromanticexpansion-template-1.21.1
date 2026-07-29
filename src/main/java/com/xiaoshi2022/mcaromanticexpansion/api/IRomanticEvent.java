package com.xiaoshi2022.mcaromanticexpansion.api;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 自定义浪漫事件接口。
 * <p>
 * 其他模组可以实现此接口，并通过
 * {@link RomanticExpansionAPI#registerCustomEvent(IRomanticEvent)} 注册自己的浪漫事件，
 * 注册后事件会进入随机触发池，并可被手动触发。
 * <p>
 * <b>示例：</b>
 * <pre>{@code
 * public class StarlightDanceEvent implements IRomanticEvent {
 *     @Override public String id() { return "mymod:starlight_dance"; }
 *     @Override public double weight() { return 0.5; }
 *     @Override public int affectionBonus() { return 20; }
 *
 *     @Override
 *     public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
 *         // 播放星光粒子效果
 *         ServerLevel level = player.serverLevel();
 *         for (int i = 0; i < 100; i++) {
 *             double x = player.getX() + (Math.random() - 0.5) * 6;
 *             double y = player.getY() + 2 + Math.random() * 4;
 *             double z = player.getZ() + (Math.random() - 0.5) * 6;
 *             level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
 *         }
 *         player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600));
 *         partner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600));
 *     }
 *
 *     @Override public Component getPlayerMessage() {
 *         return Component.literal("你与伴侣在星光下翩翩起舞！");
 *     }
 *
 *     @Override public Component getPartnerMessage() {
 *         return Component.literal("你与伴侣在星光下翩翩起舞！");
 *     }
 * }
 *
 * // 在Mod构造函数中注册：
 * RomanticExpansionAPI.registerCustomEvent(new StarlightDanceEvent());
 * }</pre>
 */
public interface IRomanticEvent {

    /**
     * 事件唯一ID，建议使用 "modid:event_name" 格式以避免冲突。
     * 示例："mymod:starlight_dance"
     */
    String id();

    /**
     * 随机抽取权重，值越大越容易被抽到。
     * 参考内置事件：分享故事 1.0、轻吻 0.4、告白 0.2
     */
    double weight();

    /**
     * 触发时双方获得的好感度加成。
     * 参考内置事件：牵手 +8、告白 +25、彩虹契约 +15
     */
    int affectionBonus();

    /**
     * 触发事件的实际效果（粒子、药水效果、播放音效等）。
     * 此方法在服务端调用。
     */
    void triggerEffect(ServerPlayer player, ServerPlayer partner);

    /**
     * 发送给玩家A的消息。返回null则不发送。
     */
    default Component getPlayerMessage() {
        return null;
    }

    /**
     * 发送给玩家B的消息。返回null则不发送。
     */
    default Component getPartnerMessage() {
        return null;
    }
}
