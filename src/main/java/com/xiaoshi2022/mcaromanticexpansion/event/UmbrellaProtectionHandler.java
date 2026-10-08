package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UmbrellaProtectionHandler {
    private static final int PROTECTION_RANGE = 3;
    private static final int HEARTS_PER_INTERVAL = 1;
    private static final int TICKS_PER_INTERVAL = 200;
    private static final Map<UUID, Long> lastHeartsAdd = new HashMap<>();
    private static final Map<UUID, Long> lastDurabilityDamage = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Level level = player.level();

        if (!level.isRaining()) {
            return;
        }

        ItemStack mainHandStack = player.getMainHandItem();
        ItemStack offHandStack = player.getOffhandItem();

        ItemStack umbrellaStack = ItemStack.EMPTY;
        InteractionHand umbrellaHand = null;

        if (mainHandStack.is(ModItems.UMBRELLA.get()) && isUmbrellaOpen(mainHandStack)) {
            umbrellaStack = mainHandStack;
            umbrellaHand = InteractionHand.MAIN_HAND;
        } else if (offHandStack.is(ModItems.UMBRELLA.get()) && isUmbrellaOpen(offHandStack)) {
            umbrellaStack = offHandStack;
            umbrellaHand = InteractionHand.OFF_HAND;
        }

        if (umbrellaStack.isEmpty()) {
            return;
        }

        final InteractionHand hand = umbrellaHand;

        long currentTime = level.getGameTime();

        // 伞耐久消耗：下雨天手持打开的伞，每200 tick扣1点耐久（独立于村民）
        if (currentTime - lastDurabilityDamage.getOrDefault(player.getUUID(), 0L) >= TICKS_PER_INTERVAL) {
            int newDamage = umbrellaStack.getDamageValue() + 1;
            if (newDamage >= umbrellaStack.getMaxDamage()) {
                umbrellaStack.shrink(1);
            } else {
                umbrellaStack.setDamageValue(newDamage);
            }
            lastDurabilityDamage.put(player.getUUID(), currentTime);
        }

        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(PROTECTION_RANGE))) {
            if (entity instanceof VillagerEntityMCA villager && villager.isAlive()) {
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
                    villager.getVillagerBrain().rewardHearts(player, HEARTS_PER_INTERVAL);
                    lastHeartsAdd.put(villagerUUID, currentTime);
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
