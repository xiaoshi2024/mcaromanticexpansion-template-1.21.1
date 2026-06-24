package com.xiaoshi2022.mcaromanticexpansion;

import com.xiaoshi2022.mcaromanticexpansion.client.AffectionHUD;
import com.xiaoshi2022.mcaromanticexpansion.client.ClientEventHandler;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.HUDConfigScreen;
import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesModel;
import com.xiaoshi2022.mcaromanticexpansion.client.renderer.UmbrellaStandRenderer;
import com.xiaoshi2022.mcaromanticexpansion.config.HUDConfig;
import com.xiaoshi2022.mcaromanticexpansion.item.GiftBoxItem;
import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlockEntities;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlocks;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(modid = MCARomanticExpansion.MODID, value = Dist.CLIENT)
public class MCARomanticExpansionClient {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // 注册配置界面
        ModContainer modContainer = ModList.get().getModContainerById(MCARomanticExpansion.MODID)
                .orElseThrow(() -> new IllegalStateException("Mod container not found"));

        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (modContainer1, parent) -> new HUDConfigScreen(parent)
        );

        event.enqueueWork(() -> {
            // 物品属性注册
            ItemProperties.register(ModItems.GIFT_BOX.get(),
                    ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "variant"),
                    (stack, level, entity, seed) -> GiftBoxItem.getModelVariantValue(stack));

            ItemProperties.register(ModItems.UMBRELLA.get(),
                    ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "umbrella_state"),
                    (stack, level, entity, seed) -> UmbrellaItem.getUmbrellaState(stack));

            // HUD 初始化
            AffectionHUD.init();
            HUDConfig.applyConfig();  // ★★★ 只在这里应用配置 ★★★
            ClientEventHandler.init();
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WeddingClothesModel.LAYER_LOCATION, WeddingClothesModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.UMBRELLA_STAND_BLOCK_ENTITY.get(), UmbrellaStandRenderer::new);
    }
}