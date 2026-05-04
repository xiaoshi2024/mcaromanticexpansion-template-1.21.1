package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenBouquetGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenMarriageGUIPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.OpenProposalGUIPacket;
import net.conczin.mca.item.BouquetItem;
import net.conczin.mca.item.EngagementRingItem;
import net.conczin.mca.item.WeddingRingItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@SuppressWarnings("unused")
public class PlayerInteractionHandler {

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!(event.getTarget() instanceof Player targetPlayer)) {
            return;
        }

        if (!(targetPlayer instanceof ServerPlayer targetServerPlayer)) {
            return;
        }

        if (player == targetPlayer) {
            return;
        }

        var stack = event.getItemStack();
        var item = stack.getItem();

        MCARomanticExpansion.LOGGER.info("Player {} interacting with {} using {}",
                player.getName().getString(), targetPlayer.getName().getString(), item.getClass().getSimpleName());

        if (item instanceof BouquetItem) {
            MCARomanticExpansion.LOGGER.info("Detected BouquetItem! Sending bouquet GUI to {}", targetPlayer.getName().getString());
            sendBouquetRequest(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        } else if (item instanceof EngagementRingItem) {
            MCARomanticExpansion.LOGGER.info("Detected EngagementRingItem! Sending proposal GUI to {}", targetPlayer.getName().getString());
            sendProposalRequest(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        } else if (item instanceof WeddingRingItem) {
            MCARomanticExpansion.LOGGER.info("Detected WeddingRingItem! Sending marriage GUI to {}", targetPlayer.getName().getString());
            sendMarriageRequest(serverPlayer, targetServerPlayer);
            event.setCanceled(true);
        }
    }

    private static void sendProposalRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenProposalGUIPacket packet = new OpenProposalGUIPacket(sender.getUUID(), sender.getName().getString());
        receiver.connection.send(packet);
    }

    private static void sendBouquetRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenBouquetGUIPacket packet = new OpenBouquetGUIPacket(sender.getUUID());
        MCARomanticExpansion.LOGGER.info("Sending OpenBouquetGUIPacket to {}", receiver.getName().getString());
        receiver.connection.send(packet);
    }

    private static void sendMarriageRequest(ServerPlayer sender, ServerPlayer receiver) {
        OpenMarriageGUIPacket packet = new OpenMarriageGUIPacket(sender.getUUID());
        MCARomanticExpansion.LOGGER.info("Sending OpenMarriageGUIPacket to {}", receiver.getName().getString());
        receiver.connection.send(packet);
    }
}