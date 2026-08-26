package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.lang.reflect.Method;
import java.util.function.Supplier;

@EventBusSubscriber(modid = MCARomanticExpansion.MODID)
public class CuriosIntegration {

    private static boolean curiosAvailable = false;
    private static final String RING_SLOT = "ring";

    // 反射缓存（仅客户端使用）
    private static Class<?> iCurioRendererClass;
    private static Method registerMethod;

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                // 只在 Common 阶段检查 Curios API 是否存在（服务端可用的类）
                Class.forName("top.theillusivec4.curios.api.CuriosApi");
                curiosAvailable = true;
                MCARomanticExpansion.LOGGER.info("Curios mod detected (soft dependency)");
            } catch (ClassNotFoundException e) {
                curiosAvailable = false;
                MCARomanticExpansion.LOGGER.info("Curios mod not detected, skipping integration");
            } catch (Exception e) {
                curiosAvailable = false;
                MCARomanticExpansion.LOGGER.warn("Failed to detect Curios: {}", e.getMessage());
            }
        });
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (!curiosAvailable) {
                return;
            }

            try {
                // 在客户端安全地加载客户端专用类
                iCurioRendererClass = Class.forName("top.theillusivec4.curios.api.client.ICurioRenderer");
                registerMethod = iCurioRendererClass.getMethod("register",
                        Item.class,
                        Supplier.class);

                registerRenderers();
                MCARomanticExpansion.LOGGER.info("Curios renderers registered successfully (soft dependency)");
            } catch (ClassNotFoundException e) {
                MCARomanticExpansion.LOGGER.warn("Curios client API not found, renderers disabled");
            } catch (NoSuchMethodException e) {
                MCARomanticExpansion.LOGGER.warn("Curios API version mismatch: {}", e.getMessage());
            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.warn("Failed to register Curios renderers: {}", e.getMessage());
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerRenderers() {
        // 创建渲染器工厂
        Supplier<Object> corsageSupplier = () -> new CorsageRenderer();
        Supplier<Object> ringSupplier = () -> new RingCuriosRenderer();
        Supplier<Object> headSupplier = () -> new HeadAdornmentRenderer();
        Supplier<Object> weddingSupplier = () -> new WeddingClothesRenderer();

        // 注册所有物品...
        registerItem("mca", "wedding_ring", ringSupplier);
        registerItem("mca", "wedding_ring_rg", ringSupplier);
        registerItem("mca", "engagement_ring", ringSupplier);
        registerItem("mca", "engagement_ring_rg", ringSupplier);

        registerItem(MCARomanticExpansion.MODID, "rose_brooch_red", corsageSupplier);
        registerItem(MCARomanticExpansion.MODID, "rose_brooch_pink", corsageSupplier);
        registerItem(MCARomanticExpansion.MODID, "rose_brooch_white", corsageSupplier);

        registerItem(MCARomanticExpansion.MODID, "red_veil", headSupplier);
        registerItem(MCARomanticExpansion.MODID, "golden_hairpin", headSupplier);

        String[] weddingClothes = {
                "chinese_wedding_male", "chinese_wedding_female",
                "western_wedding_male", "western_wedding_female",
                "east_african_wedding_male", "east_african_wedding_female",
                "west_african_wedding_male", "west_african_wedding_female",
                "ancient_greek_wedding_male", "ancient_greek_wedding_female",
                "japanese_wedding_male", "japanese_wedding_female",
                "german_wedding_male", "german_wedding_female",
                "scottish_wedding_male", "scottish_wedding_female",
                "slavic_wedding_male", "slavic_wedding_female"
        };
        for (String name : weddingClothes) {
            registerItem(MCARomanticExpansion.MODID, name, weddingSupplier);
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerItem(String modId, String itemName, Supplier<Object> rendererSupplier) {
        try {
            var optional = BuiltInRegistries.ITEM.get(
                    Identifier.fromNamespaceAndPath(modId, itemName)
            );

            if (optional.isEmpty()) {
                MCARomanticExpansion.LOGGER.debug("Item not found: {}:{}", modId, itemName);
                return;
            }

            Holder.Reference<Item> holder = optional.get();
            Item item = holder.value();  // ← 关键：提取真正的 Item 对象

            // 现在传入的是 Item 对象，类型匹配
            registerMethod.invoke(null, item, rendererSupplier);

            MCARomanticExpansion.LOGGER.debug("Registered Curios renderer for {}:{}", modId, itemName);

        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to register renderer for {}:{}: {}",
                    modId, itemName, e.getMessage());
            e.printStackTrace();  // 临时加个堆栈打印以便调试
        }
    }

    public static boolean isCuriosAvailable() {
        return curiosAvailable;
    }

    public static String getRingSlot() {
        return RING_SLOT;
    }
}