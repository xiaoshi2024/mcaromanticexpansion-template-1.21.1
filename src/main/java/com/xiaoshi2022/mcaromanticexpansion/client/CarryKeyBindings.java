package com.xiaoshi2022.mcaromanticexpansion.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "mcaromanticexpansion", value = Dist.CLIENT)
public class CarryKeyBindings {
    public static final String KEY_CATEGORY = "key.categories.mcaromanticexpansion";

    public static final KeyMapping KEY_PRINCESS_CARRY = new KeyMapping(
            "key.mcaromanticexpansion.princess_carry",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(KEY_PRINCESS_CARRY);
    }
}
