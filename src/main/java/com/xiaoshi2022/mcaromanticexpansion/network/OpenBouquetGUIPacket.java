package com.xiaoshi2022.mcaromanticexpansion.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class OpenBouquetGUIPacket {
    private final UUID giverUUID;
    private final String giverName;

    public OpenBouquetGUIPacket(UUID giverUUID, String giverName) {
        this.giverUUID = giverUUID;
        this.giverName = giverName;
    }

    public UUID getGiverUUID() {
        return giverUUID;
    }

    public String getGiverName() {
        return giverName;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(giverUUID);
        buf.writeUtf(giverName, 64);
    }

    public static OpenBouquetGUIPacket decode(FriendlyByteBuf buf) {
        return new OpenBouquetGUIPacket(buf.readUUID(), buf.readUtf(64));
    }

    public static void handle(OpenBouquetGUIPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            GUIPacketHandlers.handleOpenBouquetGUI(packet);
        });
        context.setPacketHandled(true);
    }
}