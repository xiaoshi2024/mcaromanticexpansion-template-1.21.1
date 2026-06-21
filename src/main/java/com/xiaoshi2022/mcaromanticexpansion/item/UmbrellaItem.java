package com.xiaoshi2022.mcaromanticexpansion.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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

    public static float getUmbrellaState(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(STATE_TAG)) {
                return tag.getFloat(STATE_TAG);
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
        return State.fromValue(getUmbrellaState(stack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            State currentState = getState(stack);
            State nextState = currentState.next();
            setUmbrellaState(stack, nextState);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}