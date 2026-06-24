package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import net.conczin.mca.entity.ai.relationship.RelationshipState;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

public class AffectionDecayHandler {

    private static final int DAMAGE_THRESHOLD = 3;
    private static final int AFFECTION_PENALTY = 10;

    @SubscribeEvent
    public static void onPlayerAttack(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        if (source == null) {
            return;
        }

        // 获取攻击者
        if (!(source.getDirectEntity() instanceof Player attacker)) {
            return;
        }

        // 获取被攻击者
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        // 确保是服务器玩家
        if (!(attacker instanceof ServerPlayer serverAttacker)) {
            return;
        }

        if (!(target instanceof ServerPlayer serverTarget)) {
            return;
        }

        // 不能攻击自己
        if (attacker == target) {
            return;
        }

        // 获取伤害值
        float damage = event.getNewDamage();
        if (damage <= DAMAGE_THRESHOLD) {
            return;
        }

        // 检查是否是配偶关系
        if (!isSpouse(serverAttacker, serverTarget)) {
            return;
        }

        // 获取当前好感度
        int currentAffection = AffectionManager.getAffection(serverTarget, serverAttacker);
        if (currentAffection <= 0) {
            return;
        }

        // 计算惩罚值
        int penalty = calculatePenalty(damage);
        AffectionManager.addAffection(serverTarget, serverAttacker, -penalty);

        // 发送消息
        serverAttacker.sendSystemMessage(Component.translatable(
                        "message.mcaromanticexpansion.affection.decay.attacker",
                        serverTarget.getName().getString(), penalty)
                .withStyle(ChatFormatting.RED));

        serverTarget.sendSystemMessage(Component.translatable(
                        "message.mcaromanticexpansion.affection.decay.target",
                        serverAttacker.getName().getString(), penalty)
                .withStyle(ChatFormatting.RED));

        MCARomanticExpansion.LOGGER.info("Affection decayed: {} attacked {} for {} damage, penalty: {} points",
                attacker.getName().getString(), target.getName().getString(), damage, penalty);
    }

    // ★★★ 备用：如果是旧的 API，使用 AttackEntityEvent ★★★
    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof Player target)) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        if (!(target instanceof ServerPlayer serverTarget)) {
            return;
        }

        // 不能攻击自己
        if (attacker == target) {
            return;
        }

        // 检查是否是配偶关系
        if (!isSpouse(attacker, serverTarget)) {
            return;
        }

        // 获取当前好感度
        int currentAffection = AffectionManager.getAffection(serverTarget, attacker);
        if (currentAffection <= 0) {
            return;
        }

        // 攻击事件触发惩罚（不依赖伤害值，直接惩罚）
        int penalty = AFFECTION_PENALTY;
        AffectionManager.addAffection(serverTarget, attacker, -penalty);

        attacker.sendSystemMessage(Component.translatable(
                        "message.mcaromanticexpansion.affection.decay.attacker",
                        serverTarget.getName().getString(), penalty)
                .withStyle(ChatFormatting.RED));

        serverTarget.sendSystemMessage(Component.translatable(
                        "message.mcaromanticexpansion.affection.decay.target",
                        attacker.getName().getString(), penalty)
                .withStyle(ChatFormatting.RED));

        MCARomanticExpansion.LOGGER.info("Affection decayed: {} attacked {} (via AttackEntityEvent), penalty: {} points",
                attacker.getName().getString(), target.getName().getString(), penalty);
    }

    private static boolean isSpouse(ServerPlayer player, ServerPlayer target) {
        try {
            PlayerSaveData playerData = PlayerSaveData.get(player);
            return playerData.getRelationshipState() == RelationshipState.MARRIED_TO_PLAYER
                    && playerData.getPartnerUUID().isPresent()
                    && playerData.getPartnerUUID().get().equals(target.getUUID());
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to check spouse status: {}", e.getMessage());
            return false;
        }
    }

    private static int calculatePenalty(float damage) {
        int basePenalty = AFFECTION_PENALTY;
        int extraPenalty = (int) (damage - DAMAGE_THRESHOLD);
        return Math.min(basePenalty + extraPenalty, 30);
    }
}