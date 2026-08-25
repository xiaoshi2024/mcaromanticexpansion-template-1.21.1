package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.api.event.ProposalRespondedEvent;
import com.xiaoshi2022.mcaromanticexpansion.event.PregnancyAttemptHandler;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import com.xiaoshi2022.mcaromanticexpansion.util.MarriageConfig;
import com.xiaoshi2022.mcaromanticexpansion.util.RingNBTUtil;
import net.conczin.mca.entity.ai.relationship.Gender;
import net.conczin.mca.item.EngagementRingItem;
import net.conczin.mca.server.ServerInteractionManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.UUID;

public record ProposalResponsePacket(UUID proposerUUID, boolean accepted) implements CustomPacketPayload {
    public static final Type<ProposalResponsePacket> TYPE =
            new Type<>(MCARomanticExpansion.locate("proposal_response"));

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
        // 修复: 通过 level() 获取 Server
        ServerLevel serverLevel = null;
        if (responder.level() instanceof ServerLevel sl) {
            serverLevel = sl;
        }
        if (serverLevel == null) return;

        ServerPlayer proposer = serverLevel.getServer().getPlayerList().getPlayer(proposerUUID);
        if (proposer == null) {
            MCARomanticExpansion.LOGGER.warn("Proposer not found: {}", proposerUUID);
            return;
        }

        MCARomanticExpansion.LOGGER.debug("Processing proposal response: proposer={}, responder={}, accepted={}",
                proposer.getName().getString(), responder.getName().getString(), accepted);

        ProposalRespondedEvent forgeEvent = new ProposalRespondedEvent(responder, proposer, accepted);
        NeoForge.EVENT_BUS.post(forgeEvent);
        if (forgeEvent.isCanceled()) {
            MCARomanticExpansion.LOGGER.debug("Proposal response canceled by event listener");
            return;
        }

        if (accepted) {

            // ========== 验证性别 ==========
            Gender responderGender = PregnancyAttemptHandler.getGenderFromNBT(responder);
            Gender proposerGender = PregnancyAttemptHandler.getGenderFromNBT(proposer);

            if (responderGender == Gender.UNASSIGNED || proposerGender == Gender.UNASSIGNED) {
                responder.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.proposal.need_gender"));
                return;
            }

            if (responderGender == proposerGender) {
                boolean responderAllowed = MarriageConfig.isSameGenderMarriageAllowed(responder);
                boolean proposerAllowed = MarriageConfig.isSameGenderMarriageAllowed(proposer);

                if (!responderAllowed || !proposerAllowed) {
                    responder.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.proposal.same_gender_blocked_short"));
                    proposer.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.proposal.rejected_same_gender", responder.getName()));
                    return;
                }
            }

            // 先转移戒指（在调用 MCA 方法之前）
            boolean found = false;
            ItemStack ringWithNBT = null;
            for (int i = 0; i < proposer.getInventory().getContainerSize(); i++) {
                ItemStack stack = proposer.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof EngagementRingItem) {
                    ringWithNBT = stack.copy();
                    ringWithNBT.setCount(1);
                    // 清除原有自定义名称
                    ringWithNBT.remove(DataComponents.CUSTOM_NAME);
                    RingNBTUtil.setEngagementRingTarget(ringWithNBT, responder);
                    stack.shrink(1);
                    found = true;
                    MCARomanticExpansion.LOGGER.debug("Removed engagement ring from proposer");
                    break;
                }
            }

            // 然后调用 MCA 的接受求婚方法
            try {
                ServerInteractionManager.getInstance().acceptProposal(proposer, responder);
                MCARomanticExpansion.LOGGER.debug("MCA acceptProposal called successfully");
            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.error("MCA acceptProposal failed", e);
            }

            if (found && ringWithNBT != null) {
                responder.getInventory().add(ringWithNBT);
                MCARomanticExpansion.LOGGER.debug("Added custom engagement ring to responder");
            }

            // 发送成功消息 - 使用不同的格式避免与 MCA 冲突
            responder.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.proposal.received", proposer.getName()));
            proposer.sendSystemMessage(Component.translatable("mcaromanticexpansion.proposal.accepted", responder.getName()));

            // ========== 添加好感度：接受求婚 ==========
            AffectionManager.handleInteraction(AffectionManager.InteractionType.PROPOSAL_ACCEPT, proposer, responder);
            MCARomanticExpansion.LOGGER.debug("Added PROPOSAL_ACCEPT affection for {} and {}",
                    proposer.getName().getString(), responder.getName().getString());
        } else {
            ServerInteractionManager.getInstance().rejectProposal(proposer, responder);
            responder.sendSystemMessage(Component.translatable("mcaromanticexpansion.proposal.rejected"));
            proposer.sendSystemMessage(Component.translatable("mcaromanticexpansion.proposal.rejected_by", responder.getName()));
        }
    }
}