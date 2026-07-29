package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.util.CarryRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CarryResponsePacket {
    private final UUID requesterUUID;
    private final boolean accepted;

    public CarryResponsePacket(UUID requesterUUID, boolean accepted) {
        this.requesterUUID = requesterUUID;
        this.accepted = accepted;
    }

    public UUID getRequesterUUID() {
        return requesterUUID;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(requesterUUID);
        buf.writeBoolean(accepted);
    }

    public static CarryResponsePacket decode(FriendlyByteBuf buf) {
        return new CarryResponsePacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(CarryResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CarryRuntime.handleCarryResponse(player, packet);
            }
        });
        context.setPacketHandled(true);
    }
}