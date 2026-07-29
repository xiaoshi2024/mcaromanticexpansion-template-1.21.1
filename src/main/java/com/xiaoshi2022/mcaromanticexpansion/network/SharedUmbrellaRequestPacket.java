package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.client.gui.SharedUmbrellaRequestScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SharedUmbrellaRequestPacket {
    private final UUID requesterUUID;
    private final String requesterName;

    public SharedUmbrellaRequestPacket(UUID requesterUUID, String requesterName) {
        this.requesterUUID = requesterUUID;
        this.requesterName = requesterName;
    }

    public UUID getRequesterUUID() {
        return requesterUUID;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(requesterUUID);
        buf.writeUtf(requesterName, 64);
    }

    public static SharedUmbrellaRequestPacket decode(FriendlyByteBuf buf) {
        return new SharedUmbrellaRequestPacket(buf.readUUID(), buf.readUtf(64));
    }

    public static void handle(SharedUmbrellaRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            openScreen(packet);
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openScreen(SharedUmbrellaRequestPacket packet) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(
                    new SharedUmbrellaRequestScreen(packet.requesterUUID, packet.requesterName)
            );
        });
    }
}