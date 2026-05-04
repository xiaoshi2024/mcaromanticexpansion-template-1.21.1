package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.RingNBTUtil;
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

    // ProposalResponsePacket.java - 修改 handle 方法
    public void handle(ServerPlayer responder) {
        if (responder.getServer() == null) return;

        ServerPlayer proposer = responder.getServer().getPlayerList().getPlayer(proposerUUID);
        if (proposer == null) {
            MCARomanticExpansion.LOGGER.warn("Proposer not found: {}", proposerUUID);
            return;
        }

        MCARomanticExpansion.LOGGER.info("Processing proposal response: proposer={}, responder={}, accepted={}",
                proposer.getName().getString(), responder.getName().getString(), accepted);

        if (accepted) {
            // 先转移戒指（在调用 MCA 方法之前）
            boolean found = false;
            ItemStack ringWithNBT = null;
            for (int i = 0; i < proposer.getInventory().getContainerSize(); i++) {
                ItemStack stack = proposer.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof EngagementRingItem) {
                    ringWithNBT = stack.copy();
                    ringWithNBT.setCount(1);
                    // 清除原有自定义名称
                    ringWithNBT.remove(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
                    RingNBTUtil.setEngagementRingTarget(ringWithNBT, responder);
                    stack.shrink(1);
                    found = true;
                    MCARomanticExpansion.LOGGER.info("Removed engagement ring from proposer");
                    break;
                }
            }

            // 然后调用 MCA 的接受求婚方法
            try {
                ServerInteractionManager.getInstance().acceptProposal(proposer, responder);
                MCARomanticExpansion.LOGGER.info("MCA acceptProposal called successfully");
            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.error("MCA acceptProposal failed", e);
            }

            if (found && ringWithNBT != null) {
                responder.getInventory().add(ringWithNBT);
                MCARomanticExpansion.LOGGER.info("Added custom engagement ring to responder");
            }

            // 发送成功消息 - 使用不同的格式避免与 MCA 冲突
            responder.sendSystemMessage(Component.literal("§d§o" + proposer.getName().getString() + " 向你求婚了！你接受了！"));
            proposer.sendSystemMessage(Component.literal("§a§o" + responder.getName().getString() + " 接受了你的求婚！"));
        } else {
            ServerInteractionManager.getInstance().rejectProposal(proposer, responder);
            responder.sendSystemMessage(Component.translatable("mcaromanticexpansion.proposal.rejected"));
            proposer.sendSystemMessage(Component.translatable("mcaromanticexpansion.proposal.rejected_by", responder.getName()));
        }
    }
}