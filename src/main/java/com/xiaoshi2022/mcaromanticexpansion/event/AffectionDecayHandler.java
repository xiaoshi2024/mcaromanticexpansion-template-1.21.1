package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import net.conczin.mca.item.FamilyTreeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

public class AffectionDecayHandler {

    private static final int DAMAGE_THRESHOLD = 3;
    private static final int AFFECTION_PENALTY = 10;
    private static final int MIN_AFFECTION = -100;  // 最低好感度限制

    private static boolean hasFamilyTreeInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof FamilyTreeItem) {
                return true;
            }
        }
        return false;
    }

    private static void sendActionBarMessage(ServerPlayer player, Component message) {
        player.sendSystemMessage(message, true);
    }

    @SubscribeEvent
    public static void onPlayerAttack(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        if (source == null) return;

        if (!(source.getDirectEntity() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(attacker instanceof ServerPlayer serverAttacker)) return;
        if (!(target instanceof ServerPlayer serverTarget)) return;
        if (attacker == target) return;

        float damage = event.getNewDamage();
        if (damage <= DAMAGE_THRESHOLD) return;

        // ========== 修复：移除 <= 0 的检查，允许好感度降到负数 ==========
        // 不再检查 currentAffection <= 0，直接扣减

        int penalty = calculatePenalty(damage);
        AffectionManager.addAffection(serverAttacker, serverTarget, -penalty);

        // 获取扣减后的好感度用于日志
        int newAffection = AffectionManager.getAffection(serverAttacker, serverTarget);

        // 发送通知（只有持有家谱的玩家才显示）
        if (hasFamilyTreeInInventory(serverAttacker)) {
            sendActionBarMessage(serverAttacker,
                    Component.translatable("message.mcaromanticexpansion.affection.decay.attacker",
                                    serverTarget.getName().getString(), penalty)
                            .withStyle(ChatFormatting.RED));
        }

        if (hasFamilyTreeInInventory(serverTarget)) {
            sendActionBarMessage(serverTarget,
                    Component.translatable("message.mcaromanticexpansion.affection.decay.target",
                                    serverAttacker.getName().getString(), penalty)
                            .withStyle(ChatFormatting.RED));
        }

        MCARomanticExpansion.LOGGER.debug("Affection decayed: {} attacked {}, penalty: {} points, new affection: {}",
                attacker.getName().getString(), target.getName().getString(), penalty, newAffection);
    }

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof Player target)) return;
        if (!(event.getEntity() instanceof ServerPlayer attacker)) return;
        if (!(target instanceof ServerPlayer serverTarget)) return;
        if (attacker == target) return;

        // ========== 修复：移除 <= 0 的检查，允许好感度降到负数 ==========
        int penalty = AFFECTION_PENALTY;
        AffectionManager.addAffection(attacker, serverTarget, -penalty);

        int newAffection = AffectionManager.getAffection(attacker, serverTarget);

        if (hasFamilyTreeInInventory(attacker)) {
            sendActionBarMessage(attacker,
                    Component.translatable("message.mcaromanticexpansion.affection.decay.attacker",
                                    serverTarget.getName().getString(), penalty)
                            .withStyle(ChatFormatting.RED));
        }

        if (hasFamilyTreeInInventory(serverTarget)) {
            sendActionBarMessage(serverTarget,
                    Component.translatable("message.mcaromanticexpansion.affection.decay.target",
                                    attacker.getName().getString(), penalty)
                            .withStyle(ChatFormatting.RED));
        }

        MCARomanticExpansion.LOGGER.debug("Affection decayed: {} attacked {} (via AttackEntityEvent), penalty: {} points, new affection: {}",
                attacker.getName().getString(), target.getName().getString(), penalty, newAffection);
    }

    private static int calculatePenalty(float damage) {
        int basePenalty = AFFECTION_PENALTY;
        int extraPenalty = (int) (damage - DAMAGE_THRESHOLD);
        return Math.min(basePenalty + extraPenalty, 30);
    }
}