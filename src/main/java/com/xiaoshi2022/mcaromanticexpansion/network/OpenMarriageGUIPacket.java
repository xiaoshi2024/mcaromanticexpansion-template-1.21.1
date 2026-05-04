package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.MarriageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;

public record OpenMarriageGUIPacket(UUID partnerUUID) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenMarriageGUIPacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("open_marriage_gui"));

    public static final StreamCodec<FriendlyByteBuf, OpenMarriageGUIPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeUUID(packet.partnerUUID()),
            buf -> new OpenMarriageGUIPacket(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    public void handle() {
        MCARomanticExpansion.LOGGER.info("CLIENT: OpenMarriageGUIPacket received! partnerUUID={}", partnerUUID);
        Minecraft.getInstance().execute(() -> {
            MCARomanticExpansion.LOGGER.info("CLIENT: Opening MarriageScreen for UUID: {}", partnerUUID);
            Minecraft.getInstance().setScreen(new MarriageScreen(partnerUUID));
            MCARomanticExpansion.LOGGER.info("CLIENT: MarriageScreen opened successfully!");
        });
    }
}