package com.xiaoshi2022.mcaromanticexpansion.registry;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.xiaoshi2022.mcaromanticexpansion.registry.ModBlocks.UMBRELLA_STAND;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MCARomanticExpansion.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MCARomanticExpansion.MODID);

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

    // ========== 原有4套婚服（使用旧枚举，完全保留） ==========
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

    // ========== 新增14套婚服（使用新枚举） ==========
    public static final DeferredHolder<Item, WeddingClothesItem> EAST_AFRICAN_WEDDING_MALE = ITEMS.register("east_african_wedding_male",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.EAST_AFRICAN,
                    WeddingClothesItem.Gender.MALE));

    public static final DeferredHolder<Item, WeddingClothesItem> EAST_AFRICAN_WEDDING_FEMALE = ITEMS.register("east_african_wedding_female",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.EAST_AFRICAN,
                    WeddingClothesItem.Gender.FEMALE));

    public static final DeferredHolder<Item, WeddingClothesItem> WEST_AFRICAN_WEDDING_MALE = ITEMS.register("west_african_wedding_male",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.WEST_AFRICAN,
                    WeddingClothesItem.Gender.MALE));

    public static final DeferredHolder<Item, WeddingClothesItem> WEST_AFRICAN_WEDDING_FEMALE = ITEMS.register("west_african_wedding_female",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.WEST_AFRICAN,
                    WeddingClothesItem.Gender.FEMALE));

    public static final DeferredHolder<Item, WeddingClothesItem> ANCIENT_GREEK_WEDDING_MALE = ITEMS.register("ancient_greek_wedding_male",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.ANCIENT_GREEK,
                    WeddingClothesItem.Gender.MALE));

    public static final DeferredHolder<Item, WeddingClothesItem> ANCIENT_GREEK_WEDDING_FEMALE = ITEMS.register("ancient_greek_wedding_female",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.ANCIENT_GREEK,
                    WeddingClothesItem.Gender.FEMALE));

    public static final DeferredHolder<Item, WeddingClothesItem> JAPANESE_WEDDING_MALE = ITEMS.register("japanese_wedding_male",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.JAPANESE,
                    WeddingClothesItem.Gender.MALE));

    public static final DeferredHolder<Item, WeddingClothesItem> JAPANESE_WEDDING_FEMALE = ITEMS.register("japanese_wedding_female",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.JAPANESE,
                    WeddingClothesItem.Gender.FEMALE));

    public static final DeferredHolder<Item, WeddingClothesItem> GERMAN_WEDDING_MALE = ITEMS.register("german_wedding_male",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.GERMAN,
                    WeddingClothesItem.Gender.MALE));

    public static final DeferredHolder<Item, WeddingClothesItem> GERMAN_WEDDING_FEMALE = ITEMS.register("german_wedding_female",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.GERMAN,
                    WeddingClothesItem.Gender.FEMALE));

    public static final DeferredHolder<Item, WeddingClothesItem> SCOTTISH_WEDDING_MALE = ITEMS.register("scottish_wedding_male",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.SCOTTISH,
                    WeddingClothesItem.Gender.MALE));

    public static final DeferredHolder<Item, WeddingClothesItem> SCOTTISH_WEDDING_FEMALE = ITEMS.register("scottish_wedding_female",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.SCOTTISH,
                    WeddingClothesItem.Gender.FEMALE));

    public static final DeferredHolder<Item, WeddingClothesItem> SLAVIC_WEDDING_MALE = ITEMS.register("slavic_wedding_male",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.SLAVIC,
                    WeddingClothesItem.Gender.MALE));

    public static final DeferredHolder<Item, WeddingClothesItem> SLAVIC_WEDDING_FEMALE = ITEMS.register("slavic_wedding_female",
            () -> new WeddingClothesItem(new Item.Properties().stacksTo(1),
                    WeddingClothesItem.WeddingCulture.SLAVIC,
                    WeddingClothesItem.Gender.FEMALE));

    // 头饰
    public static final DeferredHolder<Item, RedVeilItem> RED_VEIL = ITEMS.register("red_veil",
            () -> new RedVeilItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, HairPinItem> GOLDEN_HAIRPIN = ITEMS.register("golden_hairpin",
            () -> new HairPinItem(new Item.Properties().stacksTo(1)));

    // 伞
    public static final DeferredHolder<Item, UmbrellaItem> UMBRELLA = ITEMS.register("umbrella",
            () -> new UmbrellaItem(new Item.Properties().stacksTo(1)));

    // 情书
    public static final DeferredHolder<Item, LoveLetterItem> LOVE_LETTER = ITEMS.register("love_letter",
            () -> new LoveLetterItem(new Item.Properties().stacksTo(16)));

    // 注册物品组选项卡
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ROMANTIC_TAB = CREATIVE_TABS.register(
            "romantic_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mcaromanticexpansion"))
                    .icon(() -> new ItemStack(ROSE_BROOCH_RED.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(GIFT_BOX.get());
                        output.accept(ROSE_BROOCH_RED.get());
                        output.accept(ROSE_BROOCH_PINK.get());
                        output.accept(ROSE_BROOCH_WHITE.get());
                        //婚服
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
                        output.accept(UMBRELLA_STAND.get());
                        output.accept(LOVE_LETTER.get());
                    })
                    .build()
    );
}