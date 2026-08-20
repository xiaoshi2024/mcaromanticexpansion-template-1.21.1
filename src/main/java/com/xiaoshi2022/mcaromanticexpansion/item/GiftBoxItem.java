package com.xiaoshi2022.mcaromanticexpansion.item;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.PlayerBirthdayData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.time.LocalDate;

public class GiftBoxItem extends Item {

    // NBT 键名常量
    private static final String KEY_HAS_GIFT = "HasGift";
    private static final String KEY_GIFT_ITEM_ID = "GiftItemId";
    private static final String KEY_GIFT_ITEM_COUNT = "GiftItemCount";
    private static final String KEY_GIFT_VARIANT = "GiftVariant";
    private static final String KEY_IS_BIRTHDAY_GIFT = "IsBirthdayGift";
    private static final String KEY_GIFT_FROM = "GiftFrom";
    private static final String KEY_GIFT_MESSAGE = "GiftMessage";
    private static final String KEY_GIFT_ITEM_TAG = "GiftItemTag";
    private static final String KEY_CUSTOM_NAME = "CustomName";

    private final String defaultVariant;

    public GiftBoxItem(Properties properties) {
        this(properties, "default");
    }

    public GiftBoxItem(Properties properties, String defaultVariant) {
        super(properties);
        this.defaultVariant = defaultVariant;
    }

    public String getDefaultVariant() {
        return defaultVariant;
    }

    public enum GiftBoxVariant implements StringRepresentable {
        DEFAULT("default"),
        VALENTINE("valentine"),
        CHRISTMAS("christmas"),
        HALLOWEEN("halloween"),
        BIRTHDAY("birthday");

        private final String name;

        GiftBoxVariant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public String getName() {
            return this.name;
        }
    }

    /**
     * 从物品栈中获取变种
     */
    public static String getVariantFromItem(ItemStack stack) {
        if (stack.getItem() instanceof GiftBoxItem giftBox) {
            CompoundTag tag = getTag(stack);
            if (tag.contains(KEY_GIFT_VARIANT)) {
                return tag.getString(KEY_GIFT_VARIANT);
            }
            return giftBox.getDefaultVariant();
        }
        return "default";
    }

    /**
     * 获取物品栈应该使用的模型变种值（用于 ItemProperty）
     */
    public static float getModelVariantValue(ItemStack stack) {
        String variant = getVariantFromItem(stack);

        switch (variant) {
            case "valentine": return 1.0f;
            case "christmas": return 2.0f;
            case "halloween": return 3.0f;
            case "birthday": return 4.0f;
            default: return 0.0f;
        }
    }

    // ========== NBT 操作基础方法 ==========

