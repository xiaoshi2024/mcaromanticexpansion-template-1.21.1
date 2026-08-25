package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.CarryClientState;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.BouquetScreen;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.MarriageScreen;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.PrincessCarryRequestScreen;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.ProposalScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GUIPacketHandlers {

    @OnlyIn(Dist.CLIENT)
    public static void handleOpenBouquetGUI(OpenBouquetGUIPacket packet) {
        MCARomanticExpansion.LOGGER.debug("CLIENT: OpenBouquetGUIPacket received! giverUUID={}, giverName={}",
                packet.giverUUID(), packet.giverName());
        Minecraft.getInstance().execute(() -> {
            MCARomanticExpansion.LOGGER.debug("CLIENT: Opening BouquetScreen for UUID: {}", packet.giverUUID());
            // 直接通过 gui 设置屏幕
            Minecraft.getInstance().gui.setScreen(new BouquetScreen(packet.giverUUID(), packet.giverName()));
            MCARomanticExpansion.LOGGER.debug("CLIENT: BouquetScreen opened successfully!");
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOpenProposalGUI(OpenProposalGUIPacket packet) {
        MCARomanticExpansion.LOGGER.debug("CLIENT: OpenProposalGUIPacket received! proposerUUID={}, proposerName={}",
                packet.proposerUUID(), packet.proposerName());
        Minecraft.getInstance().execute(() -> {
            MCARomanticExpansion.LOGGER.debug("CLIENT: Opening ProposalScreen for UUID: {}", packet.proposerUUID());
            Minecraft.getInstance().gui.setScreen(new ProposalScreen(packet.proposerUUID(), packet.proposerName()));
            MCARomanticExpansion.LOGGER.debug("CLIENT: ProposalScreen opened successfully!");
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOpenMarriageGUI(OpenMarriageGUIPacket packet) {
        MCARomanticExpansion.LOGGER.debug("CLIENT: OpenMarriageGUIPacket received! partnerUUID={}, partnerName={}",
                packet.partnerUUID(), packet.partnerName());
        Minecraft.getInstance().execute(() -> {
            MCARomanticExpansion.LOGGER.debug("CLIENT: Opening MarriageScreen for UUID: {}", packet.partnerUUID());
            Minecraft.getInstance().gui.setScreen(new MarriageScreen(packet.partnerUUID(), packet.partnerName()));
            MCARomanticExpansion.LOGGER.debug("CLIENT: MarriageScreen opened successfully!");
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOpenPrincessCarryGUI(CarryInvitePacket packet) {
        MCARomanticExpansion.LOGGER.debug("CLIENT: CarryInvitePacket received! requesterUUID={}, requesterName={}",
                packet.requesterUUID(), packet.requesterName());
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().gui.setScreen(new PrincessCarryRequestScreen(packet.requesterUUID(), packet.requesterName()));
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleCarryState(CarryStatePayload payload) {
        Minecraft.getInstance().execute(() ->
                CarryClientState.accept(payload.carrier(), payload.passenger(), payload.carrying()));
    }
}