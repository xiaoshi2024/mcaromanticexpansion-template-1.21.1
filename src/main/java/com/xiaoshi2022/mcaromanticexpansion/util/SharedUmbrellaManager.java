package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
import com.xiaoshi2022.mcaromanticexpansion.network.SharedUmbrellaRequestPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.SharedUmbrellaResponsePacket;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class SharedUmbrellaManager {
    private static final Map<UUID, SharedUmbrellaState> sharedUmbrellaStates = new HashMap<>();
    private static final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private static final int MAX_DISTANCE = 8;
    private static final int SHARE_CHECK_INTERVAL = 40;
    private static final Map<UUID, Integer> tickCounters = new HashMap<>();

    public static boolean isInSharedUmbrella(Player player) {
        return sharedUmbrellaStates.containsKey(player.getUUID());
    }

    public static Player getSharedPartner(Player player) {
        SharedUmbrellaState state = sharedUmbrellaStates.get(player.getUUID());
        if (state != null && state.partner != null && isPartnerValid(state.partner)) {
            return state.partner;
        }
        return null;
    }

    private static boolean isPartnerValid(ServerPlayer partner) {
        return partner != null && partner.isAlive() && !partner.hasDisconnected();
    }

    public static boolean hasPendingRequest(Player player) {
        return pendingRequests.containsKey(player.getUUID());
    }

    public static boolean sendRequest(ServerPlayer initiator, ServerPlayer target) {
        if (initiator == target) return false;
        if (isInSharedUmbrella(initiator) || isInSharedUmbrella(target)) {
            initiator.sendSystemMessage(Component.literal("§c对方已经在共伞状态中！").withStyle(ChatFormatting.RED));
            return false;
        }
        if (hasPendingRequest(initiator) || hasPendingRequest(target)) {
            initiator.sendSystemMessage(Component.literal("§c有未处理的共伞请求！").withStyle(ChatFormatting.RED));
            return false;
        }

        ItemStack mainHand = initiator.getMainHandItem();
        ItemStack offHand = initiator.getOffhandItem();
        
        boolean hasOpenUmbrella = false;
        if (mainHand.is(ModItems.UMBRELLA.get())) {
            hasOpenUmbrella = UmbrellaItem.getState(mainHand) == UmbrellaItem.State.FULL_OPEN;
        } else if (offHand.is(ModItems.UMBRELLA.get())) {
            hasOpenUmbrella = UmbrellaItem.getState(offHand) == UmbrellaItem.State.FULL_OPEN;
        }

        if (!hasOpenUmbrella) {
            initiator.sendSystemMessage(Component.literal("§c请先将伞完全撑开！").withStyle(ChatFormatting.RED));
            return false;
        }

        if (initiator.distanceTo(target) > MAX_DISTANCE) {
            initiator.sendSystemMessage(Component.literal("§c请靠近对方再使用！").withStyle(ChatFormatting.RED));
            return false;
        }

        pendingRequests.put(initiator.getUUID(), target.getUUID());
        pendingRequests.put(target.getUUID(), initiator.getUUID());

        SharedUmbrellaRequestPacket packet = new SharedUmbrellaRequestPacket(
                initiator.getUUID(), 
                initiator.getName().getString()
        );
        PacketDistributor.sendToPlayer(target, packet);

        initiator.sendSystemMessage(Component.literal("§a已发送共伞邀请，请等待对方回应...").withStyle(ChatFormatting.GREEN));
        return true;
    }

    public static void handleResponse(ServerPlayer responder, SharedUmbrellaResponsePacket response) {
        UUID requesterUUID = response.targetUUID();
        
        if (!pendingRequests.containsKey(responder.getUUID()) || 
            !pendingRequests.get(responder.getUUID()).equals(requesterUUID)) {
            return;
        }

        ServerPlayer requester = responder.getServer().getPlayerList().getPlayer(requesterUUID);
        
        if (requester == null || !requester.isAlive()) {
            pendingRequests.remove(responder.getUUID());
            if (requesterUUID != null) {
                pendingRequests.remove(requesterUUID);
            }
            responder.sendSystemMessage(Component.literal("§c对方已离线！").withStyle(ChatFormatting.RED));
            return;
        }

        pendingRequests.remove(responder.getUUID());
        pendingRequests.remove(requesterUUID);

        if (response.accepted()) {
            ItemStack mainHand = requester.getMainHandItem();
            ItemStack offHand = requester.getOffhandItem();
            
            boolean hasOpenUmbrella = false;
            if (mainHand.is(ModItems.UMBRELLA.get())) {
                hasOpenUmbrella = UmbrellaItem.getState(mainHand) == UmbrellaItem.State.FULL_OPEN;
            } else if (offHand.is(ModItems.UMBRELLA.get())) {
                hasOpenUmbrella = UmbrellaItem.getState(offHand) == UmbrellaItem.State.FULL_OPEN;
            }

            if (!hasOpenUmbrella) {
                requester.sendSystemMessage(Component.literal("§c你的伞已经合上了！").withStyle(ChatFormatting.RED));
                responder.sendSystemMessage(Component.literal("§c对方的伞已经合上了！").withStyle(ChatFormatting.RED));
                return;
            }

            if (requester.distanceTo(responder) > MAX_DISTANCE) {
                requester.sendSystemMessage(Component.literal("§c距离太远，无法共伞！").withStyle(ChatFormatting.RED));
                responder.sendSystemMessage(Component.literal("§c距离太远，无法共伞！").withStyle(ChatFormatting.RED));
                return;
            }

            SharedUmbrellaState state1 = new SharedUmbrellaState(requester, responder);
            SharedUmbrellaState state2 = new SharedUmbrellaState(responder, requester);

            sharedUmbrellaStates.put(requester.getUUID(), state1);
            sharedUmbrellaStates.put(responder.getUUID(), state2);

            requester.sendSystemMessage(Component.literal("§a☂ 你们共撑一把伞！").withStyle(ChatFormatting.GREEN));
            responder.sendSystemMessage(Component.literal("§a☂ 你们共撑一把伞！").withStyle(ChatFormatting.GREEN));
        } else {
            requester.sendSystemMessage(Component.literal("§c对方拒绝了你的共伞邀请").withStyle(ChatFormatting.RED));
            responder.sendSystemMessage(Component.literal("§a已拒绝共伞邀请").withStyle(ChatFormatting.GREEN));
        }
    }

    public static void endSharedUmbrella(Player player) {
        SharedUmbrellaState state = sharedUmbrellaStates.remove(player.getUUID());
        if (state != null && state.partner != null) {
            sharedUmbrellaStates.remove(state.partner.getUUID());
            state.partner.sendSystemMessage(Component.literal("§c对方离开了，共伞结束").withStyle(ChatFormatting.RED));
        }
    }

    public static void onPlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        Integer counter = tickCounters.compute(player.getUUID(), (uuid, val) -> val == null ? 0 : val + 1);
        if (counter % SHARE_CHECK_INTERVAL != 0) return;

        SharedUmbrellaState state = sharedUmbrellaStates.get(player.getUUID());
        if (state == null) return;

        if (!isPartnerValid(state.partner)) {
            endSharedUmbrella(player);
            return;
        }

        double distance = player.distanceTo(state.partner);
        if (distance > MAX_DISTANCE) {
            endSharedUmbrella(player);
            return;
        }

        boolean initiatorHasUmbrella = false;
        ItemStack mainHand = state.initiator.getMainHandItem();
        ItemStack offHand = state.initiator.getOffhandItem();
        
        if (mainHand.is(ModItems.UMBRELLA.get())) {
            initiatorHasUmbrella = UmbrellaItem.getState(mainHand) == UmbrellaItem.State.FULL_OPEN;
        } else if (offHand.is(ModItems.UMBRELLA.get())) {
            initiatorHasUmbrella = UmbrellaItem.getState(offHand) == UmbrellaItem.State.FULL_OPEN;
        }

        if (!initiatorHasUmbrella) {
            endSharedUmbrella(player);
            return;
        }

        state.ticksUnderUmbrella++;

        if (state.ticksUnderUmbrella % 200 == 0) {
            AffectionManager.handleInteraction(AffectionManager.InteractionType.SHARED_UMBRELLA, 
                    state.initiator, state.partner);
            AffectionManager.handleInteraction(AffectionManager.InteractionType.SHARED_UMBRELLA, 
                    state.partner, state.initiator);
        }
    }

    public static int getSharedUmbrellaDuration(Player player) {
        SharedUmbrellaState state = sharedUmbrellaStates.get(player.getUUID());
        return state != null ? state.ticksUnderUmbrella : 0;
    }

    private static class SharedUmbrellaState {
        final ServerPlayer initiator;
        final ServerPlayer partner;
        int ticksUnderUmbrella;

        SharedUmbrellaState(ServerPlayer initiator, ServerPlayer partner) {
            this.initiator = initiator;
            this.partner = partner;
            this.ticksUnderUmbrella = 0;
        }
    }
}
