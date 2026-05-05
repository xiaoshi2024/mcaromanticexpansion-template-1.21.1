package com.xiaoshi2022.mcaromanticexpansion;

import com.xiaoshi2022.mcaromanticexpansion.item.GiftBoxItem;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = MCARomanticExpansion.MODID, value = Dist.CLIENT)
public class MCARomanticExpansionClient {

    // MCARomanticExpansionClient.java
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.GIFT_BOX.get(),
                    ResourceLocation.fromNamespaceAndPath(MCARomanticExpansion.MODID, "variant"),
                    (stack, level, entity, seed) -> GiftBoxItem.getModelVariantValue(stack));
        });
    }
}