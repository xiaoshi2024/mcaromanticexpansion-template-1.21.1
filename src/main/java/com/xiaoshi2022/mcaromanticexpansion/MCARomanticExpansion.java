package com.xiaoshi2022.mcaromanticexpansion;

import com.xiaoshi2022.mcaromanticexpansion.event.PlayerInteractionHandler;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MCARomanticExpansion.MODID)
public class MCARomanticExpansion {
    public static final String MODID = "mcaromanticexpansion";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MCARomanticExpansion(IEventBus modEventBus) {
        RomanceNetwork.registerPackets(modEventBus);
        NeoForge.EVENT_BUS.register(PlayerInteractionHandler.class);
    }

    public static ResourceLocation locate(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }
}
