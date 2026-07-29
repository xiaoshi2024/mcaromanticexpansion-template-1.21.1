package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.client.gui.PrincessCarryRequestScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CarryInvitePacket {
    private final UUID requesterUUID;
    private final String requesterName;

    public CarryInvitePacket(UUID requesterUUID, String requesterName) {
        this.requesterUUID = requesterUUID;
        this.requesterName = requesterName;
    }

    public UUID getRequesterUUID() {
        return requesterUUID;
    }

    public String getRequesterName() {
        return requesterName;
    }

    // 编码方法
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(requesterUUID);
        buf.writeUtf(requesterName, 64);
    }

    // 解码方法
    public static CarryInvitePacket decode(FriendlyByteBuf buf) {
        UUID requesterUUID = buf.readUUID();
        String requesterName = buf.readUtf(64);
        return new CarryInvitePacket(requesterUUID, requesterName);
    }

    // 客户端处理
    public static void handle(CarryInvitePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            openCarryInviteGUI(packet);
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openCarryInviteGUI(CarryInvitePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.setScreen(new PrincessCarryRequestScreen(packet.requesterUUID, packet.requesterName));
        });
    }
}