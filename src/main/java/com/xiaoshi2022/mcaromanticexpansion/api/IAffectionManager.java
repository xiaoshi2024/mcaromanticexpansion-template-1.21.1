package com.xiaoshi2022.mcaromanticexpansion.api;

import net.minecraft.world.entity.player.Player;

/**
 * 好感度管理器接口。
 * <p>
 * 通过 {@link RomanticExpansionAPI#getAffectionManager()} 获取实例。
 * 每位玩家对其他每位玩家分别存储一个好感度数值（范围：-100 ~ 无上限）。
 */
public interface IAffectionManager {

    /**
     * 获取 player 对 target 的好感度值。
     *
     * @param player  主体玩家（谁的视角）
     * @param target  目标玩家（对谁的好感）
     * @return 好感度数值，默认0，负数为讨厌，正数为喜欢
     */
    int getAffection(Player player, Player target);

    /**
     * 增加（或减少）player 对 target 的好感度。
     *
     * @param amount  正数增加，负数减少
     */
    void addAffection(Player player, Player target, int amount);

    /**
     * 直接设置 player 对 target 的好感度值（最高100）。
     */
    void setAffection(Player player, Player target, int value);

    /**
     * 使用内置的交互类型快捷地增加好感度。
     * 每种交互类型有预设的加值。
     */
    void handleInteraction(InteractionType type, Player player, Player target);

    /**
     * 内置交互类型，对应预设的好感度加值。
     */
    enum InteractionType {
        /** 赠送礼物：+5 */
        GIFT,
        /** 送花束：+8 */
        BOUQUET,
        /** 共伞一段时间：+1 */
        SHARED_UMBRELLA,
        /** 亲吻：+15 */
        KISS,
        /** 跳舞：+10 */
        DANCE,
        /** 接受求婚：+20 */
        PROPOSAL_ACCEPT,
        /** 结婚：+30 */
        MARRIAGE,
        /** 拥抱：+6 */
        HUG,
        /** 悄悄话：+3 */
        WHISPER
    }
}
