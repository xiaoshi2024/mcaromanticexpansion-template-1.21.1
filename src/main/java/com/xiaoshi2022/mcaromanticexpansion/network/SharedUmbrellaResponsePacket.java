package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.util.SharedUmbrellaManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SharedUmbrellaResponsePacket {
    private final UUID targetUUID;
    private final boolean accepted;

    public SharedUmbrellaResponsePacket(UUID targetUUID, boolean accepted) {
        this.targetUUID = targetUUID;
        this.accepted = accepted;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(targetUUID);
        buf.writeBoolean(accepted);
    }

    public static SharedUmbrellaResponsePacket decode(FriendlyByteBuf buf) {
        return new SharedUmbrellaResponsePacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(SharedUmbrellaResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                SharedUmbrellaManager.handleResponse(player, packet);
            }
        });
        context.setPacketHandled(true);
    }
}