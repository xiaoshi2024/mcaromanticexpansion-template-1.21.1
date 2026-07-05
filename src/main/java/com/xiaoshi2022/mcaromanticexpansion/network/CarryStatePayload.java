package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record CarryStatePayload(UUID carrier, UUID passenger, boolean carrying) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CarryStatePayload> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("carry_state"));

    public static final StreamCodec<FriendlyByteBuf, CarryStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.carrier());
                buf.writeUUID(packet.passenger());
                buf.writeBoolean(packet.carrying());
            },
            buf -> new CarryStatePayload(buf.readUUID(), buf.readUUID(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
