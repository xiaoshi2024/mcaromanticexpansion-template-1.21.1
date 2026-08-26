package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.advancement.CriterionTriggerRegister;
import com.xiaoshi2022.mcaromanticexpansion.api.event.MarriageChangedEvent;
import com.xiaoshi2022.mcaromanticexpansion.api.event.ProposalSentEvent;
import com.xiaoshi2022.mcaromanticexpansion.api.event.SharedUmbrellaRequestEvent;
import com.xiaoshi2022.mcaromanticexpansion.item.GiftBoxItem;
import com.xiaoshi2022.mcaromanticexpansion.item.LoveLetterItem;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
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
            player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.interact.self"));
            event.setCanceled(true);
            return;
        }

        var stack = event.getItemStack();
        var item = stack.getItem();

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
        } else if (UmbrellaItem.isUmbrella(stack)) {  // 修复：使用 isUmbrella 检查
            handleSharedUmbrella(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        } else if (stack.isEmpty()) {
            handleUnveilVeil(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        }
    }

    private static void handleSharedUmbrella(ServerPlayer player, ServerPlayer target) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // 检查主手是否是伞（任意状态）
        if (UmbrellaItem.isUmbrella(mainHand)) {
            if (UmbrellaItem.getState(mainHand) != UmbrellaItem.State.FULL_OPEN) {
                // 如果是基础伞（NBT存储方式），使用 setUmbrellaState
                if (mainHand.is(ModItems.UMBRELLA.get())) {
                    UmbrellaItem.setUmbrellaState(mainHand, UmbrellaItem.State.FULL_OPEN);
                } else {
                    // 如果是独立状态物品，替换为全开状态
                    ItemStack newStack = UmbrellaItem.getStackForState(UmbrellaItem.State.FULL_OPEN);
                    newStack.setCount(1);
                    player.setItemInHand(InteractionHand.MAIN_HAND, newStack);
                }
            }
        }
        // 检查副手是否是伞（任意状态）
        else if (UmbrellaItem.isUmbrella(offHand)) {
            if (UmbrellaItem.getState(offHand) != UmbrellaItem.State.FULL_OPEN) {
                if (offHand.is(ModItems.UMBRELLA.get())) {
                    UmbrellaItem.setUmbrellaState(offHand, UmbrellaItem.State.FULL_OPEN);
                } else {
                    ItemStack newStack = UmbrellaItem.getStackForState(UmbrellaItem.State.FULL_OPEN);
                    newStack.setCount(1);
                    player.setItemInHand(InteractionHand.OFF_HAND, newStack);
                }
            }
        } else {
            // 没有伞，不处理
            return;
        }

        SharedUmbrellaRequestEvent requestEvent = new SharedUmbrellaRequestEvent(player, target);
        NeoForge.EVENT_BUS.post(requestEvent);
        if (requestEvent.isCanceled()) {
            MCARomanticExpansion.LOGGER.debug("Shared umbrella request canceled by event listener for {} -> {}",
                    player.getName().getString(), target.getName().getString());
            return;
        }

        SharedUmbrellaManager.sendRequest(player, target);
    }

    private static void handleGiftBox(ServerPlayer giver, ServerPlayer receiver, ItemStack giftBoxStack) {
        // 检查礼盒是否有礼物
        if (!GiftBoxItem.hasGift(giftBoxStack)) {
            giver.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.gift_box.empty"));
            return;
        }

        // 修复: loadGiftItem 需要 Provider 参数，使用 receiver 的 registryAccess
        ItemStack giftItem = GiftBoxItem.loadGiftItem(giftBoxStack, receiver.registryAccess());

        // 尝试将礼物放入接收者背包
        boolean added = receiver.getInventory().add(giftItem);
        if (!added) {
            receiver.drop(giftItem, false);
            receiver.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.inventory.full.gift"));
        } else {
            receiver.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.gift.received", giver.getName().getString()));
            giver.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.gift.sent", receiver.getName().getString()));

            // 清空礼盒
            GiftBoxItem.clearGift(giftBoxStack);

            // 添加好感度：赠送礼物
            AffectionManager.handleInteraction(AffectionManager.InteractionType.GIFT, giver, receiver);
            MCARomanticExpansion.LOGGER.debug("Added GIFT affection for {} -> {}",
                    giver.getName().getString(), receiver.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        var stack = event.getItemStack();

        // 检查物品中是否夹藏了情书
        if (tryExtractConcealedLetter(player, stack)) {
            event.setCanceled(true);
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var item = stack.getItem();
        var hand = event.getHand();

        // 礼盒操作：Shift+右键放入物品
        if (item instanceof GiftBoxItem && player.isShiftKeyDown()) {
            if (GiftBoxItem.hasGift(stack)) {
                serverPlayer.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.gift_box.has_gift"));
                event.setCanceled(true);
                return;
            }

            // 获取另一只手的物品
            ItemStack otherHandItem = hand == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? serverPlayer.getOffhandItem()
                    : serverPlayer.getMainHandItem();

            if (!otherHandItem.isEmpty()) {
                if (otherHandItem.getItem() instanceof GiftBoxItem) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.gift_box.nested"));
                    event.setCanceled(true);
                    return;
                }

                GiftBoxItem.saveGiftItem(stack, otherHandItem);

                // 修复: 使用正确的方法清空另一只手的物品
                if (hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
                    // 清空副手 (副手槽位索引是 40)
                    serverPlayer.getInventory().setItem(40, ItemStack.EMPTY);
                } else {
                    // 清空主手 - 使用 getSelectedSlot() 获取当前选中的槽位
                    int selectedSlot = serverPlayer.getInventory().getSelectedSlot();
                    serverPlayer.getInventory().setItem(selectedSlot, ItemStack.EMPTY);
                }

                serverPlayer.sendSystemMessage(Component.translatable("mcaromanticexpansion.message.gift_placed"));
                event.setCanceled(true);
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.gift_box.need_other_hand"));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        var stack = event.getItemStack();

        // 检查物品中是否夹藏了情书
        if (tryExtractConcealedLetter(player, stack)) {
            event.setCanceled(true);
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var item = stack.getItem();

        // 右键方块打开礼盒
        if (item instanceof GiftBoxItem) {
            // 检查是否有礼物
            if (!GiftBoxItem.hasGift(stack)) {
                serverPlayer.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.gift_box.empty"));
                event.setCanceled(true);
                return;
            }

            // 检查是否是生日礼盒
            if (GiftBoxItem.isBirthdayGift(stack)) {
                GiftBoxItem.openBirthdayGift(serverPlayer, stack);
                event.setCanceled(true);
                return;
            }

            // 修复: loadGiftItem 需要 Provider 参数
            ItemStack giftItem = GiftBoxItem.loadGiftItem(stack, serverPlayer.registryAccess());
            if (!giftItem.isEmpty()) {
                boolean added = serverPlayer.getInventory().add(giftItem);
                if (!added) {
                    serverPlayer.drop(giftItem, false);
                    serverPlayer.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.inventory.full.gift"));
                } else {
                    serverPlayer.sendSystemMessage(Component.translatable("mcaromanticexpansion.message.gift_opened"));
                }
                GiftBoxItem.clearGift(stack);
                event.setCanceled(true);
            }
        }
    }

    /**
     * 尝试从物品中提取夹藏的情书
     * @return true 如果已提取（调用方应取消事件），false 如果未处理
     */
    private static boolean tryExtractConcealedLetter(Player player, ItemStack stack) {
        if (!LoveLetterItem.hasConcealedLetter(stack)) {
            return false;
        }

        String recipient = LoveLetterItem.getConcealedRecipient(stack);
        String playerName = player.getName().getString();

        // 收信人可以提取；如果情书已回信，原发信人也可以提取（查看回信）
        boolean isRecipient = playerName.equalsIgnoreCase(recipient);
        boolean isSenderWithReply = LoveLetterItem.hasConcealedReply(stack)
                && playerName.equalsIgnoreCase(LoveLetterItem.getConcealedSender(stack));

        if (!isRecipient && !isSenderWithReply) {
            // 既不是收信人，也不是已回信的发信人，不提取
            return false;
        }

        // 是收信人，提取情书
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack letter = LoveLetterItem.extractFromItem(stack);
            boolean added = serverPlayer.getInventory().add(letter);
            if (!added) {
                serverPlayer.drop(letter, false);
                serverPlayer.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.inventory.full.letter"));
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.discovered"));
            }
            serverPlayer.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8f, 1.2f);
            serverPlayer.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.5f);

            MCARomanticExpansion.LOGGER.debug("Love letter extracted by {} (from {})",
                    playerName, LoveLetterItem.getSender(letter));
        }

        return true;
    }

    private static void handleBouquet(ServerPlayer sender, ServerPlayer receiver) {
        if (CooldownManager.isOnCooldown(sender.getUUID(), "bouquet")) {
            long remaining = CooldownManager.getRemainingCooldown(sender.getUUID(), "bouquet");
            sender.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.cooldown.bouquet", remaining / 1000));
            return;
        }

        sendBouquetRequest(sender, receiver);
        CooldownManager.setCooldown(sender.getUUID(), "bouquet");
    }

    private static void handleProposal(ServerPlayer proposer, ServerPlayer target) {
        if (CooldownManager.isOnCooldown(proposer.getUUID(), "proposal")) {
            long remaining = CooldownManager.getRemainingCooldown(proposer.getUUID(), "proposal");
            proposer.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.cooldown.proposal", remaining / 1000));
            return;
        }

        // ===== 使用强制读取，不使用缓存 =====
        Gender proposerGender = PregnancyAttemptHandler.getGenderFromMCAForce(proposer);
        Gender targetGender = PregnancyAttemptHandler.getGenderFromMCAForce(target);

        MCARomanticExpansion.LOGGER.debug("Proposal gender check: {} is {}, {} is {}",
                proposer.getName().getString(), proposerGender,
                target.getName().getString(), targetGender);

        if (proposerGender == Gender.UNASSIGNED || targetGender == Gender.UNASSIGNED) {
            if (proposerGender == Gender.UNASSIGNED) {
                proposer.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.need_set_gender"));
            }
            if (targetGender == Gender.UNASSIGNED) {
                proposer.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.proposal.target_no_gender"));
            }
            return;
        }

        if (proposerGender == targetGender) {
            boolean proposerAllowed = MarriageConfig.isSameGenderMarriageAllowed(proposer);
            boolean targetAllowed = MarriageConfig.isSameGenderMarriageAllowed(target);

            if (!proposerAllowed || !targetAllowed) {
                proposer.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.proposal.same_gender_blocked"));
                return;
            }
        }

        sendProposalRequest(proposer, target);
        CooldownManager.setCooldown(proposer.getUUID(), "proposal");
        proposer.sendSystemMessage(Component.translatable("mcaromanticexpansion.message.proposal_sent", target.getName().getString()));
    }

    private static void handleMarriage(ServerPlayer sender, ServerPlayer target) {
        if (CooldownManager.isOnCooldown(sender.getUUID(), "marriage")) {
            long remaining = CooldownManager.getRemainingCooldown(sender.getUUID(), "marriage");
            sender.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.cooldown.marriage", remaining / 1000));
            return;
        }

        // ===== 使用强制读取，不使用缓存 =====
        Gender senderGender = PregnancyAttemptHandler.getGenderFromMCAForce(sender);
        Gender targetGender = PregnancyAttemptHandler.getGenderFromMCAForce(target);

        MCARomanticExpansion.LOGGER.debug("Marriage gender check: {} is {}, {} is {}",
                sender.getName().getString(), senderGender,
                target.getName().getString(), targetGender);

        if (senderGender == Gender.UNASSIGNED || targetGender == Gender.UNASSIGNED) {
            sender.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.marriage.need_gender"));
            return;
        }

        if (senderGender == targetGender) {
            boolean senderAllowed = MarriageConfig.isSameGenderMarriageAllowed(sender);
            boolean targetAllowed = MarriageConfig.isSameGenderMarriageAllowed(target);

            if (!senderAllowed || !targetAllowed) {
                sender.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.marriage.same_gender_blocked"));
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
            sender.sendSystemMessage(Component.translatable("mcaromanticexpansion.marriage.missing_ring"));
            return;
        }

        sendMarriageRequest(sender, target);
        CooldownManager.setCooldown(sender.getUUID(), "marriage");
        sender.sendSystemMessage(Component.translatable("mcaromanticexpansion.message.marriage_sent", target.getName().getString()));
    }

    private static void handleDivorcePapers(ServerPlayer sender, ServerPlayer target) {
        PlayerSaveData senderData = PlayerSaveData.get(sender);

        boolean senderIsMarried = senderData.getRelationshipState() == RelationshipState.MARRIED_TO_PLAYER;
        boolean targetIsSpouse = senderData.getPartnerUUID().isPresent() &&
                senderData.getPartnerUUID().get().equals(target.getUUID());

        if (!senderIsMarried || !targetIsSpouse) {
            sender.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.divorce.not_married"));
            return;
        }

        MarriageChangedEvent forgeEvent = new MarriageChangedEvent(
                sender, target, MarriageChangedEvent.ChangeType.DIVORCED
        );
        NeoForge.EVENT_BUS.post(forgeEvent);
        if (forgeEvent.isCanceled()) {
            MCARomanticExpansion.LOGGER.debug("Divorce canceled by event listener for {} -> {}",
                    sender.getName().getString(), target.getName().getString());
            return;
        }

        ServerInteractionManager.getInstance().endMarriage(sender);

        for (int i = 0; i < sender.getInventory().getContainerSize(); i++) {
            var stack = sender.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ItemsMCA.DIVORCE_PAPERS)) {
                stack.shrink(1);
                break;
            }
        }

        sender.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.divorce.sender", target.getName().getString()));
        target.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.divorce.target", sender.getName().getString()));
    }

    private static void sendProposalRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenProposalGUIPacket packet = new OpenProposalGUIPacket(sender.getUUID(), sender.getName().getString());
        MCARomanticExpansion.LOGGER.debug("Sending OpenProposalGUIPacket to {} from {}",
                receiver.getName().getString(), sender.getName().getString());
        receiver.connection.send(packet);
    }

    private static void sendBouquetRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenBouquetGUIPacket packet = new OpenBouquetGUIPacket(sender.getUUID(), sender.getName().getString());
        MCARomanticExpansion.LOGGER.debug("Sending OpenBouquetGUIPacket to {} from {}",
                receiver.getName().getString(), sender.getName().getString());
        receiver.connection.send(packet);
    }

    private static void sendMarriageRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenMarriageGUIPacket packet = new OpenMarriageGUIPacket(sender.getUUID(), sender.getName().getString());
        MCARomanticExpansion.LOGGER.debug("Sending OpenMarriageGUIPacket to {} from {}",
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
                        player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.inventory.full.veil"));
                    } else {
                        player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.unveil_veil.player", target.getName().getString()));
                        target.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.unveil_veil.target", player.getName().getString()));

                        try {
                            MCARomanticExpansion.LOGGER.debug("Attempting to trigger unveil_veil advancement for player: {}", player.getName().getString());
                            var trigger = CriterionTriggerRegister.UNVEIL_VEIL.get();
                            if (trigger != null) {
                                trigger.trigger(player);
                                MCARomanticExpansion.LOGGER.debug("Successfully triggered unveil_veil advancement!");
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