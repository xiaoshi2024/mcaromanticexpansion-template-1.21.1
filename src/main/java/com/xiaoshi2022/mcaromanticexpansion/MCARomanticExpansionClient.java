package com.xiaoshi2022.mcaromanticexpansion;

import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesModel;
import com.xiaoshi2022.mcaromanticexpansion.item.GiftBoxItem;
import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MCARomanticExpansion.MODID, value = Dist.CLIENT)
public class MCARomanticExpansionClient {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.GIFT_BOX.get(),
                    ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "variant"),
                    (stack, level, entity, seed) -> GiftBoxItem.getModelVariantValue(stack));

            ItemProperties.register(ModItems.UMBRELLA.get(),
                    ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "umbrella_state"),
                    (stack, level, entity, seed) -> UmbrellaItem.getUmbrellaState(stack));
        });

    }
    // 新增：注册模型层定义
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WeddingClothesModel.LAYER_LOCATION, WeddingClothesModel::createBodyLayer);
    }
}