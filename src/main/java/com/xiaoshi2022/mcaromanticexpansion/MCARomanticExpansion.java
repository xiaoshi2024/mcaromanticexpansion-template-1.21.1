package com.xiaoshi2022.mcaromanticexpansion;

import com.xiaoshi2022.mcaromanticexpansion.advancement.CriterionTriggerRegister;
import com.xiaoshi2022.mcaromanticexpansion.command.BirthdayCommand;
import com.xiaoshi2022.mcaromanticexpansion.command.PregnancyCommand;
import com.xiaoshi2022.mcaromanticexpansion.event.PlayerInteractionHandler;
import com.xiaoshi2022.mcaromanticexpansion.event.PregnancyAttemptHandler;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import com.xiaoshi2022.mcaromanticexpansion.util.PregnancyManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

@Mod(MCARomanticExpansion.MODID)
public class MCARomanticExpansion {
    public static final String MODID = "mcaromanticexpansion";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MCARomanticExpansion(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);
        RomanceNetwork.registerPackets(modEventBus);
        CriterionTriggerRegister.TRIGGER_TYPES.register(modEventBus);
        NeoForge.EVENT_BUS.register(PlayerInteractionHandler.class);
        NeoForge.EVENT_BUS.register(PregnancyAttemptHandler.class);
        NeoForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation locate(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        BirthdayCommand.register(event.getDispatcher());
        PregnancyCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PregnancyManager.onPlayerDeath(player);
        }
    }
}
