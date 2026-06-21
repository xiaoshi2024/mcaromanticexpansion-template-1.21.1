package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SharedUmbrellaRequestHandler {
    public static void handleClient(final SharedUmbrellaRequestPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Class<?> handlerClass = Class.forName("com.xiaoshi2022.mcaromanticexpansion.network.SharedUmbrellaRequestHandlerClient");
                java.lang.reflect.Method method = handlerClass.getMethod("handle", SharedUmbrellaRequestPacket.class);
                method.invoke(null, packet);
            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.warn("Failed to handle SharedUmbrellaRequestPacket (likely server-side)", e);
            }
        });
    }
}
