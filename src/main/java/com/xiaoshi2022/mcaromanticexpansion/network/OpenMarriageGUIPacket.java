package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record OpenMarriageGUIPacket(UUID partnerUUID, String partnerName) implements CustomPacketPayload {
    public static final Type<OpenMarriageGUIPacket> TYPE =
            new Type<>(MCARomanticExpansion.locate("open_marriage_gui"));

    public static final StreamCodec<FriendlyByteBuf, OpenMarriageGUIPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.partnerUUID());
                buf.writeUtf(packet.partnerName(), 64);
            },
            buf -> new OpenMarriageGUIPacket(buf.readUUID(), buf.readUtf(64))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
