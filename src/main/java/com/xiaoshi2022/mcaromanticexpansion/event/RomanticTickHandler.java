package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.util.RomanticEventManager;
import com.xiaoshi2022.mcaromanticexpansion.util.SharedUmbrellaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;  // 修改：使用 LivingDeathEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;  // 新增：使用 PlayerTickEvent

public class RomanticTickHandler {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {  // 修改：使用 PlayerTickEvent
        Player player = event.getEntity();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        SharedUmbrellaManager.onPlayerTick(player);
        RomanticEventManager.onPlayerTick(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        SharedUmbrellaManager.endSharedUmbrella(player);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {  // 修改：使用 LivingDeathEvent
        if (event.getEntity() instanceof Player player) {
            SharedUmbrellaManager.endSharedUmbrella(player);
        }
    }
}