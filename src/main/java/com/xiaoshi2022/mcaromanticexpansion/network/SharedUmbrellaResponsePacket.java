package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record SharedUmbrellaResponsePacket(UUID targetUUID, boolean accepted) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SharedUmbrellaResponsePacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("shared_umbrella_response"));

    public static final StreamCodec<FriendlyByteBuf, SharedUmbrellaResponsePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.targetUUID());
                buf.writeBoolean(packet.accepted());
            },
            buf -> new SharedUmbrellaResponsePacket(buf.readUUID(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
