package com.xiaoshi2022.mcaromanticexpansion.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 共伞请求发送事件。
 * <p>
 * 当一位持伞玩家右键另一位玩家发起共伞邀请时触发，在向对方发送共伞请求数据包之前。
 * 此事件在<b>服务端</b>触发。
 * <p>
 * 此事件是<b>可取消</b>的。如果取消，对方不会收到共伞邀请。
 */
public class SharedUmbrellaRequestEvent extends PlayerEvent implements ICancellableEvent {

    private final ServerPlayer target;

    public SharedUmbrellaRequestEvent(ServerPlayer initiator, ServerPlayer target) {
        super(initiator);
        this.target = target;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    /** 发起共伞邀请的玩家 */
    public ServerPlayer getInitiator() {
        return getEntity();
    }

    /** 被邀请的玩家 */
    public ServerPlayer getTarget() {
        return target;
    }
}
