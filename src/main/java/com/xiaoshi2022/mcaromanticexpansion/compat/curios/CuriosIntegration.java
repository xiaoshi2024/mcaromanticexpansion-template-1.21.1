package com.xiaoshi2022.mcaromanticexpansion.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = MCARomanticExpansion.MODID)
public class CuriosIntegration {

    private static boolean curiosAvailable = false;
    private static final String RING_SLOT = "ring";

    // ========== 反射缓存 ==========
    // 缓存 Curios API 相关的方法
    private static Class<?> curiosRendererRegistryClass;
    private static Class<?> iCurioRendererClass;
    private static Method registerRendererMethod;
    private static Method getFromRegistryMethod;

    // 缓存 SlotContext 的 getEntity 方法
    private static final ConcurrentHashMap<Class<?>, Method> getEntityMethodCache = new ConcurrentHashMap<>();

    // 缓存 BuiltInRegistries.ITEM 的 get 方法
    private static Method registryGetMethod;

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                Class.forName("top.theillusivec4.curios.api.SlotContext");
                Class.forName("top.theillusivec4.curios.api.type.capability.ICurioItem");

                curiosAvailable = true;

                // 预先缓存反射方法
                cacheReflectionMethods();

                MCARomanticExpansion.LOGGER.info("Curios mod detected, enabling ring slot integration");

            } catch (ClassNotFoundException e) {
                curiosAvailable = false;
                MCARomanticExpansion.LOGGER.info("Curios mod not detected, skipping ring slot integration");
            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.warn("Failed to cache Curios reflection methods: {}", e.getMessage());
            }
        });
    }

    private static void cacheReflectionMethods() throws Exception {
        // 缓存 CuriosRendererRegistry 相关
        curiosRendererRegistryClass = Class.forName("top.theillusivec4.curios.api.client.CuriosRendererRegistry");
        iCurioRendererClass = Class.forName("top.theillusivec4.curios.api.client.ICurioRenderer");
        registerRendererMethod = curiosRendererRegistryClass.getMethod("register",
                Class.forName("net.minecraft.world.item.Item"),
                java.util.function.Supplier.class);

        // 缓存 BuiltInRegistries.ITEM 的 get 方法
        registryGetMethod = net.minecraft.core.registries.BuiltInRegistries.ITEM.getClass()
                .getMethod("get", ResourceLocation.class);
    }

    // 获取 SlotContext 的 getEntity 方法（带缓存）
    private static Method getGetEntityMethod(Class<?> slotContextClass) {
        return getEntityMethodCache.computeIfAbsent(slotContextClass, clazz -> {
            try {
                Method method = clazz.getMethod("entity");
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                MCARomanticExpansion.LOGGER.warn("Failed to find entity method for {}", clazz.getName());
                return null;
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
        // 戒指渲染器
        RingCuriosRenderer ringCuriosRenderer = new RingCuriosRenderer();
        Object ringRendererProxy = java.lang.reflect.Proxy.newProxyInstance(
                CuriosIntegration.class.getClassLoader(),
                new Class[]{iCurioRendererClass},
                new RingRenderInvocationHandler(ringCuriosRenderer)
        );

        registerRingRendererCached(ringRendererProxy, "mca:wedding_ring");
        registerRingRendererCached(ringRendererProxy, "mca:wedding_ring_rg");
        registerRingRendererCached(ringRendererProxy, "mca:engagement_ring");
        registerRingRendererCached(ringRendererProxy, "mca:engagement_ring_rg");

        // 胸花渲染器
        CorsageRenderer corsageRenderer = new CorsageRenderer();
        Object corsageProxy = createRenderProxy(corsageRenderer, "corsage");
        registerRingRendererCached(corsageProxy, "mcaromanticexpansion:rose_brooch_red");
        registerRingRendererCached(corsageProxy, "mcaromanticexpansion:rose_brooch_pink");
        registerRingRendererCached(corsageProxy, "mcaromanticexpansion:rose_brooch_white");

        // 婚服渲染器（需要特殊处理，因为需要获取 entity）
        WeddingClothesRenderer weddingRenderer = new WeddingClothesRenderer();
        Object weddingProxy = createWeddingRenderProxy(weddingRenderer);
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:chinese_wedding_male");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:chinese_wedding_female");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:western_wedding_male");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:western_wedding_female");

        // 新增14套
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:east_african_wedding_male");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:east_african_wedding_female");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:west_african_wedding_male");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:west_african_wedding_female");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:ancient_greek_wedding_male");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:ancient_greek_wedding_female");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:japanese_wedding_male");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:japanese_wedding_female");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:german_wedding_male");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:german_wedding_female");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:scottish_wedding_male");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:scottish_wedding_female");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:slavic_wedding_male");
        registerRingRendererCached(weddingProxy, "mcaromanticexpansion:slavic_wedding_female");

        // 头饰渲染器
        HeadAdornmentRenderer headRenderer = new HeadAdornmentRenderer();
        Object headProxy = createRenderProxy(headRenderer, "head");
        registerRingRendererCached(headProxy, "mcaromanticexpansion:red_veil");
        registerRingRendererCached(headProxy, "mcaromanticexpansion:golden_hairpin");
    }

    // 创建简单的渲染代理（不需要获取 entity）
    private static Object createRenderProxy(Object renderer, String name) {
        return java.lang.reflect.Proxy.newProxyInstance(
                CuriosIntegration.class.getClassLoader(),
                new Class[]{iCurioRendererClass},
                (proxy, method, args) -> {
                    if ("render".equals(method.getName()) && args != null && args.length >= 12) {
                        // 使用缓存的方法调用
                        invokeRender(renderer, args);
                    }
                    return null;
                }
        );
    }

    // 创建婚服渲染代理（需要获取 entity）
    private static Object createWeddingRenderProxy(WeddingClothesRenderer renderer) {
        return java.lang.reflect.Proxy.newProxyInstance(
                CuriosIntegration.class.getClassLoader(),
                new Class[]{iCurioRendererClass},
                (proxy, method, args) -> {
                    if ("render".equals(method.getName()) && args != null && args.length >= 12) {
                        ItemStack stack = (ItemStack) args[0];
                        Object slotContext = args[1];
                        PoseStack poseStack = (PoseStack) args[2];
                        @SuppressWarnings("unchecked")
                        RenderLayerParent<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> renderLayerParent =
                                (RenderLayerParent<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>>) args[3];
                        MultiBufferSource buffer = (MultiBufferSource) args[4];
                        int light = (int) args[5];
                        float limbSwing = (float) args[6];
                        float limbSwingAmount = (float) args[7];
                        float partialTicks = (float) args[8];
                        float ageInTicks = (float) args[9];
                        float netHeadYaw = (float) args[10];
                        float headPitch = (float) args[11];

                        // 使用缓存的方法获取 entity
                        LivingEntity entity = getEntityFromSlotContext(slotContext);
                        if (entity == null) {
                            return null;
                        }

                        if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel) {
                            @SuppressWarnings("unchecked")
                            HumanoidModel<LivingEntity> playerModel = (HumanoidModel<LivingEntity>) humanoidModel;
                            renderer.render(stack, entity, playerModel, poseStack, buffer, light,
                                    limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
                        }
                    }
                    return null;
                }
        );
    }

    // 从 SlotContext 获取实体（使用缓存的方法）
    private static LivingEntity getEntityFromSlotContext(Object slotContext) {
        if (slotContext == null) return null;

        Method getEntityMethod = getGetEntityMethod(slotContext.getClass());
        if (getEntityMethod == null) return null;

        try {
            return (LivingEntity) getEntityMethod.invoke(slotContext);
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to get entity from SlotContext: {}", e.getMessage());
            return null;
        }
    }

    // 通用的渲染调用方法
    private static void invokeRender(Object renderer, Object[] args) {
        // 根据 renderer 类型调用对应的方法
        if (renderer instanceof CorsageRenderer) {
            ((CorsageRenderer) renderer).render(
                    (ItemStack) args[0], args[1], (PoseStack) args[2],
                    (RenderLayerParent<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>>) args[3],
                    (MultiBufferSource) args[4], (int) args[5], (float) args[6], (float) args[7],
                    (float) args[8], (float) args[9], (float) args[10], (float) args[11]);
        } else if (renderer instanceof HeadAdornmentRenderer) {
            ((HeadAdornmentRenderer) renderer).render(
                    (ItemStack) args[0], args[1], (PoseStack) args[2],
                    (RenderLayerParent<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>>) args[3],
                    (MultiBufferSource) args[4], (int) args[5], (float) args[6], (float) args[7],
                    (float) args[8], (float) args[9], (float) args[10], (float) args[11]);
        }
    }

    // 使用缓存的反射方法注册渲染器
    private static void registerRingRendererCached(Object renderer, String itemId) {
        try {
            Class<?> itemClass = Class.forName("net.minecraft.world.item.Item");
            Object item = registryGetMethod.invoke(net.minecraft.core.registries.BuiltInRegistries.ITEM,
                    ResourceLocation.parse(itemId));

            if (item != null) {
                java.util.function.Supplier<?> supplier = () -> renderer;
                registerRendererMethod.invoke(null, item, supplier);
                MCARomanticExpansion.LOGGER.info("Registered ring renderer for {}", itemId);
            }
        } catch (Exception e) {
            MCARomanticExpansion.LOGGER.warn("Failed to register ring renderer for {}: {}", itemId, e.getMessage());
        }
    }

    // 独立的 InvocationHandler 类（戒指渲染）
    private static class RingRenderInvocationHandler implements java.lang.reflect.InvocationHandler {
        private final RingCuriosRenderer renderer;

        RingRenderInvocationHandler(RingCuriosRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            if ("render".equals(method.getName()) && args != null && args.length >= 12) {
                renderer.render(
                        (ItemStack) args[0], args[1], (PoseStack) args[2],
                        (RenderLayerParent<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>>) args[3],
                        (MultiBufferSource) args[4], (int) args[5], (float) args[6], (float) args[7],
                        (float) args[8], (float) args[9], (float) args[10], (float) args[11]);
            }
            return null;
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