package com.xiaoshi2022.mcaromanticexpansion.advancement;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ModAdvancements {

    // 成就 ID 常量
    public static final String UNVEIL_VEIL = "unveil_veil";
    public static final String SHARED_UMBRELLA = "shared_umbrella";
    public static final String AFFECTION_LEVEL = "affection_level";
    public static final String ROMANTIC_EVENT = "romantic_event";
    public static final String FIRST_UMBRELLA_GIFT = "first_umbrella_gift";
    public static final String RAINY_UMBRELLA_GIFT = "rainy_umbrella_gift";
    public static final String MUTUAL_UMBRELLA_GIFT = "mutual_umbrella_gift";
    public static final String LOVE_LETTER_REPLY = "love_letter_reply";

    /**
     * 触发成就
     * @param player 玩家
     * @param advancementId 成就ID（在 advancement 文件夹中定义的 JSON 文件名）
     */
    public static void trigger(ServerPlayer player, String advancementId) {
        if (player == null) return;

        try {
            // 使用 new ResourceLocation (1.20.1 推荐)
            ResourceLocation location = new ResourceLocation(MCARomanticExpansion.MODID, advancementId);
            Advancement advancement = player.server.getAdvancements().getAdvancement(location);

            if (advancement != null) {
                player.getAdvancements().award(advancement, "trigger");
                MCARomanticExpansion.LOGGER.debug("Triggered advancement: {} for {}",
                        advancementId, player.getName().getString());
            } else {
                MCARomanticExpansion.LOGGER.debug("Advancement not found: {}", advancementId);
            }
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to trigger advancement {}: {}", advancementId, e.getMessage());
        }
    }

    // ========== 便捷方法（所有方法只接受玩家参数） ==========

    public static void triggerUnveilVeil(ServerPlayer player) {
        trigger(player, UNVEIL_VEIL);
    }

    public static void triggerSharedUmbrella(ServerPlayer player) {
        trigger(player, SHARED_UMBRELLA);
    }

    public static void triggerAffectionLevel(ServerPlayer player) {
        trigger(player, AFFECTION_LEVEL);
    }

    public static void triggerRomanticEvent(ServerPlayer player, String eventId) {
        trigger(player, ROMANTIC_EVENT);
    }

    public static void triggerFirstUmbrellaGift(ServerPlayer player) {
        trigger(player, FIRST_UMBRELLA_GIFT);
    }

    public static void triggerRainyUmbrellaGift(ServerPlayer player) {
        trigger(player, RAINY_UMBRELLA_GIFT);
    }

    public static void triggerMutualUmbrellaGift(ServerPlayer player) {
        trigger(player, MUTUAL_UMBRELLA_GIFT);
    }

    public static void triggerLoveLetterReply(ServerPlayer player) {
        trigger(player, LOVE_LETTER_REPLY);
    }
}