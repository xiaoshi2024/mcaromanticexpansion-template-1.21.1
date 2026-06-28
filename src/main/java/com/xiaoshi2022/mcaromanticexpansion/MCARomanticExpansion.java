package com.xiaoshi2022.mcaromanticexpansion;

import com.mojang.logging.LogUtils;
import com.xiaoshi2022.mcaromanticexpansion.advancement.CriterionTriggerRegister;
import com.xiaoshi2022.mcaromanticexpansion.command.BirthdayCommand;
import com.xiaoshi2022.mcaromanticexpansion.command.MarriageConfigCommand;
import com.xiaoshi2022.mcaromanticexpansion.command.PregnancyCommand;
import com.xiaoshi2022.mcaromanticexpansion.command.UpdateCommand;
import com.xiaoshi2022.mcaromanticexpansion.config.HUDConfig;
import com.xiaoshi2022.mcaromanticexpansion.event.*;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlockEntities;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlocks;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModParticles;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import com.xiaoshi2022.mcaromanticexpansion.util.ModInfo;
import com.xiaoshi2022.mcaromanticexpansion.util.PregnancyManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

@Mod(MCARomanticExpansion.MODID)
public class MCARomanticExpansion {
    public static final String MODID = "mcaromanticexpansion";

    public static final String MOD_NAME = ModInfo.getModName();  // 从配置文件读取
    public static final String MOD_VERSION = ModInfo.getModVersion();  // 从配置文件读取

    public static final Logger LOGGER = LogUtils.getLogger();

    public MCARomanticExpansion(IEventBus modEventBus, ModContainer modContainer) {
        // 注册配置文件
        modContainer.registerConfig(ModConfig.Type.CLIENT, HUDConfig.SPEC, "mcaromanticexpansion-client.toml");
        HUDConfig.markLoaded();
// 打印版本信息
        LOGGER.info("Loading {} version {}", MOD_NAME, MOD_VERSION);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);  // ← 确保这行存在！
        RomanceNetwork.registerPackets(modEventBus);
        CriterionTriggerRegister.TRIGGER_TYPES.register(modEventBus);
        NeoForge.EVENT_BUS.register(PlayerInteractionHandler.class);
        NeoForge.EVENT_BUS.register(PregnancyAttemptHandler.class);
        NeoForge.EVENT_BUS.register(UmbrellaProtectionHandler.class);
        NeoForge.EVENT_BUS.register(RomanticTickHandler.class);
        NeoForge.EVENT_BUS.register(AffectionDecayHandler.class);
        NeoForge.EVENT_BUS.register(RomanticAdvancementListener.class);
        NeoForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation locate(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        BirthdayCommand.register(event.getDispatcher());
        PregnancyCommand.register(event.getDispatcher());
        MarriageConfigCommand.register(event.getDispatcher());
        UpdateCommand.register(event.getDispatcher()); // 新增
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PregnancyManager.onPlayerDeath(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PregnancyManager.loadFromPersistentData(serverPlayer);
            AffectionManager.initializeAffectionData(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PregnancyManager.saveToPersistentData(serverPlayer);
        }
    }
}