package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.api.event.ProposalRespondedEvent;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ProposalResponsePacket {
    private final UUID proposerUUID;
    private final boolean accepted;

    public ProposalResponsePacket(UUID proposerUUID, boolean accepted) {
        this.proposerUUID = proposerUUID;
        this.accepted = accepted;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(proposerUUID);
        buf.writeBoolean(accepted);
    }

    public static ProposalResponsePacket decode(FriendlyByteBuf buf) {
        return new ProposalResponsePacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(ProposalResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer responder = context.getSender();
            if (responder == null) return;

            ServerPlayer proposer = responder.getServer().getPlayerList().getPlayer(packet.proposerUUID);
            if (proposer == null) return;

            // 触发求婚响应事件
            ProposalRespondedEvent event = new ProposalRespondedEvent(
                    responder, proposer, packet.accepted
            );

            if (MinecraftForge.EVENT_BUS.post(event)) {
                // 事件被取消
                MCARomanticExpansion.LOGGER.debug("Proposal response cancelled by event");
                return;
            }

            if (packet.accepted) {
                // 接受求婚
                MCARomanticExpansion.LOGGER.info("{} accepted proposal from {}!",
                        responder.getName().getString(), proposer.getName().getString());

                // 增加好感度
                AffectionManager.handleInteraction(AffectionManager.InteractionType.PROPOSAL_ACCEPT,
                        proposer, responder);

                // 这里可以添加订婚逻辑...

            } else {
                // 拒绝求婚
                MCARomanticExpansion.LOGGER.info("{} rejected proposal from {}",
                        responder.getName().getString(), proposer.getName().getString());
            }
        });
        context.setPacketHandled(true);
    }
}