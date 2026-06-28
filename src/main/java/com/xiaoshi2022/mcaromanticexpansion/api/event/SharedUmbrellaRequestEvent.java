package com.xiaoshi2022.mcaromanticexpansion.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 共伞请求发送事件。
 * <p>
 * 当一位玩家向另一位玩家发送共伞邀请时触发。
 * 此事件在<b>服务端</b>触发。
 * <p>
 * 此事件是可取消的。
 */
public class SharedUmbrellaRequestEvent extends PlayerEvent {

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

    @Override
    public boolean isCancelable() {
        return true;
    }
}
