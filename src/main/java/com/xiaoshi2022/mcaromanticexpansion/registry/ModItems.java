package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.item.CorsageItem;
import com.xiaoshi2022.mcaromanticexpansion.item.GiftBoxItem;
import com.xiaoshi2022.mcaromanticexpansion.item.WeddingClothesItem;
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

    // 胸花
    public static final DeferredHolder<Item, CorsageItem> ROSE_BROOCH_RED = ITEMS.register("rose_brooch_red",
            () -> new CorsageItem(new Item.Properties().stacksTo(1), CorsageItem.CorsageColor.RED));

    public static final DeferredHolder<Item, CorsageItem> ROSE_BROOCH_PINK = ITEMS.register("rose_brooch_pink",
            () -> new CorsageItem(new Item.Properties().stacksTo(1), CorsageItem.CorsageColor.PINK));

    public static final DeferredHolder<Item, CorsageItem> ROSE_BROOCH_WHITE = ITEMS.register("rose_brooch_white",
            () -> new CorsageItem(new Item.Properties().stacksTo(1), CorsageItem.CorsageColor.WHITE));

    // 婚服（不需要颜色参数）
    public static final DeferredHolder<Item, WeddingClothesItem> CHINESE_WEDDING_MALE = ITEMS.register("chinese_wedding_male",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingType.CHINESE,
                    WeddingClothesItem.Gender.MALE));

    public static final DeferredHolder<Item, WeddingClothesItem> CHINESE_WEDDING_FEMALE = ITEMS.register("chinese_wedding_female",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingType.CHINESE,
                    WeddingClothesItem.Gender.FEMALE));

    public static final DeferredHolder<Item, WeddingClothesItem> WESTERN_WEDDING_MALE = ITEMS.register("western_wedding_male",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingType.WESTERN,
                    WeddingClothesItem.Gender.MALE));

    public static final DeferredHolder<Item, WeddingClothesItem> WESTERN_WEDDING_FEMALE = ITEMS.register("western_wedding_female",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingType.WESTERN,
                    WeddingClothesItem.Gender.FEMALE));

    // 注册物品组选项卡
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ROMANTIC_TAB = CREATIVE_TABS.register(
            "romantic_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mcaromanticexpansion"))
                    .icon(() -> new ItemStack(GIFT_BOX.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(GIFT_BOX.get());
                        output.accept(ROSE_BROOCH_RED.get());
                        output.accept(ROSE_BROOCH_PINK.get());
                        output.accept(ROSE_BROOCH_WHITE.get());
                        output.accept(CHINESE_WEDDING_MALE.get());
                        output.accept(CHINESE_WEDDING_FEMALE.get());
                        output.accept(WESTERN_WEDDING_MALE.get());
                        output.accept(WESTERN_WEDDING_FEMALE.get());
                    })
                    .build()
    );
}