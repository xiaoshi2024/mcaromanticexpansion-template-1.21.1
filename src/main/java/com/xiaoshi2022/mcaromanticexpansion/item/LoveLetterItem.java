package com.xiaoshi2022.mcaromanticexpansion.item;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 情书物品
 * - 玩家潜行右键打开编辑界面，写入收信人名字和情话正文
 * - 右键打开阅读界面，查看情书内容
 * - 可以将情书夹藏在书本等物品中，收信人使用该物品时自动提取情书到背包
 */
public class LoveLetterItem extends Item {

    // NBT 键名
    private static final String KEY_RECIPIENT = "RecipientName";
    private static final String KEY_SENDER = "SenderName";
    private static final String KEY_SENDER_UUID = "SenderUUID";
    private static final String KEY_MESSAGE = "Message";
    private static final String KEY_WRITTEN = "Written";
    // 回信 NBT 键（回信写在同一封情书上，作为"第二页"）
    private static final String KEY_REPLY_MESSAGE = "ReplyMessage";
    private static final String KEY_REPLY_SENDER = "ReplySender";
    private static final String KEY_REPLY_SENDER_UUID = "ReplySenderUUID";
    private static final String KEY_HAS_REPLY = "HasReply";

    // 夹藏情书的容器 NBT 键
    public static final String KEY_CONCEALED_LETTER = "ConcealedLoveLetter";

    public LoveLetterItem(Properties properties) {
        super(properties);
    }

    // ========== NBT 基础操作 ==========

