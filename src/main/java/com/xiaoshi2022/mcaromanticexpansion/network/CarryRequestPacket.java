package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record CarryRequestPacket(UUID targetUUID) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CarryRequestPacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("carry_request"));

    public static final StreamCodec<FriendlyByteBuf, CarryRequestPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeUUID(packet.targetUUID()),
            buf -> new CarryRequestPacket(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
