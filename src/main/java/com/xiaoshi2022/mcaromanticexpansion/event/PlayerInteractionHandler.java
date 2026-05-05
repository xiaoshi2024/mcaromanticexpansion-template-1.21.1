package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenBouquetGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenMarriageGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenProposalGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import com.xiaoshi2022.mcaromanticexpansion.util.CooldownManager;
import com.xiaoshi2022.mcaromanticexpansion.item.GiftBoxItem;
import net.conczin.mca.item.BouquetItem;
import net.conczin.mca.item.EngagementRingItem;
import net.conczin.mca.item.WeddingRingItem;
import net.conczin.mca.server.world.data.PlayerSaveData;
import net.conczin.mca.server.ServerInteractionManager;
import net.conczin.mca.entity.ai.relationship.RelationshipState;
import net.conczin.mca.registry.ItemsMCA;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

//        MCARomanticExpansion.LOGGER.info("Player {} interacting with {} using {}",
//                player.getName().getString(), targetPlayer.getName().getString(), item.getClass().getSimpleName());

        if (item instanceof BouquetItem) {
            handleBouquet(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        } else if (item instanceof EngagementRingItem) {
            handleProposal(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        } else if (item instanceof WeddingRingItem) {
            handleMarriage(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        } else if (item == ItemsMCA.DIVORCE_PAPERS) {
            handleDivorcePapers(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var stack = event.getItemStack();
        var item = stack.getItem();
        var hand = event.getHand();

        // 礼盒操作：Shift+右键放入物品
        if (item instanceof GiftBoxItem && player.isShiftKeyDown()) {
            // 获取另一只手的物品
            ItemStack otherHandItem = hand == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? serverPlayer.getOffhandItem()
                    : serverPlayer.getMainHandItem();

            if (!otherHandItem.isEmpty()) {
                GiftBoxItem.saveGiftItem(stack, otherHandItem);

                // 清空另一只手
                if (hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
                    serverPlayer.getInventory().offhand.set(0, ItemStack.EMPTY);
                } else {
                    serverPlayer.getInventory().items.set(serverPlayer.getInventory().selected, ItemStack.EMPTY);
                }

                serverPlayer.sendSystemMessage(Component.literal("§a已将物品放入礼盒！").withStyle(ChatFormatting.GREEN));
                event.setCanceled(true);
            } else {
                serverPlayer.sendSystemMessage(Component.literal("§c另一只手需要持有物品！").withStyle(ChatFormatting.RED));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var stack = event.getItemStack();
        var item = stack.getItem();

        // 右键方块打开礼盒（当玩家对准方块但想打开礼盒时）
        if (item instanceof GiftBoxItem && GiftBoxItem.hasGift(stack)) {
            // 打开礼盒获得物品
            ItemStack giftItem = GiftBoxItem.loadGiftItem(stack);
            if (!giftItem.isEmpty()) {
                serverPlayer.getInventory().add(giftItem);
                GiftBoxItem.clearGift(stack);
                serverPlayer.sendSystemMessage(Component.literal("§a打开礼盒获得了物品！").withStyle(ChatFormatting.GREEN));
                event.setCanceled(true);
            }
        }
    }

    private static void handleBouquet(ServerPlayer sender, ServerPlayer receiver) {
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
        if (CooldownManager.isOnCooldown(proposer.getUUID(), "proposal")) {
            long remaining = CooldownManager.getRemainingCooldown(proposer.getUUID(), "proposal");
            proposer.sendSystemMessage(Component.literal("§c请等待 " + (remaining / 1000) + " 秒后再求婚！")
                    .withStyle(ChatFormatting.RED));
            return;
        }

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
        if (CooldownManager.isOnCooldown(sender.getUUID(), "marriage")) {
            long remaining = CooldownManager.getRemainingCooldown(sender.getUUID(), "marriage");
            sender.sendSystemMessage(Component.literal("§c请等待 " + (remaining / 1000) + " 秒后再举行婚礼！")
                    .withStyle(ChatFormatting.RED));
            return;
        }

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

    private static void handleDivorcePapers(ServerPlayer sender, ServerPlayer target) {
        // 检查是否是配偶关系
        PlayerSaveData senderData = PlayerSaveData.get(sender);

        // 检查发送者是否已婚且配偶是目标玩家
        boolean senderIsMarried = senderData.getRelationshipState() == RelationshipState.MARRIED_TO_PLAYER;
        boolean targetIsSpouse = senderData.getPartnerUUID().isPresent() && 
                senderData.getPartnerUUID().get().equals(target.getUUID());

        if (!senderIsMarried || !targetIsSpouse) {
            sender.sendSystemMessage(Component.literal("§c只有已婚玩家才能向配偶递交离婚协议书！").withStyle(ChatFormatting.RED));
            return;
        }

        // 执行离婚
        ServerInteractionManager.getInstance().endMarriage(sender);
        
        // 消耗离婚协议书
        for (int i = 0; i < sender.getInventory().getContainerSize(); i++) {
            var stack = sender.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ItemsMCA.DIVORCE_PAPERS)) {
                stack.shrink(1);
                break;
            }
        }
        
        sender.sendSystemMessage(Component.literal("§c你与 " + target.getName().getString() + " 的婚姻已结束。").withStyle(ChatFormatting.RED));
        target.sendSystemMessage(Component.literal("§c" + sender.getName().getString() + " 已递交离婚协议书，你们的婚姻已结束。").withStyle(ChatFormatting.RED));
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
