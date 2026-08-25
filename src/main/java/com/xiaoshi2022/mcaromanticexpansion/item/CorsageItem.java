package com.xiaoshi2022.mcaromanticexpansion.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class CorsageItem extends Item {

    private final CorsageColor color;

    public CorsageItem(Properties properties, CorsageColor color) {
        super(properties);
        this.color = color;
    }

    public CorsageColor getColor() {
        return color;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.mcaromanticexpansion.corsage"));
        tooltip.accept(Component.translatable("tooltip.mcaromanticexpansion.corsage." + color.getName()));
    }

    public enum CorsageColor {
        RED("red", 0xFF5555),
        PINK("pink", 0xFFAABB),
        WHITE("white", 0xFFFFFF);

        private final String name;
        private final int color;

        CorsageColor(String name, int color) {
            this.name = name;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public int getColor() {
            return color;
        }
    }
}