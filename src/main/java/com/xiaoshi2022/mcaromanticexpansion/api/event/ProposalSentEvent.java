package com.xiaoshi2022.mcaromanticexpansion.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * 求婚请求发送事件。
 * <p>
 * 当一位玩家持订婚戒指右键另一位玩家时触发，在向被求婚者发送求婚 GUI 数据包之前。
 * 此事件在<b>服务端</b>触发。
 * <p>
 * 此事件是<b>可取消</b>的。如果取消：
 * <ul>
 *   <li>求婚 GUI 不会打开</li>
 *   <li>求婚冷却不会生效</li>
 * </ul>
 */
@Cancelable
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
}