package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CarryStopPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CarryStopPacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("carry_stop"));

    public static final StreamCodec<FriendlyByteBuf, CarryStopPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> { },
            buf -> new CarryStopPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
