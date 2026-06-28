package com.xiaoshi2022.mcaromanticexpansion.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 婚姻状态变化事件。
 * <p>
 * 当两位玩家结婚或离婚时触发。
 * 此事件在<b>服务端</b>触发。
 * <p>
 * 结婚事件可取消（取消后不会执行结婚/离婚操作）。
 */
public class MarriageChangedEvent extends PlayerEvent {

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

    @Override
    public boolean isCancelable() {
        return true;
    }

    public enum ChangeType {
        /** 结婚 */
        MARRIED,
        /** 离婚 */
        DIVORCED
    }
}
