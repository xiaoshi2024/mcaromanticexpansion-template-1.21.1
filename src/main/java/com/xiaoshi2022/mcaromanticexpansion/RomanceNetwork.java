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

        registrar.playToClient(
                OpenBouquetGUIPacket.TYPE,
                OpenBouquetGUIPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::handle)
        );

        registrar.playToClient(
                OpenProposalGUIPacket.TYPE,
                OpenProposalGUIPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::handle)
        );

        registrar.playToClient(
                OpenMarriageGUIPacket.TYPE,
                OpenMarriageGUIPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(payload::handle)
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
    }
}
