package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.util.CarryRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CarryRequestPacket {
    private final UUID targetUUID;

    public CarryRequestPacket(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(targetUUID);
    }

    public static CarryRequestPacket decode(FriendlyByteBuf buf) {
        return new CarryRequestPacket(buf.readUUID());
    }

    public static void handle(CarryRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CarryRuntime.handleCarryRequest(player, packet);
            }
        });
        context.setPacketHandled(true);
    }
}