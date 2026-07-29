package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UmbrellaProtectionHandler {
    private static final int PROTECTION_RANGE = 3;
    private static final int HEARTS_PER_INTERVAL = 1;
    private static final int TICKS_PER_INTERVAL = 200;
    private static final Map<UUID, Long> lastHeartsAdd = new HashMap<>();

    // 缓存 VillagerEntityMCA 相关反射方法
    private static Class<?> villagerEntityClass;
    private static Method isVillagerMethod;
    private static Method getVillagerBrainMethod;
    private static Method rewardHeartsMethod;

    static {
        try {
            // 尝试加载 VillagerEntityMCA 类
            villagerEntityClass = Class.forName("forge.net.mca.entity.VillagerEntityMCA");
            getVillagerBrainMethod = villagerEntityClass.getMethod("getVillagerBrain");
            // 获取 rewardHearts 方法
            Class<?> villagerBrainClass = Class.forName("net.conczin.mca.entity.ai.VillagerBrainMCA");
            rewardHeartsMethod = villagerBrainClass.getMethod("rewardHearts", net.minecraft.world.entity.player.Player.class, int.class);
            MCARomanticExpansion.LOGGER.info("MCA VillagerEntityMCA found for umbrella protection");
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("MCA VillagerEntityMCA not found, umbrella protection will be disabled: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务端执行
        if (event.side.isClient()) return;
        if (event.phase != TickEvent.Phase.END) return;

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        Level level = player.level();

        if (!level.isRaining()) {
            return;
        }

        ItemStack mainHandStack = player.getMainHandItem();
        ItemStack offHandStack = player.getOffhandItem();

        boolean hasOpenUmbrella = false;

        if (mainHandStack.is(ModItems.UMBRELLA.get())) {
            hasOpenUmbrella = isUmbrellaOpen(mainHandStack);
        } else if (offHandStack.is(ModItems.UMBRELLA.get())) {
            hasOpenUmbrella = isUmbrellaOpen(offHandStack);
        }

        if (!hasOpenUmbrella) {
            return;
        }

        // 如果 MCA 不可用，跳过
        if (villagerEntityClass == null) {
            return;
        }

        long currentTime = level.getGameTime();

        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(PROTECTION_RANGE))) {
            // 检查是否是 MCA 村民
            if (villagerEntityClass.isInstance(entity) && entity instanceof LivingEntity villager && villager.isAlive()) {
                if (!isEntityInRain(level, villager)) {
                    continue;
                }

                double distance = player.distanceTo(villager);
                if (distance > PROTECTION_RANGE) {
                    continue;
                }

                UUID villagerUUID = villager.getUUID();
                long lastTime = lastHeartsAdd.getOrDefault(villagerUUID, 0L);

                if (currentTime - lastTime >= TICKS_PER_INTERVAL) {
                    // 使用反射调用 rewardHearts
                    try {
                        Object brain = getVillagerBrainMethod.invoke(villager);
                        if (brain != null && rewardHeartsMethod != null) {
                            rewardHeartsMethod.invoke(brain, player, HEARTS_PER_INTERVAL);
                            lastHeartsAdd.put(villagerUUID, currentTime);
                            MCARomanticExpansion.LOGGER.debug("Gave {} hearts to villager {} from umbrella",
                                    HEARTS_PER_INTERVAL, villagerUUID);
                        }
                    } catch (Exception e) {
                        MCARomanticExpansion.LOGGER.warn("Failed to reward hearts to villager: {}", e.getMessage());
                    }
                }
            }
        }
    }

    private static boolean isUmbrellaOpen(ItemStack stack) {
        float state = UmbrellaItem.getUmbrellaState(stack);
        return state >= 0.5F;
    }

    private static boolean isEntityInRain(Level level, LivingEntity entity) {
        if (!level.isRaining()) {
            return false;
        }

        if (entity.isInWater()) {
            return false;
        }

        return level.canSeeSky(BlockPos.containing(entity.getEyePosition()));
    }
}