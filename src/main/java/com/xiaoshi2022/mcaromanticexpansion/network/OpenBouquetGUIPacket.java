package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.BouquetScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record OpenBouquetGUIPacket(UUID giverUUID) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenBouquetGUIPacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("open_bouquet_gui"));

    public static final StreamCodec<FriendlyByteBuf, OpenBouquetGUIPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeUUID(packet.giverUUID()),
            buf -> new OpenBouquetGUIPacket(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new BouquetScreen(giverUUID()));
        });
    }
}