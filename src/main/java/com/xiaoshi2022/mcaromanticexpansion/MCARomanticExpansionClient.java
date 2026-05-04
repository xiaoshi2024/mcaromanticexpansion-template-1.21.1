package com.xiaoshi2022.mcaromanticexpansion;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = MCARomanticExpansion.MODID, value = Dist.CLIENT)
public class MCARomanticExpansionClient {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        MCARomanticExpansion.LOGGER.info("MCA Romantic Expansion Client Setup");
    }
}
