package com.xiaoshi2022.mcaromanticexpansion.api;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * 玩家关系状态查询接口。
 * <p>
 * 通过 {@link RomanticExpansionAPI#getRelationshipManager()} 获取实例。
 * 用于查询玩家的婚姻、订婚、怀孕、共伞等状态。
 */
public interface IRelationshipManager {

    /**
     * 检查玩家是否已婚（与其他玩家结婚）。
     */
    boolean isMarried(Player player);

    /**
     * 检查玩家是否已订婚（与其他玩家）。
     */
    boolean isEngaged(Player player);

    /**
     * 获取玩家的配偶/未婚夫UUID（如果存在）。
     *
     * @return 伴侣的UUID；若单身则返回 empty Optional
     */
    Optional<UUID> getPartnerUUID(Player player);

    /**
     * 检查玩家是否处于怀孕期。
     */
    boolean isPregnant(Player player);

    /**
     * 检查玩家是否正在与他人共伞。
     */
    boolean isInSharedUmbrella(Player player);

    /**
     * 获取当前与该玩家共伞的伴侣。
     *
     * @return 共伞伴侣；如果不在共伞状态则返回 empty Optional
     */
    Optional<Player> getSharedUmbrellaPartner(Player player);

    /**
     * 检查该玩家是否被允许进行同性结婚。
     * 先检查玩家个人覆盖配置，否则使用全局配置。
     */
    boolean isSameGenderMarriageAllowed(Player player);
}
