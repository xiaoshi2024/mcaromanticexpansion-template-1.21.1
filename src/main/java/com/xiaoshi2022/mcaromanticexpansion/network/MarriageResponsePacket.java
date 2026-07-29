package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.api.event.MarriageChangedEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class MarriageResponsePacket {
    private final UUID partnerUUID;
    private final boolean confirmed;

    public MarriageResponsePacket(UUID partnerUUID, boolean confirmed) {
        this.partnerUUID = partnerUUID;
        this.confirmed = confirmed;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(partnerUUID);
        buf.writeBoolean(confirmed);
    }

    public static MarriageResponsePacket decode(FriendlyByteBuf buf) {
        return new MarriageResponsePacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(MarriageResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer responder = context.getSender();
            if (responder == null) return;

            ServerPlayer partner = responder.getServer().getPlayerList().getPlayer(packet.partnerUUID);
            if (partner == null) return;

            if (packet.confirmed) {
                // 触发婚姻事件
                MarriageChangedEvent event = new MarriageChangedEvent(
                        responder, partner, MarriageChangedEvent.ChangeType.MARRIED
                );
                if (MinecraftForge.EVENT_BUS.post(event)) {
                    // 事件被取消
                    return;
                }

                // 执行结婚逻辑
                // ... 你的结婚代码 ...

                MCARomanticExpansion.LOGGER.info("{} and {} got married!",
                        responder.getName().getString(), partner.getName().getString());
            } else {
                MCARomanticExpansion.LOGGER.info("{} rejected marriage from {}",
                        responder.getName().getString(), partner.getName().getString());
            }
        });
        context.setPacketHandled(true);
    }
}