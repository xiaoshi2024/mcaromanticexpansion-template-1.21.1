package com.xiaoshi2022.mcaromanticexpansion.client;

import com.xiaoshi2022.mcaromanticexpansion.util.SharedUmbrellaManager;
import net.conczin.mca.item.FamilyTreeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ClientEventHandler {
    public static void init() {
        NeoForge.EVENT_BUS.addListener(ClientEventHandler::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(ClientEventHandler::onClientTick);
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

    private static void onMouseScroll(MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || minecraft.screen != null) {
            return;
        }

        if (!hasFamilyTreeInInventory(player)) {
            AffectionHUD.clearTargetPlayer();
            return;
        }

        double scrollDeltaY = event.getScrollDeltaY();

        if (scrollDeltaY > 0) {
            Player target = findNearbyPlayer(player);
            if (target != null) {
                AffectionHUD.setTargetPlayer(target);
            }
        } else if (scrollDeltaY < 0) {
            Player target = findNearbyPlayer(player);
            if (target != null) {
                AffectionHUD.setTargetPlayer(target);
            }
        }
    }

    private static Player findNearbyPlayer(Player player) {
        return player.level().getEntitiesOfClass(Player.class,
                        player.getBoundingBox().inflate(5.0))
                .stream()
                .filter(p -> p != player && p.isAlive())
                .sorted((a, b) -> {
                    double distA = player.distanceTo(a);
                    double distB = player.distanceTo(b);
                    return Double.compare(distA, distB);
                })
                .findFirst()
                .orElse(null);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null) return;

        if (!hasFamilyTreeInInventory(player)) {
            AffectionHUD.clearTargetPlayer();
            return;
        }

        Player lookedPlayer = findPlayerLookingAt(player);

        if (SharedUmbrellaManager.isInSharedUmbrella(player)) {
            Player partner = SharedUmbrellaManager.getSharedPartner(player);
            if (partner != null && lookedPlayer == partner) {
                AffectionHUD.setTargetPlayer(partner);
            } else {
                AffectionHUD.clearTargetPlayer();
            }
        } else if (lookedPlayer != null) {
            AffectionHUD.setTargetPlayer(lookedPlayer);
        } else {
            AffectionHUD.clearTargetPlayer();
        }
    }

    private static Player findPlayerLookingAt(Player viewer) {
        double maxDistance = 10.0;
        net.minecraft.world.phys.Vec3 start = viewer.getEyePosition(1.0F);
        net.minecraft.world.phys.Vec3 look = viewer.getLookAngle();
        net.minecraft.world.phys.Vec3 end = start.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);

        net.minecraft.world.phys.AABB aabb = new net.minecraft.world.phys.AABB(start, end).inflate(1.0);
        java.util.List<net.minecraft.world.entity.Entity> entities = viewer.level().getEntities(viewer, aabb);

        double closestDistance = maxDistance;
        Player foundPlayer = null;

        for (net.minecraft.world.entity.Entity entity : entities) {
            if (entity instanceof Player otherPlayer && otherPlayer != viewer && otherPlayer.isAlive()) {
                net.minecraft.world.phys.Vec3 entityPos = otherPlayer.getEyePosition(1.0F);
                double distance = entityPos.distanceToSqr(start);
                
                net.minecraft.world.phys.Vec3 toEntity = entityPos.subtract(start).normalize();
                double dot = toEntity.dot(look);
                
                if (dot > 0.9 && distance < closestDistance) {
                    closestDistance = distance;
                    foundPlayer = otherPlayer;
                }
            }
        }

        return foundPlayer;
    }
}
