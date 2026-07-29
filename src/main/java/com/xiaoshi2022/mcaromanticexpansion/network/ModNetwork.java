package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MCARomanticExpansion.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int packetId = 0;

    public static void register() {
        // ========== 共伞相关包 ==========
        CHANNEL.registerMessage(packetId++, SharedUmbrellaRequestPacket.class,
                SharedUmbrellaRequestPacket::encode,
                SharedUmbrellaRequestPacket::decode,
                SharedUmbrellaRequestPacket::handle);

        CHANNEL.registerMessage(packetId++, SharedUmbrellaResponsePacket.class,
                SharedUmbrellaResponsePacket::encode,
                SharedUmbrellaResponsePacket::decode,
                SharedUmbrellaResponsePacket::handle);

        // ========== 伞架同步包 ==========
        CHANNEL.registerMessage(packetId++, UmbrellaStandSyncPacket.class,
                UmbrellaStandSyncPacket::encode,
                UmbrellaStandSyncPacket::decode,
                UmbrellaStandSyncPacket::handle);

        // ========== 公主抱相关包 ==========
        CHANNEL.registerMessage(packetId++, CarryRequestPacket.class,
                CarryRequestPacket::encode,
                CarryRequestPacket::decode,
                CarryRequestPacket::handle);

        CHANNEL.registerMessage(packetId++, CarryResponsePacket.class,
                CarryResponsePacket::encode,
                CarryResponsePacket::decode,
                CarryResponsePacket::handle);

        CHANNEL.registerMessage(packetId++, CarryInvitePacket.class,
                CarryInvitePacket::encode,
                CarryInvitePacket::decode,
                CarryInvitePacket::handle);

        CHANNEL.registerMessage(packetId++, CarryStatePayload.class,
                CarryStatePayload::encode,
                CarryStatePayload::decode,
                CarryStatePayload::handle);

        CHANNEL.registerMessage(packetId++, CarryStopPacket.class,
                CarryStopPacket::encode,
                CarryStopPacket::decode,
                CarryStopPacket::handle);

        // ========== 情书相关包 ==========
        CHANNEL.registerMessage(packetId++, LoveLetterSavePacket.class,
                LoveLetterSavePacket::encode,
                LoveLetterSavePacket::decode,
                LoveLetterSavePacket::handle);

        // ========== GUI 打开包（服务端 → 客户端） ==========
        CHANNEL.registerMessage(packetId++, OpenProposalGUIPacket.class,
                OpenProposalGUIPacket::encode,
                OpenProposalGUIPacket::decode,
                OpenProposalGUIPacket::handle);

        CHANNEL.registerMessage(packetId++, OpenBouquetGUIPacket.class,
                OpenBouquetGUIPacket::encode,
                OpenBouquetGUIPacket::decode,
                OpenBouquetGUIPacket::handle);

        CHANNEL.registerMessage(packetId++, OpenMarriageGUIPacket.class,
                OpenMarriageGUIPacket::encode,
                OpenMarriageGUIPacket::decode,
                OpenMarriageGUIPacket::handle);

        // ========== 好感度同步包 ==========
        CHANNEL.registerMessage(packetId++, AffectionSyncPacket.class,
                AffectionSyncPacket::encode,
                AffectionSyncPacket::decode,
                AffectionSyncPacket::handle);

        // ========== 🆕 求婚响应包（客户端 → 服务端） ==========
        CHANNEL.registerMessage(packetId++, ProposalResponsePacket.class,
                ProposalResponsePacket::encode,
                ProposalResponsePacket::decode,
                ProposalResponsePacket::handle);

        // ========== 🆕 结婚响应包（客户端 → 服务端） ==========
        CHANNEL.registerMessage(packetId++, MarriageResponsePacket.class,
                MarriageResponsePacket::encode,
                MarriageResponsePacket::decode,
                MarriageResponsePacket::handle);

        // ========== 🆕 花束响应包（客户端 → 服务端） ==========
        CHANNEL.registerMessage(packetId++, BouquetResponsePacket.class,
                BouquetResponsePacket::encode,
                BouquetResponsePacket::decode,
                BouquetResponsePacket::handle);

        MCARomanticExpansion.LOGGER.info("Registered {} network messages", packetId);
    }
}