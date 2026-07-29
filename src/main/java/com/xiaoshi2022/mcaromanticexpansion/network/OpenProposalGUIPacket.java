package com.xiaoshi2022.mcaromanticexpansion.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class OpenProposalGUIPacket {
    private final UUID proposerUUID;
    private final String proposerName;

    public OpenProposalGUIPacket(UUID proposerUUID, String proposerName) {
        this.proposerUUID = proposerUUID;
        this.proposerName = proposerName;
    }

    public UUID getProposerUUID() {
        return proposerUUID;
    }

    public String getProposerName() {
        return proposerName;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(proposerUUID);
        buf.writeUtf(proposerName, 64);
    }

    public static OpenProposalGUIPacket decode(FriendlyByteBuf buf) {
        return new OpenProposalGUIPacket(buf.readUUID(), buf.readUtf(64));
    }

    public static void handle(OpenProposalGUIPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            GUIPacketHandlers.handleOpenProposalGUI(packet);
        });
        context.setPacketHandled(true);
    }
}