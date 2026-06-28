package com.xiaoshi2022.mcaromanticexpansion.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 共伞关系建立事件。
 * <p>
 * 当两位玩家成功建立共伞关系（对方接受了邀请）时触发。
 * 此事件在<b>服务端</b>触发。
 * <p>
 * 不可取消。
 */
public class SharedUmbrellaEstablishedEvent extends PlayerEvent {

    private final ServerPlayer partner;

    public SharedUmbrellaEstablishedEvent(ServerPlayer player, ServerPlayer partner) {
        super(player);
        this.partner = partner;
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
}