    private static CompoundTag getTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }

    private static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // ========== 礼物内容操作 ==========

    public static boolean hasGift(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.getBoolean(KEY_HAS_GIFT);
    }

    public static void saveGiftItem(ItemStack giftBox, ItemStack giftItem) {
        // 检查是否已经有礼物
        if (hasGift(giftBox)) {
            MCARomanticExpansion.LOGGER.warn("Cannot save gift to box that already has a gift!");
            return;
        }

        // 防止将礼盒放入自己
        if (giftItem.getItem() instanceof GiftBoxItem) {
            MCARomanticExpansion.LOGGER.warn("Cannot put gift box inside another gift box!");
            return;
        }

        CompoundTag tag = getTag(giftBox);
        tag.putString(KEY_GIFT_ITEM_ID, BuiltInRegistries.ITEM.getKey(giftItem.getItem()).toString());
        tag.putInt(KEY_GIFT_ITEM_COUNT, giftItem.getCount());
        tag.putBoolean(KEY_HAS_GIFT, true);

        // 保存自定义名称
        if (giftItem.has(DataComponents.CUSTOM_NAME)) {
            Component customName = giftItem.get(DataComponents.CUSTOM_NAME);
            String json = Component.Serializer.toJson(customName, RegistryAccess.EMPTY);
            tag.putString(KEY_CUSTOM_NAME, json);
        }

        setTag(giftBox, tag);
    }

    public static ItemStack loadGiftItem(ItemStack giftBox) {
        CompoundTag tag = getTag(giftBox);
        if (tag.getBoolean(KEY_HAS_GIFT) && tag.contains(KEY_GIFT_ITEM_ID)) {
            String itemId = tag.getString(KEY_GIFT_ITEM_ID);
            int count = tag.getInt(KEY_GIFT_ITEM_COUNT);

            // 跳过礼盒自己
            if (itemId.equals("mcaromanticexpansion:gift_box")) {
                MCARomanticExpansion.LOGGER.warn("Cannot load gift box from itself!");
                clearGift(giftBox);
                return ItemStack.EMPTY;
            }

            // 创建基础物品
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            if (item == net.minecraft.world.item.Items.AIR) {
                MCARomanticExpansion.LOGGER.warn("Unknown item: {}", itemId);
                clearGift(giftBox);
                return ItemStack.EMPTY;
            }

            ItemStack result = new ItemStack(item, count);

            // 恢复自定义名称
            if (tag.contains(KEY_CUSTOM_NAME)) {
                try {
                    String json = tag.getString(KEY_CUSTOM_NAME);
                    Component customName = Component.Serializer.fromJson(json, RegistryAccess.EMPTY);
                    if (customName != null) {
                        result.set(DataComponents.CUSTOM_NAME, customName);
                    }
                } catch (Exception e) {
                    MCARomanticExpansion.LOGGER.warn("Failed to parse custom name: {}", e.getMessage());
                }
            }

            return result;
        }
        return ItemStack.EMPTY;
    }

    public static void clearGift(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        tag.remove(KEY_GIFT_ITEM_ID);
        tag.remove(KEY_GIFT_ITEM_COUNT);
        tag.remove(KEY_GIFT_ITEM_TAG);
        tag.remove(KEY_CUSTOM_NAME);
        tag.putBoolean(KEY_HAS_GIFT, false);
        setTag(stack, tag);
    }

    // ========== 礼盒变体操作 ==========

    public static GiftBoxVariant getGlobalVariant() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        if (month == 2 && day == 14) {
            return GiftBoxVariant.VALENTINE;
        }
        if (month == 12 && day == 25) {
            return GiftBoxVariant.CHRISTMAS;
        }
        if (month == 10 && day == 31) {
            return GiftBoxVariant.HALLOWEEN;
        }
        return GiftBoxVariant.DEFAULT;
    }

    public static GiftBoxVariant getVariantForReceiver(Player receiver) {
        if (receiver != null && PlayerBirthdayData.isBirthdayToday(receiver)) {
            return GiftBoxVariant.BIRTHDAY;
        }
        return getGlobalVariant();
    }

    public static void setBirthdayGift(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        tag.putString(KEY_GIFT_VARIANT, GiftBoxVariant.BIRTHDAY.getName());
        tag.putBoolean(KEY_IS_BIRTHDAY_GIFT, true);
        setTag(stack, tag);

        stack.set(DataComponents.CUSTOM_NAME,
                Component.translatable("item.mcaromanticexpansion.gift_box.birthday"));
    }

    public static boolean isBirthdayGift(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.getBoolean(KEY_IS_BIRTHDAY_GIFT);
    }

    public static GiftBoxVariant getStoredVariant(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (tag.contains(KEY_GIFT_VARIANT)) {
            String variantName = tag.getString(KEY_GIFT_VARIANT);
            for (GiftBoxVariant variant : GiftBoxVariant.values()) {
                if (variant.getName().equals(variantName)) {
                    return variant;
                }
            }
        }
        return null;
    }

    public static void setVariant(ItemStack stack, GiftBoxVariant variant) {
        CompoundTag tag = getTag(stack);
        tag.putString(KEY_GIFT_VARIANT, variant.getName());
        if (variant == GiftBoxVariant.BIRTHDAY) {
            tag.putBoolean(KEY_IS_BIRTHDAY_GIFT, true);
        }
        setTag(stack, tag);

        if (variant != GiftBoxVariant.DEFAULT) {
            stack.set(DataComponents.CUSTOM_NAME,
                    Component.translatable("item.mcaromanticexpansion.gift_box." + variant.getName()));
        } else {
            stack.remove(DataComponents.CUSTOM_NAME);
        }
    }

    public static void setGiftFrom(ItemStack stack, Player giver) {
        CompoundTag tag = getTag(stack);
        tag.putString(KEY_GIFT_FROM, giver.getName().getString());
        setTag(stack, tag);
    }

    public static String getGiftFrom(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.contains(KEY_GIFT_FROM) ? tag.getString(KEY_GIFT_FROM) : null;
    }

    public static void setGiftMessage(ItemStack stack, String message) {
        CompoundTag tag = getTag(stack);
        tag.putString(KEY_GIFT_MESSAGE, message);
        setTag(stack, tag);
    }

    public static String getGiftMessage(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.contains(KEY_GIFT_MESSAGE) ? tag.getString(KEY_GIFT_MESSAGE) : null;
    }

    // ========== 礼盒打开特效 ==========

    public static void openBirthdayGift(ServerPlayer player, ItemStack giftBox) {
        ItemStack giftItem = loadGiftItem(giftBox);
        if (!giftItem.isEmpty()) {
            boolean added = player.getInventory().add(giftItem);
            if (!added) {
                player.drop(giftItem, false);
                player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.inventory.full.gift"));
            } else {
                player.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.gift_box.birthday_opened", giftItem.getDisplayName().getString()));
            }

            clearGift(giftBox);

            player.playSound(net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
            player.playSound(net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.FIREWORK,
                        player.getX(), player.getY() + 1, player.getZ(),
                        30, 0.8, 0.8, 0.8, 0.1
                );
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.HEART,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        15, 0.5, 0.5, 0.5, 0
                );
            }
        }
    }

    // ========== 物品显示 ==========

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasGift(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        String variant = getVariantFromItem(stack);

        if (!variant.equals("default")) {
            return Component.translatable("item.mcaromanticexpansion.gift_box." + variant);
        }

        GiftBoxVariant globalVariant = getGlobalVariant();
        if (globalVariant != GiftBoxVariant.DEFAULT) {
            return Component.translatable("item.mcaromanticexpansion.gift_box." + globalVariant.getName());
        }

        return Component.translatable("item.mcaromanticexpansion.gift_box");
    }

    public static Component getNameForReceiver(ItemStack stack, Player receiver) {
        GiftBoxVariant variant = getVariantForReceiver(receiver);
        String variantName = variant.getName();

        if (!variantName.equals("default")) {
            return Component.translatable("item.mcaromanticexpansion.gift_box." + variantName);
        }
        return Component.translatable("item.mcaromanticexpansion.gift_box");
    }

    public static void appendGiftInfo(ItemStack stack, java.util.List<Component> tooltip) {
        String from = getGiftFrom(stack);
        String message = getGiftMessage(stack);

        if (from != null) {
            tooltip.add(Component.translatable("tooltip.mcaromanticexpansion.gift_box.from", from));
        }
        if (message != null && !message.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.mcaromanticexpansion.gift_box.message", message));
        }
        if (isBirthdayGift(stack)) {
            tooltip.add(Component.translatable("tooltip.mcaromanticexpansion.gift_box.birthday"));
        }
    }
}