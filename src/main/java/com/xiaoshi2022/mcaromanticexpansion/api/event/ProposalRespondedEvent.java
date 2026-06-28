package com.xiaoshi2022.mcaromanticexpansion.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 求婚响应事件。
 * <p>
 * 当被求婚者在GUI界面点击「接受」或「拒绝」后触发。
 * 此事件在<b>服务端</b>触发，发生在婚约逻辑执行之前。
 * <p>
 * 此事件是<b>可取消</b>的。如果取消，不会执行 MCA 的接受/拒绝操作，戒指不会转移，好感度不会增加。
 */
public class ProposalRespondedEvent extends PlayerEvent {

    private final ServerPlayer proposer;
    private final boolean accepted;

    /**
     * @param responder 响应者（被求婚者）
     * @param proposer  求婚者
     * @param accepted  是否接受
     */
    public ProposalRespondedEvent(ServerPlayer responder, ServerPlayer proposer, boolean accepted) {
        super(responder);
        this.proposer = proposer;
        this.accepted = accepted;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    /** 响应者（被求婚者） */
    public ServerPlayer getResponder() {
        return getEntity();
    }

    /** 求婚者 */
    public ServerPlayer getProposer() {
        return proposer;
    }

    /** 是否接受求婚 */
    public boolean isAccepted() {
        return accepted;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }
}
