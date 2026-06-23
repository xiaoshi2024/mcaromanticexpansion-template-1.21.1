package com.xiaoshi2022.mcaromanticexpansion;

import com.xiaoshi2022.mcaromanticexpansion.network.*;
import com.xiaoshi2022.mcaromanticexpansion.util.SharedUmbrellaManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class RomanceNetwork {
    public static void registerPackets(IEventBus modEventBus) {
        modEventBus.addListener(RomanceNetwork::registerNetworkPackets);
    }

    public static void registerNetworkPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MCARomanticExpansion.MODID).versioned("1.0.0");

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
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        payload.handle(serverPlayer);
                    }
                })
        );

        registrar.playToServer(
                ProposalResponsePacket.TYPE,
                ProposalResponsePacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        payload.handle(serverPlayer);
                    }
                })
        );

        registrar.playToServer(
                MarriageResponsePacket.TYPE,
                MarriageResponsePacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        payload.handle(serverPlayer);
                    }
                })
        );

        registrar.playToClient(
                SharedUmbrellaRequestPacket.TYPE,
                SharedUmbrellaRequestPacket.STREAM_CODEC,
                SharedUmbrellaRequestHandler::handleClient
        );

        // 好感度同步包
        registrar.playToClient(
                AffectionSyncPacket.TYPE,
                AffectionSyncPacket.STREAM_CODEC,
                AffectionSyncPacket::handleClient
        );

        registrar.playToServer(
                SharedUmbrellaResponsePacket.TYPE,
                SharedUmbrellaResponsePacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        SharedUmbrellaManager.handleResponse(serverPlayer, payload);
                    }
                })
        );
    }
}
