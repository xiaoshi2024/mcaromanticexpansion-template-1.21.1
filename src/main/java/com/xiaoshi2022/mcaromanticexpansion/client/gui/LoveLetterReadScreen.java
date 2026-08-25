package com.xiaoshi2022.mcaromanticexpansion.client.gui;

import com.xiaoshi2022.mcaromanticexpansion.item.LoveLetterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

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
    private int boxHeight = 140;

    private List<FormattedCharSequence> wrappedMessageLines = new ArrayList<>();
    private List<FormattedCharSequence> wrappedReplyLines = new ArrayList<>();

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
        Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int maxTextWidth = BOX_WIDTH - 30;
        wrappedMessageLines = this.font.split(Component.literal(message.isEmpty() ? "..." : message), maxTextWidth);

        if (hasReply) {
            wrappedReplyLines = this.font.split(Component.literal(replyMessage), maxTextWidth);
        }

        int contentHeight = 60 + wrappedMessageLines.size() * 10;
        if (hasReply) {
            contentHeight += 40 + wrappedReplyLines.size() * 10;
        }
        contentHeight += 40;
        this.boxHeight = Math.max(140, contentHeight);

        int boxY = centerY - boxHeight / 2;

        if (!hasReply) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("mcaromanticexpansion.gui.love_letter.reply"),
                    button -> startReply()
            ).pos(centerX - 90, boxY + boxHeight - 28).size(80, 20).build());
        }

        this.addRenderableWidget(Button.builder(
                Component.translatable("mcaromanticexpansion.gui.love_letter.close"),
                button -> this.onClose()
        ).pos(centerX + 10, boxY + boxHeight - 28).size(80, 20).build());
    }

    private void startReply() {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new LoveLetterEditScreen(hand, true));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int boxX = centerX - BOX_WIDTH / 2;
        int boxY = centerY - boxHeight / 2;

        guiGraphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + boxHeight, 0xCC2a1018);
        guiGraphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + 2, 0xFFd4457a);
        guiGraphics.fill(boxX, boxY + boxHeight - 2, boxX + BOX_WIDTH, boxY + boxHeight, 0xFFd4457a);
        guiGraphics.fill(boxX, boxY, boxX + 2, boxY + boxHeight, 0xFFd4457a);
        guiGraphics.fill(boxX + BOX_WIDTH - 2, boxY, boxX + BOX_WIDTH, boxY + boxHeight, 0xFFd4457a);

        int textX = boxX + 15;
        int currentY = boxY + 12;

        guiGraphics.centeredText(this.font, this.title, centerX, currentY, 0xFFFFB6C1);
        currentY += 14;

        guiGraphics.horizontalLine(boxX + 20, boxX + BOX_WIDTH - 20, currentY, 0xAAFF69B4);
        currentY += 12;

        guiGraphics.text(this.font,
                Component.translatable("mcaromanticexpansion.gui.love_letter.read.to", recipient), textX, currentY, 0xFFFFFFFF);
        currentY += 16;
        currentY += 4;

        for (FormattedCharSequence line : wrappedMessageLines) {
            guiGraphics.text(this.font, line, textX, currentY, 0xFFEEEEEE);
            currentY += 10;
        }
        currentY += 8;

        Component fromLine = Component.translatable("mcaromanticexpansion.gui.love_letter.read.from", sender);
        int fromWidth = this.font.width(fromLine);
        guiGraphics.text(this.font, fromLine,
                boxX + BOX_WIDTH - 15 - fromWidth, currentY, 0xFFCCCCCC);

        if (hasReply) {
            currentY += 16;

            guiGraphics.centeredText(this.font,
                    Component.translatable("mcaromanticexpansion.gui.love_letter.read.reply_header"), centerX, currentY, 0xFF69B4FF);
            currentY += 14;
            guiGraphics.horizontalLine(boxX + 40, boxX + BOX_WIDTH - 40, currentY, 0xAA69B4FF);
            currentY += 10;

            for (FormattedCharSequence line : wrappedReplyLines) {
                guiGraphics.text(this.font, line, textX, currentY, 0xFFDDEEFF);
                currentY += 10;
            }
            currentY += 6;

            Component replyFromLine = Component.translatable("mcaromanticexpansion.gui.love_letter.read.reply_from", replySender);
            int replyFromWidth = this.font.width(replyFromLine);
            guiGraphics.text(this.font, replyFromLine,
                    boxX + BOX_WIDTH - 15 - replyFromWidth, currentY, 0xFFAACCFF);
        }
    }
}