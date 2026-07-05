package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record CarryResponsePacket(UUID requesterUUID, boolean accepted) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CarryResponsePacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("carry_response"));

    public static final StreamCodec<FriendlyByteBuf, CarryResponsePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.requesterUUID());
                buf.writeBoolean(packet.accepted());
            },
            buf -> new CarryResponsePacket(buf.readUUID(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
