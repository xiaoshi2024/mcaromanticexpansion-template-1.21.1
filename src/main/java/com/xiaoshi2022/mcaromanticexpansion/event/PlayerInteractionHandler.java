package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.advancement.ModAdvancements;
import com.xiaoshi2022.mcaromanticexpansion.api.event.MarriageChangedEvent;
import com.xiaoshi2022.mcaromanticexpansion.api.event.ProposalSentEvent;
import com.xiaoshi2022.mcaromanticexpansion.api.event.SharedUmbrellaRequestEvent;
import com.xiaoshi2022.mcaromanticexpansion.item.GiftBoxItem;
import com.xiaoshi2022.mcaromanticexpansion.item.LoveLetterItem;
import com.xiaoshi2022.mcaromanticexpansion.item.RedVeilItem;
import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
import com.xiaoshi2022.mcaromanticexpansion.network.ModNetwork;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenBouquetGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenMarriageGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenProposalGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import com.xiaoshi2022.mcaromanticexpansion.util.CooldownManager;
import com.xiaoshi2022.mcaromanticexpansion.util.MarriageConfig;
import com.xiaoshi2022.mcaromanticexpansion.util.SharedUmbrellaManager;
import forge.net.mca.entity.ai.relationship.Gender;
import forge.net.mca.entity.ai.relationship.RelationshipState;
import forge.net.mca.item.BouquetItem;
import forge.net.mca.item.EngagementRingItem;
import forge.net.mca.item.ItemsMCA;
import forge.net.mca.item.WeddingRingItem;
import forge.net.mca.server.ServerInteractionManager;
import forge.net.mca.server.world.data.PlayerSaveData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkDirection;

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
        } else if (item == ItemsMCA.DIVORCE_PAPERS.get()) {
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

        SharedUmbrellaRequestEvent requestEvent = new SharedUmbrellaRequestEvent(player, target);
        if (MinecraftForge.EVENT_BUS.post(requestEvent)) {
            MCARomanticExpansion.LOGGER.debug("Shared umbrella request canceled by event listener for {} -> {}",
                    player.getName().getString(), target.getName().getString());
            return;
        }

        SharedUmbrellaManager.sendRequest(player, target);
    }

    private static void handleGiftBox(ServerPlayer giver, ServerPlayer receiver, ItemStack giftBoxStack) {
        if (!GiftBoxItem.hasGift(giftBoxStack)) {
            giver.sendSystemMessage(Component.literal("§c礼盒是空的！请先放入物品。").withStyle(ChatFormatting.RED));
            return;
        }

        ItemStack giftItem = GiftBoxItem.loadGiftItem(giftBoxStack);

        boolean added = receiver.getInventory().add(giftItem);
        if (!added) {
            receiver.drop(giftItem, false);
            receiver.sendSystemMessage(Component.literal("§c背包已满，礼物掉落在了地上！").withStyle(ChatFormatting.RED));
        } else {
            receiver.sendSystemMessage(Component.literal("§a你收到了 " + giver.getName().getString() + " 的礼物！").withStyle(ChatFormatting.GREEN));
            giver.sendSystemMessage(Component.literal("§a你成功赠送了礼物给 " + receiver.getName().getString() + "！").withStyle(ChatFormatting.GREEN));

            GiftBoxItem.clearGift(giftBoxStack);

            AffectionManager.handleInteraction(AffectionManager.InteractionType.GIFT, giver, receiver);
            MCARomanticExpansion.LOGGER.debug("Added GIFT affection for {} -> {}",
                    giver.getName().getString(), receiver.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        var stack = event.getItemStack();

        if (tryExtractConcealedLetter(player, stack)) {
            event.setCanceled(true);
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var item = stack.getItem();
        var hand = event.getHand();

        if (item instanceof GiftBoxItem && player.isShiftKeyDown()) {
            if (GiftBoxItem.hasGift(stack)) {
                serverPlayer.sendSystemMessage(Component.literal("§c礼盒中已经有礼物了！不能重复放入！")
                        .withStyle(ChatFormatting.RED));
                event.setCanceled(true);
                return;
            }

            ItemStack otherHandItem = hand == InteractionHand.MAIN_HAND
                    ? serverPlayer.getOffhandItem()
                    : serverPlayer.getMainHandItem();

            if (!otherHandItem.isEmpty()) {
                if (otherHandItem.getItem() instanceof GiftBoxItem) {
                    serverPlayer.sendSystemMessage(Component.literal("§c不能将礼盒放入另一个礼盒中！")
                            .withStyle(ChatFormatting.RED));
                    event.setCanceled(true);
                    return;
                }

                GiftBoxItem.saveGiftItem(stack, otherHandItem);

                if (hand == InteractionHand.MAIN_HAND) {
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
        var stack = event.getItemStack();

        if (tryExtractConcealedLetter(player, stack)) {
            event.setCanceled(true);
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var item = stack.getItem();

        if (item instanceof GiftBoxItem) {
            if (!GiftBoxItem.hasGift(stack)) {
                serverPlayer.sendSystemMessage(Component.literal("§c礼盒是空的！请先放入物品。")
                        .withStyle(ChatFormatting.RED));
                event.setCanceled(true);
                return;
            }

            if (GiftBoxItem.isBirthdayGift(stack)) {
                GiftBoxItem.openBirthdayGift(serverPlayer, stack);
                event.setCanceled(true);
                return;
            }

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

    private static boolean tryExtractConcealedLetter(Player player, ItemStack stack) {
        if (!LoveLetterItem.hasConcealedLetter(stack)) {
            return false;
        }

        String recipient = LoveLetterItem.getConcealedRecipient(stack);
        String playerName = player.getName().getString();

        boolean isRecipient = playerName.equalsIgnoreCase(recipient);
        boolean isSenderWithReply = LoveLetterItem.hasConcealedReply(stack)
                && playerName.equalsIgnoreCase(LoveLetterItem.getConcealedSender(stack));

        if (!isRecipient && !isSenderWithReply) {
            return false;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack letter = LoveLetterItem.extractFromItem(stack);
            boolean added = serverPlayer.getInventory().add(letter);
            if (!added) {
                serverPlayer.drop(letter, false);
                serverPlayer.sendSystemMessage(Component.literal("§c背包已满，情书掉落在了地上！")
                        .withStyle(ChatFormatting.RED));
            } else {
                serverPlayer.sendSystemMessage(Component.literal("§d§l你发现了一封情书！§r§d快打开看看吧~")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
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
            sender.sendSystemMessage(Component.literal("§c请等待 " + (remaining / 1000) + " 秒后再送花束！")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        sendBouquetRequest(sender, receiver);
        CooldownManager.setCooldown(sender.getUUID(), "bouquet");
    }

    // ==================== 🆕 修复后的求婚方法 ====================
    private static void handleProposal(ServerPlayer proposer, ServerPlayer target) {
        if (CooldownManager.isOnCooldown(proposer.getUUID(), "proposal")) {
            long remaining = CooldownManager.getRemainingCooldown(proposer.getUUID(), "proposal");
            proposer.sendSystemMessage(Component.literal("§c请等待 " + (remaining / 1000) + " 秒后再求婚！")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // ========== 🆕 强制修复性别数据 ==========
        PregnancyAttemptHandler.forceFixGenderData(proposer);
        PregnancyAttemptHandler.forceFixGenderData(target);

        // ========== 诊断性别数据 ==========
        PregnancyAttemptHandler.diagnoseGenderData(proposer);
        PregnancyAttemptHandler.diagnoseGenderData(target);

        // ========== 检查性别 ==========
        Gender proposerGender = PregnancyAttemptHandler.getGenderFromNBT(proposer);
        Gender targetGender = PregnancyAttemptHandler.getGenderFromNBT(target);

        MCARomanticExpansion.LOGGER.info("🔍 [Proposal] {} gender: {}, {} gender: {}",
                proposer.getName().getString(), proposerGender,
                target.getName().getString(), targetGender);

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

        ProposalSentEvent sentEvent = new ProposalSentEvent(proposer, target);
        if (MinecraftForge.EVENT_BUS.post(sentEvent)) {
            MCARomanticExpansion.LOGGER.debug("Proposal canceled by event listener for {} -> {}",
                    proposer.getName().getString(), target.getName().getString());
            return;
        }

        sendProposalRequest(proposer, target);
        CooldownManager.setCooldown(proposer.getUUID(), "proposal");
        proposer.sendSystemMessage(Component.literal("§a已向 " + target.getName().getString() + " 发送求婚请求！").withStyle(ChatFormatting.GREEN));
    }

    // ==================== 🆕 修复后的结婚方法 ====================
    private static void handleMarriage(ServerPlayer sender, ServerPlayer target) {
        if (CooldownManager.isOnCooldown(sender.getUUID(), "marriage")) {
            long remaining = CooldownManager.getRemainingCooldown(sender.getUUID(), "marriage");
            sender.sendSystemMessage(Component.literal("§c请等待 " + (remaining / 1000) + " 秒后再举行婚礼！")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // ========== 🆕 强制修复性别数据 ==========
        PregnancyAttemptHandler.forceFixGenderData(sender);
        PregnancyAttemptHandler.forceFixGenderData(target);

        // ========== 检查性别 ==========
        Gender senderGender = PregnancyAttemptHandler.getGenderFromNBT(sender);
        Gender targetGender = PregnancyAttemptHandler.getGenderFromNBT(target);

        MCARomanticExpansion.LOGGER.info("🔍 [Marriage] {} gender: {}, {} gender: {}",
                sender.getName().getString(), senderGender,
                target.getName().getString(), targetGender);

        if (senderGender == Gender.UNASSIGNED || targetGender == Gender.UNASSIGNED) {
            sender.sendSystemMessage(Component.literal("§c请使用 /mca editor 打开编辑器设置性别后再结婚！")
                    .withStyle(ChatFormatting.RED));
            return;
        }

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
        PlayerSaveData senderData = PlayerSaveData.get(sender);

        boolean senderIsMarried = senderData.getRelationshipState() == RelationshipState.MARRIED_TO_PLAYER;
        boolean targetIsSpouse = senderData.getPartnerUUID().isPresent() &&
                senderData.getPartnerUUID().get().equals(target.getUUID());

        if (!senderIsMarried || !targetIsSpouse) {
            sender.sendSystemMessage(Component.literal("§c只有已婚玩家才能向配偶递交离婚协议书！").withStyle(ChatFormatting.RED));
            return;
        }

        MarriageChangedEvent forgeEvent = new MarriageChangedEvent(
                sender, target, MarriageChangedEvent.ChangeType.DIVORCED
        );
        if (MinecraftForge.EVENT_BUS.post(forgeEvent)) {
            MCARomanticExpansion.LOGGER.debug("Divorce canceled by event listener for {} -> {}",
                    sender.getName().getString(), target.getName().getString());
            return;
        }

        ServerInteractionManager.getInstance().endMarriage(sender);

        for (int i = 0; i < sender.getInventory().getContainerSize(); i++) {
            var stack = sender.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == ItemsMCA.DIVORCE_PAPERS.get()) {
                stack.shrink(1);
                break;
            }
        }

        sender.sendSystemMessage(Component.literal("§c你与 " + target.getName().getString() + " 的婚姻已结束。").withStyle(ChatFormatting.RED));
        target.sendSystemMessage(Component.literal("§c" + sender.getName().getString() + " 已递交离婚协议书，你们的婚姻已结束。").withStyle(ChatFormatting.RED));
    }

    private static void sendProposalRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenProposalGUIPacket packet = new OpenProposalGUIPacket(sender.getUUID(), sender.getName().getString());
        MCARomanticExpansion.LOGGER.debug("Sending OpenProposalGUIPacket to {} from {}",
                receiver.getName().getString(), sender.getName().getString());
        ModNetwork.CHANNEL.sendTo(packet, receiver.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static void sendBouquetRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenBouquetGUIPacket packet = new OpenBouquetGUIPacket(sender.getUUID(), sender.getName().getString());
        MCARomanticExpansion.LOGGER.debug("Sending OpenBouquetGUIPacket to {} from {}",
                receiver.getName().getString(), sender.getName().getString());
        ModNetwork.CHANNEL.sendTo(packet, receiver.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static void sendMarriageRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenMarriageGUIPacket packet = new OpenMarriageGUIPacket(sender.getUUID(), sender.getName().getString());
        MCARomanticExpansion.LOGGER.debug("Sending OpenMarriageGUIPacket to {} from {}",
                receiver.getName().getString(), sender.getName().getString());
        ModNetwork.CHANNEL.sendTo(packet, receiver.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static void handleUnveilVeil(ServerPlayer player, ServerPlayer target) {
        MCARomanticExpansion.LOGGER.info("🔵 [UnveilVeil] {} right-clicked {}",
                player.getName().getString(), target.getName().getString());

        if (!com.xiaoshi2022.mcaromanticexpansion.compat.curios.CuriosIntegration.isCuriosAvailable()) {
            MCARomanticExpansion.LOGGER.warn("❌ Curios not available");
            return;
        }

        PlayerSaveData playerData = PlayerSaveData.get(player);
        if (playerData == null) {
            MCARomanticExpansion.LOGGER.warn("❌ PlayerSaveData is null for {}", player.getName().getString());
            return;
        }

        boolean playerIsMarried = playerData.getRelationshipState() == RelationshipState.MARRIED_TO_PLAYER;
        boolean targetIsSpouse = playerData.getPartnerUUID().isPresent() &&
                playerData.getPartnerUUID().get().equals(target.getUUID());

        MCARomanticExpansion.LOGGER.info("🔵 Married: {}, IsSpouse: {}", playerIsMarried, targetIsSpouse);

        if (!playerIsMarried || !targetIsSpouse) {
            MCARomanticExpansion.LOGGER.warn("❌ Not married or not spouse");
            return;
        }

        try {
            // ========== 🆕 使用 Curios Capability 直接获取 ==========
            var curiosInventoryOpt = target.getCapability(
                    top.theillusivec4.curios.api.CuriosCapability.INVENTORY);

            if (!curiosInventoryOpt.isPresent()) {
                MCARomanticExpansion.LOGGER.info("❌ No curios inventory for {}", target.getName().getString());
                return;
            }

            var curiosInventory = curiosInventoryOpt.resolve().get();

            // 查找红盖头
            var optionalSlotResult = curiosInventory.findFirstCurio(stack ->
                    !stack.isEmpty() && stack.getItem() instanceof RedVeilItem);

            if (optionalSlotResult.isEmpty()) {
                MCARomanticExpansion.LOGGER.info("❌ No red veil found in curios slots for {}", target.getName().getString());
                return;
            }

            var slotResult = optionalSlotResult.get();
            ItemStack veilStack = slotResult.stack().copy();
            var slotContext = slotResult.slotContext();

            // 移除红盖头
            curiosInventory.setEquippedCurio(slotContext.identifier(), slotContext.index(), ItemStack.EMPTY);

            MCARomanticExpansion.LOGGER.info("✅ Removed red veil from {} slot {}",
                    slotContext.identifier(), slotContext.index());

            // 将红盖头给玩家
            if (!player.getInventory().add(veilStack)) {
                target.drop(veilStack, false);
                player.sendSystemMessage(Component.literal("§c背包已满，红盖头掉落在地上！").withStyle(ChatFormatting.RED));
            } else {
                player.sendSystemMessage(Component.literal("§a你轻轻摘下了 " + target.getName().getString() + " 的红盖头！").withStyle(ChatFormatting.GREEN));
                target.sendSystemMessage(Component.literal("§a" + player.getName().getString() + " 轻轻摘下了你的红盖头！").withStyle(ChatFormatting.GREEN));

                try {
                    MCARomanticExpansion.LOGGER.info("🔵 Triggering unveil_veil achievement for {}", player.getName().getString());
                    ModAdvancements.triggerUnveilVeil(player);
                    MCARomanticExpansion.LOGGER.info("✅ UnveilVeil achievement triggered successfully!");
                } catch (Exception ex) {
                    MCARomanticExpansion.LOGGER.error("Failed to trigger unveil_veil advancement: {}", ex.getMessage());
                }
            }

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.error("Failed to handle unveil veil", e);
            e.printStackTrace();
        }
    }
}