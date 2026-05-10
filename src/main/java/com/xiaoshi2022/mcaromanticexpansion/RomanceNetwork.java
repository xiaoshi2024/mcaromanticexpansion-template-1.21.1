package com.xiaoshi2022.mcaromanticexpansion;

import com.xiaoshi2022.mcaromanticexpansion.network.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class RomanceNetwork {
    public static void registerPackets(IEventBus modEventBus) {
        modEventBus.addListener(RomanceNetwork::registerNetworkPackets);
    }

    public static void registerNetworkPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MCARomanticExpansion.MODID).versioned("1.0.0");

        // 服务端 -> 客户端：打开花束GUI
        registrar.playToClient(
                OpenBouquetGUIPacket.TYPE,
                OpenBouquetGUIPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::handleClient)
        );

        // 服务端 -> 客户端：打开求婚GUI
        registrar.playToClient(
                OpenProposalGUIPacket.TYPE,
                OpenProposalGUIPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::handleClient)
        );

        // 服务端 -> 客户端：打开婚礼GUI
        registrar.playToClient(
                OpenMarriageGUIPacket.TYPE,
                OpenMarriageGUIPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::handleClient)
        );

        // 客户端 -> 服务端：花束响应
        registrar.playToServer(
                BouquetResponsePacket.TYPE,
                BouquetResponsePacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        payload.handle(serverPlayer);
                    }
                })
        );

        // 客户端 -> 服务端：求婚响应
        registrar.playToServer(
                ProposalResponsePacket.TYPE,
                ProposalResponsePacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        payload.handle(serverPlayer);
                    }
                })
        );

        // 客户端 -> 服务端：婚礼响应
        registrar.playToServer(
                MarriageResponsePacket.TYPE,
                MarriageResponsePacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        payload.handle(serverPlayer);
                    }
                })
        );
    }
}