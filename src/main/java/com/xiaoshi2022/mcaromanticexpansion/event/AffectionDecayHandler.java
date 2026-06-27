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
        player.displayClientMessage(message, true);
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

        // ✅ 修复：检查攻击者对目标的好感度
        int currentAffection = AffectionManager.getAffection(serverAttacker, serverTarget);
        if (currentAffection <= 0) {
            // 没有好感度，不需要惩罚（或者惩罚为0）
            MCARomanticExpansion.LOGGER.debug("No affection from {} to {}, skipping decay",
                    attacker.getName().getString(), target.getName().getString());
            return;
        }

        // ✅ 修复：攻击者对目标的好感度减少
        int penalty = calculatePenalty(damage);
        AffectionManager.addAffection(serverAttacker, serverTarget, -penalty);

        // 发送通知
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

        MCARomanticExpansion.LOGGER.info("Affection decayed: {} attacked {}, penalty: {} points (from {} to {})",
                attacker.getName().getString(), target.getName().getString(), penalty,
                serverAttacker.getName().getString(), serverTarget.getName().getString());
    }

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof Player target)) return;
        if (!(event.getEntity() instanceof ServerPlayer attacker)) return;
        if (!(target instanceof ServerPlayer serverTarget)) return;
        if (attacker == target) return;

        // ✅ 修复：检查攻击者对目标的好感度
        int currentAffection = AffectionManager.getAffection(attacker, serverTarget);
        if (currentAffection <= 0) return;

        int penalty = AFFECTION_PENALTY;
        // ✅ 修复：攻击者对目标的好感度减少
        AffectionManager.addAffection(attacker, serverTarget, -penalty);

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

        MCARomanticExpansion.LOGGER.info("Affection decayed: {} attacked {} (via AttackEntityEvent), penalty: {} points",
                attacker.getName().getString(), target.getName().getString(), penalty);
    }

    private static int calculatePenalty(float damage) {
        int basePenalty = AFFECTION_PENALTY;
        int extraPenalty = (int) (damage - DAMAGE_THRESHOLD);
        return Math.min(basePenalty + extraPenalty, 30);
    }
}