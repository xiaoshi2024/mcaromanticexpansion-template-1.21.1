package com.xiaoshi2022.mcaromanticexpansion;

import com.xiaoshi2022.mcaromanticexpansion.network.*;
import com.xiaoshi2022.mcaromanticexpansion.util.SharedUmbrellaManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class RomanceNetwork {

    // ✅ 添加静态 CHANNEL 字段供 CarryRuntime 使用
    public static PayloadRegistrar CHANNEL;

    public static void registerPackets(IEventBus modEventBus) {
        modEventBus.addListener(RomanceNetwork::registerNetworkPackets);
    }

    public static void registerNetworkPackets(RegisterPayloadHandlersEvent event) {
        // ✅ 关键修复：使用 optional() 而不是 versioned()
        // 这样客户端即使没有这些通道也能连接
        CHANNEL = event.registrar(MCARomanticExpansion.MODID).optional();
        PayloadRegistrar registrar = CHANNEL;


        registrar.playToClient(
                UmbrellaStandSyncPacket.TYPE,
                UmbrellaStandSyncPacket.STREAM_CODEC,
                UmbrellaStandSyncHandler::handleClient
        );

        registrar.playToClient(
                OpenBouquetGUIPacket.TYPE,
                OpenBouquetGUIPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    try {
                        Class<?> handlerClass = Class.forName("com.xiaoshi2022.mcaromanticexpansion.network.GUIPacketHandlers");
                        java.lang.reflect.Method method = handlerClass.getMethod("handleOpenBouquetGUI", OpenBouquetGUIPacket.class);
                        method.invoke(null, payload);
                    } catch (Exception e) {
                        MCARomanticExpansion.LOGGER.warn("Failed to handle OpenBouquetGUIPacket (likely server-side)", e);
                    }
                })
        );

        registrar.playToClient(
                OpenProposalGUIPacket.TYPE,
                OpenProposalGUIPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    try {
                        Class<?> handlerClass = Class.forName("com.xiaoshi2022.mcaromanticexpansion.network.GUIPacketHandlers");
                        java.lang.reflect.Method method = handlerClass.getMethod("handleOpenProposalGUI", OpenProposalGUIPacket.class);
                        method.invoke(null, payload);
                    } catch (Exception e) {
                        MCARomanticExpansion.LOGGER.warn("Failed to handle OpenProposalGUIPacket (likely server-side)", e);
                    }
                })
        );

        registrar.playToClient(
                OpenMarriageGUIPacket.TYPE,
                OpenMarriageGUIPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    try {
                        Class<?> handlerClass = Class.forName("com.xiaoshi2022.mcaromanticexpansion.network.GUIPacketHandlers");
                        java.lang.reflect.Method method = handlerClass.getMethod("handleOpenMarriageGUI", OpenMarriageGUIPacket.class);
                        method.invoke(null, payload);
                    } catch (Exception e) {
                        MCARomanticExpansion.LOGGER.warn("Failed to handle OpenMarriageGUIPacket (likely server-side)", e);
                    }
                })
        );

        registrar.playToServer(
                BouquetResponsePacket.TYPE,
                BouquetResponsePacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        payload.handle(serverPlayer);
                    }
                })
        );

        registrar.playToServer(
                ProposalResponsePacket.TYPE,
                ProposalResponsePacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        payload.handle(serverPlayer);
                    }
                })
        );

        registrar.playToServer(
                MarriageResponsePacket.TYPE,
                MarriageResponsePacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        payload.handle(serverPlayer);
                    }
                })
        );

        registrar.playToClient(
                SharedUmbrellaRequestPacket.TYPE,
                SharedUmbrellaRequestPacket.STREAM_CODEC,
                SharedUmbrellaRequestHandler::handleClient
        );

        registrar.playToClient(
                AffectionSyncPacket.TYPE,
                AffectionSyncPacket.STREAM_CODEC,
                AffectionSyncPacket::handleClient
        );

        registrar.playToServer(
                SharedUmbrellaResponsePacket.TYPE,
                SharedUmbrellaResponsePacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        SharedUmbrellaManager.handleResponse(serverPlayer, payload);
                    }
                })
        );

        registrar.playToServer(
                LoveLetterSavePacket.TYPE,
                LoveLetterSavePacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        payload.handle(serverPlayer);
                    }
                })
        );
    }
}
