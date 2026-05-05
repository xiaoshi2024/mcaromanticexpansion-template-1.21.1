package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.item.GiftBoxItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MCARomanticExpansion.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MCARomanticExpansion.MODID);

    // 注册默认礼盒（default 变种）
    public static final DeferredHolder<Item, GiftBoxItem> GIFT_BOX = ITEMS.register("gift_box",
            () -> new GiftBoxItem(new Item.Properties().stacksTo(1), "default"));


    // 注册物品组选项卡
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ROMANTIC_TAB = CREATIVE_TABS.register(
            "romantic_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mcaromanticexpansion"))
                    .icon(() -> new ItemStack(GIFT_BOX.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(GIFT_BOX.get());
                    })
                    .build()
    );
}