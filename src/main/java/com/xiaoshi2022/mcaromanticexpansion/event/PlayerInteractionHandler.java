package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenBouquetGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenMarriageGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenProposalGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.util.CooldownManager;
import net.conczin.mca.item.BouquetItem;
import net.conczin.mca.item.EngagementRingItem;
import net.conczin.mca.item.WeddingRingItem;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.conczin.mca.entity.ai.relationship.RelationshipState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@SuppressWarnings("unused")
public class PlayerInteractionHandler {

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!(event.getTarget() instanceof Player targetPlayer)) {
            return;
        }

        if (!(targetPlayer instanceof ServerPlayer targetServerPlayer)) {
            return;
        }

        if (player == targetPlayer) {
            player.sendSystemMessage(Component.literal("§c你不能对自己使用！").withStyle(ChatFormatting.RED));
            event.setCanceled(true);
            return;
        }

        var stack = event.getItemStack();
        var item = stack.getItem();

        MCARomanticExpansion.LOGGER.info("Player {} interacting with {} using {}",
                player.getName().getString(), targetPlayer.getName().getString(), item.getClass().getSimpleName());

        if (item instanceof BouquetItem) {
            handleBouquet(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        } else if (item instanceof EngagementRingItem) {
            handleProposal(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        } else if (item instanceof WeddingRingItem) {
            handleMarriage(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        }
    }

    private static void handleBouquet(ServerPlayer sender, ServerPlayer receiver) {
        // 检查冷却
        if (CooldownManager.isOnCooldown(sender.getUUID(), "bouquet")) {
            long remaining = CooldownManager.getRemainingCooldown(sender.getUUID(), "bouquet");
            sender.sendSystemMessage(Component.literal("§c请等待 " + (remaining / 1000) + " 秒后再送花束！")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        sendBouquetRequest(sender, receiver);
        CooldownManager.setCooldown(sender.getUUID(), "bouquet");
    }

    private static void handleProposal(ServerPlayer proposer, ServerPlayer target) {
        PlayerSaveData proposerData = PlayerSaveData.get(proposer);
        PlayerSaveData targetData = PlayerSaveData.get(target);

        // 检查是否已经在恋爱/订婚/结婚关系中
        // 根据 MCA 源码，婚姻状态存储在 super (EntityRelationship) 中
        // 可以通过检查是否已经有配偶来判断

        // 尝试获取配偶 UUID (MCA 没有直接的 getSpouse 方法，需要通过 FamilyTreeNode 或其他方式)
        // 这里使用 endRelationShip 的方式，但更好的方法是检查关系状态

        // 检查冷却
        if (CooldownManager.isOnCooldown(proposer.getUUID(), "proposal")) {
            long remaining = CooldownManager.getRemainingCooldown(proposer.getUUID(), "proposal");
            proposer.sendSystemMessage(Component.literal("§c请等待 " + (remaining / 1000) + " 秒后再求婚！")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // 检查订婚戒指数量
        int ringCount = 0;
        for (int i = 0; i < proposer.getInventory().getContainerSize(); i++) {
            var stack = proposer.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof EngagementRingItem) {
                ringCount += stack.getCount();
            }
        }

        if (ringCount == 0) {
            proposer.sendSystemMessage(Component.literal("§c你需要一个订婚戒指！").withStyle(ChatFormatting.RED));
            return;
        }

        sendProposalRequest(proposer, target);
        CooldownManager.setCooldown(proposer.getUUID(), "proposal");
        proposer.sendSystemMessage(Component.literal("§a已向 " + target.getName().getString() + " 发送求婚请求！").withStyle(ChatFormatting.GREEN));
    }

    private static void handleMarriage(ServerPlayer sender, ServerPlayer target) {
        // 检查结婚冷却
        if (CooldownManager.isOnCooldown(sender.getUUID(), "marriage")) {
            long remaining = CooldownManager.getRemainingCooldown(sender.getUUID(), "marriage");
            sender.sendSystemMessage(Component.literal("§c请等待 " + (remaining / 1000) + " 秒后再举行婚礼！")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // 检查双方是否有结婚戒指
        boolean senderHasRing = false;
        boolean targetHasRing = false;

        for (int i = 0; i < sender.getInventory().getContainerSize(); i++) {
            var stack = sender.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof WeddingRingItem) {
                senderHasRing = true;
                break;
            }
        }

        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            var stack = target.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof WeddingRingItem) {
                targetHasRing = true;
                break;
            }
        }

        if (!senderHasRing || !targetHasRing) {
            sender.sendSystemMessage(Component.literal("§c双方都需要拥有结婚戒指！").withStyle(ChatFormatting.RED));
            return;
        }

        sendMarriageRequest(sender, target);
        CooldownManager.setCooldown(sender.getUUID(), "marriage");
        sender.sendSystemMessage(Component.literal("§a已向 " + target.getName().getString() + " 发送婚礼请求！").withStyle(ChatFormatting.GREEN));
    }

    private static void sendProposalRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenProposalGUIPacket packet = new OpenProposalGUIPacket(sender.getUUID(), sender.getName().getString());
        receiver.connection.send(packet);
    }

    private static void sendBouquetRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenBouquetGUIPacket packet = new OpenBouquetGUIPacket(sender.getUUID());
        MCARomanticExpansion.LOGGER.info("Sending OpenBouquetGUIPacket to {}", receiver.getName().getString());
        receiver.connection.send(packet);
    }

    private static void sendMarriageRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenMarriageGUIPacket packet = new OpenMarriageGUIPacket(sender.getUUID());
        MCARomanticExpansion.LOGGER.info("Sending OpenMarriageGUIPacket to {}", receiver.getName().getString());
        receiver.connection.send(packet);
    }
}