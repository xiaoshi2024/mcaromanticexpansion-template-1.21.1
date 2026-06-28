package com.xiaoshi2022.mcaromanticexpansion.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 求婚请求已发送事件。
 * <p>
 * 当一位玩家向另一位玩家发送了求婚请求（订婚戒指右键）时触发。
 * 此事件在<b>服务端</b>触发，发生在 GUI 数据包发送之前。
 * <p>
 * 此事件是<b>可取消</b>的。如果取消，则不会发送求婚GUI，求婚也不会进行。
 */
public class ProposalSentEvent extends PlayerEvent {

    private final ServerPlayer target;

    public ProposalSentEvent(ServerPlayer proposer, ServerPlayer target) {
        super(proposer);
        this.target = target;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    /** 求婚者 */
    public ServerPlayer getProposer() {
        return getEntity();
    }

    /** 被求婚者 */
    public ServerPlayer getTarget() {
        return target;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }
}
