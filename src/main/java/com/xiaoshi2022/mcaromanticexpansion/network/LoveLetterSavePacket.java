package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.advancement.ModAdvancements;
import com.xiaoshi2022.mcaromanticexpansion.item.LoveLetterItem;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 客户端 -> 服务端：保存情书内容
 * - 普通模式：写信（收信人 + 正文）
 * - 回信模式：在已有情书上写回信（只有正文），不创建新物品，触发心动值+成就
 */
public class LoveLetterSavePacket {
    private final InteractionHand hand;
    private final String recipient;
    private final String message;
    private final boolean isReply;

    public LoveLetterSavePacket(InteractionHand hand, String recipient, String message, boolean isReply) {
        this.hand = hand;
        this.recipient = recipient;
        this.message = message;
        this.isReply = isReply;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public boolean isReply() {
        return isReply;
    }

    // 编码方法
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        buf.writeUtf(recipient, 64);
        buf.writeUtf(message, 1024);
        buf.writeBoolean(isReply);
    }

    // 解码方法
    public static LoveLetterSavePacket decode(FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        String recipient = buf.readUtf(64);
        String message = buf.readUtf(1024);
        boolean isReply = buf.readBoolean();
        return new LoveLetterSavePacket(hand, recipient, message, isReply);
    }

    // 处理消息
    public static void handle(LoveLetterSavePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            packet.handle(player);
        });
        context.setPacketHandled(true);
    }

    public void handle(ServerPlayer player) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof LoveLetterItem)) {
            return;
        }

        if (message.isBlank()) {
            player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.empty_message"));
            return;
        }

        if (isReply) {
            handleReply(stack, player);
        } else {
            handleWriteLetter(stack, player);
        }
    }

    /** 写信 */
    private void handleWriteLetter(ItemStack stack, ServerPlayer player) {
        if (recipient.isBlank()) {
            player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.empty_recipient"));
            return;
        }
        LoveLetterItem.writeLetter(stack, player, recipient, message);
        player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.written"));
        player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8f, 1.2f);
        MCARomanticExpansion.LOGGER.debug("Love letter saved by {} to {}",
                player.getName().getString(), recipient);
    }

    /**
     * 写回信（写在同一封情书上，不创建新物品）
     * 1. 检查情书已写好且没有回信
     * 2. 写入回信数据到同一物品NBT
     * 3. 双方各加心动值 +3
     * 4. 触发回信成就
     */
    private void handleReply(ItemStack stack, ServerPlayer player) {
        if (!LoveLetterItem.isWritten(stack)) {
            player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.reply_not_written"));
            return;
        }
        if (LoveLetterItem.hasReply(stack)) {
            player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.already_replied"));
            return;
        }

        // 不能给自己回信
        String senderUUIDStr = LoveLetterItem.getSenderUUID(stack);
        if (senderUUIDStr.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.no_sender_info"));
            return;
        }
        if (senderUUIDStr.equals(player.getUUID().toString())) {
            player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.cannot_reply_self"));
            return;
        }

        UUID senderUUID;
        try {
            senderUUID = UUID.fromString(senderUUIDStr);
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.invalid_sender_uuid"));
            return;
        }

        // 写入回信到同一物品
        LoveLetterItem.writeReply(stack, player, message);

        // 查找原发信人是否在线
        ServerPlayer sender = player.server.getPlayerList().getPlayer(senderUUID);
        String senderName = LoveLetterItem.getSender(stack);

        // 双方加心动值（+3）
        if (sender != null) {
            AffectionManager.addAffection(player, sender, 3);
            AffectionManager.addAffection(sender, player, 3);
            sender.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.replied_notification", player.getName()));
            sender.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
        }

        // 通知回信人
        player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.reply_written"));
        if (sender != null) {
            player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.affection_increased"));
        } else {
            player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.love_letter.sender_offline", senderName));
        }
        player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8f, 1.2f);

        // 【修复】使用 ModAdvancements 替代 CriterionTriggerRegister
        ModAdvancements.triggerLoveLetterReply(player);

        MCARomanticExpansion.LOGGER.debug("Love letter reply by {} to {} (affection +3 both)",
                player.getName().getString(), senderName);
    }
}