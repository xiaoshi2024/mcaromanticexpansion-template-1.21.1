package com.xiaoshi2022.mcaromanticexpansion;

import com.xiaoshi2022.mcaromanticexpansion.client.AffectionHUD;
import com.xiaoshi2022.mcaromanticexpansion.client.CarryClientState;
import com.xiaoshi2022.mcaromanticexpansion.client.CarryKeyBindings;
import com.xiaoshi2022.mcaromanticexpansion.client.ClientEventHandler;
import com.xiaoshi2022.mcaromanticexpansion.client.gui.HUDConfigScreen;
import com.xiaoshi2022.mcaromanticexpansion.client.model.WeddingClothesModel;
import com.xiaoshi2022.mcaromanticexpansion.client.particle.RainbowParticle;
import com.xiaoshi2022.mcaromanticexpansion.client.renderer.UmbrellaStandRenderer;
import com.xiaoshi2022.mcaromanticexpansion.compat.curios.CuriosIntegration;
import com.xiaoshi2022.mcaromanticexpansion.config.HUDConfig;
import com.xiaoshi2022.mcaromanticexpansion.item.GiftBoxItem;
import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
import com.xiaoshi2022.mcaromanticexpansion.network.CarryRequestPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.CarryStopPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.ModNetwork;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModBlockEntities;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = MCARomanticExpansion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MCARomanticExpansionClient {

    private static boolean wasCarryKeyDown = false;
    private static boolean wasShiftDown = false;

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 【添加】注册 Curios 集成 - 必须在客户端设置时调用
            CuriosIntegration.onClientSetup(event);

            ItemProperties.register(ModItems.GIFT_BOX.get(),
                    new ResourceLocation(MCARomanticExpansion.MODID, "variant"),
                    (stack, level, entity, seed) -> GiftBoxItem.getModelVariantValue(stack));

            ItemProperties.register(ModItems.UMBRELLA.get(),
                    new ResourceLocation(MCARomanticExpansion.MODID, "umbrella_state"),
                    (stack, level, entity, seed) -> UmbrellaItem.getUmbrellaState(stack));

            AffectionHUD.init();
            HUDConfig.applyConfig();

            MinecraftForge.EVENT_BUS.addListener(MCARomanticExpansionClient::onClientTick);
        });
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

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
            ModNetwork.CHANNEL.sendToServer(new CarryStopPacket());
        }
    }

    private static void handlePrincessCarryKeyPressed(Minecraft mc) {
        Player me = mc.player;
        if (me == null || mc.screen != null) return;

        if (CarryClientState.isCarrier(me.getUUID()) || CarryClientState.isCarried(me.getUUID())) {
            ModNetwork.CHANNEL.sendToServer(new CarryStopPacket());
            return;
        }

        Player target = findPlayerLookingAt(me);
        if (target == null) {
            me.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.carry.no_target"));
            return;
        }
        if (target == me) return;

        ModNetwork.CHANNEL.sendToServer(new CarryRequestPacket(target.getUUID()));
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
        MCARomanticExpansion.LOGGER.debug("Registered rainbow particle provider");
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