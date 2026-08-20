package com.xiaoshi2022.mcaromanticexpansion.client.gui;

import com.xiaoshi2022.mcaromanticexpansion.item.LoveLetterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 情书阅读界面
 * 显示格式：
 *   ✉ 情书 ✉
 *   ─────────────
 *   致 XXX
 *   （正文内容）
 *   —— 来自 YYY
 *
 *   ── 回信 ──       （如果有回信）
 *   （回信内容）
 *   —— 回信 ZZZ
 *
 *   [回信]  [收起]   （如果没有回信，显示回信按钮）
 */
@OnlyIn(Dist.CLIENT)
public class LoveLetterReadScreen extends Screen {

    private final InteractionHand hand;
    private final ItemStack letterStack;
    private final String recipient;
    private final String sender;
    private final String message;
    private final boolean hasReply;
    private final String replyMessage;
    private final String replySender;

    private static final int BOX_WIDTH = 260;
    private static final int MIN_BOX_HEIGHT = 140;

    private List<FormattedCharSequence> wrappedMessageLines = new ArrayList<>();
    private List<FormattedCharSequence> wrappedReplyLines = new ArrayList<>();
    private int boxHeight = MIN_BOX_HEIGHT;

    public LoveLetterReadScreen(InteractionHand hand, ItemStack stack) {
        super(Component.translatable("mcaromanticexpansion.gui.love_letter.read.title"));
        this.hand = hand;
        this.letterStack = stack;
        this.recipient = LoveLetterItem.getRecipient(stack);
        this.sender = LoveLetterItem.getSender(stack);
        this.message = LoveLetterItem.getMessage(stack);
        this.hasReply = LoveLetterItem.hasReply(stack);
        this.replyMessage = LoveLetterItem.getReplyMessage(stack);
        this.replySender = LoveLetterItem.getReplySender(stack);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 预先计算正文换行
        int maxTextWidth = BOX_WIDTH - 30;
        Component messageComponent = Component.literal(message.isEmpty() ? "..." : message);
        this.wrappedMessageLines = this.font.split(messageComponent, maxTextWidth);

        // 如果有回信，计算回信换行
        if (hasReply) {
            Component replyComponent = Component.literal(replyMessage);
            this.wrappedReplyLines = this.font.split(replyComponent, maxTextWidth);
        }

        // 动态计算高度
        int contentHeight = 60 + wrappedMessageLines.size() * 10;
        if (hasReply) {
            contentHeight += 40 + wrappedReplyLines.size() * 10; // 回信区域
        }
        contentHeight += 40; // 按钮区域
        this.boxHeight = Math.max(MIN_BOX_HEIGHT, contentHeight);

        int boxY = centerY - boxHeight / 2;

        if (!hasReply) {
            // 没有回信：显示回信按钮
            this.addRenderableWidget(Button.builder(
                    Component.translatable("mcaromanticexpansion.gui.love_letter.reply"),
                    button -> startReply()
            ).pos(centerX - 90, boxY + boxHeight - 28).size(80, 20).build());
        }

        // 关闭按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("mcaromanticexpansion.gui.love_letter.close"),
                button -> this.onClose()
        ).pos(centerX + 10, boxY + boxHeight - 28).size(80, 20).build());
    }

    /**
     * 打开回信编辑界面（直接在客户端打开，不创建新物品）
     */
    private void startReply() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new LoveLetterEditScreen(hand, true));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int boxX = centerX - BOX_WIDTH / 2;
        int boxY = centerY - boxHeight / 2;

        // 信纸背景
        guiGraphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + boxHeight, 0xCC2a1018);
        guiGraphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + 2, 0xFFd4457a);
        guiGraphics.fill(boxX, boxY + boxHeight - 2, boxX + BOX_WIDTH, boxY + boxHeight, 0xFFd4457a);
        guiGraphics.fill(boxX, boxY, boxX + 2, boxY + boxHeight, 0xFFd4457a);
        guiGraphics.fill(boxX + BOX_WIDTH - 2, boxY, boxX + BOX_WIDTH, boxY + boxHeight, 0xFFd4457a);

        int textX = boxX + 15;
        int currentY = boxY + 12;

        // 标题
        guiGraphics.drawCenteredString(this.font, this.title, centerX, currentY, 0xFFFFB6C1);
        currentY += 14;

        // 分隔线
        guiGraphics.hLine(boxX + 20, boxX + BOX_WIDTH - 20, currentY, 0xAAFF69B4);
        currentY += 12;

        // "致 XXX"
        guiGraphics.drawString(this.font,
                Component.translatable("mcaromanticexpansion.gui.love_letter.read.to", recipient), textX, currentY, 0xFFFFFFFF);
        currentY += 16;
        currentY += 4;

        // 正文
        for (FormattedCharSequence line : wrappedMessageLines) {
            guiGraphics.drawString(this.font, line, textX, currentY, 0xFFEEEEEE);
            currentY += 10;
        }
        currentY += 8;

        // "—— 来自 YYY"
        Component fromLine = Component.translatable("mcaromanticexpansion.gui.love_letter.read.from", sender);
        int fromWidth = this.font.width(fromLine);
        guiGraphics.drawString(this.font, fromLine,
                boxX + BOX_WIDTH - 15 - fromWidth, currentY, 0xFFCCCCCC);

        // 回信区域
        if (hasReply) {
            currentY += 16;

            // 回信分隔线
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("mcaromanticexpansion.gui.love_letter.read.reply_header"), centerX, currentY, 0xFF69B4FF);
            currentY += 14;
            guiGraphics.hLine(boxX + 40, boxX + BOX_WIDTH - 40, currentY, 0xAA69B4FF);
            currentY += 10;

            // 回信正文
            for (FormattedCharSequence line : wrappedReplyLines) {
                guiGraphics.drawString(this.font, line, textX, currentY, 0xFFDDEEFF);
                currentY += 10;
            }
            currentY += 6;

            // "—— 回信 ZZZ"
            Component replyFromLine = Component.translatable("mcaromanticexpansion.gui.love_letter.read.reply_from", replySender);
            int replyFromWidth = this.font.width(replyFromLine);
            guiGraphics.drawString(this.font, replyFromLine,
                    boxX + BOX_WIDTH - 15 - replyFromWidth, currentY, 0xFFAACCFF);
        }
    }
}