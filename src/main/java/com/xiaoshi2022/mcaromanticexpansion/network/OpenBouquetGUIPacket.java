package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.BouquetScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;

public record OpenBouquetGUIPacket(UUID giverUUID, String giverName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenBouquetGUIPacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("open_bouquet_gui"));

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

    // 客户端处理方法
    @OnlyIn(Dist.CLIENT)
    public void handleClient() {
        MCARomanticExpansion.LOGGER.info("CLIENT: OpenBouquetGUIPacket received! giverUUID={}, giverName={}",
                giverUUID, giverName);
        Minecraft.getInstance().execute(() -> {
            MCARomanticExpansion.LOGGER.info("CLIENT: Opening BouquetScreen for UUID: {}", giverUUID);
            Minecraft.getInstance().setScreen(new BouquetScreen(giverUUID, giverName));
            MCARomanticExpansion.LOGGER.info("CLIENT: BouquetScreen opened successfully!");
        });
    }
}