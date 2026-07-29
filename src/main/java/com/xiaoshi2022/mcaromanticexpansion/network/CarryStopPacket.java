package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.util.CarryRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CarryStopPacket {
    // 空记录，不需要字段

    public CarryStopPacket() {
    }

    public void encode(FriendlyByteBuf buf) {
        // 没有数据需要写入
    }

    public static CarryStopPacket decode(FriendlyByteBuf buf) {
        return new CarryStopPacket();
    }

    public static void handle(CarryStopPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CarryRuntime.handleStopRequest(player);
            }
        });
        context.setPacketHandled(true);
    }
}