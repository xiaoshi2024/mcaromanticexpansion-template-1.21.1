package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.advancement.CriterionTriggerRegister;
import com.xiaoshi2022.mcaromanticexpansion.item.GiftBoxItem;
import com.xiaoshi2022.mcaromanticexpansion.item.RedVeilItem;
import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenBouquetGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenMarriageGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenProposalGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import com.xiaoshi2022.mcaromanticexpansion.util.CooldownManager;
import com.xiaoshi2022.mcaromanticexpansion.util.MarriageConfig;
import com.xiaoshi2022.mcaromanticexpansion.util.SharedUmbrellaManager;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.conczin.mca.entity.ai.relationship.RelationshipState;
import net.conczin.mca.item.BouquetItem;
import net.conczin.mca.item.EngagementRingItem;
import net.conczin.mca.item.WeddingRingItem;
import net.conczin.mca.registry.ItemsMCA;
import net.conczin.mca.server.ServerInteractionManager;
import net.conczin.mca.server.world.data.PlayerSaveData;
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
        } else if (item instanceof GiftBoxItem) {
            handleGiftBox(serverPlayer, targetServerPlayer, stack);
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
        } else if (stack.is(ModItems.UMBRELLA.get())) {
            handleSharedUmbrella(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        } else if (stack.isEmpty()) {
            handleUnveilVeil(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        }
    }

    private static void handleSharedUmbrella(ServerPlayer player, ServerPlayer target) {
        // 检查伞是否被意外关闭（因为 RightClickItem 先于 EntityInteract 触发）
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (mainHand.is(ModItems.UMBRELLA.get())) {
            if (UmbrellaItem.getState(mainHand) != UmbrellaItem.State.FULL_OPEN) {
                UmbrellaItem.setUmbrellaState(mainHand, UmbrellaItem.State.FULL_OPEN);
            }
        } else if (offHand.is(ModItems.UMBRELLA.get())) {
            if (UmbrellaItem.getState(offHand) != UmbrellaItem.State.FULL_OPEN) {
                UmbrellaItem.setUmbrellaState(offHand, UmbrellaItem.State.FULL_OPEN);
            }
        }

        SharedUmbrellaManager.sendRequest(player, target);
    }

    private static void handleGiftBox(ServerPlayer giver, ServerPlayer receiver, ItemStack giftBoxStack) {
        // 检查礼盒是否有礼物
        if (!GiftBoxItem.hasGift(giftBoxStack)) {
            giver.sendSystemMessage(Component.literal("§c礼盒是空的！请先放入物品。").withStyle(ChatFormatting.RED));
            return;
        }

        // 获取礼物物品
        ItemStack giftItem = GiftBoxItem.loadGiftItem(giftBoxStack);
        
        // 尝试将礼物放入接收者背包
        boolean added = receiver.getInventory().add(giftItem);
        if (!added) {
            receiver.drop(giftItem, false);
            receiver.sendSystemMessage(Component.literal("§c背包已满，礼物掉落在了地上！").withStyle(ChatFormatting.RED));
        } else {
            receiver.sendSystemMessage(Component.literal("§a你收到了 " + giver.getName().getString() + " 的礼物！").withStyle(ChatFormatting.GREEN));
            giver.sendSystemMessage(Component.literal("§a你成功赠送了礼物给 " + receiver.getName().getString() + "！").withStyle(ChatFormatting.GREEN));
            
            // 清空礼盒
            GiftBoxItem.clearGift(giftBoxStack);
            
            // ========== 添加好感度：赠送礼物 ==========
            AffectionManager.handleInteraction(AffectionManager.InteractionType.GIFT, giver, receiver);
            MCARomanticExpansion.LOGGER.info("Added GIFT affection for {} -> {}",
                    giver.getName().getString(), receiver.getName().getString());
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
            // ========== 修复：检查礼盒是否已经有礼物 ==========
            if (GiftBoxItem.hasGift(stack)) {
                serverPlayer.sendSystemMessage(Component.literal("§c礼盒中已经有礼物了！不能重复放入！")
                        .withStyle(ChatFormatting.RED));
                event.setCanceled(true);
                return;
            }

            // 获取另一只手的物品
            ItemStack otherHandItem = hand == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? serverPlayer.getOffhandItem()
                    : serverPlayer.getMainHandItem();

            if (!otherHandItem.isEmpty()) {
                // ========== 额外检查：不能放入另一个礼盒 ==========
                if (otherHandItem.getItem() instanceof GiftBoxItem) {
                    serverPlayer.sendSystemMessage(Component.literal("§c不能将礼盒放入另一个礼盒中！")
                            .withStyle(ChatFormatting.RED));
                    event.setCanceled(true);
                    return;
                }

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

        // 右键方块打开礼盒
        if (item instanceof GiftBoxItem) {
            // 检查是否有礼物
            if (!GiftBoxItem.hasGift(stack)) {
                serverPlayer.sendSystemMessage(Component.literal("§c礼盒是空的！请先放入物品。")
                        .withStyle(ChatFormatting.RED));
                event.setCanceled(true);
                return;
            }

            // 检查是否是生日礼盒
            if (GiftBoxItem.isBirthdayGift(stack)) {
                GiftBoxItem.openBirthdayGift(serverPlayer, stack);
                event.setCanceled(true);
                return;
            }

            // 普通礼盒打开
            ItemStack giftItem = GiftBoxItem.loadGiftItem(stack);
            if (!giftItem.isEmpty()) {
                boolean added = serverPlayer.getInventory().add(giftItem);
                if (!added) {
                    serverPlayer.drop(giftItem, false);
                    serverPlayer.sendSystemMessage(Component.literal("§c背包已满，礼物掉落在了地上！")
                            .withStyle(ChatFormatting.RED));
                } else {
                    serverPlayer.sendSystemMessage(Component.literal("§a打开礼盒获得了物品！")
                            .withStyle(ChatFormatting.GREEN));
                }
                GiftBoxItem.clearGift(stack);
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

        // ========== 新增：检查性别 ==========
        Gender proposerGender = PregnancyAttemptHandler.getGenderFromNBT(proposer);
        Gender targetGender = PregnancyAttemptHandler.getGenderFromNBT(target);

        if (proposerGender == Gender.UNASSIGNED || targetGender == Gender.UNASSIGNED) {
            proposer.sendSystemMessage(Component.literal("§c请双方都使用 /mca editor 设置性别后再求婚！")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        if (proposerGender == targetGender) {
            boolean proposerAllowed = MarriageConfig.isSameGenderMarriageAllowed(proposer);
            boolean targetAllowed = MarriageConfig.isSameGenderMarriageAllowed(target);

            if (!proposerAllowed || !targetAllowed) {
                proposer.sendSystemMessage(Component.literal("§c同性求婚已被禁止！如需启用请联系管理员使用 /marriageconfig allowSameGender true")
                        .withStyle(ChatFormatting.RED));
                return;
            }
        }

        // 检查戒指...
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

        // ========== 检查性别 ==========
        Gender senderGender = PregnancyAttemptHandler.getGenderFromNBT(sender);
        Gender targetGender = PregnancyAttemptHandler.getGenderFromNBT(target);

        if (senderGender == Gender.UNASSIGNED || targetGender == Gender.UNASSIGNED) {
            sender.sendSystemMessage(Component.literal("§c请使用 /mca editor 打开编辑器设置性别后再结婚！")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // 如果是同性结婚，检查是否允许
        if (senderGender == targetGender) {
            boolean senderAllowed = MarriageConfig.isSameGenderMarriageAllowed(sender);
            boolean targetAllowed = MarriageConfig.isSameGenderMarriageAllowed(target);

            if (!senderAllowed || !targetAllowed) {
                sender.sendSystemMessage(Component.literal("§c同性结婚已被禁止！如需启用请联系管理员使用 /marriageconfig allowSameGender true")
                        .withStyle(ChatFormatting.RED));
                return;
            }
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
        MCARomanticExpansion.LOGGER.info("Sending OpenProposalGUIPacket to {} from {}",
                receiver.getName().getString(), sender.getName().getString());
        receiver.connection.send(packet);
    }

    private static void sendBouquetRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenBouquetGUIPacket packet = new OpenBouquetGUIPacket(sender.getUUID(), sender.getName().getString());
        MCARomanticExpansion.LOGGER.info("Sending OpenBouquetGUIPacket to {} from {}",
                receiver.getName().getString(), sender.getName().getString());
        receiver.connection.send(packet);
    }

    private static void sendMarriageRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenMarriageGUIPacket packet = new OpenMarriageGUIPacket(sender.getUUID(), sender.getName().getString());
        MCARomanticExpansion.LOGGER.info("Sending OpenMarriageGUIPacket to {} from {}",
                receiver.getName().getString(), sender.getName().getString());
        receiver.connection.send(packet);
    }

    private static void handleUnveilVeil(ServerPlayer player, ServerPlayer target) {
        if (!com.xiaoshi2022.mcaromanticexpansion.compat.curios.CuriosIntegration.isCuriosAvailable()) {
            return;
        }

        PlayerSaveData playerData = PlayerSaveData.get(player);
        PlayerSaveData targetData = PlayerSaveData.get(target);

        boolean playerIsMarried = playerData.getRelationshipState() == RelationshipState.MARRIED_TO_PLAYER;
        boolean targetIsSpouse = playerData.getPartnerUUID().isPresent() &&
                playerData.getPartnerUUID().get().equals(target.getUUID());

        if (!playerIsMarried || !targetIsSpouse) {
            return;
        }

        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");

            Object optionalCuriosInventory = curiosApiClass.getDeclaredMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class)
                    .invoke(null, target);

            if (optionalCuriosInventory instanceof java.util.Optional<?> opt && opt.isPresent()) {
                Object curiosInventory = opt.get();
                Class<?> inventoryClass = curiosInventory.getClass();

                Object optionalSlotResult = inventoryClass.getDeclaredMethod("findFirstCurio", java.util.function.Predicate.class)
                        .invoke(curiosInventory, (java.util.function.Predicate<ItemStack>) stack ->
                            !stack.isEmpty() && stack.getItem() instanceof RedVeilItem);

                if (optionalSlotResult instanceof java.util.Optional<?> slotOpt && slotOpt.isPresent()) {
                    Object slotResult = slotOpt.get();
                    Class<?> slotResultClass = slotResult.getClass();

                    ItemStack stack = (ItemStack) slotResultClass.getDeclaredMethod("stack").invoke(slotResult);
                    ItemStack veilStack = stack.copy();

                    Object slotContext = slotResultClass.getDeclaredMethod("slotContext").invoke(slotResult);
                    Class<?> slotContextClass = slotContext.getClass();

                    String identifier = (String) slotContextClass.getDeclaredMethod("identifier").invoke(slotContext);
                    int index = (int) slotContextClass.getDeclaredMethod("index").invoke(slotContext);

                    inventoryClass.getDeclaredMethod("setEquippedCurio", String.class, int.class, ItemStack.class)
                            .invoke(curiosInventory, identifier, index, ItemStack.EMPTY);

                    if (!player.getInventory().add(veilStack)) {
                        target.drop(veilStack, false);
                        player.sendSystemMessage(Component.literal("§c背包已满，红盖头掉落在地上！").withStyle(ChatFormatting.RED));
                    } else {
                        player.sendSystemMessage(Component.literal("§a你轻轻摘下了 " + target.getName().getString() + " 的红盖头！").withStyle(ChatFormatting.GREEN));
                        target.sendSystemMessage(Component.literal("§a" + player.getName().getString() + " 轻轻摘下了你的红盖头！").withStyle(ChatFormatting.GREEN));

                        // 触发成就：红妆揭面
                        try {
                            MCARomanticExpansion.LOGGER.info("Attempting to trigger unveil_veil advancement for player: {}", player.getName().getString());
                            var trigger = CriterionTriggerRegister.UNVEIL_VEIL.get();
                            if (trigger != null) {
                                trigger.trigger(player);
                                MCARomanticExpansion.LOGGER.info("Successfully triggered unveil_veil advancement!");
                            } else {
                                MCARomanticExpansion.LOGGER.error("UnveilVeilTrigger is null, cannot trigger advancement");
                            }
                        } catch (Exception ex) {
                            MCARomanticExpansion.LOGGER.error("Failed to trigger unveil_veil advancement: {}", ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to check for red veil: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
