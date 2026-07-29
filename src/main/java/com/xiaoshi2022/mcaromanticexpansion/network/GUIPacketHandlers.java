package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.CarryClientState;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.BouquetScreen;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.MarriageScreen;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.PrincessCarryRequestScreen;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.ProposalScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GUIPacketHandlers {

    @OnlyIn(Dist.CLIENT)
    public static void handleOpenBouquetGUI(OpenBouquetGUIPacket packet) {
        MCARomanticExpansion.LOGGER.debug("CLIENT: OpenBouquetGUIPacket received! giverUUID={}, giverName={}",
                packet.getGiverUUID(), packet.getGiverName());
        Minecraft.getInstance().execute(() -> {
            MCARomanticExpansion.LOGGER.debug("CLIENT: Opening BouquetScreen for UUID: {}", packet.getGiverUUID());
            Minecraft.getInstance().setScreen(new BouquetScreen(packet.getGiverUUID(), packet.getGiverName()));
            MCARomanticExpansion.LOGGER.debug("CLIENT: BouquetScreen opened successfully!");
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOpenProposalGUI(OpenProposalGUIPacket packet) {
        MCARomanticExpansion.LOGGER.debug("CLIENT: OpenProposalGUIPacket received! proposerUUID={}, proposerName={}",
                packet.getProposerUUID(), packet.getProposerName());
        Minecraft.getInstance().execute(() -> {
            MCARomanticExpansion.LOGGER.debug("CLIENT: Opening ProposalScreen for UUID: {}", packet.getProposerUUID());
            Minecraft.getInstance().setScreen(new ProposalScreen(packet.getProposerUUID(), packet.getProposerName()));
            MCARomanticExpansion.LOGGER.debug("CLIENT: ProposalScreen opened successfully!");
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOpenMarriageGUI(OpenMarriageGUIPacket packet) {
        MCARomanticExpansion.LOGGER.debug("CLIENT: OpenMarriageGUIPacket received! partnerUUID={}, partnerName={}",
                packet.getPartnerUUID(), packet.getPartnerName());
        Minecraft.getInstance().execute(() -> {
            MCARomanticExpansion.LOGGER.debug("CLIENT: Opening MarriageScreen for UUID: {}", packet.getPartnerUUID());
            Minecraft.getInstance().setScreen(new MarriageScreen(packet.getPartnerUUID(), packet.getPartnerName()));
            MCARomanticExpansion.LOGGER.debug("CLIENT: MarriageScreen opened successfully!");
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOpenPrincessCarryGUI(CarryInvitePacket packet) {
        MCARomanticExpansion.LOGGER.debug("CLIENT: CarryInvitePacket received! requesterUUID={}, requesterName={}",
                packet.getRequesterUUID(), packet.getRequesterName());
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new PrincessCarryRequestScreen(packet.getRequesterUUID(), packet.getRequesterName()));
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleCarryState(CarryStatePayload payload) {
        Minecraft.getInstance().execute(() ->
                CarryClientState.accept(payload.getCarrierId(), payload.getPassengerId(), payload.isCarrying()));
    }
}