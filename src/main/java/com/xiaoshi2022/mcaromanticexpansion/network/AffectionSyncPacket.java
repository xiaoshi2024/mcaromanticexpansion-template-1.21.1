package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class AffectionSyncPacket {
    private final UUID targetUUID;
    private final int affection;

    public AffectionSyncPacket(UUID targetUUID, int affection) {
        this.targetUUID = targetUUID;
        this.affection = affection;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public int getAffection() {
        return affection;
    }

    // 编码方法
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(targetUUID);
        buf.writeInt(affection);
    }

    // 解码方法
    public static AffectionSyncPacket decode(FriendlyByteBuf buf) {
        UUID targetUUID = buf.readUUID();
        int affection = buf.readInt();
        return new AffectionSyncPacket(targetUUID, affection);
    }

    // 客户端处理
    public static void handle(AffectionSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    UUID playerUUID = mc.player.getUUID();
                    AffectionManager.ClientCache.setAffection(playerUUID, packet.targetUUID, packet.affection);

                    MCARomanticExpansion.LOGGER.debug("Client affection synced: {} -> {} = {}",
                            playerUUID, packet.targetUUID, packet.affection);
                }
            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.warn("Failed to handle AffectionSyncPacket on client", e);
            }
        });
        context.setPacketHandled(true);
    }

    // 服务端发送到客户端
    public static void sendToClient(ServerPlayer player, UUID targetUUID, int affection) {
        AffectionSyncPacket packet = new AffectionSyncPacket(targetUUID, affection);
        // 使用 SimpleChannel 发送到客户端
        ModNetwork.CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}