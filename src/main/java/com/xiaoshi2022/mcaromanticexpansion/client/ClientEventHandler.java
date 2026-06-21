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
        }

        if (SharedUmbrellaManager.isInSharedUmbrella(player)) {
            Player partner = SharedUmbrellaManager.getSharedPartner(player);
            if (partner != null) {
                AffectionHUD.setTargetPlayer(partner);
            }
        }
    }
}
