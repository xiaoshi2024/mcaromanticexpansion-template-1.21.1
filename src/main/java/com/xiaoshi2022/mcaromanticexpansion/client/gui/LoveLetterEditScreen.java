package com.xiaoshi2022.mcaromanticexpansion.client.gui;

import com.xiaoshi2022.mcaromanticexpansion.network.LoveLetterSavePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LoveLetterEditScreen extends Screen {

    private final InteractionHand hand;
    private final boolean replyMode;
    private EditBox recipientField;
    private MultiLineEditBox messageField;

    private static final int BOX_WIDTH = 240;
    private static final int BOX_HEIGHT = 160;

    private final String tempRecipient;
    private final String tempMessage;

    public LoveLetterEditScreen(InteractionHand hand, String currentRecipient, String currentMessage) {
        this(hand, currentRecipient, currentMessage, false);
    }

    public LoveLetterEditScreen(InteractionHand hand, boolean replyMode) {
        this(hand, "", "", replyMode);
    }

    private LoveLetterEditScreen(InteractionHand hand, String currentRecipient, String currentMessage, boolean replyMode) {
        super(replyMode
                ? Component.translatable("mcaromanticexpansion.gui.love_letter.reply_title")
                : Component.translatable("mcaromanticexpansion.gui.love_letter.edit.title"));
        this.hand = hand;
        this.replyMode = replyMode;
        this.tempRecipient = currentRecipient;
        this.tempMessage = currentMessage;
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
        int boxX = centerX - BOX_WIDTH / 2;
        int boxY = centerY - BOX_HEIGHT / 2;

        if (!replyMode) {
            // 收信人输入框
            this.recipientField = new EditBox(this.font, boxX + 80, boxY + 35, 150, 16,
                    Component.translatable("mcaromanticexpansion.gui.love_letter.recipient"));
            this.recipientField.setMaxLength(32);
            this.recipientField.setValue(this.tempRecipient != null ? this.tempRecipient : "");
            this.addRenderableWidget(this.recipientField);

            // 正文多行输入框 - 使用 Builder 模式
            this.messageField = MultiLineEditBox.builder()
                    .setX(boxX + 10)
                    .setY(boxY + 65)
                    .setPlaceholder(Component.translatable("mcaromanticexpansion.gui.love_letter.message_label"))
                    .build(this.font, BOX_WIDTH - 20, 70,
                            Component.translatable("mcaromanticexpansion.gui.love_letter.message_label"));
        } else {
            // 回信模式：正文输入框占据更大空间
            this.messageField = MultiLineEditBox.builder()
                    .setX(boxX + 10)
                    .setY(boxY + 40)
                    .setPlaceholder(Component.translatable("mcaromanticexpansion.gui.love_letter.reply_message_label"))
                    .build(this.font, BOX_WIDTH - 20, 95,
                            Component.translatable("mcaromanticexpansion.gui.love_letter.reply_message_label"));
        }
        this.messageField.setValue(this.tempMessage != null ? this.tempMessage : "");
        this.addRenderableWidget(this.messageField);

        // 保存按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("mcaromanticexpansion.gui.love_letter.save"),
                button -> saveAndClose()
        ).pos(centerX - 62, boxY + BOX_HEIGHT - 28).size(60, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("mcaromanticexpansion.gui.love_letter.cancel"),
                button -> this.onClose()
        ).pos(centerX + 2, boxY + BOX_HEIGHT - 28).size(60, 20).build());
    }

    private void saveAndClose() {
        String message = this.messageField.getValue().trim();
        if (message.isEmpty()) return;

        if (Minecraft.getInstance().getConnection() != null) {
            if (replyMode) {
                Minecraft.getInstance().getConnection().send(
                        new LoveLetterSavePacket(hand, "", message, true));
            } else {
                String recipient = this.recipientField.getValue().trim();
                if (recipient.isEmpty()) return;
                Minecraft.getInstance().getConnection().send(
                        new LoveLetterSavePacket(hand, recipient, message, false));
            }
        }
        this.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        }
        if (event.key() == 257 && !event.hasShiftDown()) {
            if ((replyMode || this.recipientField.isFocused()) || this.messageField.isFocused()) {
                saveAndClose();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int boxX = centerX - BOX_WIDTH / 2;
        int boxY = centerY - BOX_HEIGHT / 2;

        guiGraphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xCC1a0a1a);
        guiGraphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + 2, 0xFFd4457a);
        guiGraphics.fill(boxX, boxY + BOX_HEIGHT - 2, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xFFd4457a);

        guiGraphics.centeredText(this.font, this.title, centerX, boxY + 10, 0xFFFFB6C1);

        if (!replyMode) {
            guiGraphics.text(this.font,
                    Component.translatable("mcaromanticexpansion.gui.love_letter.recipient_label"),
                    boxX + 10, boxY + 38, 0xFFE0B0FF);
            guiGraphics.text(this.font,
                    Component.translatable("mcaromanticexpansion.gui.love_letter.message_label"),
                    boxX + 10, boxY + 53, 0xFFE0B0FF);
        } else {
            guiGraphics.text(this.font,
                    Component.translatable("mcaromanticexpansion.gui.love_letter.reply_message_label"),
                    boxX + 10, boxY + 28, 0xFFE0B0FF);
        }
    }
}