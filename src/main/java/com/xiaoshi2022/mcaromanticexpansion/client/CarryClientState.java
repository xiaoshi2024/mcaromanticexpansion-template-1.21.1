package com.xiaoshi2022.mcaromanticexpansion.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CarryClientState {
    private static final Map<UUID, UUID> CARRIER_TO_PASSENGER = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PASSENGER_TO_CARRIER = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> UUID_TO_ENTITY_ID = new ConcurrentHashMap<>();

    private CarryClientState() {}

    public static boolean isCarrier(UUID playerId) {
        return CARRIER_TO_PASSENGER.containsKey(playerId);
    }

    public static boolean isCarried(UUID playerId) {
        return PASSENGER_TO_CARRIER.containsKey(playerId);
    }

    public static UUID passengerOf(UUID carrierId) {
        return CARRIER_TO_PASSENGER.get(carrierId);
    }

    public static UUID carrierOf(UUID passengerId) {
        return PASSENGER_TO_CARRIER.get(passengerId);
    }

    public static void accept(UUID carrierId, UUID passengerId, boolean carrying) {
        if (carrying) {
            CARRIER_TO_PASSENGER.put(carrierId, passengerId);
            PASSENGER_TO_CARRIER.put(passengerId, carrierId);
            cacheEntityIds(carrierId, passengerId);
            applyRidingOnClient(carrierId, passengerId, true);
        } else {
            UUID removedP = CARRIER_TO_PASSENGER.remove(carrierId);
            if (removedP != null) {
                PASSENGER_TO_CARRIER.remove(removedP);
                UUID_TO_ENTITY_ID.remove(removedP);
            }
            UUID removedC = PASSENGER_TO_CARRIER.remove(passengerId);
            if (removedC != null) {
                CARRIER_TO_PASSENGER.remove(removedC);
                UUID_TO_ENTITY_ID.remove(removedC);
            }
            UUID_TO_ENTITY_ID.remove(carrierId);
            UUID_TO_ENTITY_ID.remove(passengerId);
            applyRidingOnClient(carrierId, passengerId, false);
        }
    }

    public static Entity getEntityByUUID(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        // 先从缓存中获取实体 ID
        Integer entityId = UUID_TO_ENTITY_ID.get(uuid);
        if (entityId != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity != null && entity.getUUID().equals(uuid)) {
                return entity;
            }
            UUID_TO_ENTITY_ID.remove(uuid);
        }

        // ✅ 方案1：使用 getEntitiesForRendering()（最可靠）
        // 这个方法会返回所有可见的实体
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) {
                UUID_TO_ENTITY_ID.put(uuid, entity.getId());
                return entity;
            }
        }
        return null;
    }

    // 或者使用方案2：遍历玩家列表 + 实体列表
    public static Entity getEntityByUUIDAlternative(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        // 先检查缓存
        Integer entityId = UUID_TO_ENTITY_ID.get(uuid);
        if (entityId != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity != null && entity.getUUID().equals(uuid)) {
                return entity;
            }
            UUID_TO_ENTITY_ID.remove(uuid);
        }

        // 遍历所有加载的实体（使用 getEntitiesForRendering）
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) {
                UUID_TO_ENTITY_ID.put(uuid, entity.getId());
                return entity;
            }
        }
        return null;
    }

    public static Player getPlayerByUUID(UUID uuid) {
        Entity entity = getEntityByUUID(uuid);
        return entity instanceof Player ? (Player) entity : null;
    }

    private static void cacheEntityIds(UUID carrierId, UUID passengerId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // 使用 getEntitiesForRendering()
        for (Entity entity : mc.level.entitiesForRendering()) {
            UUID uuid = entity.getUUID();
            if (uuid.equals(carrierId)) {
                UUID_TO_ENTITY_ID.put(carrierId, entity.getId());
            }
            if (uuid.equals(passengerId)) {
                UUID_TO_ENTITY_ID.put(passengerId, entity.getId());
            }
        }
    }

    private static void applyRidingOnClient(UUID carrierId, UUID passengerId, boolean start) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity carrier = getEntityByUUID(carrierId);
        Entity passenger = getEntityByUUID(passengerId);

        if (carrier == null || passenger == null) return;

        if (start) {
            // 确保乘客还没有骑乘其他实体
            if (passenger.getVehicle() != null) {
                passenger.stopRiding();
            }
            passenger.startRiding(carrier, true);
        } else {
            if (passenger.getVehicle() == carrier) {
                passenger.stopRiding();
            }
        }
    }

    public static void clear() {
        CARRIER_TO_PASSENGER.clear();
        PASSENGER_TO_CARRIER.clear();
        UUID_TO_ENTITY_ID.clear();
    }
}