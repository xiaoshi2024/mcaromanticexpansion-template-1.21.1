package com.xiaoshi2022.mcaromanticexpansion.client.gui;

import com.xiaoshi2022.mcaromanticexpansion.network.CarryResponsePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class PrincessCarryRequestScreen extends Screen {
    private final UUID requesterUUID;
    private final String requesterName;
    private Button acceptButton;
    private Button declineButton;
    private int ticks = 0;
    private static final int TIMEOUT_TICKS = 600;

    public PrincessCarryRequestScreen(UUID requesterUUID, String requesterName) {
        super(Component.translatable("mcaromanticexpansion.gui.princess_carry.title"));
        this.requesterUUID = requesterUUID;
        this.requesterName = requesterName;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        acceptButton = Button.builder(Component.translatable("mcaromanticexpansion.gui.princess_carry.accept"), button -> acceptRequest())
                .bounds(centerX - 100, centerY + 20, 90, 20)
                .build();
        this.addRenderableWidget(acceptButton);

        declineButton = Button.builder(Component.translatable("mcaromanticexpansion.gui.princess_carry.reject"), button -> declineRequest())
                .bounds(centerX + 10, centerY + 20, 90, 20)
                .build();
        this.addRenderableWidget(declineButton);
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (ticks >= TIMEOUT_TICKS) {
            declineRequest();
        }
    }

    private void acceptRequest() {
        sendResponse(true);
        this.onClose();
    }

    private void declineRequest() {
        sendResponse(false);
        this.onClose();
    }

    private void sendResponse(boolean accepted) {
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().send(new CarryResponsePacket(requesterUUID, accepted));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.fill(centerX - 110, centerY - 60, centerX + 110, centerY + 60, 0x88220044);
        guiGraphics.fill(centerX - 106, centerY - 56, centerX + 106, centerY + 56, 0x77331155);
        guiGraphics.centeredText(this.font, this.title, centerX, centerY - 45, 0xFFFFAAFF);
        guiGraphics.horizontalLine(centerX - 80, centerX + 80, centerY - 35, 0xAAFF88FF);

        Component question = Component.translatable("mcaromanticexpansion.gui.princess_carry.question", requesterName);
        guiGraphics.centeredText(this.font, question, centerX, centerY - 20, 0xFFFFFFFF);

        int timeLeft = (TIMEOUT_TICKS - ticks) / 20;
        Component timeText = Component.translatable("mcaromanticexpansion.gui.shared_umbrella.time_left", timeLeft);
        guiGraphics.centeredText(this.font, timeText, centerX, centerY + 50, 0x888888);
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
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}