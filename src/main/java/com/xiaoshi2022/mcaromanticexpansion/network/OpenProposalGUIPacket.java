package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record OpenProposalGUIPacket(UUID proposerUUID, String proposerName) implements CustomPacketPayload {
    public static final Type<OpenProposalGUIPacket> TYPE =
            new Type<>(MCARomanticExpansion.locate("open_proposal_gui"));

    public static final StreamCodec<FriendlyByteBuf, OpenProposalGUIPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.proposerUUID());
                buf.writeUtf(packet.proposerName(), 64);
            },
            buf -> new OpenProposalGUIPacket(buf.readUUID(), buf.readUtf(64))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
