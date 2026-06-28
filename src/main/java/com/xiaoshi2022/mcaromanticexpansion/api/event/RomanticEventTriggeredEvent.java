package com.xiaoshi2022.mcaromanticexpansion.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 浪漫事件触发事件。
 * <p>
 * 当两位玩家之间触发了一个浪漫事件（内置或自定义）时，本事件会被发布到 NeoForge 事件总线。
 * 此事件在<b>服务端</b>触发。
 * <p>
 * 此事件是<b>可取消</b>的。如果取消，则不会应用好感度加成、不会发送消息、不会触发效果。
 * <p>
 * <b>示例：</b>
 * <pre>{@code
 * @SubscribeEvent
 * public void onRomanticEvent(RomanticEventTriggeredEvent event) {
 *     if ("rainbow_pact".equals(event.getEventId())) {
 *         // 彩虹契约触发时给双方额外奖励
 *         event.getPlayer().giveExperienceLevels(5);
 *         event.getPartner().giveExperienceLevels(5);
 *     }
 * }
 * }</pre>
 */
public class RomanticEventTriggeredEvent extends PlayerEvent {

    private final ServerPlayer partner;
    private final String eventId;
    private final boolean customEvent;
    private final int affectionBonus;

    /**
     * @param player         玩家A
     * @param partner        玩家B
     * @param eventId        事件ID
     * @param customEvent    是否是自定义事件（非内置）
     * @param affectionBonus 本次事件的好感度加成
     */
    public RomanticEventTriggeredEvent(ServerPlayer player, ServerPlayer partner,
                                     String eventId, boolean customEvent, int affectionBonus) {
        super(player);
        this.partner = partner;
        this.eventId = eventId;
        this.customEvent = customEvent;
        this.affectionBonus = affectionBonus;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public ServerPlayer getPlayer() {
        return getEntity();
    }

    public ServerPlayer getPartner() {
        return partner;
    }

    public String getEventId() {
        return eventId;
    }

    public boolean isCustomEvent() {
        return customEvent;
    }

    public int getAffectionBonus() {
        return affectionBonus;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }
}
