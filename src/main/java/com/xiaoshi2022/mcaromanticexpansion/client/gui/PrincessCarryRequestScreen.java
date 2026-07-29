package com.xiaoshi2022.mcaromanticexpansion.client.gui;

import com.xiaoshi2022.mcaromanticexpansion.network.CarryResponsePacket;
import com.xiaoshi2022.mcaromanticexpansion.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

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
        // 【Forge 移植】使用 ModNetwork.CHANNEL.sendToServer 替代 Minecraft.getInstance().getConnection().send()
        ModNetwork.CHANNEL.sendToServer(new CarryResponsePacket(requesterUUID, accepted));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.fill(centerX - 110, centerY - 60, centerX + 110, centerY + 60, 0x88220044);
        guiGraphics.fill(centerX - 106, centerY - 56, centerX + 106, centerY + 56, 0x77331155);
        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 45, 0xFFFFAAFF);
        guiGraphics.hLine(centerX - 80, centerX + 80, centerY - 35, 0xAAFF88FF);

        Component question = Component.translatable("mcaromanticexpansion.gui.princess_carry.question", requesterName);
        guiGraphics.drawCenteredString(this.font, question, centerX, centerY - 20, 0xFFFFFFFF);

        int timeLeft = (TIMEOUT_TICKS - ticks) / 20;
        Component timeText = Component.translatable("mcaromanticexpansion.gui.shared_umbrella.time_left", timeLeft);
        guiGraphics.drawCenteredString(this.font, timeText, centerX, centerY + 50, 0x888888);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}