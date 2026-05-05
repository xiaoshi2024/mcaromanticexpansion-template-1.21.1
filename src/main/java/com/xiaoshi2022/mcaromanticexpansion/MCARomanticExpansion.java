package com.xiaoshi2022.mcaromanticexpansion;

import com.xiaoshi2022.mcaromanticexpansion.command.BirthdayCommand;
import com.xiaoshi2022.mcaromanticexpansion.event.PlayerInteractionHandler;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

@Mod(MCARomanticExpansion.MODID)
public class MCARomanticExpansion {
    public static final String MODID = "mcaromanticexpansion";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MCARomanticExpansion(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);  // 新增：注册物品组
        RomanceNetwork.registerPackets(modEventBus);
        NeoForge.EVENT_BUS.register(PlayerInteractionHandler.class);
        NeoForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation locate(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        BirthdayCommand.register(event.getDispatcher());
    }
}
