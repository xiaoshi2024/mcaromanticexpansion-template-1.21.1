package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
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

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                Class.forName("top.theillusivec4.curios.api.SlotContext");
                curiosAvailable = true;
                MCARomanticExpansion.LOGGER.info("Curios mod detected, enabling ring slot integration");
            } catch (ClassNotFoundException e) {
                curiosAvailable = false;
                MCARomanticExpansion.LOGGER.info("Curios mod not detected, skipping ring slot integration");
            }
        });
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (curiosAvailable) {
                try {
                    registerCuriosRenderers();
                    MCARomanticExpansion.LOGGER.info("Curios renderers registered successfully");
                } catch (Exception e) {
                    MCARomanticExpansion.LOGGER.warn("Curios renderer registration failed: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * 使用反射注册渲染器 - 完全不依赖 Curios API 的硬编码
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerRenderer(String modId, String itemName, Class<?> rendererClass) {
        if (!curiosAvailable) {
            return;
        }

        try {
            // 检查物品是否存在
            var itemOptional = BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(modId, itemName));
            if (itemOptional.isEmpty()) {
                MCARomanticExpansion.LOGGER.warn("Item not found: {}:{}", modId, itemName);
                return;
            }

            Item item = itemOptional.get().value();

            // 使用反射获取 ICurioRenderer 接口
            Class<?> iCurioRendererClass = Class.forName("top.theillusivec4.curios.api.client.ICurioRenderer");

            // 检查渲染器类是否实现了 ICurioRenderer 接口
            if (!iCurioRendererClass.isAssignableFrom(rendererClass)) {
                MCARomanticExpansion.LOGGER.debug("Renderer {} does not implement ICurioRenderer, skipping", rendererClass.getSimpleName());
                return;
            }

            // 获取 register 方法
            Method registerMethod = iCurioRendererClass.getMethod("register", Item.class, Supplier.class);

            // 创建 Supplier，返回渲染器实例
            Supplier<Object> supplier = () -> {
                try {
                    return rendererClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    MCARomanticExpansion.LOGGER.warn("Failed to create renderer instance for {}: {}", itemName, e.getMessage());
                    return null;
                }
            };

            // 调用注册方法
            registerMethod.invoke(null, item, supplier);
            MCARomanticExpansion.LOGGER.info("Registered renderer for {}", itemName);

        } catch (ClassNotFoundException e) {
            // Curios API 不存在，静默忽略
            MCARomanticExpansion.LOGGER.debug("Curios API not available, skipping renderer registration for {}", itemName);
        } catch (NoSuchMethodException e) {
            MCARomanticExpansion.LOGGER.warn("ICurioRenderer.register method not found: {}", e.getMessage());
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to register renderer for {}: {}", itemName, e.getMessage());
        }
    }

    private static void registerCuriosRenderers() {
        // 注册戒指渲染器 (MCA 模组的戒指)
        registerRenderer("mca", "wedding_ring", RingCuriosRenderer.class);
        registerRenderer("mca", "wedding_ring_rg", RingCuriosRenderer.class);
        registerRenderer("mca", "engagement_ring", RingCuriosRenderer.class);
        registerRenderer("mca", "engagement_ring_rg", RingCuriosRenderer.class);

        // 注册胸花渲染器
        registerRenderer(MCARomanticExpansion.MODID, "rose_brooch_red", CorsageRenderer.class);
        registerRenderer(MCARomanticExpansion.MODID, "rose_brooch_pink", CorsageRenderer.class);
        registerRenderer(MCARomanticExpansion.MODID, "rose_brooch_white", CorsageRenderer.class);

        // 注册头饰渲染器
        registerRenderer(MCARomanticExpansion.MODID, "red_veil", HeadAdornmentRenderer.class);
        registerRenderer(MCARomanticExpansion.MODID, "golden_hairpin", HeadAdornmentRenderer.class);

        // 注册婚服渲染器
        registerWeddingClothesRenderers();
    }

    private static void registerWeddingClothesRenderers() {
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
            registerRenderer(MCARomanticExpansion.MODID, name, WeddingClothesRenderer.class);
        }
    }

    public static boolean isCuriosAvailable() {
        return curiosAvailable;
    }

    public static String getRingSlot() {
        return RING_SLOT;
    }

    public static void safeExecute(Runnable action) {
        if (curiosAvailable) {
            try {
                action.run();
            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.warn("Curios operation failed: {}", e.getMessage());
            }
        }
    }
}