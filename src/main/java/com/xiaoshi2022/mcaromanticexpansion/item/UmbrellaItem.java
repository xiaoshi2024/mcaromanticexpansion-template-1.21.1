package com.xiaoshi2022.mcaromanticexpansion.item;

import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class UmbrellaItem extends Item {
    public static final String STATE_TAG = "UmbrellaState";

    public enum State {
        CLOSED(0.0F),
        HALF_OPEN(0.5F),
        FULL_OPEN(1.0F);

        private final float value;

        State(float value) {
            this.value = value;
        }

        public float getValue() {
            return value;
        }

        public State next() {
            return switch (this) {
                case CLOSED -> HALF_OPEN;
                case HALF_OPEN -> FULL_OPEN;
                case FULL_OPEN -> CLOSED;
            };
        }

        public static State fromValue(float value) {
            if (value < 0.25F) return CLOSED;
            if (value < 0.75F) return HALF_OPEN;
            return FULL_OPEN;
        }
    }

    public UmbrellaItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // ========== 原有方法（保持不变） ==========

    public static float getUmbrellaState(ItemStack stack) {
        // 新增：如果物品是独立的伞状态物品，直接返回对应值
        if (stack.is(ModItems.UMBRELLA_CLOSED.get())) return State.CLOSED.getValue();
        if (stack.is(ModItems.UMBRELLA_HALF.get())) return State.HALF_OPEN.getValue();
        if (stack.is(ModItems.UMBRELLA_OPEN.get())) return State.FULL_OPEN.getValue();

        // 原有的 NBT 读取逻辑
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(STATE_TAG)) {
                return tag.getFloat(STATE_TAG).orElse(State.CLOSED.getValue());
            }
        }
        return State.CLOSED.getValue();
    }

    public static void setUmbrellaState(ItemStack stack, State state) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(STATE_TAG, state.getValue());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static State getState(ItemStack stack) {
        // 新增：如果物品是独立的伞状态物品，直接返回对应状态
        if (stack.is(ModItems.UMBRELLA_CLOSED.get())) return State.CLOSED;
        if (stack.is(ModItems.UMBRELLA_HALF.get())) return State.HALF_OPEN;
        if (stack.is(ModItems.UMBRELLA_OPEN.get())) return State.FULL_OPEN;

        // 原有的 fromValue 逻辑
        return State.fromValue(getUmbrellaState(stack));
    }

    // ========== 新增方法 ==========

    /**
     * 检查是否是伞（任意状态）
     */
    public static boolean isUmbrella(ItemStack stack) {
        return stack.is(ModItems.UMBRELLA.get()) ||
                stack.is(ModItems.UMBRELLA_CLOSED.get()) ||
                stack.is(ModItems.UMBRELLA_HALF.get()) ||
                stack.is(ModItems.UMBRELLA_OPEN.get());
    }

    /**
     * 根据状态获取对应的物品实例
     */
    public static ItemStack getStackForState(State state) {
        return switch (state) {
            case CLOSED -> new ItemStack(ModItems.UMBRELLA_CLOSED.get());
            case HALF_OPEN -> new ItemStack(ModItems.UMBRELLA_HALF.get());
            case FULL_OPEN -> new ItemStack(ModItems.UMBRELLA_OPEN.get());
        };
    }

    // ========== 原有的 use 方法（修改为支持物品切换） ==========
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 检查是否正在看向其他玩家
        if (isLookingAtPlayer(player)) {
            if (!level.isClientSide()) {
                // 直接切换到全开独立物品
                ItemStack newStack = getStackForState(State.FULL_OPEN);
                newStack.setCount(1);
                player.setItemInHand(hand, newStack);
            }
            return InteractionResult.SUCCESS;
        }

        // 切换伞状态（只在服务端修改）
        if (!level.isClientSide()) {
            State currentState = getState(stack);
            State nextState = currentState.next();

            // 所有情况都切换到独立物品实例
            ItemStack newStack = getStackForState(nextState);
            newStack.setCount(1);
            player.setItemInHand(hand, newStack);

//            System.out.println("Umbrella state: " + currentState + " -> " + nextState);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 检查玩家是否正在看向其他玩家（距离足够近，用于共伞检测）
     */
    private boolean isLookingAtPlayer(Player player) {
        double maxDistance = 8.0;
        var start = player.getEyePosition(1.0F);
        var look = player.getLookAngle();

        var aabb = player.getBoundingBox().inflate(maxDistance);
        var entities = player.level().getEntities(player, aabb);

        for (var entity : entities) {
            if (entity instanceof Player otherPlayer && otherPlayer != player && otherPlayer.isAlive()) {
                var toEntity = otherPlayer.getEyePosition(1.0F).subtract(start).normalize();
                double dot = toEntity.dot(look);
                double distance = player.distanceTo(otherPlayer);
                if (dot > 0.85 && distance <= maxDistance) {
                    return true;
                }
            }
        }
        return false;
    }
}