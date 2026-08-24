package com.xiaoshi2022.mcaromanticexpansion.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class WeddingClothesItem extends Item {

    // 保留原有枚举（向后兼容）
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

    // 新增文化枚举（包含所有文化）
    public enum WeddingCulture {
        CHINESE("chinese"),
        WESTERN("western"),
        EAST_AFRICAN("east_african"),
        WEST_AFRICAN("west_african"),
        ANCIENT_GREEK("ancient_greek"),
        JAPANESE("japanese"),
        GERMAN("german"),
        SCOTTISH("scottish"),
        SLAVIC("slavic");

        private final String name;

        WeddingCulture(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        // 获取翻译键
        public String getTranslationKey() {
            return "culture.mcaromanticexpansion." + name;
        }

        // 从 WeddingType 转换
        public static WeddingCulture fromType(WeddingType type) {
            return type == WeddingType.CHINESE ? CHINESE : WESTERN;
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

        // 获取翻译键
        public String getTranslationKey() {
            return "gender.mcaromanticexpansion." + name;
        }
    }

    // 双重存储：兼容旧代码
    private final WeddingType type;      // 旧枚举（仅 CHINESE/WESTERN）
    private final WeddingCulture culture; // 新枚举（所有文化）
    private final Gender gender;

    // 旧构造函数（保留，用于 CHINESE/WESTERN）
    public WeddingClothesItem(Properties properties, WeddingType type, Gender gender) {
        super(properties);
        this.type = type;
        this.culture = WeddingCulture.fromType(type);
        this.gender = gender;
    }

    // 新构造函数（用于所有文化）
    public WeddingClothesItem(Properties properties, WeddingCulture culture, Gender gender) {
        super(properties);
        this.type = null; // 如果是新文化，旧枚举为 null
        this.culture = culture;
        this.gender = gender;
    }

    // 旧 getter（保持兼容）
    public WeddingType getType() {
        // 如果 culture 是 CHINESE 或 WESTERN，返回对应的枚举
        if (culture == WeddingCulture.CHINESE) return WeddingType.CHINESE;
        if (culture == WeddingCulture.WESTERN) return WeddingType.WESTERN;
        return null; // 新文化没有对应的 WeddingType
    }

    // 新 getter
    public WeddingCulture getCulture() {
        return culture;
    }

    public Gender getGender() {
        return gender;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        // 使用翻译键显示文化和性别
        Component cultureDisplay = Component.translatable(culture.getTranslationKey());
        Component genderDisplay = Component.translatable(gender.getTranslationKey());

        tooltip.add(Component.translatable("tooltip.mcaromanticexpansion.wedding_clothes.line1",
                cultureDisplay, genderDisplay));
        tooltip.add(Component.translatable("tooltip.mcaromanticexpansion.wearable.curio"));
    }
}
