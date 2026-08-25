package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class UmbrellaStandSyncHandler {
    public static void handleClient(final UmbrellaStandSyncPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Class<?> handlerClass = Class.forName("com.xiaoshi2022.mcaromanticexpansion.network.UmbrellaStandSyncHandlerClient");
                java.lang.reflect.Method method = handlerClass.getMethod("handle", UmbrellaStandSyncPacket.class);
                method.invoke(null, packet);
            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.warn("Failed to handle UmbrellaStandSyncPacket (likely server-side)", e);
            }
        });
    }
}
