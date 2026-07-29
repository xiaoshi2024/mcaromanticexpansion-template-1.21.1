package com.xiaoshi2022.mcaromanticexpansion.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CarryStatePayload {
    private final UUID carrierId;
    private final UUID passengerId;
    private final boolean carrying;

    public CarryStatePayload(UUID carrierId, UUID passengerId, boolean carrying) {
        this.carrierId = carrierId;
        this.passengerId = passengerId;
        this.carrying = carrying;
    }

    public UUID getCarrierId() {
        return carrierId;
    }

    public UUID getPassengerId() {
        return passengerId;
    }

    public boolean isCarrying() {
        return carrying;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(carrierId);
        buf.writeUUID(passengerId);
        buf.writeBoolean(carrying);
    }

    public static CarryStatePayload decode(FriendlyByteBuf buf) {
        return new CarryStatePayload(buf.readUUID(), buf.readUUID(), buf.readBoolean());
    }

    public static void handle(CarryStatePayload packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            GUIPacketHandlers.handleCarryState(packet);
        });
        context.setPacketHandled(true);
    }
}