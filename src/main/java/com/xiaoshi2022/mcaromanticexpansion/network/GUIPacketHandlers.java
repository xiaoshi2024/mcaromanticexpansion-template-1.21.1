package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.BouquetScreen;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.MarriageScreen;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.ProposalScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GUIPacketHandlers {
    @OnlyIn(Dist.CLIENT)
    public static void handleOpenBouquetGUI(OpenBouquetGUIPacket packet) {
        MCARomanticExpansion.LOGGER.info("CLIENT: OpenBouquetGUIPacket received! giverUUID={}, giverName={}",
                packet.giverUUID(), packet.giverName());
        Minecraft.getInstance().execute(() -> {
            MCARomanticExpansion.LOGGER.info("CLIENT: Opening BouquetScreen for UUID: {}", packet.giverUUID());
            Minecraft.getInstance().setScreen(new BouquetScreen(packet.giverUUID(), packet.giverName()));
            MCARomanticExpansion.LOGGER.info("CLIENT: BouquetScreen opened successfully!");
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOpenProposalGUI(OpenProposalGUIPacket packet) {
        MCARomanticExpansion.LOGGER.info("CLIENT: OpenProposalGUIPacket received! proposerUUID={}, proposerName={}",
                packet.proposerUUID(), packet.proposerName());
        Minecraft.getInstance().execute(() -> {
            MCARomanticExpansion.LOGGER.info("CLIENT: Opening ProposalScreen for UUID: {}", packet.proposerUUID());
            Minecraft.getInstance().setScreen(new ProposalScreen(packet.proposerUUID(), packet.proposerName()));
            MCARomanticExpansion.LOGGER.info("CLIENT: ProposalScreen opened successfully!");
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOpenMarriageGUI(OpenMarriageGUIPacket packet) {
        MCARomanticExpansion.LOGGER.info("CLIENT: OpenMarriageGUIPacket received! partnerUUID={}, partnerName={}",
                packet.partnerUUID(), packet.partnerName());
        Minecraft.getInstance().execute(() -> {
            MCARomanticExpansion.LOGGER.info("CLIENT: Opening MarriageScreen for UUID: {}", packet.partnerUUID());
            Minecraft.getInstance().setScreen(new MarriageScreen(packet.partnerUUID(), packet.partnerName()));
            MCARomanticExpansion.LOGGER.info("CLIENT: MarriageScreen opened successfully!");
        });
    }
}
