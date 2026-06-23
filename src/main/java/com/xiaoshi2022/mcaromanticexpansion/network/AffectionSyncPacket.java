package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public record AffectionSyncPacket(UUID targetUUID, int affection) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AffectionSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(MCARomanticExpansion.locate("affection_sync"));

    public static final StreamCodec<FriendlyByteBuf, AffectionSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.targetUUID());
                buf.writeInt(packet.affection());
            },
            buf -> new AffectionSyncPacket(buf.readUUID(), buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(AffectionSyncPacket payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                // 使用反射避免服务端加载客户端类
                Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
                Object minecraftInstance = minecraftClass.getMethod("getInstance").invoke(null);
                Object player = minecraftClass.getField("player").get(minecraftInstance);
                
                if (player != null) {
                    Class<?> playerClass = player.getClass();
                    java.lang.reflect.Method getUUIDMethod = playerClass.getMethod("getUUID");
                    UUID playerUUID = (UUID) getUUIDMethod.invoke(player);
                    
                    AffectionManager.ClientCache.setAffection(playerUUID, payload.targetUUID(), payload.affection());
                }
            } catch (ClassNotFoundException e) {
                // 服务端环境，忽略
            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.warn("Failed to handle AffectionSyncPacket", e);
            }
        });
    }

    public static void sendToClient(ServerPlayer player, UUID targetUUID, int affection) {
        AffectionSyncPacket packet = new AffectionSyncPacket(targetUUID, affection);
        PacketDistributor.sendToPlayer(player, packet);
    }
}
