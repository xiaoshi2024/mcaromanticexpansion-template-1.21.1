package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record OpenBouquetGUIPacket(UUID giverUUID, String giverName) implements CustomPacketPayload {
    public static final Type<OpenBouquetGUIPacket> TYPE =
            new Type<>(MCARomanticExpansion.locate("open_bouquet_gui"));

    public static final StreamCodec<FriendlyByteBuf, OpenBouquetGUIPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.giverUUID());
                buf.writeUtf(packet.giverName(), 64);
            },
            buf -> new OpenBouquetGUIPacket(buf.readUUID(), buf.readUtf(64))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
