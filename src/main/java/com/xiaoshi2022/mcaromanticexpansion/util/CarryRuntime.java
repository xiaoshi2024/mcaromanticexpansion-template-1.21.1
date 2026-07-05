package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.network.CarryInvitePacket;
import com.xiaoshi2022.mcaromanticexpansion.network.CarryRequestPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.CarryResponsePacket;
import com.xiaoshi2022.mcaromanticexpansion.network.CarryStatePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CarryRuntime {
    private static final double CARRY_HEIGHT = 0.85d;

    private static final Map<UUID, UUID> CARRIER_PASSENGER = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PASSENGER_CARRIER = new ConcurrentHashMap<>();

    private CarryRuntime() {
    }

    public static double carryHeight() {
        return CARRY_HEIGHT;
    }

    public static boolean isCarryPair(UUID carrierId, UUID passengerId) {
        if (carrierId == null || passengerId == null) return false;
        UUID stored = CARRIER_PASSENGER.get(carrierId);
        return passengerId.equals(stored);
    }

    public static boolean isCarrier(UUID playerId) {
        return CARRIER_PASSENGER.containsKey(playerId);
    }

    public static boolean isCarried(UUID playerId) {
        return PASSENGER_CARRIER.containsKey(playerId);
    }

    public static UUID getPassengerOf(UUID carrierId) {
        return CARRIER_PASSENGER.get(carrierId);
    }

    public static UUID getCarrierOf(UUID passengerId) {
        return PASSENGER_CARRIER.get(passengerId);
    }

    public static void handleCarryRequest(ServerPlayer requester, CarryRequestPacket packet) {
        ServerLevel level = requester.serverLevel();
        Entity targetEntity = level.getEntity(packet.targetUUID());
        if (!(targetEntity instanceof ServerPlayer target)) {
            return;
        }
        if (target == requester) return;

        if (isCarrier(requester.getUUID()) || isCarried(requester.getUUID())
                || isCarrier(target.getUUID()) || isCarried(target.getUUID())) {
            requester.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.carry.busy"), true);
            return;
        }

        double dist = requester.distanceTo(target);
        if (dist > 5.0d) {
            requester.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.carry.too_far"), true);
            return;
        }

        PacketDistributor.sendToPlayer(target,
                new CarryInvitePacket(requester.getUUID(), requester.getGameProfile().getName()));
        requester.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.carry.sent",
                target.getGameProfile().getName()), true);
    }

    public static void handleCarryResponse(ServerPlayer responder, CarryResponsePacket packet) {
        ServerLevel level = responder.serverLevel();
        Entity requesterEntity = level.getEntity(packet.requesterUUID());
        if (!(requesterEntity instanceof ServerPlayer requester)) {
            return;
        }
        if (!packet.accepted()) {
            requester.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.carry.rejected",
                    responder.getGameProfile().getName()));
            return;
        }
        if (isCarrier(requester.getUUID()) || isCarried(requester.getUUID())
                || isCarrier(responder.getUUID()) || isCarried(responder.getUUID())) {
            requester.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.carry.busy"));
            return;
        }
        double dist = requester.distanceTo(responder);
        if (dist > 6.0d) {
            requester.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.carry.too_far_response"));
            return;
        }
        startCarry(requester, responder);
    }

    public static void startCarry(ServerPlayer carrier, ServerPlayer passenger) {
        boolean success = passenger.startRiding(carrier, true);
        if (!success) {
            carrier.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.carry.failed"));
            return;
        }
        CARRIER_PASSENGER.put(carrier.getUUID(), passenger.getUUID());
        PASSENGER_CARRIER.put(passenger.getUUID(), carrier.getUUID());
        broadcastCarryState(carrier.serverLevel(), carrier.getUUID(), passenger.getUUID(), true);
        MCARomanticExpansion.LOGGER.info("Princess carry started: {} -> {}", carrier.getName().getString(),
                passenger.getName().getString());
    }

    public static void stopCarry(UUID carrierId, ServerLevel level) {
        UUID passengerId = CARRIER_PASSENGER.remove(carrierId);
        if (passengerId != null) {
            PASSENGER_CARRIER.remove(passengerId);
            Entity carrier = level.getEntity(carrierId);
            Entity passenger = level.getEntity(passengerId);
            if (passenger != null) {
                passenger.stopRiding();
            }
            broadcastCarryState(level, carrierId, passengerId, false);
            if (carrier instanceof Player c && passenger instanceof Player p) {
                MCARomanticExpansion.LOGGER.info("Princess carry stopped: {} -> {}",
                        c.getName().getString(), p.getName().getString());
            }
        }
    }

    public static void stopCarryFor(ServerLevel level, UUID playerId) {
        UUID asCarrierPassenger = CARRIER_PASSENGER.get(playerId);
        if (asCarrierPassenger != null) {
            stopCarry(playerId, level);
        }
        UUID asPassengerCarrier = PASSENGER_CARRIER.get(playerId);
        if (asPassengerCarrier != null) {
            stopCarry(asPassengerCarrier, level);
        }
    }

    public static void onPlayerLoggedOut(ServerLevel level, UUID playerId) {
        stopCarryFor(level, playerId);
    }

    public static void syncCarryStatesTo(ServerPlayer player) {
        for (Map.Entry<UUID, UUID> entry : CARRIER_PASSENGER.entrySet()) {
            PacketDistributor.sendToPlayer(player, new CarryStatePayload(entry.getKey(), entry.getValue(), true));
        }
    }

    public static void broadcastCarryState(ServerLevel level, UUID carrierId, UUID passengerId, boolean carrying) {
        CarryStatePayload payload = new CarryStatePayload(carrierId, passengerId, carrying);
        if (level.getServer() != null) {
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static java.util.Set<UUID> snapshotCarriers() {
        return java.util.Set.copyOf(CARRIER_PASSENGER.keySet());
    }

    public static void handleStopRequest(ServerPlayer player) {
        stopCarryFor(player.serverLevel(), player.getUUID());
    }
}
