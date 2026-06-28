package com.xiaoshi2022.mcaromanticexpansion.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 好感度变化事件。
 * <p>
 * 当任意两位玩家之间的好感度数值发生变化时（增加、减少、设置），本事件会被发布到 NeoForge 事件总线。
 * 此事件在<b>服务端</b>触发。
 * <p>
 * 此事件是<b>可取消</b>的。如果取消，好感度数值不会被写入。
 */
public class AffectionChangedEvent extends PlayerEvent {

    private final ServerPlayer target;
    private final int oldValue;
    private final int newValue;
    private final ChangeReason reason;

    /**
     * @param player   主体玩家（对谁的好感度）
     * @param target   目标玩家（好感度指向谁）
     * @param oldValue 变化前的数值
     * @param newValue 变化后的数值
     * @param reason   变化原因
     */
    public AffectionChangedEvent(ServerPlayer player, ServerPlayer target,
                                 int oldValue, int newValue, ChangeReason reason) {
        super(player);
        this.target = target;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    /** 好感度持有者 */
    public ServerPlayer getAffectionOwner() {
        return getEntity();
    }

    /** 好感度指向的目标玩家 */
    public ServerPlayer getTarget() {
        return target;
    }

    public int getOldValue() {
        return oldValue;
    }

    public int getNewValue() {
        return newValue;
    }

    public ChangeReason getReason() {
        return reason;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    /** 好感度变化的原因 */
    public enum ChangeReason {
        /** 通过 addAffection() 增加 */
        ADD,
        /** 通过 setAffection() 设置 */
        SET,
        /** 时间衰减 */
        DECAY,
        /** 内置交互（礼物、花束、求婚等） */
        INTERACTION,
        /** 浪漫事件触发的加成 */
        ROMANTIC_EVENT,
        /** 其他模组通过 API 调用 */
        API
    }
}
