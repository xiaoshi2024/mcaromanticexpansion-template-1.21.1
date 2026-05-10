package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.ProposalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;

public record OpenProposalGUIPacket(UUID proposerUUID, String proposerName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenProposalGUIPacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("open_proposal_gui"));

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

    // 添加客户端处理方法
    @OnlyIn(Dist.CLIENT)
    public void handleClient() {
        MCARomanticExpansion.LOGGER.info("CLIENT: OpenProposalGUIPacket received! proposerUUID={}, proposerName={}",
                proposerUUID, proposerName);
        Minecraft.getInstance().execute(() -> {
            MCARomanticExpansion.LOGGER.info("CLIENT: Opening ProposalScreen for UUID: {}", proposerUUID);
            Minecraft.getInstance().setScreen(new ProposalScreen(proposerUUID, proposerName));
            MCARomanticExpansion.LOGGER.info("CLIENT: ProposalScreen opened successfully!");
        });
    }
}