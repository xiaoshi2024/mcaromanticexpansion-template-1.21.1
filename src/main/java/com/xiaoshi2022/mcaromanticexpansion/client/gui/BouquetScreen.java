package com.xiaoshi2022.mcaromanticexpansion.client.gui;

import com.xiaoshi2022.mcaromanticexpansion.network.BouquetResponsePacket;
import com.xiaoshi2022.mcaromanticexpansion.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class BouquetScreen extends Screen {

    private final UUID giverUUID;
    private final String giverName;
    private long lastClickTime = 0;

    public BouquetScreen(UUID giverUUID, String giverName) {
        super(Component.translatable("mcaromanticexpansion.gui.bouquet.title"));
        this.giverUUID = giverUUID;
        this.giverName = giverName;
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

        this.addRenderableWidget(Button.builder(Component.translatable("mcaromanticexpansion.gui.bouquet.accept"), button -> {
            if (System.currentTimeMillis() - lastClickTime > 500) {
                lastClickTime = System.currentTimeMillis();
                sendResponse(true);
                this.onClose();
            }
        }).pos(centerX - 90, centerY + 10).size(80, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("mcaromanticexpansion.gui.bouquet.reject"), button -> {
            if (System.currentTimeMillis() - lastClickTime > 500) {
                lastClickTime = System.currentTimeMillis();
                sendResponse(false);
                this.onClose();
            }
        }).pos(centerX + 10, centerY + 10).size(80, 20).build());
    }

    private void sendResponse(boolean accepted) {
        // 使用 Forge 网络系统发送
        BouquetResponsePacket packet = new BouquetResponsePacket(giverUUID, accepted);
        ModNetwork.CHANNEL.sendToServer(packet);
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

        guiGraphics.fill(centerX - 100, centerY - 60, centerX + 100, centerY + 40, 0x77000000);
        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 45, 0xFFFFFFFF);
        guiGraphics.hLine(centerX - 80, centerX + 80, centerY - 35, 0xAAFFFFFF);

        // 显示带名字的询问信息
        Component question = Component.translatable("mcaromanticexpansion.gui.bouquet.question", giverName);
        guiGraphics.drawCenteredString(this.font, question, centerX, centerY - 20, 0xFFFFFFFF);
    }
}