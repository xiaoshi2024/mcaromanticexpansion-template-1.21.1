package com.xiaoshi2022.mcaromanticexpansion.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 婚姻状态变化事件。
 * <p>
 * 当两位玩家结婚或离婚时触发。
 * 此事件在<b>服务端</b>触发，在 MCA 婚姻/离婚逻辑执行之前。
 * <p>
 * 此事件是<b>可取消</b>的。如果取消，则不会执行 MCA 的结婚/离婚操作，戒指不会被消耗或返还。
 * <pre>{@code
 * // 在 Mod 主类构造函数中注册：
 * NeoForge.EVENT_BUS.addListener(this::onMarriageChanged);
 * }</pre>
 */
public class MarriageChangedEvent extends PlayerEvent implements ICancellableEvent {

    private final ServerPlayer partner;
    private final ChangeType changeType;

    /**
     * @param player     玩家A
     * @param partner    玩家B
     * @param changeType MARRIED 或 DIVORCED
     */
    public MarriageChangedEvent(ServerPlayer player, ServerPlayer partner, ChangeType changeType) {
        super(player);
        this.partner = partner;
        this.changeType = changeType;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public ServerPlayer getPlayerA() {
        return getEntity();
    }

    public ServerPlayer getPlayerB() {
        return partner;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public enum ChangeType {
        /** 结婚 */
        MARRIED,
        /** 离婚 */
        DIVORCED
    }
}
