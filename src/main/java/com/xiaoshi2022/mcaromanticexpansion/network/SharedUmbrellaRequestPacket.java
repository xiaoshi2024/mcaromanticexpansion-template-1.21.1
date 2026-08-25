package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record SharedUmbrellaRequestPacket(UUID requesterUUID, String requesterName) implements CustomPacketPayload {
    public static final Type<SharedUmbrellaRequestPacket> TYPE =
            new Type<>(MCARomanticExpansion.locate("shared_umbrella_request"));

    public static final StreamCodec<FriendlyByteBuf, SharedUmbrellaRequestPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.requesterUUID());
                buf.writeUtf(packet.requesterName(), 64);
            },
            buf -> new SharedUmbrellaRequestPacket(buf.readUUID(), buf.readUtf(64))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
