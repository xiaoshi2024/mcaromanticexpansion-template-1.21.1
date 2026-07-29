package com.xiaoshi2022.mcaromanticexpansion.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "mcaromanticexpansion", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CarryKeyBindings {
    public static final String KEY_CATEGORY = "key.categories.mcaromanticexpansion";

    public static final KeyMapping KEY_PRINCESS_CARRY = new KeyMapping(
            "key.mcaromanticexpansion.princess_carry",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(KEY_PRINCESS_CARRY);
    }
}