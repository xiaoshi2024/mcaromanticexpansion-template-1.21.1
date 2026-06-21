package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.client.gui.SharedUmbrellaRequestScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SharedUmbrellaRequestHandlerClient {
    @OnlyIn(Dist.CLIENT)
    public static void handle(final SharedUmbrellaRequestPacket packet) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new SharedUmbrellaRequestScreen(
                    packet.requesterUUID(), 
                    packet.requesterName()
            ));
        });
    }
}
