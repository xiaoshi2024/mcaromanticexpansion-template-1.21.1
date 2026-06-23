// SharedUmbrellaManager.java - 修复后的完整版本

package com.xiaoshi2022.mcaromanticexpansion.util;

import com.xiaoshi2022.mcaromanticexpansion.MCARomanticExpansion;
import com.xiaoshi2022.mcaromanticexpansion.advancement.CriterionTriggerRegister;
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

        // 检查发起者是否有打开的伞
        if (!hasOpenUmbrella(initiator)) {
            initiator.sendSystemMessage(Component.literal("§c请先将伞完全撑开！").withStyle(ChatFormatting.RED));
            return false;
        }
        
        ItemStack mainHand = initiator.getMainHandItem();
        ItemStack offHand = initiator.getOffhandItem();
        MCARomanticExpansion.LOGGER.debug("Sending shared umbrella request: {} has umbrella in {}",
                initiator.getName().getString(),
                mainHand.is(ModItems.UMBRELLA.get()) ? "main hand" : "off hand");

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
            // 重新检查发起者是否还有打开的伞
            ItemStack mainHand = requester.getMainHandItem();
            ItemStack offHand = requester.getOffhandItem();
            
            // 首先检查是否持有伞
            if (!mainHand.is(ModItems.UMBRELLA.get()) && !offHand.is(ModItems.UMBRELLA.get())) {
                requester.sendSystemMessage(Component.literal("§c你手里的伞不见了！").withStyle(ChatFormatting.RED));
                responder.sendSystemMessage(Component.literal("§c对方手里的伞不见了！").withStyle(ChatFormatting.RED));
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
                    requester.sendSystemMessage(Component.literal("§c你的伞已经合上了！").withStyle(ChatFormatting.RED));
                    responder.sendSystemMessage(Component.literal("§c对方的伞已经合上了！").withStyle(ChatFormatting.RED));
                    return;
                }
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

            // 【关键修复】重置双方的 tick 计数器，避免立即触发检查
            tickCounters.put(requester.getUUID(), 1);
            tickCounters.put(responder.getUUID(), 1);

            MCARomanticExpansion.LOGGER.debug("Shared umbrella established: {} holding {} in {}",
                    requester.getName().getString(),
                    mainHand.is(ModItems.UMBRELLA.get()) ? "umbrella (main)" : (offHand.is(ModItems.UMBRELLA.get()) ? "umbrella (off)" : "no umbrella"),
                    mainHand.is(ModItems.UMBRELLA.get()) ? "main hand" : "off hand");

            requester.sendSystemMessage(Component.literal("§a☂ 你们共撑一把伞！").withStyle(ChatFormatting.GREEN));
            responder.sendSystemMessage(Component.literal("§a☂ 你们共撑一把伞！").withStyle(ChatFormatting.GREEN));

            // 【成就触发】首次送伞给玩家
            CriterionTriggerRegister.FIRST_UMBRELLA_GIFT.get().trigger(requester);
            
            // 【成就触发】检查是否是雨天送伞
            boolean isRaining = requester.level().isRaining();
            if (isRaining) {
                // 获取雨天送伞次数并触发成就
                int rainyCount = getRainyUmbrellaCount(requester);
                rainyCount++;
                setRainyUmbrellaCount(requester, rainyCount);
                CriterionTriggerRegister.RAINY_UMBRELLA_GIFT.get().trigger(requester, rainyCount);
            }
            
            // 【成就触发】互赠伞累计次数
            int mutualCount = getMutualUmbrellaCount(requester, responder);
            mutualCount++;
            setMutualUmbrellaCount(requester, responder, mutualCount);
            CriterionTriggerRegister.MUTUAL_UMBRELLA_GIFT.get().trigger(requester, mutualCount);
            CriterionTriggerRegister.MUTUAL_UMBRELLA_GIFT.get().trigger(responder, mutualCount);

            // 触发共伞浪漫事件
            RomanticEventManager.onSharedUmbrellaEstablished(requester, responder);
        } else {
            requester.sendSystemMessage(Component.literal("§c对方拒绝了你的共伞邀请").withStyle(ChatFormatting.RED));
            responder.sendSystemMessage(Component.literal("§a已拒绝共伞邀请").withStyle(ChatFormatting.GREEN));
        }
    }

    public static void endSharedUmbrella(Player player) {
        endSharedUmbrella(player, "对方离开了，共伞结束");
    }

    public static void endSharedUmbrella(Player player, String message) {
        SharedUmbrellaState state = sharedUmbrellaStates.remove(player.getUUID());
        if (state != null && state.partner != null) {
            sharedUmbrellaStates.remove(state.partner.getUUID());
            state.partner.sendSystemMessage(Component.literal("§c" + message).withStyle(ChatFormatting.RED));
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

    // SharedUmbrellaManager.java - 修复 onPlayerTick 方法

    public static void onPlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        SharedUmbrellaState state = sharedUmbrellaStates.get(player.getUUID());
        if (state == null) return;

        // 计数器从 1 开始，避免共伞建立后立即触发检查
        Integer counter = tickCounters.compute(player.getUUID(), (uuid, val) -> {
            if (val == null) return 1; // 从 1 开始，而不是 0
            return val + 1;
        });

        if (counter % SHARE_CHECK_INTERVAL != 0) return;

        if (!isPartnerValid(state.partner)) {
            endSharedUmbrella(player);
            return;
        }

        double distance = player.distanceTo(state.partner);
        if (distance > MAX_DISTANCE) {
            endSharedUmbrella(player, "距离太远，共伞结束");
            return;
        }

        // 检查伞是否仍然打开 - 检查任意一方是否持有打开的伞
        boolean hasOpen = hasOpenUmbrella(player) || hasOpenUmbrella(state.partner);
        
        if (!hasOpen) {
            // 检查谁丢失了伞
            boolean playerHasUmbrella = player.getMainHandItem().is(ModItems.UMBRELLA.get()) || 
                                       player.getOffhandItem().is(ModItems.UMBRELLA.get());
            boolean partnerHasUmbrella = state.partner.getMainHandItem().is(ModItems.UMBRELLA.get()) || 
                                         state.partner.getOffhandItem().is(ModItems.UMBRELLA.get());
            
            if (!playerHasUmbrella && !partnerHasUmbrella) {
                MCARomanticExpansion.LOGGER.debug("Both players {} and {} no longer holding umbrella",
                        player.getName().getString(), state.partner.getName().getString());
                endSharedUmbrella(player, "伞已丢失，共伞结束");
            } else {
                // 尝试强制打开双方的伞
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
                
                // 重新检查
                hasOpen = hasOpenUmbrella(player) || hasOpenUmbrella(state.partner);
                if (!hasOpen) {
                    MCARomanticExpansion.LOGGER.debug("Umbrella still closed after forcing for {} and {}",
                            player.getName().getString(), state.partner.getName().getString());
                    endSharedUmbrella(player, "伞已合上，共伞结束");
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
    
    // 雨天送伞次数存储
    private static final String RAINY_UMBRELLA_COUNT_KEY = "mcaromanticexpansion.rainy_umbrella_count";
    
    private static int getRainyUmbrellaCount(ServerPlayer player) {
        return player.getPersistentData().getInt(RAINY_UMBRELLA_COUNT_KEY);
    }
    
    private static void setRainyUmbrellaCount(ServerPlayer player, int count) {
        player.getPersistentData().putInt(RAINY_UMBRELLA_COUNT_KEY, count);
    }
    
    // 互赠伞次数存储（key格式: mcaromanticexpansion.mutual_umbrella_count_<partner_uuid>）
    private static String getMutualUmbrellaKey(UUID playerId) {
        return "mcaromanticexpansion.mutual_umbrella_count_" + playerId.toString();
    }
    
    private static int getMutualUmbrellaCount(ServerPlayer player, ServerPlayer partner) {
        String key = getMutualUmbrellaKey(partner.getUUID());
        return player.getPersistentData().getInt(key);
    }
    
    private static void setMutualUmbrellaCount(ServerPlayer player, ServerPlayer partner, int count) {
        String key = getMutualUmbrellaKey(partner.getUUID());
        player.getPersistentData().putInt(key, count);
    }
}