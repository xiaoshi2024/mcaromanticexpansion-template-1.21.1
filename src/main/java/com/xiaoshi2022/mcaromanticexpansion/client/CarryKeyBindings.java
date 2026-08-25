package com.xiaoshi2022.mcaromanticexpansion.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "mcaromanticexpansion", value = Dist.CLIENT)
public class CarryKeyBindings {
    public static final String KEY_CATEGORY = "key.categories.mcaromanticexpansion";

    public static final KeyMapping KEY_PRINCESS_CARRY = new KeyMapping(
            "key.mcaromanticexpansion.princess_carry",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KeyMapping.Category.register(Identifier.parse(KEY_CATEGORY))
    );

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(KEY_PRINCESS_CARRY);
    }
}