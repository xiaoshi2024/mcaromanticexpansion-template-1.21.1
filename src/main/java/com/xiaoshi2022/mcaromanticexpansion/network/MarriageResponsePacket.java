package com.xiaoshi2022.mcaromanticexpansion.network;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.api.event.MarriageChangedEvent;
import com.xiaoshi2022.mcaromanticexpansion.util.AffectionManager;
import com.xiaoshi2022.mcaromanticexpansion.util.RingNBTUtil;
import forge.net.mca.item.WeddingRingItem;
import forge.net.mca.server.world.data.PlayerSaveData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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

    public UUID getPartnerUUID() {
        return partnerUUID;
    }

    public boolean isConfirmed() {
        return confirmed;
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
            if (responder == null || responder.getServer() == null) {
                MCARomanticExpansion.LOGGER.warn("MarriageResponsePacket: responder is null or server is null");
                return;
            }

            ServerPlayer partner = responder.getServer().getPlayerList().getPlayer(packet.getPartnerUUID());
            if (partner == null) {
                MCARomanticExpansion.LOGGER.warn("MarriageResponsePacket: partner not found: {}", packet.getPartnerUUID());
                responder.sendSystemMessage(Component.literal("§c对方已离线！"));
                return;
            }

            if (!packet.isConfirmed()) {
                responder.sendSystemMessage(Component.translatable("mcaromanticexpansion.marriage.declined"));
                partner.sendSystemMessage(Component.literal("§c" + responder.getName().getString() + " 拒绝了你的结婚请求！"));
                return;
            }

            // ========== 执行结婚逻辑 ==========

            // 1. 触发事件
            MarriageChangedEvent event = new MarriageChangedEvent(
                    responder, partner, MarriageChangedEvent.ChangeType.MARRIED
            );
            if (MinecraftForge.EVENT_BUS.post(event)) {
                MCARomanticExpansion.LOGGER.debug("Marriage event canceled");
                responder.sendSystemMessage(Component.literal("§c结婚被取消！"));
                return;
            }

            // 2. 检查双方是否都有结婚戒指
            ItemStack responderRing = findAndRemoveWeddingRing(responder);
            ItemStack partnerRing = findAndRemoveWeddingRing(partner);

            if (responderRing.isEmpty() || partnerRing.isEmpty()) {
                if (!responderRing.isEmpty()) responder.getInventory().add(responderRing);
                if (!partnerRing.isEmpty()) partner.getInventory().add(partnerRing);
                responder.sendSystemMessage(Component.literal("§c双方都需要持有结婚戒指！"));
                partner.sendSystemMessage(Component.literal("§c双方都需要持有结婚戒指！"));
                return;
            }

            try {
                // 3. 使用 MCA 的婚姻系统
                PlayerSaveData responderData = PlayerSaveData.get(responder);
                PlayerSaveData partnerData = PlayerSaveData.get(partner);

                responderData.marry(partner);
                partnerData.marry(responder);

                // 4. 创建带有对方名字的结婚戒指
                ItemStack customResponderRing = createWeddingRingWithPartner(responderRing, partner, true);
                ItemStack customPartnerRing = createWeddingRingWithPartner(partnerRing, responder, false);

                responder.getInventory().add(customResponderRing);
                partner.getInventory().add(customPartnerRing);

                // 5. 发送成功消息
                responder.sendSystemMessage(Component.literal("§a§l🎉 你与 " + partner.getName().getString() + " 结婚了！"));
                partner.sendSystemMessage(Component.literal("§a§l🎉 你与 " + responder.getName().getString() + " 结婚了！"));

                // 6. 添加好感度
                AffectionManager.handleInteraction(AffectionManager.InteractionType.MARRIAGE, responder, partner);

                MCARomanticExpansion.LOGGER.info("🎉 {} and {} got married!",
                        responder.getName().getString(), partner.getName().getString());

            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.error("Failed to marry players", e);
                responder.getInventory().add(responderRing);
                partner.getInventory().add(partnerRing);
                responder.sendSystemMessage(Component.literal("§c结婚失败，请重试！"));
                partner.sendSystemMessage(Component.literal("§c结婚失败，请重试！"));
            }
        });
        context.setPacketHandled(true);
    }

    // ========== 辅助方法 ==========

    private static ItemStack findAndRemoveWeddingRing(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof WeddingRingItem) {
                ItemStack copy = stack.copy();
                stack.shrink(1);
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack createWeddingRingWithPartner(ItemStack originalRing, ServerPlayer partner, boolean isReceiver) {
        // 如果戒指为空，创建一个新的结婚戒指
        if (originalRing.isEmpty()) {
            // 1.20.1 中需要通过 Item 注册获取
            try {
                // 方法1：使用 ResourceLocation 获取
                net.minecraft.resources.ResourceLocation ringId = new net.minecraft.resources.ResourceLocation("mca", "wedding_ring");
                net.minecraft.world.item.Item ringItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ringId);
                if (ringItem != null) {
                    originalRing = new ItemStack(ringItem);
                } else {
                    MCARomanticExpansion.LOGGER.warn("Failed to find wedding ring in registry!");
                    return ItemStack.EMPTY;
                }
            } catch (Exception e) {
                MCARomanticExpansion.LOGGER.error("Failed to create wedding ring", e);
                return ItemStack.EMPTY;
            }
        } else {
            originalRing = originalRing.copy();
            originalRing.setCount(1);
        }

        // ✅ 1.20.1 使用旧的 NBT 方式（而不是 DataComponents）
        // 清除原有的自定义名称（使用 NBT）
        if (originalRing.hasTag()) {
            originalRing.getTag().remove("display");
        }

        // 设置伴侣信息
        return RingNBTUtil.setWeddingRingPartner(originalRing, partner, isReceiver);
    }
}