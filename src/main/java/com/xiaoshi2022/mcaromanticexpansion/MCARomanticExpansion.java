package com.xiaoshi2022.mcaromanticexpansion;

import com.mojang.logging.LogUtils;
import com.xiaoshi2022.mcaromanticexpansion.advancement.CriterionTriggerRegister;
import com.xiaoshi2022.mcaromanticexpansion.command.BirthdayCommand;
import com.xiaoshi2022.mcaromanticexpansion.command.MarriageConfigCommand;
import com.xiaoshi2022.mcaromanticexpansion.command.PregnancyCommand;
import com.xiaoshi2022.mcaromanticexpansion.config.HUDConfig;
import com.xiaoshi2022.mcaromanticexpansion.event.*;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlockEntities;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlocks;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModParticles;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import com.xiaoshi2022.mcaromanticexpansion.util.CarryRuntime;
import com.xiaoshi2022.mcaromanticexpansion.util.PregnancyManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

@Mod(MCARomanticExpansion.MODID)
public class MCARomanticExpansion {
    public static final String MODID = "mcaromanticexpansion";

    public static final Logger LOGGER = LogUtils.getLogger();

    public MCARomanticExpansion(IEventBus modEventBus, ModContainer modContainer) {
        // 注册配置文件
        modContainer.registerConfig(ModConfig.Type.CLIENT, HUDConfig.SPEC, "mcaromanticexpansion-client.toml");
        HUDConfig.markLoaded();

        // ========== 重要：注册顺序 ==========
        // 1. 先注册方块（Block 是 BlockItem 的基础）
        ModBlocks.register(modEventBus);

        // 2. 再注册物品（BlockItem 需要 Block 实例）
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);

        // 3. 然后注册 BlockEntity（需要 Block 和 Item 已注册）
        ModBlockEntities.register(modEventBus);

        // 4. 注册粒子
        ModParticles.PARTICLES.register(modEventBus);

        // 5. 注册网络包
        RomanceNetwork.registerPackets(modEventBus);

        // 6. 注册触发器类型
        CriterionTriggerRegister.TRIGGER_TYPES.register(modEventBus);

        // 7. 注册事件处理器（这些是 @SubscribeEvent 监听器，使用 NeoForge.EVENT_BUS）
        NeoForge.EVENT_BUS.register(PlayerInteractionHandler.class);
        NeoForge.EVENT_BUS.register(PregnancyAttemptHandler.class);
        NeoForge.EVENT_BUS.register(UmbrellaProtectionHandler.class);
        NeoForge.EVENT_BUS.register(RomanticTickHandler.class);
        NeoForge.EVENT_BUS.register(AffectionDecayHandler.class);
        NeoForge.EVENT_BUS.register(RomanticAdvancementListener.class);
        NeoForge.EVENT_BUS.register(this);
    }

    // 使用 Identifier 替代 ResourceLocation
    public static Identifier locate(String id) {
        return Identifier.fromNamespaceAndPath(MODID, id);
    }

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        BirthdayCommand.register(event.getDispatcher());
        PregnancyCommand.register(event.getDispatcher());
        MarriageConfigCommand.register(event.getDispatcher());
    }

    // 修复 serverLevel() 调用
    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PregnancyManager.onPlayerDeath(player);
            // 修复：使用 level() 替代 serverLevel()
            CarryRuntime.stopCarryFor(player.level(), player.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PregnancyManager.saveToPersistentData(serverPlayer);
            // 修复：使用 level() 替代 serverLevel()
            CarryRuntime.onPlayerLoggedOut(serverPlayer.level(), serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (CarryRuntime.isCarrier(serverPlayer.getUUID())
                    || CarryRuntime.isCarried(serverPlayer.getUUID())) {
                // 修复：使用 level() 替代 serverLevel()
                CarryRuntime.stopCarryFor(serverPlayer.level(), serverPlayer.getUUID());
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