    private static CompoundTag getTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }

    private static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // ========== 情书数据读写 ==========

    public static boolean isWritten(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.getBoolean(KEY_WRITTEN);
    }

    public static String getRecipient(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.contains(KEY_RECIPIENT) ? tag.getString(KEY_RECIPIENT) : "";
    }

    public static String getSender(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.contains(KEY_SENDER) ? tag.getString(KEY_SENDER) : "";
    }

    public static String getSenderUUID(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.contains(KEY_SENDER_UUID) ? tag.getString(KEY_SENDER_UUID) : "";
    }

    public static String getMessage(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.contains(KEY_MESSAGE) ? tag.getString(KEY_MESSAGE) : "";
    }

    public static void writeLetter(ItemStack stack, Player sender, String recipientName, String message) {
        CompoundTag tag = getTag(stack);
        tag.putString(KEY_RECIPIENT, recipientName);
        tag.putString(KEY_SENDER, sender.getName().getString());
        tag.putString(KEY_SENDER_UUID, sender.getUUID().toString());
        tag.putString(KEY_MESSAGE, message);
        tag.putBoolean(KEY_WRITTEN, true);
        setTag(stack, tag);
    }

    // ========== 回信读写（写在同一封情书上） ==========

    public static boolean hasReply(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.getBoolean(KEY_HAS_REPLY);
    }

    public static String getReplyMessage(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.contains(KEY_REPLY_MESSAGE) ? tag.getString(KEY_REPLY_MESSAGE) : "";
    }

    public static String getReplySender(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.contains(KEY_REPLY_SENDER) ? tag.getString(KEY_REPLY_SENDER) : "";
    }

    public static String getReplySenderUUID(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.contains(KEY_REPLY_SENDER_UUID) ? tag.getString(KEY_REPLY_SENDER_UUID) : "";
    }

    /**
     * 在情书上写回信（第二页），不创建新物品
     */
    public static void writeReply(ItemStack stack, Player replier, String replyMessage) {
        CompoundTag tag = getTag(stack);
        tag.putString(KEY_REPLY_MESSAGE, replyMessage);
        tag.putString(KEY_REPLY_SENDER, replier.getName().getString());
        tag.putString(KEY_REPLY_SENDER_UUID, replier.getUUID().toString());
        tag.putBoolean(KEY_HAS_REPLY, true);
        setTag(stack, tag);
    }

    // ========== 夹藏与提取 ==========

    /**
     * 将情书夹藏在另一个物品中（如书本），消耗情书本身
     */
    public static boolean concealInItem(ItemStack letter, ItemStack container, Player player) {
        if (!(letter.getItem() instanceof LoveLetterItem)) return false;
        if (!isWritten(letter)) {
            player.sendSystemMessage(Component.literal("§c情书还没有写内容，无法夹藏！"));
            return false;
        }
        if (container.isEmpty()) return false;
        if (container.getItem() instanceof LoveLetterItem) {
            player.sendSystemMessage(Component.literal("§c不能把情书夹藏在另一封情书里！"));
            return false;
        }
        if (hasConcealedLetter(container)) {
            player.sendSystemMessage(Component.literal("§c该物品里已经夹藏了一封情书！"));
            return false;
        }

        CompoundTag containerTag = getTag(container);
        CompoundTag letterData = new CompoundTag();
        letterData.putString(KEY_RECIPIENT, getRecipient(letter));
        letterData.putString(KEY_SENDER, getSender(letter));
        letterData.putString(KEY_SENDER_UUID, getSenderUUID(letter));
        letterData.putString(KEY_MESSAGE, getMessage(letter));
        // 保存回信数据
        letterData.putBoolean(KEY_HAS_REPLY, hasReply(letter));
        if (hasReply(letter)) {
            letterData.putString(KEY_REPLY_MESSAGE, getReplyMessage(letter));
            letterData.putString(KEY_REPLY_SENDER, getReplySender(letter));
            letterData.putString(KEY_REPLY_SENDER_UUID, getReplySenderUUID(letter));
        }
        containerTag.put(KEY_CONCEALED_LETTER, letterData);
        setTag(container, containerTag);

        // 消耗情书
        letter.shrink(1);

        player.sendSystemMessage(Component.literal("§a你悄悄将情书夹藏在了 " + container.getDisplayName().getString() + " 里！"));
        player.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.8f, 1.2f);
        return true;
    }

    /**
     * 检查物品中是否夹藏了情书
     */
    public static boolean hasConcealedLetter(ItemStack container) {
        if (container.isEmpty()) return false;
        CompoundTag tag = getTag(container);
        return tag.contains(KEY_CONCEALED_LETTER);
    }

    /**
     * 获取夹藏情书的收信人名字
     */
    public static String getConcealedRecipient(ItemStack container) {
        CompoundTag tag = getTag(container);
        if (tag.contains(KEY_CONCEALED_LETTER)) {
            CompoundTag letterData = tag.getCompound(KEY_CONCEALED_LETTER);
            return letterData.getString(KEY_RECIPIENT);
        }
        return "";
    }

    /**
     * 获取夹藏情书的发信人名字（用于已回信的信件提取权限判断）
     */
    public static String getConcealedSender(ItemStack container) {
        CompoundTag tag = getTag(container);
        if (tag.contains(KEY_CONCEALED_LETTER)) {
            CompoundTag letterData = tag.getCompound(KEY_CONCEALED_LETTER);
            return letterData.getString(KEY_SENDER);
        }
        return "";
    }

    /**
     * 夹藏的情书是否已包含回信
     */
    public static boolean hasConcealedReply(ItemStack container) {
        CompoundTag tag = getTag(container);
        if (tag.contains(KEY_CONCEALED_LETTER)) {
            CompoundTag letterData = tag.getCompound(KEY_CONCEALED_LETTER);
            return letterData.getBoolean(KEY_HAS_REPLY);
        }
        return false;
    }

    /**
     * 从物品中提取夹藏的情书，返回新的情书物品栈，并清除容器上的夹藏数据
     */
    public static ItemStack extractFromItem(ItemStack container) {
        CompoundTag tag = getTag(container);
        if (!tag.contains(KEY_CONCEALED_LETTER)) return ItemStack.EMPTY;

        CompoundTag letterData = tag.getCompound(KEY_CONCEALED_LETTER);
        tag.remove(KEY_CONCEALED_LETTER);
        setTag(container, tag);

        ItemStack letter = new ItemStack(ModItems.LOVE_LETTER.get());
        CompoundTag letterTag = getTag(letter);
        letterTag.putString(KEY_RECIPIENT, letterData.getString(KEY_RECIPIENT));
        letterTag.putString(KEY_SENDER, letterData.getString(KEY_SENDER));
        letterTag.putString(KEY_SENDER_UUID, letterData.getString(KEY_SENDER_UUID));
        letterTag.putString(KEY_MESSAGE, letterData.getString(KEY_MESSAGE));
        letterTag.putBoolean(KEY_WRITTEN, true);
        // 恢复回信数据
        if (letterData.getBoolean(KEY_HAS_REPLY)) {
            letterTag.putBoolean(KEY_HAS_REPLY, true);
            letterTag.putString(KEY_REPLY_MESSAGE, letterData.getString(KEY_REPLY_MESSAGE));
            letterTag.putString(KEY_REPLY_SENDER, letterData.getString(KEY_REPLY_SENDER));
            letterTag.putString(KEY_REPLY_SENDER_UUID, letterData.getString(KEY_REPLY_SENDER_UUID));
        }
        setTag(letter, letterTag);

        return letter;
    }

    // ========== 物品交互 ==========

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // 潜行右键：检查是否要夹藏
            ItemStack offhand = player.getOffhandItem();
            if (!offhand.isEmpty()
                    && !(offhand.getItem() instanceof LoveLetterItem)) {
                // 服务端执行夹藏
                if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    concealInItem(stack, offhand, serverPlayer);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }

            // 没有副手物品：打开编辑界面
            if (level.isClientSide()) {
                openScreenOnClient(hand, stack, true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // 普通右键：阅读
        if (isWritten(stack)) {
            if (level.isClientSide()) {
                openScreenOnClient(hand, stack, false);
            }
        } else {
            if (level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("mcaromanticexpansion.love_letter.not_written")
                                .withStyle(ChatFormatting.YELLOW), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * 通过反射调用客户端方法打开 GUI，避免在服务端加载客户端类
     * 直接传递 ItemStack，客户端自行读取 NBT 数据
     */
    private static void openScreenOnClient(InteractionHand hand, ItemStack stack, boolean editMode) {
        try {
            Class<?> cls = Class.forName("com.xiaoshi2022.mcaromanticexpansion.client.LoveLetterClient");
            cls.getMethod("openScreen", InteractionHand.class, ItemStack.class, boolean.class)
                    .invoke(null, hand, stack, editMode);
        } catch (Exception ignored) {
            // 服务端忽略
        }
    }

    // ========== 显示 ==========

    @Override
    public boolean isFoil(ItemStack stack) {
        return isWritten(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (isWritten(stack)) {
            String recipient = getRecipient(stack);
            String sender = getSender(stack);
            if (!recipient.isEmpty()) {
                tooltip.add(Component.literal("§d致: §f" + recipient)
                        .withStyle(ChatFormatting.ITALIC));
            }
            if (!sender.isEmpty()) {
                tooltip.add(Component.literal("§7来自: §f" + sender));
            }
            if (hasReply(stack)) {
                String replySender = getReplySender(stack);
                tooltip.add(Component.literal("§b↩ 已回信: §f" + replySender)
                        .withStyle(ChatFormatting.ITALIC));
            }
            tooltip.add(Component.literal("§8右键阅读 · 潜行右键编辑")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("§7一封空白的情书")
                    .withStyle(ChatFormatting.ITALIC));
            tooltip.add(Component.literal("§8潜行右键写下你的情话")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
