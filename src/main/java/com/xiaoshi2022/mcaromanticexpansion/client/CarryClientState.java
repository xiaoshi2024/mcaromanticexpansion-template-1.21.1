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

    private CarryClientState() {
    }

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

    /**
     * 通过 UUID 获取实体（供 Mixin 使用）
     */
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
            // 如果缓存中的 ID 无效，移除它
            UUID_TO_ENTITY_ID.remove(uuid);
        }

        // 缓存中没有或无效，遍历查找
        // 使用 getEntitiesForRendering() 或直接遍历实体列表
        for (Entity entity : getClientLevelEntities(mc)) {
            if (entity.getUUID().equals(uuid)) {
                UUID_TO_ENTITY_ID.put(uuid, entity.getId());
                return entity;
            }
        }
        return null;
    }

    /**
     * 获取客户端世界中的所有实体
     * 使用反射来访问 protected 的 getEntities() 方法
     */
    private static Iterable<Entity> getClientLevelEntities(Minecraft mc) {
        try {
            // 尝试使用反射调用 getEntities()
            java.lang.reflect.Method method = mc.level.getClass().getDeclaredMethod("getEntities");
            method.setAccessible(true);
            return (Iterable<Entity>) method.invoke(mc.level);
        } catch (Exception e) {
            // 如果反射失败，返回空列表
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 通过 UUID 获取玩家实体（便利方法）
     */
    public static Player getPlayerByUUID(UUID uuid) {
        Entity entity = getEntityByUUID(uuid);
        return entity instanceof Player ? (Player) entity : null;
    }

    private static void cacheEntityIds(UUID carrierId, UUID passengerId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (Entity entity : getClientLevelEntities(mc)) {
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

        Integer carrierEntityId = UUID_TO_ENTITY_ID.get(carrierId);
        Integer passengerEntityId = UUID_TO_ENTITY_ID.get(passengerId);

        if (carrierEntityId == null || passengerEntityId == null) {
            Entity carrier = getEntityByUUID(carrierId);
            Entity passenger = getEntityByUUID(passengerId);
            if (carrier == null || passenger == null) return;

            UUID_TO_ENTITY_ID.put(carrierId, carrier.getId());
            UUID_TO_ENTITY_ID.put(passengerId, passenger.getId());

            if (start) {
                passenger.startRiding(carrier, true);
            } else {
                if (passenger.getVehicle() == carrier) {
                    passenger.stopRiding();
                }
            }
            return;
        }

        Entity carrier = mc.level.getEntity(carrierEntityId);
        Entity passenger = mc.level.getEntity(passengerEntityId);

        if (carrier == null || passenger == null) return;

        if (start) {
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