package com.xiaoshi2022.mcaromanticexpansion.client;

import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import forge.net.mca.item.FamilyTreeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AffectionHUD {
    private static Player targetPlayer = null;
    private static int affection = 0;
    private static int lastAffection = 0;
    private static long lastUpdateTime = 0;
    private static long lastChangeTime = 0;
    private static final double MAX_LOOK_DISTANCE = 10.0;
    private static final long CHANGE_DISPLAY_DURATION = 2000;

    // HUD位置配置
    public enum HUDPosition {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT,
        CUSTOM
    }

    private static HUDPosition position = HUDPosition.BOTTOM_CENTER;
    private static int customOffsetX = 0;
    private static int customOffsetY = 0;
    private static int customX = -1;
    private static int customY = -1;

    private static final int DEFAULT_PADDING = 20;
    private static final int HUD_WIDTH = 200;
    private static final int HUD_HEIGHT = 80;
    private static boolean showDebugInfo = false;

    public static void init() {
        // 使用 RenderGuiOverlayEvent
        MinecraftForge.EVENT_BUS.addListener(AffectionHUD::onRenderHUD);
    }

    public static void setPosition(HUDPosition pos) {
        position = pos;
    }

    public static void setCustomOffset(int offsetX, int offsetY) {
        customOffsetX = offsetX;
        customOffsetY = offsetY;
    }

    public static void setCustomPosition(int x, int y) {
        customX = x;
        customY = y;
        position = HUDPosition.CUSTOM;
    }

    public static void setShowDebugInfo(boolean show) {
        showDebugInfo = show;
    }

    public static void setTargetPlayer(Player player) {
        targetPlayer = player;
        updateAffection();
    }

    public static void clearTargetPlayer() {
        targetPlayer = null;
    }

    private static void updateAffection() {
        Player player = Minecraft.getInstance().player;
        if (player != null && targetPlayer != null) {
            int newAffection = AffectionManager.getAffection(player, targetPlayer);
            if (newAffection != affection) {
                lastAffection = affection;
                affection = newAffection;
                lastChangeTime = System.currentTimeMillis();
            }
        } else {
            affection = 0;
        }
    }

    private static boolean hasFamilyTreeInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            // 注意：FamilyTreeItem 来自 MCA 模组
            if (stack.getItem() instanceof FamilyTreeItem) {
                return true;
            }
        }
        return false;
    }

    private static Player findPlayerLookingAt(Player viewer) {
        Vec3 start = viewer.getEyePosition(1.0F);
        Vec3 look = viewer.getLookAngle();
        Vec3 end = start.add(look.x * MAX_LOOK_DISTANCE, look.y * MAX_LOOK_DISTANCE, look.z * MAX_LOOK_DISTANCE);

        AABB aabb = new AABB(start, end).inflate(1.0);
        List<Entity> entities = viewer.level().getEntities(viewer, aabb);

        double closestDistance = MAX_LOOK_DISTANCE * MAX_LOOK_DISTANCE;
        Player foundPlayer = null;

        for (Entity entity : entities) {
            if (entity instanceof Player otherPlayer && otherPlayer != viewer && otherPlayer.isAlive()) {
                Vec3 entityPos = otherPlayer.getEyePosition(1.0F);
                double distance = entityPos.distanceToSqr(start);

                Vec3 toEntity = entityPos.subtract(start).normalize();
                double dot = toEntity.dot(look);

                if (dot > 0.9 && distance < closestDistance) {
                    closestDistance = distance;
                    foundPlayer = otherPlayer;
                }
            }
        }

        return foundPlayer;
    }

    private static int[] calculateHUDPosition(int screenWidth, int screenHeight) {
        int x, y;
        int hudWidth = HUD_WIDTH;
        int hudHeight = HUD_HEIGHT;

        switch (position) {
            case TOP_LEFT:
                x = DEFAULT_PADDING + customOffsetX;
                y = DEFAULT_PADDING + customOffsetY;
                break;
            case TOP_CENTER:
                x = (screenWidth - hudWidth) / 2 + customOffsetX;
                y = DEFAULT_PADDING + customOffsetY;
                break;
            case TOP_RIGHT:
                x = screenWidth - hudWidth - DEFAULT_PADDING + customOffsetX;
                y = DEFAULT_PADDING + customOffsetY;
                break;
            case BOTTOM_LEFT:
                x = DEFAULT_PADDING + customOffsetX;
                y = screenHeight - hudHeight - DEFAULT_PADDING + customOffsetY;
                break;
            case BOTTOM_CENTER:
                x = (screenWidth - hudWidth) / 2 + customOffsetX;
                y = screenHeight - hudHeight - DEFAULT_PADDING + customOffsetY;
                break;
            case BOTTOM_RIGHT:
                x = screenWidth - hudWidth - DEFAULT_PADDING + customOffsetX;
                y = screenHeight - hudHeight - DEFAULT_PADDING + customOffsetY;
                break;
            case CUSTOM:
                x = customX >= 0 ? customX : (screenWidth - hudWidth) / 2;
                y = customY >= 0 ? customY : (screenHeight - hudHeight) / 2;
                break;
            default:
                x = (screenWidth - hudWidth) / 2;
                y = screenHeight - hudHeight - DEFAULT_PADDING;
        }

        return new int[]{x, y};
    }

    // 使用 RenderGuiOverlayEvent.Post 替代 RenderGuiLayer.Post
    private static void onRenderHUD(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null) {
            return;
        }

        if (!hasFamilyTreeInInventory(player)) {
            return;
        }

        Player lookedPlayer = findPlayerLookingAt(player);

        if (lookedPlayer != null && (targetPlayer == null || lookedPlayer != targetPlayer)) {
            targetPlayer = lookedPlayer;
            updateAffection();
            lastUpdateTime = System.currentTimeMillis();
        } else if (lookedPlayer == null && targetPlayer != null && !targetPlayer.isAlive()) {
            targetPlayer = null;
            affection = 0;
            return;
        }

        if (targetPlayer == null || !targetPlayer.isAlive()) {
            return;
        }

        if (System.currentTimeMillis() - lastUpdateTime > 1000) {
            updateAffection();
            lastUpdateTime = System.currentTimeMillis();
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int[] pos = calculateHUDPosition(screenWidth, screenHeight);
        int baseX = pos[0];
        int baseY = pos[1];

        // 标题
        String targetName = targetPlayer.getName().getString();
        Component title = Component.translatable("hud.mcaromanticexpansion.affection.title", targetName).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
        int titleWidth = minecraft.font.width(title);
        guiGraphics.drawString(minecraft.font, title, baseX + (HUD_WIDTH - titleWidth) / 2, baseY, 0xFFFFFF);

        // 分隔线
        int lineY = baseY + 12;
        guiGraphics.hLine(baseX + 10, baseX + HUD_WIDTH - 10, lineY, 0xAAFFFFFF);

        // 进度条背景
        int barWidth = HUD_WIDTH - 40;
        int barHeight = 8;
        int barX = baseX + 20;
        int barY = lineY + 5;

        guiGraphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF000000);

        // 进度条填充
        int filledWidth = (int) ((Math.min(affection, 100) / 100.0) * barWidth);
        int color = getAffectionColor(affection);
        if (filledWidth > 0) {
            guiGraphics.fill(barX, barY, barX + filledWidth, barY + barHeight, color);
        }

        // 心形图标
        drawHeartIcon(guiGraphics, baseX + 8, barY - 1, color);
        drawHeartIcon(guiGraphics, baseX + HUD_WIDTH - 16, barY - 1, color);

        // 数值显示
        Component valueText = Component.literal(Math.min(affection, 100) + "/100").withStyle(net.minecraft.ChatFormatting.GOLD);
        int valueWidth = minecraft.font.width(valueText);
        guiGraphics.drawString(minecraft.font, valueText, baseX + (HUD_WIDTH - valueWidth) / 2, barY + barHeight + 4, 0xFFFFFF);

        // 好感度等级标签
        String levelKey = getAffectionLevelKey(affection);
        if (levelKey != null) {
            Component levelComponent = Component.translatable(levelKey).withStyle(net.minecraft.ChatFormatting.WHITE);
            int levelWidth = minecraft.font.width(levelComponent);
            guiGraphics.drawString(minecraft.font, levelComponent, baseX + (HUD_WIDTH - levelWidth) / 2, barY + barHeight + 18, 0x888888);
        }

        // 显示变化动画
        long timeSinceChange = System.currentTimeMillis() - lastChangeTime;
        if (timeSinceChange < CHANGE_DISPLAY_DURATION && lastAffection != affection) {
            int change = affection - lastAffection;
            String changeText = (change > 0 ? "↑ +" : "↓ ") + Math.abs(change);
            int changeColor = change > 0 ? 0xFF00FF00 : 0xFFFF0000;
            Component changeComponent = Component.literal(changeText);
            guiGraphics.drawString(minecraft.font, changeComponent, baseX + HUD_WIDTH / 2 + 30, barY + barHeight + 4, changeColor);
        }

        // 调试信息
        if (showDebugInfo) {
            String debugText = String.format("Pos: %s (%d,%d)", position.name(), baseX, baseY);
            Component debugComponent = Component.literal(debugText).withStyle(net.minecraft.ChatFormatting.GRAY);
            guiGraphics.drawString(minecraft.font, debugComponent, 10, 10, 0x888888);
        }
    }

    private static void drawHeartIcon(GuiGraphics guiGraphics, int x, int y, int color) {
        int size = 6;
        int halfSize = size / 2;

        guiGraphics.fill(x, y + halfSize, x + halfSize, y + size, color);
        guiGraphics.fill(x + halfSize, y + halfSize, x + size, y + size, color);

        for (int i = 0; i <= halfSize; i++) {
            guiGraphics.fill(x + i, y + size - i, x + size - i, y + size - i + 1, color);
        }
    }

    private static int getAffectionColor(int affection) {
        if (affection >= 80) {
            return 0xFFFF69B4;
        } else if (affection >= 60) {
            return 0xFFFF1493;
        } else if (affection >= 40) {
            return 0xFFFF4500;
        } else if (affection >= 20) {
            return 0xFFFFD700;
        } else {
            return 0xFFC0C0C0;
        }
    }

    private static String getAffectionLevelKey(int affection) {
        if (affection >= 80) {
            return "hud.mcaromanticexpansion.affection.level.passionate";
        } else if (affection >= 60) {
            return "hud.mcaromanticexpansion.affection.level.crush";
        } else if (affection >= 40) {
            return "hud.mcaromanticexpansion.affection.level.favorable";
        } else if (affection >= 20) {
            return "hud.mcaromanticexpansion.affection.level.acquainted";
        } else {
            return null;
        }
    }
}