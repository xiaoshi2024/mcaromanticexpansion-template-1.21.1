package com.xiaoshi2022.mcaromanticexpansion.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class WeddingClothesItem extends Item {

    private final WeddingType type;
    private final Gender gender;

    public enum WeddingType {
        CHINESE("chinese"),
        WESTERN("western");

        private final String name;

        WeddingType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum Gender {
        MALE("male"),
        FEMALE("female");

        private final String name;

        Gender(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public WeddingClothesItem(Properties properties, WeddingType type, Gender gender) {
        super(properties);
        this.type = type;
        this.gender = gender;
    }

    public WeddingType getType() {
        return type;
    }

    public Gender getGender() {
        return gender;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mcaromanticexpansion.wedding_clothes.line1",
                Component.translatable("tooltip.mcaromanticexpansion.wedding_clothes.type." + type.getName()),
                Component.translatable("tooltip.mcaromanticexpansion.wedding_clothes.gender." + gender.getName())));
        tooltip.add(Component.translatable("tooltip.mcaromanticexpansion.wearable.curio"));
    }
}