package com.xiaoshi2022.mcaromanticexpansion.client.gui;

import com.xiaoshi2022.mcaromanticexpansion.client.AffectionHUD;
import com.xiaoshi2022.mcaromanticexpansion.config.HUDConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HUDConfigScreen extends Screen {
    private final Screen parent;
    private CycleButton<String> positionButton;
    private EditBox customXField;
    private EditBox customYField;
    private boolean isEditing = false;

    public HUDConfigScreen(Screen parent) {
        super(Component.translatable("mcaromanticexpansion.gui.hud_config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // ========== 位置选择 ==========
        this.positionButton = CycleButton.<String>builder((value) ->
                        Component.translatable("mcaromanticexpansion.gui.hud_config.position." + value.toLowerCase()))
                .withValues("TOP_LEFT", "TOP_CENTER", "TOP_RIGHT",
                        "BOTTOM_LEFT", "BOTTOM_CENTER", "BOTTOM_RIGHT", "CUSTOM")
                .withInitialValue(getCurrentPosition())
                .create(centerX - 100, centerY - 30, 120, 20,
                        Component.translatable("mcaromanticexpansion.gui.hud_config.position"), (button, value) -> {
                            HUDConfig.setHudPosition(value);
                            try {
                                AffectionHUD.HUDPosition position = AffectionHUD.HUDPosition.valueOf(value);
                                AffectionHUD.setPosition(position);

                                updateCoordinateDisplay(value);

                                if (!value.equals("CUSTOM")) {
                                    isEditing = false;
                                    customXField.setEditable(false);
                                    customYField.setEditable(false);
                                }
                            } catch (Exception e) {
                                // 处理异常
                            }
                        });
        this.addRenderableWidget(this.positionButton);

        // ========== 坐标输入框 ==========
        this.customXField = new EditBox(this.font, centerX + 40, centerY - 30, 60, 20,
                Component.literal("X"));
        this.customXField.setMaxLength(5);
        this.customXField.setFilter(s -> s.matches("-?\\d*"));
        this.customXField.setEditable(false);
        this.addRenderableWidget(this.customXField);

        this.customYField = new EditBox(this.font, centerX + 110, centerY - 30, 60, 20,
                Component.literal("Y"));
        this.customYField.setMaxLength(5);
        this.customYField.setFilter(s -> s.matches("-?\\d*"));
        this.customYField.setEditable(false);
        this.addRenderableWidget(this.customYField);

        // ========== 编辑按钮 ==========
        this.addRenderableWidget(Button.builder(Component.literal("✏"), (button) -> {
            String currentPos = getCurrentPosition();
            if (currentPos.equals("CUSTOM")) {
                isEditing = !isEditing;
                customXField.setEditable(isEditing);
                customYField.setEditable(isEditing);
                button.setMessage(Component.literal(isEditing ? "✔" : "✏"));
            }
        }).bounds(centerX + 175, centerY - 30, 20, 20).build());

        // ========== 按钮 ==========
        this.addRenderableWidget(Button.builder(Component.translatable("mcaromanticexpansion.gui.hud_config.apply"), (button) -> {
            applyCustomPosition();
            HUDConfig.save();
            if (isEditing) {
                isEditing = false;
                customXField.setEditable(false);
                customYField.setEditable(false);
                this.children().forEach(child -> {
                    if (child instanceof Button btn && btn.getMessage().getString().equals("✔")) {
                        btn.setMessage(Component.literal("✏"));
                    }
                });
            }
            this.minecraft.setScreen(this.parent);
        }).bounds(centerX - 100, centerY + 20, 95, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("mcaromanticexpansion.gui.hud_config.reset"), (button) -> {
            HUDConfig.resetToDefault();
            this.positionButton.setValue("BOTTOM_CENTER");
            isEditing = false;
            customXField.setEditable(false);
            customYField.setEditable(false);
            updateCoordinateDisplay("BOTTOM_CENTER");
            this.children().forEach(child -> {
                if (child instanceof Button btn && btn.getMessage().getString().equals("✔")) {
                    btn.setMessage(Component.literal("✏"));
                }
            });
        }).bounds(centerX + 5, centerY + 20, 95, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("mcaromanticexpansion.gui.hud_config.close"), (button) -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(centerX - 100, centerY + 50, 200, 20).build());

        updateCoordinateDisplay(getCurrentPosition());
    }

    private void updateCoordinateDisplay(String position) {
        int screenWidth = this.width;
        int screenHeight = this.height;
        int hudWidth = 200;
        int hudHeight = 80;
        int padding = 20;

        int x, y;

        switch (position) {
            case "TOP_LEFT":
                x = padding;
                y = padding;
                break;
            case "TOP_CENTER":
                x = (screenWidth - hudWidth) / 2;
                y = padding;
                break;
            case "TOP_RIGHT":
                x = screenWidth - hudWidth - padding;
                y = padding;
                break;
            case "BOTTOM_LEFT":
                x = padding;
                y = screenHeight - hudHeight - padding;
                break;
            case "BOTTOM_CENTER":
                x = (screenWidth - hudWidth) / 2;
                y = screenHeight - hudHeight - padding;
                break;
            case "BOTTOM_RIGHT":
                x = screenWidth - hudWidth - padding;
                y = screenHeight - hudHeight - padding;
                break;
            case "CUSTOM":
                x = HUDConfig.getCustomX();
                y = HUDConfig.getCustomY();
                break;
            default:
                x = 0;
                y = 0;
        }

        customXField.setValue(String.valueOf(x));
        customYField.setValue(String.valueOf(y));

        if (!position.equals("CUSTOM")) {
            customXField.setEditable(false);
            customYField.setEditable(false);
            isEditing = false;
        }
    }

    private void applyCustomPosition() {
        try {
            int x = Integer.parseInt(this.customXField.getValue());
            int y = Integer.parseInt(this.customYField.getValue());
            HUDConfig.setCustomX(x);
            HUDConfig.setCustomY(y);

            if (HUDConfig.getHudPosition().equals("CUSTOM")) {
                AffectionHUD.setCustomPosition(x, y);
            }
        } catch (NumberFormatException e) {
            // 无效输入
        }
    }

    private String getCurrentPosition() {
        try {
            return HUDConfig.getHudPosition();
        } catch (Exception e) {
            return "BOTTOM_CENTER";
        }
    }

    private String formatDisplayName(String value) {
        return value;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 【修复】1.20.1 中 renderBackground 只需要 GuiGraphics 参数
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        String currentPos = getCurrentPosition();

        guiGraphics.drawString(this.font,
                Component.translatable("mcaromanticexpansion.gui.hud_config.position_label"),
                centerX - 145, centerY - 28, 0xAAAAAA);

        guiGraphics.drawString(this.font,
                Component.translatable("mcaromanticexpansion.gui.hud_config.coordinate_label"),
                centerX - 5, centerY - 28, 0xAAAAAA);

        String coorddebug = getPositiondebug(currentPos);
        guiGraphics.drawString(this.font,
                Component.translatable(coorddebug),
                centerX - 145, centerY + 10, 0x666666);

        if (currentPos.equals("CUSTOM")) {
            guiGraphics.drawString(this.font,
                    Component.translatable(isEditing
                            ? "mcaromanticexpansion.gui.hud_config.editing"
                            : "mcaromanticexpansion.gui.hud_config.click_to_edit"),
                    centerX + 200, centerY - 28, isEditing ? 0xFFFF00 : 0x888888);
        } else {
            guiGraphics.drawString(this.font,
                    Component.translatable("mcaromanticexpansion.gui.hud_config.auto_coordinate"),
                    centerX + 200, centerY - 28, 0x666666);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private String getPositiondebug(String position) {
        switch (position) {
            case "TOP_LEFT": return "mcaromanticexpansion.gui.hud_config.debug.top_left";
            case "TOP_CENTER": return "mcaromanticexpansion.gui.hud_config.debug.top_center";
            case "TOP_RIGHT": return "mcaromanticexpansion.gui.hud_config.debug.top_right";
            case "BOTTOM_LEFT": return "mcaromanticexpansion.gui.hud_config.debug.bottom_left";
            case "BOTTOM_CENTER": return "mcaromanticexpansion.gui.hud_config.debug.bottom_center";
            case "BOTTOM_RIGHT": return "mcaromanticexpansion.gui.hud_config.debug.bottom_right";
            case "CUSTOM": return "mcaromanticexpansion.gui.hud_config.debug.custom";
            default: return "";
        }
    }
}