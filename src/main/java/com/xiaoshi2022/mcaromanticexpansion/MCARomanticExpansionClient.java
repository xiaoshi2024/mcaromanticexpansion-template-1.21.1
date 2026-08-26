package com.xiaoshi2022.mcaromanticexpansion;

import com.xiaoshi2022.mcaromanticexpansion.client.AffectionHUD;
import com.xiaoshi2022.mcaromanticexpansion.client.CarryClientState;
import com.xiaoshi2022.mcaromanticexpansion.client.CarryKeyBindings;
import com.xiaoshi2022.mcaromanticexpansion.client.ClientEventHandler;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.HUDConfigScreen;
import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesModel;
import com.xiaoshi2022.mcaromanticexpansion.client.particle.RainbowParticle;
import com.xiaoshi2022.mcaromanticexpansion.client.renderer.UmbrellaStandRenderer;
import com.xiaoshi2022.mcaromanticexpansion.config.HUDConfig;
import com.xiaoshi2022.mcaromanticexpansion.network.CarryRequestPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.CarryStopPacket;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlockEntities;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

@EventBusSubscriber(modid = MCARomanticExpansion.MODID, value = Dist.CLIENT)
public class MCARomanticExpansionClient {

    private static boolean wasCarryKeyDown = false;
    private static boolean wasShiftDown = false;

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ModContainer modContainer = ModList.get().getModContainerById(MCARomanticExpansion.MODID)
                .orElseThrow(() -> new IllegalStateException("Mod container not found"));

        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (modContainer1, parent) -> new HUDConfigScreen(parent)
        );

        event.enqueueWork(() -> {
            // NeoForge 26.2 中 ItemProperties 已移除
            // 物品模型变种通过 JSON 模型文件中的 "overrides" 控制
            // 无需代码注册

            AffectionHUD.init();
            HUDConfig.applyConfig();
            ClientEventHandler.init();
            NeoForge.EVENT_BUS.addListener(MCARomanticExpansionClient::onClientTick);
        });
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        boolean isDown = CarryKeyBindings.KEY_PRINCESS_CARRY.consumeClick();
        if (isDown && !wasCarryKeyDown) {
            handlePrincessCarryKeyPressed(mc);
        }
        wasCarryKeyDown = isDown;

        boolean shiftDown = mc.options.keyShift.isDown();
        if (shiftDown && !wasShiftDown) {
            handleShiftPressed(mc);
        }
        wasShiftDown = shiftDown;
    }

    private static void handleShiftPressed(Minecraft mc) {
        Player me = mc.player;
        if (me == null) return;
        if (CarryClientState.isCarrier(me.getUUID()) || CarryClientState.isCarried(me.getUUID())) {
            mc.getConnection().send(new CarryStopPacket());
        }
    }

    private static void handlePrincessCarryKeyPressed(Minecraft mc) {
        Player me = mc.player;
        if (me == null) return;
        if (mc.gui.screen() != null) return;
        if (CarryClientState.isCarrier(me.getUUID()) || CarryClientState.isCarried(me.getUUID())) {
            mc.getConnection().send(new CarryStopPacket());
            return;
        }
        Player target = findPlayerLookingAt(me);
        if (target == null) {
            me.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.carry.no_target"));
            return;
        }
        if (target == me) return;
        mc.getConnection().send(new CarryRequestPacket(target.getUUID()));
    }

    private static Player findPlayerLookingAt(Player viewer) {
        double maxDistance = 6.0;
        Vec3 start = viewer.getEyePosition(1.0F);
        Vec3 look = viewer.getLookAngle();
        Vec3 end = start.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);
        AABB aabb = new AABB(start, end).inflate(1.2);
        List<net.minecraft.world.entity.Entity> entities = viewer.level().getEntities(viewer, aabb);
        double closestDistance = maxDistance;
        Player foundPlayer = null;
        for (net.minecraft.world.entity.Entity entity : entities) {
            if (entity instanceof Player otherPlayer && otherPlayer != viewer && otherPlayer.isAlive()) {
                Vec3 entityPos = otherPlayer.getEyePosition(1.0F);
                double distance = entityPos.distanceToSqr(start);
                Vec3 toEntity = entityPos.subtract(start).normalize();
                double dot = toEntity.dot(look);
                if (dot > 0.85 && distance < closestDistance) {
                    closestDistance = distance;
                    foundPlayer = otherPlayer;
                }
            }
        }
        return foundPlayer;
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                ModParticles.RAINBOW_PARTICLE.get(),
                RainbowParticle.Provider::new
        );
        MCARomanticExpansion.LOGGER.debug("✅ Registered rainbow particle provider");
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