package com.xiaoshi2022.mcaromanticexpansion;

import com.mojang.logging.LogUtils;
import com.xiaoshi2022.mcaromanticexpansion.advancement.ModCriteriaTriggers;
import com.xiaoshi2022.mcaromanticexpansion.command.BirthdayCommand;
import com.xiaoshi2022.mcaromanticexpansion.command.MarriageConfigCommand;
import com.xiaoshi2022.mcaromanticexpansion.command.PregnancyCommand;
import com.xiaoshi2022.mcaromanticexpansion.config.HUDConfig;
import com.xiaoshi2022.mcaromanticexpansion.event.*;
import com.xiaoshi2022.mcaromanticexpansion.network.ModNetwork;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlockEntities;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlocks;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModParticles;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import com.xiaoshi2022.mcaromanticexpansion.util.CarryRuntime;
import com.xiaoshi2022.mcaromanticexpansion.util.PregnancyManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MCARomanticExpansion.MODID)
public class MCARomanticExpansion {
    public static final String MODID = "mcaromanticexpansion";

//    public static final String MOD_NAME = ModInfo.getModName();
//    public static final String MOD_VERSION = ModInfo.getModVersion();

    public static final Logger LOGGER = LogUtils.getLogger();

    // 【修复】添加 @SuppressWarnings 忽略过时警告
    @SuppressWarnings("removal")
    public MCARomanticExpansion() {
        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HUDConfig.SPEC, "mcaromanticexpansion-client.toml");
        HUDConfig.markLoaded();

//        LOGGER.info("Loading {} version {}", MOD_NAME, MOD_VERSION);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册所有内容
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(PlayerInteractionHandler.class);
        MinecraftForge.EVENT_BUS.register(PregnancyAttemptHandler.class);
        MinecraftForge.EVENT_BUS.register(UmbrellaProtectionHandler.class);
        MinecraftForge.EVENT_BUS.register(RomanticTickHandler.class);
        MinecraftForge.EVENT_BUS.register(AffectionDecayHandler.class);
        MinecraftForge.EVENT_BUS.register(RomanticAdvancementListener.class);
        MinecraftForge.EVENT_BUS.register(this);

        // ✅ 确保自定义触发器被加载
        ModCriteriaTriggers.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
        });
    }

    public static ResourceLocation locate(String id) {
        return new ResourceLocation(MODID, id);
    }

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        BirthdayCommand.register(event.getDispatcher());
        PregnancyCommand.register(event.getDispatcher());
        MarriageConfigCommand.register(event.getDispatcher());
//        UpdateCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PregnancyManager.onPlayerDeath(player);
            CarryRuntime.stopCarryFor(player.serverLevel(), player.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PregnancyManager.loadFromPersistentData(serverPlayer);
            AffectionManager.initializeAffectionData(serverPlayer);
            CarryRuntime.syncCarryStatesTo(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PregnancyManager.saveToPersistentData(serverPlayer);
            // ✅ 新增：清理内存缓存
            PregnancyManager.clearMemoryCache(serverPlayer.getUUID());
            CarryRuntime.onPlayerLoggedOut(serverPlayer.serverLevel(), serverPlayer.getUUID());
        }
    }

    // 修复 CurseForge 反馈：玩家在公主抱状态下跳入维度传送门（下界/末地等）会导致
    // 骑乘关系跨维度损坏，进而崩溃玩家且无法重新加入，并可能阻止服务器正常关闭。
    // 在实体穿越维度之前触发，提前干净地解除公主抱，避免损坏状态产生。
    @SubscribeEvent
    public void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (CarryRuntime.isCarrier(serverPlayer.getUUID())
                    || CarryRuntime.isCarried(serverPlayer.getUUID())) {
                CarryRuntime.stopCarryFor(serverPlayer.serverLevel(), serverPlayer.getUUID());
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        event.getServer().getAllLevels().forEach(level -> {
            for (java.util.UUID pid : CarryRuntime.snapshotCarriers()) {
                CarryRuntime.stopCarry(pid, level);
            }
        });
    }
}