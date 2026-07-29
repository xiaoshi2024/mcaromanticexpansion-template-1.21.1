package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AffectionDecayHandler {

    private static final int DAMAGE_THRESHOLD = 3;
    private static final int AFFECTION_PENALTY = 10;
    private static final int MIN_AFFECTION = -100;

    private static boolean hasFamilyTreeInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            // 使用反射或直接判断类名，避免硬依赖
            if (stack.getItem().getClass().getSimpleName().equals("FamilyTreeItem")) {
                return true;
            }
        }
        return false;
    }

    private static void sendActionBarMessage(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }

    @SubscribeEvent
    public static void onPlayerAttack(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        if (source == null) return;

        if (!(source.getDirectEntity() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(attacker instanceof ServerPlayer serverAttacker)) return;
        if (!(target instanceof ServerPlayer serverTarget)) return;
        if (attacker == target) return;

        // 【修复】1.20.1 中使用 getAmount() 替代 getNewDamage()
        float damage = event.getAmount();
        if (damage <= DAMAGE_THRESHOLD) return;

        int penalty = calculatePenalty(damage);
        AffectionManager.addAffection(serverAttacker, serverTarget, -penalty);

        int newAffection = AffectionManager.getAffection(serverAttacker, serverTarget);

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