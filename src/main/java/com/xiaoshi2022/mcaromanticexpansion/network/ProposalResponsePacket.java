package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.conczin.mca.item.EngagementRingItem;
import net.conczin.mca.registry.ItemsMCA;
import net.conczin.mca.server.ServerInteractionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public record ProposalResponsePacket(UUID proposerUUID, boolean accepted) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ProposalResponsePacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("proposal_response"));

    public static final StreamCodec<FriendlyByteBuf, ProposalResponsePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.proposerUUID());
                buf.writeBoolean(packet.accepted());
            },
            buf -> new ProposalResponsePacket(buf.readUUID(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer responder) {  // responder 是被求婚者
        if (responder.getServer() == null) return;

        ServerPlayer proposer = responder.getServer().getPlayerList().getPlayer(proposerUUID);
        if (proposer == null) {
            MCARomanticExpansion.LOGGER.warn("Proposer not found: {}", proposerUUID);
            return;
        }

        MCARomanticExpansion.LOGGER.info("Processing proposal response: proposer={}, responder={}, accepted={}",
                proposer.getName().getString(), responder.getName().getString(), accepted);

        if (accepted) {
            // 重要：让求婚者发起接受请求，而不是被求婚者
            // 正确的方式是让求婚者调用 acceptProposal，参数为（求婚者，被求婚者）
            // 根据 MCA 源码，acceptProposal 的第一个参数是主动方

            // 尝试1：求婚者接受回应者的求婚
            ServerInteractionManager.getInstance().acceptProposal(proposer, responder);

            // 转移订婚戒指：从求婚者背包移除，添加到接受者背包
            boolean found = false;
            for (int i = 0; i < proposer.getInventory().getContainerSize(); i++) {
                ItemStack stack = proposer.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof EngagementRingItem) {
                    stack.shrink(1);
                    found = true;
                    MCARomanticExpansion.LOGGER.info("Removed engagement ring from proposer");
                    break;
                }
            }
            if (found) {
                responder.getInventory().add(new ItemStack(ItemsMCA.ENGAGEMENT_RING));
                MCARomanticExpansion.LOGGER.info("Added engagement ring to responder");
            }

            // 发送成功消息
            responder.sendSystemMessage(Component.translatable("mcaromanticexpansion.proposal.accepted", proposer.getName()));
            proposer.sendSystemMessage(Component.translatable("mcaromanticexpansion.proposal.accepted", responder.getName()));
        } else {
            // 拒绝求婚
            ServerInteractionManager.getInstance().rejectProposal(proposer, responder);
            responder.sendSystemMessage(Component.translatable("mcaromanticexpansion.proposal.rejected"));
            proposer.sendSystemMessage(Component.translatable("mcaromanticexpansion.proposal.rejected_by", responder.getName()));
        }
    }
}