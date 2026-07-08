package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.advancement.CriterionTriggerRegister;
import com.xiaoshi2022.mcaromanticexpansion.item.LoveLetterItem;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 客户端 -> 服务端：保存情书内容
 * - 普通模式：写信（收信人 + 正文）
 * - 回信模式：在已有情书上写回信（只有正文），不创建新物品，触发心动值+成就
 */
public record LoveLetterSavePacket(InteractionHand hand, String recipient, String message, boolean isReply) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LoveLetterSavePacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("love_letter_save"));

    public static final StreamCodec<FriendlyByteBuf, LoveLetterSavePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeEnum(packet.hand());
                buf.writeUtf(packet.recipient(), 64);
                buf.writeUtf(packet.message(), 1024);
                buf.writeBoolean(packet.isReply());
            },
            buf -> new LoveLetterSavePacket(
                    buf.readEnum(InteractionHand.class),
                    buf.readUtf(64),
                    buf.readUtf(1024),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof LoveLetterItem)) {
            return;
        }

        if (message.isBlank()) {
            player.sendSystemMessage(Component.literal("§c正文不能为空！"));
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
            player.sendSystemMessage(Component.literal("§c收信人不能为空！"));
            return;
        }
        LoveLetterItem.writeLetter(stack, player, recipient, message);
        player.sendSystemMessage(Component.literal("§a情书已写好！"));
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
            player.sendSystemMessage(Component.literal("§c这封情书还没有内容，无法回信！"));
            return;
        }
        if (LoveLetterItem.hasReply(stack)) {
            player.sendSystemMessage(Component.literal("§c这封情书已经回过信了！"));
            return;
        }

        // 不能给自己回信
        String senderUUIDStr = LoveLetterItem.getSenderUUID(stack);
        if (senderUUIDStr.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c找不到发信人信息，无法回信！"));
            return;
        }
        if (senderUUIDStr.equals(player.getUUID().toString())) {
            player.sendSystemMessage(Component.literal("§c不能给自己回信！"));
            return;
        }

        UUID senderUUID;
        try {
            senderUUID = UUID.fromString(senderUUIDStr);
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("§c发信人UUID无效，无法回信！"));
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
            sender.sendSystemMessage(Component.literal("§d" + player.getName().getString()
                    + " 回了你的情书！心动值 +3"));
            sender.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
        }

        // 通知回信人
        player.sendSystemMessage(Component.literal("§d回信已写好！"));
        if (sender != null) {
            player.sendSystemMessage(Component.literal("§d双方心动值 +3！"));
        } else {
            player.sendSystemMessage(Component.literal("§e" + senderName + " 目前不在线，心动值需对方在线才能增加。"));
        }
        player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8f, 1.2f);

        // 触发回信成就
        CriterionTriggerRegister.LOVE_LETTER_REPLY.get().trigger(player);

        MCARomanticExpansion.LOGGER.debug("Love letter reply by {} to {} (affection +3 both)",
                player.getName().getString(), senderName);
    }
}
