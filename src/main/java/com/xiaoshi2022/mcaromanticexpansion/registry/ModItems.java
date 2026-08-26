package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    // 关键修复：使用 DeferredRegister.Items
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MCARomanticExpansion.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MCARomanticExpansion.MODID);

    // ========== 方块物品 - 修复 ==========
    public static final DeferredHolder<Item, BlockItem> UMBRELLA_STAND_ITEM =
            ITEMS.registerItem("umbrella_stand",
                    props -> new BlockItem(ModBlocks.UMBRELLA_STAND.get(), props),
                    p -> p);

    // 注册默认礼盒（default 变种）- 修复
    public static final DeferredHolder<Item, GiftBoxItem> GIFT_BOX =
            ITEMS.registerItem("gift_box",
                    props -> new GiftBoxItem(props.stacksTo(1), "default"),
                    p -> p);

    // ========== 普通物品 ==========
    // 关键修复：MC 26.2 / NeoForge 26.2.0.59 的 Item 构造函数会调用 properties.itemIdOrThrow()，
    // 若 Properties.id 未设置会抛出 NullPointerException: Item id not set。
    // 必须使用 registerItem(name, func, UnaryOperator) —— 它会在构造物品前
    // 调用 properties.setId(ResourceKey.create(Registries.ITEM, key)) 设置 id。
    // 而 ITEMS.register(name, Supplier) 会丢弃 key，不设置 id，导致崩溃。
    // 胸花
    public static final DeferredHolder<Item, CorsageItem> ROSE_BROOCH_RED =
            ITEMS.registerItem("rose_brooch_red",
                    props -> new CorsageItem(props, CorsageItem.CorsageColor.RED),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, CorsageItem> ROSE_BROOCH_PINK =
            ITEMS.registerItem("rose_brooch_pink",
                    props -> new CorsageItem(props, CorsageItem.CorsageColor.PINK),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, CorsageItem> ROSE_BROOCH_WHITE =
            ITEMS.registerItem("rose_brooch_white",
                    props -> new CorsageItem(props, CorsageItem.CorsageColor.WHITE),
                    p -> p.stacksTo(1));

    // 婚服
    public static final DeferredHolder<Item, WeddingClothesItem> CHINESE_WEDDING_MALE =
            ITEMS.registerItem("chinese_wedding_male",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingType.CHINESE,
                            WeddingClothesItem.Gender.MALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> CHINESE_WEDDING_FEMALE =
            ITEMS.registerItem("chinese_wedding_female",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingType.CHINESE,
                            WeddingClothesItem.Gender.FEMALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> WESTERN_WEDDING_MALE =
            ITEMS.registerItem("western_wedding_male",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingType.WESTERN,
                            WeddingClothesItem.Gender.MALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> WESTERN_WEDDING_FEMALE =
            ITEMS.registerItem("western_wedding_female",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingType.WESTERN,
                            WeddingClothesItem.Gender.FEMALE),
                    p -> p.stacksTo(1));

    // 新增14套婚服
    public static final DeferredHolder<Item, WeddingClothesItem> EAST_AFRICAN_WEDDING_MALE =
            ITEMS.registerItem("east_african_wedding_male",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.EAST_AFRICAN,
                            WeddingClothesItem.Gender.MALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> EAST_AFRICAN_WEDDING_FEMALE =
            ITEMS.registerItem("east_african_wedding_female",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.EAST_AFRICAN,
                            WeddingClothesItem.Gender.FEMALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> WEST_AFRICAN_WEDDING_MALE =
            ITEMS.registerItem("west_african_wedding_male",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.WEST_AFRICAN,
                            WeddingClothesItem.Gender.MALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> WEST_AFRICAN_WEDDING_FEMALE =
            ITEMS.registerItem("west_african_wedding_female",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.WEST_AFRICAN,
                            WeddingClothesItem.Gender.FEMALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> ANCIENT_GREEK_WEDDING_MALE =
            ITEMS.registerItem("ancient_greek_wedding_male",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.ANCIENT_GREEK,
                            WeddingClothesItem.Gender.MALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> ANCIENT_GREEK_WEDDING_FEMALE =
            ITEMS.registerItem("ancient_greek_wedding_female",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.ANCIENT_GREEK,
                            WeddingClothesItem.Gender.FEMALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> JAPANESE_WEDDING_MALE =
            ITEMS.registerItem("japanese_wedding_male",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.JAPANESE,
                            WeddingClothesItem.Gender.MALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> JAPANESE_WEDDING_FEMALE =
            ITEMS.registerItem("japanese_wedding_female",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.JAPANESE,
                            WeddingClothesItem.Gender.FEMALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> GERMAN_WEDDING_MALE =
            ITEMS.registerItem("german_wedding_male",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.GERMAN,
                            WeddingClothesItem.Gender.MALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> GERMAN_WEDDING_FEMALE =
            ITEMS.registerItem("german_wedding_female",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.GERMAN,
                            WeddingClothesItem.Gender.FEMALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> SCOTTISH_WEDDING_MALE =
            ITEMS.registerItem("scottish_wedding_male",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.SCOTTISH,
                            WeddingClothesItem.Gender.MALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> SCOTTISH_WEDDING_FEMALE =
            ITEMS.registerItem("scottish_wedding_female",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.SCOTTISH,
                            WeddingClothesItem.Gender.FEMALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> SLAVIC_WEDDING_MALE =
            ITEMS.registerItem("slavic_wedding_male",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.SLAVIC,
                            WeddingClothesItem.Gender.MALE),
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, WeddingClothesItem> SLAVIC_WEDDING_FEMALE =
            ITEMS.registerItem("slavic_wedding_female",
                    props -> new WeddingClothesItem(props,
                            WeddingClothesItem.WeddingCulture.SLAVIC,
                            WeddingClothesItem.Gender.FEMALE),
                    p -> p.stacksTo(1));

    // 头饰
    public static final DeferredHolder<Item, RedVeilItem> RED_VEIL =
            ITEMS.registerItem("red_veil",
                    RedVeilItem::new,
                    p -> p.stacksTo(1));

    public static final DeferredHolder<Item, HairPinItem> GOLDEN_HAIRPIN =
            ITEMS.registerItem("golden_hairpin",
                    HairPinItem::new,
                    p -> p.stacksTo(1));

    // ========== 伞 - 四个状态 ==========
// 基础伞（默认关闭状态）
    public static final DeferredHolder<Item, UmbrellaItem> UMBRELLA =
            ITEMS.registerItem("umbrella",
                    UmbrellaItem::new,
                    p -> p.stacksTo(1));

    // 关闭状态的伞（独立物品）
    public static final DeferredHolder<Item, UmbrellaItem> UMBRELLA_CLOSED =
            ITEMS.registerItem("umbrella_closed",
                    UmbrellaItem::new,
                    p -> p.stacksTo(1));

    // 半开状态的伞（独立物品）
    public static final DeferredHolder<Item, UmbrellaItem> UMBRELLA_HALF =
            ITEMS.registerItem("umbrella_half",
                    UmbrellaItem::new,
                    p -> p.stacksTo(1));

    // 全开状态的伞（独立物品）
    public static final DeferredHolder<Item, UmbrellaItem> UMBRELLA_OPEN =
            ITEMS.registerItem("umbrella_open",
                    UmbrellaItem::new,
                    p -> p.stacksTo(1));


    // 情书
    public static final DeferredHolder<Item, LoveLetterItem> LOVE_LETTER =
            ITEMS.registerItem("love_letter",
                    LoveLetterItem::new,
                    p -> p.stacksTo(16));

    // 注册物品组选项卡
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ROMANTIC_TAB =
            CREATIVE_TABS.register("romantic_tab",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.mcaromanticexpansion"))
                            .icon(() -> new ItemStack(ROSE_BROOCH_RED.get()))
                            .displayItems((parameters, output) -> {
                                output.accept(UMBRELLA_STAND_ITEM.get());
                                output.accept(GIFT_BOX.get());
                                output.accept(ROSE_BROOCH_RED.get());
                                output.accept(ROSE_BROOCH_PINK.get());
                                output.accept(ROSE_BROOCH_WHITE.get());
                                // 婚服
                                output.accept(CHINESE_WEDDING_MALE.get());
                                output.accept(CHINESE_WEDDING_FEMALE.get());
                                output.accept(WESTERN_WEDDING_MALE.get());
                                output.accept(WESTERN_WEDDING_FEMALE.get());
                                // 新增14套
                                output.accept(EAST_AFRICAN_WEDDING_MALE.get());
                                output.accept(EAST_AFRICAN_WEDDING_FEMALE.get());
                                output.accept(WEST_AFRICAN_WEDDING_MALE.get());
                                output.accept(WEST_AFRICAN_WEDDING_FEMALE.get());
                                output.accept(ANCIENT_GREEK_WEDDING_MALE.get());
                                output.accept(ANCIENT_GREEK_WEDDING_FEMALE.get());
                                output.accept(JAPANESE_WEDDING_MALE.get());
                                output.accept(JAPANESE_WEDDING_FEMALE.get());
                                output.accept(GERMAN_WEDDING_MALE.get());
                                output.accept(GERMAN_WEDDING_FEMALE.get());
                                output.accept(SCOTTISH_WEDDING_MALE.get());
                                output.accept(SCOTTISH_WEDDING_FEMALE.get());
                                output.accept(SLAVIC_WEDDING_MALE.get());
                                output.accept(SLAVIC_WEDDING_FEMALE.get());
                                output.accept(RED_VEIL.get());
                                output.accept(GOLDEN_HAIRPIN.get());
                                output.accept(UMBRELLA.get());
                                output.accept(LOVE_LETTER.get());
                            })
                            .build()
            );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_TABS.register(eventBus);
    }
}