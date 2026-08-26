package com.xiaoshi2022.mcaromanticexpansion.item;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
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

    // ========== 状态方法 ==========

    public static float getUmbrellaState(ItemStack stack) {
        if (stack.is(ModItems.UMBRELLA_CLOSED.get())) return State.CLOSED.getValue();
        if (stack.is(ModItems.UMBRELLA_HALF.get())) return State.HALF_OPEN.getValue();
        if (stack.is(ModItems.UMBRELLA_OPEN.get())) return State.FULL_OPEN.getValue();

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
        if (stack.is(ModItems.UMBRELLA_CLOSED.get())) return State.CLOSED;
        if (stack.is(ModItems.UMBRELLA_HALF.get())) return State.HALF_OPEN;
        if (stack.is(ModItems.UMBRELLA_OPEN.get())) return State.FULL_OPEN;
        return State.fromValue(getUmbrellaState(stack));
    }

    public static boolean isUmbrella(ItemStack stack) {
        return stack.is(ModItems.UMBRELLA.get()) ||
                stack.is(ModItems.UMBRELLA_CLOSED.get()) ||
                stack.is(ModItems.UMBRELLA_HALF.get()) ||
                stack.is(ModItems.UMBRELLA_OPEN.get());
    }

    public static ItemStack getStackForState(State state) {
        return switch (state) {
            case CLOSED -> new ItemStack(ModItems.UMBRELLA_CLOSED.get());
            case HALF_OPEN -> new ItemStack(ModItems.UMBRELLA_HALF.get());
            case FULL_OPEN -> new ItemStack(ModItems.UMBRELLA_OPEN.get());
        };
    }

    // ========== use 方法 - 只处理切换状态，不处理玩家交互 ==========
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ===== 只处理空手切换状态，不处理玩家交互 =====
        // 玩家交互由 PlayerInteractionHandler.onPlayerInteractEntity 处理
        // 不在这里检查是否看向玩家，避免与 PlayerInteractionHandler 冲突

        if (!level.isClientSide()) {
            State currentState = getState(stack);
            State nextState = currentState.next();

            ItemStack newStack = getStackForState(nextState);
            newStack.setCount(1);
            player.setItemInHand(hand, newStack);

            MCARomanticExpansion.LOGGER.debug("Umbrella state: {} -> {} for {}",
                    currentState, nextState, player.getName().getString());
        }
        return InteractionResult.SUCCESS;
    }
}