package com.xiaoshi2022.mcaromanticexpansion.advancement;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ModAdvancements {

    // 成就 ID 常量（与 JSON 文件名对应）
    public static final String ROOT = "root";
    public static final String UNVEIL_VEIL = "unveil_veil";
    public static final String FIRST_UMBRELLA_GIFT = "first_umbrella_gift";
    public static final String RAINY_UMBRELLA_GIFT = "rainy_umbrella_gift";
    public static final String MUTUAL_UMBRELLA_GIFT = "mutual_umbrella_gift";
    public static final String LOVE_LETTER_REPLY = "love_letter_reply";
    public static final String HEART_TO_HEART = "heart_to_heart";
    public static final String HERO_RESQUE = "hero_resque";
    public static final String MOONLIGHT_SERENADE = "moonlight_serenade";
    public static final String RAINBOW_PACT = "rainbow_pact";
    public static final String STARFALL = "starfall";
    public static final String SYNCHRONY_TEST = "synchrony_test";
    public static final String TIME_STANDS_STILL = "time_stands_still";

    /**
     * 触发成就（自动获取 criterion 名称）
     * @param player 玩家
     * @param advancementId 成就ID（JSON 文件名，不含扩展名）
     */
    public static void trigger(ServerPlayer player, String advancementId) {
        if (player == null) return;

        try {
            ResourceLocation location = new ResourceLocation(MCARomanticExpansion.MODID, advancementId);
            Advancement advancement = player.server.getAdvancements().getAdvancement(location);

            if (advancement != null) {
                // 获取进度中第一个 criterion 的名称
                String criterionName = advancement.getCriteria().keySet().iterator().next();
                player.getAdvancements().award(advancement, criterionName);
                MCARomanticExpansion.LOGGER.debug("Triggered advancement: {} with criterion: {} for {}",
                        advancementId, criterionName, player.getName().getString());
            } else {
                MCARomanticExpansion.LOGGER.warn("Advancement not found: {}", advancementId);
            }
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to trigger advancement {}: {}", advancementId, e.getMessage());
        }
    }

    /**
     * 检查玩家是否已完成某个成就
     * @param player 玩家
     * @param advancementId 成就ID
     * @return true=已完成
     */
    public static boolean isCompleted(ServerPlayer player, String advancementId) {
        if (player == null) return false;

        try {
            ResourceLocation location = new ResourceLocation(MCARomanticExpansion.MODID, advancementId);
            Advancement advancement = player.server.getAdvancements().getAdvancement(location);
            if (advancement == null) return false;
            return player.getAdvancements().getOrStartProgress(advancement).isDone();
        } catch (Exception e) {
            return false;
        }
    }

    // ========== 便捷触发方法 ==========

    public static void triggerRoot(ServerPlayer player) {
        trigger(player, ROOT);
    }

    public static void triggerUnveilVeil(ServerPlayer player) {
        trigger(player, UNVEIL_VEIL);
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

    // ========== 浪漫事件成就（使用 event_id 作为进度 ID） ==========

    public static void triggerHeartToHeart(ServerPlayer player) {
        trigger(player, HEART_TO_HEART);
    }

    public static void triggerHeroResque(ServerPlayer player) {
        trigger(player, HERO_RESQUE);
    }

    public static void triggerMoonlightSerenade(ServerPlayer player) {
        trigger(player, MOONLIGHT_SERENADE);
    }

    public static void triggerRainbowPact(ServerPlayer player) {
        trigger(player, RAINBOW_PACT);
    }

    public static void triggerStarfall(ServerPlayer player) {
        trigger(player, STARFALL);
    }

    public static void triggerSynchronyTest(ServerPlayer player) {
        trigger(player, SYNCHRONY_TEST);
    }

    public static void triggerTimeStandsStill(ServerPlayer player) {
        trigger(player, TIME_STANDS_STILL);
    }

    /**
     * 通用浪漫事件触发（根据 eventId 自动匹配对应的进度）
     * @param player 玩家
     * @param eventId 事件ID（必须与 JSON 中的 event_id 条件匹配）
     */
    public static void triggerRomanticEvent(ServerPlayer player, String eventId) {
        if (player == null || eventId == null) return;

        // 直接使用 eventId 作为进度 ID
        // 注意：eventId 必须与 JSON 文件名一致
        trigger(player, eventId);
    }
}