package com.xiaoshi2022.mcaromanticexpansion.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class OpenMarriageGUIPacket {
    private final UUID partnerUUID;
    private final String partnerName;

    public OpenMarriageGUIPacket(UUID partnerUUID, String partnerName) {
        this.partnerUUID = partnerUUID;
        this.partnerName = partnerName;
    }

    public UUID getPartnerUUID() {
        return partnerUUID;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(partnerUUID);
        buf.writeUtf(partnerName, 64);
    }

    public static OpenMarriageGUIPacket decode(FriendlyByteBuf buf) {
        return new OpenMarriageGUIPacket(buf.readUUID(), buf.readUtf(64));
    }

    public static void handle(OpenMarriageGUIPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            GUIPacketHandlers.handleOpenMarriageGUI(packet);
        });
        context.setPacketHandled(true);
    }
}