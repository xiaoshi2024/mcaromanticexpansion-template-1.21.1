package com.xiaoshi2022.mcaromanticexpansion.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    public static float getUmbrellaState(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(STATE_TAG)) {
            return tag.getFloat(STATE_TAG);
        }
        return State.CLOSED.getValue();
    }

    public static void setUmbrellaState(ItemStack stack, State state) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putFloat(STATE_TAG, state.getValue());
        stack.setTag(tag);
    }

    public static State getState(ItemStack stack) {
        return State.fromValue(getUmbrellaState(stack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 检查是否正在看向其他玩家
        if (isLookingAtPlayer(player)) {
            // 确保伞是打开状态（只在服务端修改）
            if (!level.isClientSide()) {
                setUmbrellaState(stack, State.FULL_OPEN);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // 不看向其他玩家时切换伞状态（只在服务端修改）
        if (!level.isClientSide()) {
            State currentState = getState(stack);
            State nextState = currentState.next();
            setUmbrellaState(stack, nextState);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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