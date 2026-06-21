package com.xiaoshi2022.mcaromanticexpansion.client;

import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import net.conczin.mca.item.FamilyTreeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

public class AffectionHUD {
    private static Player targetPlayer = null;
    private static int affection = 0;
    private static long lastUpdateTime = 0;
    private static final double MAX_LOOK_DISTANCE = 10.0;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(AffectionHUD::onRenderHUD);
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
            affection = AffectionManager.getAffection(player, targetPlayer);
        } else {
            affection = 0;
        }
    }

    private static boolean hasFamilyTreeInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
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

        double closestDistance = MAX_LOOK_DISTANCE;
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

    private static void onRenderHUD(RenderGuiLayerEvent.Post event) {
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

        int centerX = screenWidth / 2;
        int bottomY = screenHeight - 100;

        String targetName = targetPlayer.getName().getString();
        Component title = Component.literal("§d♥ 与 " + targetName + " 的心动值").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
        guiGraphics.drawString(minecraft.font, title, centerX - minecraft.font.width(title) / 2, bottomY - 30, 0xFFFFFF);

        int barWidth = 200;
        int barHeight = 8;
        int barX = centerX - barWidth / 2;
        int barY = bottomY - 15;

        guiGraphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF000000);

        int filledWidth = (int) ((affection / 100.0) * barWidth);
        int color = getAffectionColor(affection);
        guiGraphics.fill(barX, barY, barX + filledWidth, barY + barHeight, color);

        Component valueText = Component.literal(affection + "/100").withStyle(net.minecraft.ChatFormatting.GOLD);
        guiGraphics.drawString(minecraft.font, valueText, centerX - minecraft.font.width(valueText) / 2, bottomY, 0xFFFFFF);
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
}
