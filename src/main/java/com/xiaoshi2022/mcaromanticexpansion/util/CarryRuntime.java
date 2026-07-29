package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.network.CarryInvitePacket;
import com.xiaoshi2022.mcaromanticexpansion.network.CarryRequestPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.CarryResponsePacket;
import com.xiaoshi2022.mcaromanticexpansion.network.CarryStatePayload;
import com.xiaoshi2022.mcaromanticexpansion.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CarryRuntime {
    private static final double CARRY_HEIGHT = 0.85d;
    private static final double MAX_REQUEST_DISTANCE = 5.0d;
    private static final double MAX_RESPONSE_DISTANCE = 6.0d;

    private static final Map<UUID, UUID> CARRIER_PASSENGER = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PASSENGER_CARRIER = new ConcurrentHashMap<>();

    private CarryRuntime() {}

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

    // ========== 处理公主抱请求 ==========
    public static void handleCarryRequest(ServerPlayer requester, CarryRequestPacket packet) {
        ServerLevel level = requester.serverLevel();
        Entity targetEntity = level.getEntity(packet.getTargetUUID());
        if (!(targetEntity instanceof ServerPlayer target)) {
            requester.sendSystemMessage(Component.literal("§c目标玩家不在线！"));
            return;
        }

        // 不能抱自己
        if (target == requester) {
            requester.sendSystemMessage(Component.literal("§c你不能抱自己！"));
            return;
        }

        // 检查双方是否空闲
        if (isCarrier(requester.getUUID()) || isCarried(requester.getUUID())) {
            requester.sendSystemMessage(Component.literal("§c你正在公主抱或被公主抱中！"));
            return;
        }
        if (isCarrier(target.getUUID()) || isCarried(target.getUUID())) {
            requester.sendSystemMessage(Component.literal("§c对方正在公主抱或被公主抱中！"));
            return;
        }

        // 检查距离
        double dist = requester.distanceTo(target);
        if (dist > MAX_REQUEST_DISTANCE) {
            requester.sendSystemMessage(Component.literal("§c目标太远了！距离：" + String.format("%.1f", dist) + " 米"));
            return;
        }

        // ✅ 修复：使用 Component.literal 而不是 translatable（如果没有语言文件）
        // 发送邀请给目标玩家
        CarryInvitePacket invitePacket = new CarryInvitePacket(
                requester.getUUID(),
                requester.getGameProfile().getName()
        );

        // ✅ 修复：使用 sendTo 或 PacketDistributor.PLAYER
        ModNetwork.CHANNEL.sendTo(invitePacket, target.connection.connection, NetworkDirection.PLAY_TO_CLIENT);

        // 通知请求者
        requester.sendSystemMessage(Component.literal("§a你向 " + target.getGameProfile().getName() + " 发起了公主抱请求！"));

        MCARomanticExpansion.LOGGER.debug("Carry request: {} -> {}",
                requester.getName().getString(), target.getName().getString());
    }

    // ========== 处理公主抱响应 ==========
    public static void handleCarryResponse(ServerPlayer responder, CarryResponsePacket packet) {
        ServerLevel level = responder.serverLevel();
        Entity requesterEntity = level.getEntity(packet.getRequesterUUID());
        if (!(requesterEntity instanceof ServerPlayer requester)) {
            responder.sendSystemMessage(Component.literal("§c请求者已离线！"));
            return;
        }

        if (!packet.isAccepted()) {
            requester.sendSystemMessage(Component.literal("§c" + responder.getGameProfile().getName() + " 拒绝了你的公主抱！"));
            responder.sendSystemMessage(Component.literal("§c你拒绝了 " + requester.getGameProfile().getName() + " 的公主抱！"));
            return;
        }

        // 再次检查双方状态
        if (isCarrier(requester.getUUID()) || isCarried(requester.getUUID())) {
            requester.sendSystemMessage(Component.literal("§c你正在公主抱或被公主抱中！"));
            responder.sendSystemMessage(Component.literal("§c" + requester.getGameProfile().getName() + " 正在公主抱或被公主抱中！"));
            return;
        }
        if (isCarrier(responder.getUUID()) || isCarried(responder.getUUID())) {
            requester.sendSystemMessage(Component.literal("§c" + responder.getGameProfile().getName() + " 正在公主抱或被公主抱中！"));
            responder.sendSystemMessage(Component.literal("§c你正在公主抱或被公主抱中！"));
            return;
        }

        // 检查距离
        double dist = requester.distanceTo(responder);
        if (dist > MAX_RESPONSE_DISTANCE) {
            requester.sendSystemMessage(Component.literal("§c" + responder.getGameProfile().getName() + " 接受了，但你太远了！"));
            responder.sendSystemMessage(Component.literal("§c" + requester.getGameProfile().getName() + " 太远了！"));
            return;
        }

        // 开始公主抱
        startCarry(requester, responder);
    }

    // ========== 开始公主抱 ==========
    public static void startCarry(ServerPlayer carrier, ServerPlayer passenger) {
        // 确保乘客没有骑乘其他实体
        if (passenger.getVehicle() != null) {
            passenger.stopRiding();
        }

        boolean success = passenger.startRiding(carrier, true);
        if (!success) {
            carrier.sendSystemMessage(Component.literal("§c公主抱失败！"));
            MCARomanticExpansion.LOGGER.warn("Failed to start carry: {} -> {}",
                    carrier.getName().getString(), passenger.getName().getString());
            return;
        }

        // 记录状态
        CARRIER_PASSENGER.put(carrier.getUUID(), passenger.getUUID());
        PASSENGER_CARRIER.put(passenger.getUUID(), carrier.getUUID());

        // 广播状态给所有玩家
        broadcastCarryState(carrier.serverLevel(), carrier.getUUID(), passenger.getUUID(), true);

        // 发送消息
        carrier.sendSystemMessage(Component.literal("§a你抱起了 " + passenger.getName().getString() + "！"));
        passenger.sendSystemMessage(Component.literal("§a" + carrier.getName().getString() + " 抱起了你！"));

        MCARomanticExpansion.LOGGER.info("Princess carry started: {} -> {}",
                carrier.getName().getString(), passenger.getName().getString());
    }

    // ========== 停止公主抱 ==========
    public static void stopCarry(UUID carrierId, ServerLevel level) {
        UUID passengerId = CARRIER_PASSENGER.remove(carrierId);
        if (passengerId != null) {
            PASSENGER_CARRIER.remove(passengerId);

            Entity carrier = level.getEntity(carrierId);
            Entity passenger = level.getEntity(passengerId);

            if (passenger != null && passenger.getVehicle() == carrier) {
                passenger.stopRiding();
            }

            broadcastCarryState(level, carrierId, passengerId, false);

            if (carrier instanceof Player c && passenger instanceof Player p) {
                MCARomanticExpansion.LOGGER.info("Princess carry stopped: {} -> {}",
                        c.getName().getString(), p.getName().getString());
            }
        }
    }

    // ========== 停止与某玩家相关的所有公主抱 ==========
    public static void stopCarryFor(ServerLevel level, UUID playerId) {
        // 检查该玩家是否作为载体
        UUID passengerId = CARRIER_PASSENGER.get(playerId);
        if (passengerId != null) {
            stopCarry(playerId, level);
            return;
        }

        // 检查该玩家是否作为乘客
        UUID carrierId = PASSENGER_CARRIER.get(playerId);
        if (carrierId != null) {
            stopCarry(carrierId, level);
        }
    }

    // ========== 玩家登出时处理 ==========
    public static void onPlayerLoggedOut(ServerLevel level, UUID playerId) {
        stopCarryFor(level, playerId);
    }

    // ========== 同步状态给新玩家 ==========
    public static void syncCarryStatesTo(ServerPlayer player) {
        for (Map.Entry<UUID, UUID> entry : CARRIER_PASSENGER.entrySet()) {
            CarryStatePayload payload = new CarryStatePayload(entry.getKey(), entry.getValue(), true);
            ModNetwork.CHANNEL.sendTo(payload, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    // ========== 广播状态给所有玩家 ==========
    public static void broadcastCarryState(ServerLevel level, UUID carrierId, UUID passengerId, boolean carrying) {
        CarryStatePayload payload = new CarryStatePayload(carrierId, passengerId, carrying);

        if (level.getServer() != null) {
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                ModNetwork.CHANNEL.sendTo(payload, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            }
        }
    }

    // ========== 获取所有载体快照 ==========
    public static Set<UUID> snapshotCarriers() {
        return Set.copyOf(CARRIER_PASSENGER.keySet());
    }

    // ========== 处理停止请求 ==========
    public static void handleStopRequest(ServerPlayer player) {
        stopCarryFor(player.serverLevel(), player.getUUID());
        player.sendSystemMessage(Component.literal("§e你停止了公主抱！"));
    }
}