//package com.xiaoshi2022.mcaromanticexpansion;
//
//import com.xiaoshi2022.mcaromanticexpansion.network.*;
//import com.xiaoshi2022.mcaromanticexpansion.util.CarryRuntime;
//import com.xiaoshi2022.mcaromanticexpansion.util.SharedUmbrellaManager;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraftforge.network.NetworkDirection;
//import net.minecraftforge.network.NetworkRegistry;
//import net.minecraftforge.network.PacketDistributor;
//import net.minecraftforge.network.simple.SimpleChannel;
//
//public class RomanceNetwork {
//    private static final String PROTOCOL_VERSION = "1";
//    private static SimpleChannel INSTANCE;
//
//    public static void registerPackets() {
//        INSTANCE = NetworkRegistry.newSimpleChannel(
//                MCARomanticExpansion.locate("main"),
//                () -> PROTOCOL_VERSION,
//                PROTOCOL_VERSION::equals,
//                PROTOCOL_VERSION::equals
//        );
//
//        int id = 0;
//
//        INSTANCE.messageBuilder(UmbrellaStandSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
//                .encoder(UmbrellaStandSyncPacket::encode)
//                .decoder(UmbrellaStandSyncPacket::decode)
//                .consumer(UmbrellaStandSyncHandler::handleClient)
//                .add();
//
//        INSTANCE.messageBuilder(OpenBouquetGUIPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
//                .encoder(OpenBouquetGUIPacket::encode)
//                .decoder(OpenBouquetGUIPacket::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    try {
//                        Class<?> handlerClass = Class.forName("com.xiaoshi2022.mcaromanticexpansion.network.GUIPacketHandlers");
//                        java.lang.reflect.Method method = handlerClass.getMethod("handleOpenBouquetGUI", OpenBouquetGUIPacket.class);
//                        method.invoke(null, msg);
//                    } catch (Exception e) {
//                        MCARomanticExpansion.LOGGER.warn("Failed to handle OpenBouquetGUIPacket", e);
//                    }
//                }))
//                .add();
//
//        INSTANCE.messageBuilder(OpenProposalGUIPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
//                .encoder(OpenProposalGUIPacket::encode)
//                .decoder(OpenProposalGUIPacket::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    try {
//                        Class<?> handlerClass = Class.forName("com.xiaoshi2022.mcaromanticexpansion.network.GUIPacketHandlers");
//                        java.lang.reflect.Method method = handlerClass.getMethod("handleOpenProposalGUI", OpenProposalGUIPacket.class);
//                        method.invoke(null, msg);
//                    } catch (Exception e) {
//                        MCARomanticExpansion.LOGGER.warn("Failed to handle OpenProposalGUIPacket", e);
//                    }
//                }))
//                .add();
//
//        INSTANCE.messageBuilder(OpenMarriageGUIPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
//                .encoder(OpenMarriageGUIPacket::encode)
//                .decoder(OpenMarriageGUIPacket::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    try {
//                        Class<?> handlerClass = Class.forName("com.xiaoshi2022.mcaromanticexpansion.network.GUIPacketHandlers");
//                        java.lang.reflect.Method method = handlerClass.getMethod("handleOpenMarriageGUI", OpenMarriageGUIPacket.class);
//                        method.invoke(null, msg);
//                    } catch (Exception e) {
//                        MCARomanticExpansion.LOGGER.warn("Failed to handle OpenMarriageGUIPacket", e);
//                    }
//                }))
//                .add();
//
//        INSTANCE.messageBuilder(BouquetResponsePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
//                .encoder(BouquetResponsePacket::encode)
//                .decoder(BouquetResponsePacket::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    if (ctx.getSender() instanceof ServerPlayer serverPlayer) {
//                        msg.handle(serverPlayer);
//                    }
//                }))
//                .add();
//
//        INSTANCE.messageBuilder(ProposalResponsePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
//                .encoder(ProposalResponsePacket::encode)
//                .decoder(ProposalResponsePacket::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    if (ctx.getSender() instanceof ServerPlayer serverPlayer) {
//                        msg.handle(serverPlayer);
//                    }
//                }))
//                .add();
//
//        INSTANCE.messageBuilder(MarriageResponsePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
//                .encoder(MarriageResponsePacket::encode)
//                .decoder(MarriageResponsePacket::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    if (ctx.getSender() instanceof ServerPlayer serverPlayer) {
//                        msg.handle(serverPlayer);
//                    }
//                }))
//                .add();
//
//        INSTANCE.messageBuilder(SharedUmbrellaRequestPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
//                .encoder(SharedUmbrellaRequestPacket::encode)
//                .decoder(SharedUmbrellaRequestPacket::decode)
//                .consumer(SharedUmbrellaRequestHandler::handleClient)
//                .add();
//
//        INSTANCE.messageBuilder(AffectionSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
//                .encoder(AffectionSyncPacket::encode)
//                .decoder(AffectionSyncPacket::decode)
//                .consumer(AffectionSyncPacket::handleClient)
//                .add();
//
//        INSTANCE.messageBuilder(SharedUmbrellaResponsePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
//                .encoder(SharedUmbrellaResponsePacket::encode)
//                .decoder(SharedUmbrellaResponsePacket::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    if (ctx.getSender() instanceof ServerPlayer serverPlayer) {
//                        SharedUmbrellaManager.handleResponse(serverPlayer, msg);
//                    }
//                }))
//                .add();
//
//        INSTANCE.messageBuilder(CarryRequestPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
//                .encoder(CarryRequestPacket::encode)
//                .decoder(CarryRequestPacket::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    if (ctx.getSender() instanceof ServerPlayer serverPlayer) {
//                        CarryRuntime.handleCarryRequest(serverPlayer, msg);
//                    }
//                }))
//                .add();
//
//        INSTANCE.messageBuilder(CarryInvitePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
//                .encoder(CarryInvitePacket::encode)
//                .decoder(CarryInvitePacket::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    try {
//                        Class<?> handlerClass = Class.forName("com.xiaoshi2022.mcaromanticexpansion.network.GUIPacketHandlers");
//                        java.lang.reflect.Method method = handlerClass.getMethod("handleOpenPrincessCarryGUI", CarryInvitePacket.class);
//                        method.invoke(null, msg);
//                    } catch (Exception e) {
//                        MCARomanticExpansion.LOGGER.warn("Failed to handle CarryInvitePacket", e);
//                    }
//                }))
//                .add();
//
//        INSTANCE.messageBuilder(CarryResponsePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
//                .encoder(CarryResponsePacket::encode)
//                .decoder(CarryResponsePacket::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    if (ctx.getSender() instanceof ServerPlayer serverPlayer) {
//                        CarryRuntime.handleCarryResponse(serverPlayer, msg);
//                    }
//                }))
//                .add();
//
//        INSTANCE.messageBuilder(CarryStatePayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
//                .encoder(CarryStatePayload::encode)
//                .decoder(CarryStatePayload::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    try {
//                        Class<?> handlerClass = Class.forName("com.xiaoshi2022.mcaromanticexpansion.network.GUIPacketHandlers");
//                        java.lang.reflect.Method method = handlerClass.getMethod("handleCarryState", CarryStatePayload.class);
//                        method.invoke(null, msg);
//                    } catch (Exception e) {
//                        MCARomanticExpansion.LOGGER.warn("Failed to handle CarryStatePayload", e);
//                    }
//                }))
//                .add();
//
//        INSTANCE.messageBuilder(CarryStopPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
//                .encoder(CarryStopPacket::encode)
//                .decoder(CarryStopPacket::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    if (ctx.getSender() instanceof ServerPlayer serverPlayer) {
//                        CarryRuntime.handleStopRequest(serverPlayer);
//                    }
//                }))
//                .add();
//
//        INSTANCE.messageBuilder(LoveLetterSavePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
//                .encoder(LoveLetterSavePacket::encode)
//                .decoder(LoveLetterSavePacket::decode)
//                .consumer((msg, ctx) -> ctx.enqueueWork(() -> {
//                    if (ctx.getSender() instanceof ServerPlayer serverPlayer) {
//                        msg.handle(serverPlayer);
//                    }
//                }))
//                .add();
//    }
//
//    public static void sendToClient(ServerPlayer player, Object packet) {
//        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
//    }
//
//    public static void sendToAll(Object packet) {
//        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
//    }
//}