package com.xiaoshi2022.mcaromanticexpansion.event;

import com.xiaoshi2022.mcaromanticexpansion.util.RomanticEventManager;
import com.xiaoshi2022.mcaromanticexpansion.util.SharedUmbrellaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RomanticTickHandler {

    // 1.20.1 中使用 TickEvent.PlayerTickEvent
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务端执行，且只在 tick 结束时处理
        if (event.side.isClient()) return;
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        SharedUmbrellaManager.onPlayerTick(player);
        RomanticEventManager.onPlayerTick(player);
    }

    // 1.20.1 中使用 PlayerEvent.PlayerLoggedOutEvent
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player != null) {
            SharedUmbrellaManager.endSharedUmbrella(player);
        }
    }

    // 1.20.1 中使用 LivingDeathEvent
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            SharedUmbrellaManager.endSharedUmbrella(player);
        }
    }
}