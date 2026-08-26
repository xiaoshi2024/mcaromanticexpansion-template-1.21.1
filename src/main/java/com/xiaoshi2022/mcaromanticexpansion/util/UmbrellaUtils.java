package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.content.block.UmbrellaStandBlockEntity;
import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class UmbrellaUtils {
    // 浪漫模式的遮雨范围 - 5x5 区域
    private static final int ROMANTIC_RANGE = 2;
    // 垂直覆盖高度
    private static final int VERTICAL_HEIGHT = 12;

    public static boolean isUnderUmbrella(Level level, BlockPos pos) {
        // 检查放置在地上的伞
        for (int x = -ROMANTIC_RANGE; x <= ROMANTIC_RANGE; ++x) {
            for (int y = 0; y <= VERTICAL_HEIGHT; ++y) {
                for (int z = -ROMANTIC_RANGE; z <= ROMANTIC_RANGE; ++z) {
                    BlockPos newPos = new BlockPos(pos.getX() + x, pos.getY() + y - 1, pos.getZ() + z);
                    // 使用平方距离判断，支持更大范围
                    if (level.getBlockEntity(newPos) instanceof UmbrellaStandBlockEntity blockEntity &&
                        blockEntity.hasStack() &&
                        newPos.distSqr(pos) <= ROMANTIC_RANGE * ROMANTIC_RANGE * 2) {
                        return isUmbrellaOpen(blockEntity.getTheItem());
                    }
                }
            }
        }

        // 检查手持伞的实体（玩家或生物）
        AABB box = new AABB(
                new Vec3(pos.getX() - ROMANTIC_RANGE - 1, pos.getY(), pos.getZ() - ROMANTIC_RANGE - 1),
                new Vec3(pos.getX() + ROMANTIC_RANGE + 1, pos.getY() + VERTICAL_HEIGHT, pos.getZ() + ROMANTIC_RANGE + 1)
        );

        for (Entity entity : level.getEntities(null, box)) {
            // 支持所有生物实体，不只是玩家
            if (entity instanceof LivingEntity living) {
                // 扩大距离判断范围
                double distanceSquared = living.getOnPos().distSqr(pos);
                double maxDistanceSquared = (ROMANTIC_RANGE + 1) * (ROMANTIC_RANGE + 1);

                if (distanceSquared <= maxDistanceSquared) {
                    ItemStack mainHandStack = living.getMainHandItem();
                    if (mainHandStack.is(ModItems.UMBRELLA.get()) && isUmbrellaOpen(mainHandStack)) {
                        return true;
                    }
                    ItemStack offHandStack = living.getOffhandItem();
                    if (offHandStack.is(ModItems.UMBRELLA.get()) && isUmbrellaOpen(offHandStack)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isUmbrellaOpen(ItemStack stack) {
        float state = UmbrellaItem.getUmbrellaState(stack);
        return state >= 0.5F;
    }
}