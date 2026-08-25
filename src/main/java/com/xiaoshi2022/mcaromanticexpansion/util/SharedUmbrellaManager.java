// SharedUmbrellaManager.java - 修复后的完整版本

package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.advancement.CriterionTriggerRegister;
import com.xiaoshi2022.mcaromanticexpansion.api.event.SharedUmbrellaEstablishedEvent;
import com.xiaoshi2022.mcaromanticexpansion.item.UmbrellaItem;
import com.xiaoshi2022.mcaromanticexpansion.network.SharedUmbrellaRequestPacket;
import com.xiaoshi2022.mcaromanticexpansion.network.SharedUmbrellaResponsePacket;
import com.xiaoshi2022.mcaromanticexpansion.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
            initiator.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.already_sharing"));
            return false;
        }
        if (hasPendingRequest(initiator) || hasPendingRequest(target)) {
            initiator.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.pending_request"));
            return false;
        }

        // 检查发起者是否有打开的伞
        if (!hasOpenUmbrella(initiator)) {
            initiator.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.need_open"));
            return false;
        }

        ItemStack mainHand = initiator.getMainHandItem();
        ItemStack offHand = initiator.getOffhandItem();
        MCARomanticExpansion.LOGGER.debug("Sending shared umbrella request: {} has umbrella in {}",
                initiator.getName().getString(),
                mainHand.is(ModItems.UMBRELLA.get()) ? "main hand" : "off hand");

        if (initiator.distanceTo(target) > MAX_DISTANCE) {
            initiator.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.too_far"));
            return false;
        }

        pendingRequests.put(initiator.getUUID(), target.getUUID());
        pendingRequests.put(target.getUUID(), initiator.getUUID());

        SharedUmbrellaRequestPacket packet = new SharedUmbrellaRequestPacket(
                initiator.getUUID(),
                initiator.getName().getString()
        );
        PacketDistributor.sendToPlayer(target, packet);

        initiator.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.request_sent"));
        return true;
    }

    // 修复: 使用 level() 获取 Server
    public static void handleResponse(ServerPlayer responder, SharedUmbrellaResponsePacket response) {
        UUID requesterUUID = response.targetUUID();

        if (!pendingRequests.containsKey(responder.getUUID()) ||
                !pendingRequests.get(responder.getUUID()).equals(requesterUUID)) {
            return;
        }

        // 修复: 通过 level() 获取 Server
        ServerPlayer requester = null;
        if (responder.level() instanceof ServerLevel serverLevel) {
            requester = serverLevel.getServer().getPlayerList().getPlayer(requesterUUID);
        }

        if (requester == null || !requester.isAlive()) {
            pendingRequests.remove(responder.getUUID());
            if (requesterUUID != null) {
                pendingRequests.remove(requesterUUID);
            }
            responder.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.offline"));
            return;
        }

        pendingRequests.remove(responder.getUUID());
        pendingRequests.remove(requesterUUID);

        if (response.accepted()) {
            // 重新检查发起者是否还有打开的伞
            ItemStack mainHand = requester.getMainHandItem();
            ItemStack offHand = requester.getOffhandItem();

            // 首先检查是否持有伞
            if (!mainHand.is(ModItems.UMBRELLA.get()) && !offHand.is(ModItems.UMBRELLA.get())) {
                requester.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.lost_self"));
                responder.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.lost_other"));
                return;
            }

            // 然后检查伞是否打开
            if (!hasOpenUmbrella(requester)) {
                // 尝试强制打开
                if (mainHand.is(ModItems.UMBRELLA.get())) {
                    UmbrellaItem.setUmbrellaState(mainHand, UmbrellaItem.State.FULL_OPEN);
                } else if (offHand.is(ModItems.UMBRELLA.get())) {
                    UmbrellaItem.setUmbrellaState(offHand, UmbrellaItem.State.FULL_OPEN);
                }

                if (!hasOpenUmbrella(requester)) {
                    requester.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.closed_self"));
                    responder.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.closed_other"));
                    return;
                }
            }

            if (requester.distanceTo(responder) > MAX_DISTANCE) {
                requester.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.too_far_share"));
                responder.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.too_far_share"));
                return;
            }

            SharedUmbrellaEstablishedEvent establishedEvent = new SharedUmbrellaEstablishedEvent(requester, responder);
            NeoForge.EVENT_BUS.post(establishedEvent);
            if (establishedEvent.isCanceled()) {
                MCARomanticExpansion.LOGGER.debug("Shared umbrella establishment canceled by event listener for {} and {}",
                        requester.getName().getString(), responder.getName().getString());
                return;
            }

            SharedUmbrellaState state1 = new SharedUmbrellaState(requester, responder);
            SharedUmbrellaState state2 = new SharedUmbrellaState(responder, requester);

            sharedUmbrellaStates.put(requester.getUUID(), state1);
            sharedUmbrellaStates.put(responder.getUUID(), state2);

            // 重置双方的 tick 计数器，避免立即触发检查
            tickCounters.put(requester.getUUID(), 1);
            tickCounters.put(responder.getUUID(), 1);

            MCARomanticExpansion.LOGGER.debug("Shared umbrella established: {} holding {} in {}",
                    requester.getName().getString(),
                    mainHand.is(ModItems.UMBRELLA.get()) ? "umbrella (main)" : (offHand.is(ModItems.UMBRELLA.get()) ? "umbrella (off)" : "no umbrella"),
                    mainHand.is(ModItems.UMBRELLA.get()) ? "main hand" : "off hand");

            requester.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.established"));
            responder.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.established"));

            // 触发成就
            CriterionTriggerRegister.FIRST_UMBRELLA_GIFT.get().trigger(requester);

            // 检查是否是雨天送伞
            boolean isRaining = requester.level().isRaining();
            if (isRaining) {
                int rainyCount = getRainyUmbrellaCount(requester);
                rainyCount++;
                setRainyUmbrellaCount(requester, rainyCount);
                CriterionTriggerRegister.RAINY_UMBRELLA_GIFT.get().trigger(requester, rainyCount);
            }

            // 互赠伞累计次数
            int mutualCount = getMutualUmbrellaCount(requester, responder);
            mutualCount++;
            setMutualUmbrellaCount(requester, responder, mutualCount);
            CriterionTriggerRegister.MUTUAL_UMBRELLA_GIFT.get().trigger(requester, mutualCount);
            CriterionTriggerRegister.MUTUAL_UMBRELLA_GIFT.get().trigger(responder, mutualCount);

            // 触发共伞浪漫事件
            RomanticEventManager.onSharedUmbrellaEstablished(requester, responder);
        } else {
            requester.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.rejected"));
            responder.sendSystemMessage(Component.translatable("message.mcaromanticexpansion.umbrella.rejected_self"));
        }
    }

    public static void endSharedUmbrella(Player player) {
        endSharedUmbrella(player, "message.mcaromanticexpansion.umbrella.end_partner_left");
    }

    public static void endSharedUmbrella(Player player, String messageKey) {
        SharedUmbrellaState state = sharedUmbrellaStates.remove(player.getUUID());
        if (state != null && state.partner != null) {
            sharedUmbrellaStates.remove(state.partner.getUUID());
            state.partner.sendSystemMessage(Component.translatable(messageKey));
        }
    }

    /**
     * 检查玩家是否有打开的伞（主手或副手）
     */
    private static boolean hasOpenUmbrella(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (mainHand.is(ModItems.UMBRELLA.get()) && isUmbrellaFullyOpen(mainHand)) {
            return true;
        }
        if (offHand.is(ModItems.UMBRELLA.get()) && isUmbrellaFullyOpen(offHand)) {
            return true;
        }
        return false;
    }

    /**
     * 检查伞是否完全打开
     */
    private static boolean isUmbrellaFullyOpen(ItemStack stack) {
        return UmbrellaItem.getState(stack) == UmbrellaItem.State.FULL_OPEN;
    }

    public static void onPlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        SharedUmbrellaState state = sharedUmbrellaStates.get(player.getUUID());
        if (state == null) return;

        Integer counter = tickCounters.compute(player.getUUID(), (uuid, val) -> {
            if (val == null) return 1;
            return val + 1;
        });

        if (counter % SHARE_CHECK_INTERVAL != 0) return;

        if (!isPartnerValid(state.partner)) {
            endSharedUmbrella(player);
            return;
        }

        double distance = player.distanceTo(state.partner);
        if (distance > MAX_DISTANCE) {
            endSharedUmbrella(player, "message.mcaromanticexpansion.umbrella.end_too_far");
            return;
        }

        boolean hasOpen = hasOpenUmbrella(player) || hasOpenUmbrella(state.partner);

        if (!hasOpen) {
            boolean playerHasUmbrella = player.getMainHandItem().is(ModItems.UMBRELLA.get()) ||
                    player.getOffhandItem().is(ModItems.UMBRELLA.get());
            boolean partnerHasUmbrella = state.partner.getMainHandItem().is(ModItems.UMBRELLA.get()) ||
                    state.partner.getOffhandItem().is(ModItems.UMBRELLA.get());

            if (!playerHasUmbrella && !partnerHasUmbrella) {
                MCARomanticExpansion.LOGGER.debug("Both players {} and {} no longer holding umbrella",
                        player.getName().getString(), state.partner.getName().getString());
                endSharedUmbrella(player, "message.mcaromanticexpansion.umbrella.end_lost");
            } else {
                ItemStack playerMain = player.getMainHandItem();
                ItemStack playerOff = player.getOffhandItem();
                ItemStack partnerMain = state.partner.getMainHandItem();
                ItemStack partnerOff = state.partner.getOffhandItem();

                if (playerMain.is(ModItems.UMBRELLA.get())) {
                    UmbrellaItem.setUmbrellaState(playerMain, UmbrellaItem.State.FULL_OPEN);
                } else if (playerOff.is(ModItems.UMBRELLA.get())) {
                    UmbrellaItem.setUmbrellaState(playerOff, UmbrellaItem.State.FULL_OPEN);
                }

                if (partnerMain.is(ModItems.UMBRELLA.get())) {
                    UmbrellaItem.setUmbrellaState(partnerMain, UmbrellaItem.State.FULL_OPEN);
                } else if (partnerOff.is(ModItems.UMBRELLA.get())) {
                    UmbrellaItem.setUmbrellaState(partnerOff, UmbrellaItem.State.FULL_OPEN);
                }

                hasOpen = hasOpenUmbrella(player) || hasOpenUmbrella(state.partner);
                if (!hasOpen) {
                    MCARomanticExpansion.LOGGER.debug("Umbrella still closed after forcing for {} and {}",
                            player.getName().getString(), state.partner.getName().getString());
                    endSharedUmbrella(player, "message.mcaromanticexpansion.umbrella.end_closed");
                }
            }
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

    // 雨天送伞次数存储 - 修复: getInt 返回 Optional<Integer>
    private static final String RAINY_UMBRELLA_COUNT_KEY = "mcaromanticexpansion.rainy_umbrella_count";

    private static int getRainyUmbrellaCount(ServerPlayer player) {
        return player.getPersistentData().getInt(RAINY_UMBRELLA_COUNT_KEY).orElse(0);
    }

    private static void setRainyUmbrellaCount(ServerPlayer player, int count) {
        player.getPersistentData().putInt(RAINY_UMBRELLA_COUNT_KEY, count);
    }

    // 互赠伞次数存储
    private static String getMutualUmbrellaKey(UUID playerId) {
        return "mcaromanticexpansion.mutual_umbrella_count_" + playerId.toString();
    }

    private static int getMutualUmbrellaCount(ServerPlayer player, ServerPlayer partner) {
        String key = getMutualUmbrellaKey(partner.getUUID());
        return player.getPersistentData().getInt(key).orElse(0);
    }

    private static void setMutualUmbrellaCount(ServerPlayer player, ServerPlayer partner, int count) {
        String key = getMutualUmbrellaKey(partner.getUUID());
        player.getPersistentData().putInt(key, count);
    }
}