package com.xiaoshi2022.mcaromanticexpansion.client.gui;

import com.xiaoshi2022.mcaromanticexpansion.network.MarriageResponsePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class MarriageScreen extends Screen {
    private final UUID partnerUUID;
    private final String partnerName;
    private long lastClickTime = 0;

    public MarriageScreen(UUID partnerUUID, String partnerName) {
        super(Component.translatable("mcaromanticexpansion.gui.marriage.title"));
        this.partnerUUID = partnerUUID;
        this.partnerName = partnerName;
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

        this.addRenderableWidget(Button.builder(Component.translatable("mcaromanticexpansion.gui.marriage.confirm"), button -> {
            if (System.currentTimeMillis() - lastClickTime > 500) {
                lastClickTime = System.currentTimeMillis();
                sendResponse(true);
                this.onClose();
            }
        }).pos(centerX - 50, centerY + 10).size(100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("mcaromanticexpansion.gui.marriage.cancel"), button -> {
            if (System.currentTimeMillis() - lastClickTime > 500) {
                lastClickTime = System.currentTimeMillis();
                sendResponse(false);
                this.onClose();
            }
        }).pos(centerX - 50, centerY + 40).size(100, 20).build());
    }

    private void sendResponse(boolean confirmed) {
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().send(new MarriageResponsePacket(partnerUUID, confirmed));
        }
    }

    @Override
    public boolean keyPressed(int keyChar, int keyCode, int unknown) {
        if (keyChar == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.fill(centerX - 100, centerY - 60, centerX + 100, centerY + 60, 0x77000000);
        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 45, 0xFFFFFFFF);
        guiGraphics.hLine(centerX - 80, centerX + 80, centerY - 35, 0xAAFFFFFF);

        // 显示询问信息，包含对方名字
        Component question = Component.translatable("mcaromanticexpansion.gui.marriage.question", partnerName);
        guiGraphics.drawCenteredString(this.font, question, centerX, centerY - 20, 0xFFFFFFFF);
    }
}