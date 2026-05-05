package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = MCARomanticExpansion.MODID)
public class CuriosIntegration {

    private static boolean curiosAvailable = false;
    private static final String RING_SLOT = "ring";

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                Class.forName("top.theillusivec4.curios.api.SlotContext");
                Class.forName("top.theillusivec4.curios.api.type.capability.ICurioItem");

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
                    MCARomanticExpansion.LOGGER.info("Ring curios renderer registered successfully");
                } catch (Exception e) {
                    MCARomanticExpansion.LOGGER.warn("Ring curios renderer registration failed: {}", e.getMessage());
                }
            }
        });
    }

    private static void registerCuriosRenderers() throws Exception {
        Class<?> curiosRendererRegistryClass = Class.forName("top.theillusivec4.curios.api.client.CuriosRendererRegistry");

        RingCuriosRenderer ringCuriosRenderer = new RingCuriosRenderer();

        Class<?> iCurioRendererClass = Class.forName("top.theillusivec4.curios.api.client.ICurioRenderer");

        // 创建一个简单的 ICurioRenderer 实例，而不是使用代理
        Object renderer = java.lang.reflect.Proxy.newProxyInstance(
                CuriosIntegration.class.getClassLoader(),
                new Class[]{iCurioRendererClass},
                new RingRenderInvocationHandler(ringCuriosRenderer)
        );

        registerRingRenderer(curiosRendererRegistryClass, renderer, "mca:wedding_ring");
        registerRingRenderer(curiosRendererRegistryClass, renderer, "mca:wedding_ring_rg");
        registerRingRenderer(curiosRendererRegistryClass, renderer, "mca:engagement_ring");
        registerRingRenderer(curiosRendererRegistryClass, renderer, "mca:engagement_ring_rg");
    }

    // 独立的 InvocationHandler 类
    private static class RingRenderInvocationHandler implements java.lang.reflect.InvocationHandler {
        private final RingCuriosRenderer renderer;

        RingRenderInvocationHandler(RingCuriosRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
            if ("render".equals(method.getName()) && args != null && args.length >= 12) {
                renderer.render(
                        (net.minecraft.world.item.ItemStack) args[0],
                        (top.theillusivec4.curios.api.SlotContext) args[1],  // 强制转换 SlotContext
                        (com.mojang.blaze3d.vertex.PoseStack) args[2],
                        (net.minecraft.client.renderer.entity.RenderLayerParent<LivingEntity, EntityModel<LivingEntity>>) args[3],
                        (net.minecraft.client.renderer.MultiBufferSource) args[4],
                        (int) args[5],
                        (float) args[6],
                        (float) args[7],
                        (float) args[8],
                        (float) args[9],
                        (float) args[10],
                        (float) args[11]
                );
            }
            return null;
        }
    }

    private static void registerRingRenderer(Class<?> registryClass, Object renderer, String ringId) {
        try {
            Class<?> itemClass = Class.forName("net.minecraft.world.item.Item");
            Object registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
            Object item = registry.getClass().getMethod("get", ResourceLocation.class)
                    .invoke(registry, ResourceLocation.parse(ringId));

            if (item != null) {
                java.util.function.Supplier<?> supplier = () -> renderer;
                registryClass.getMethod("register", itemClass, java.util.function.Supplier.class)
                        .invoke(null, item, supplier);
                MCARomanticExpansion.LOGGER.info("Registered ring renderer for {}", ringId);
            }
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to register ring renderer for {}: {}", ringId, e.getMessage());
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