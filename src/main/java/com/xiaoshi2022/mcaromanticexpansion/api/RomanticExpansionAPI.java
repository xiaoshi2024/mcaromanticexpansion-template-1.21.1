package com.xiaoshi2022.mcaromanticexpansion.api;

import com.xiaoshi2022.mcaromanticexpansion.api.event.*;
import com.xiaoshi2022.mcaromanticexpansion.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;

/**
 * MCARomanticExpansion 公共 API 入口类。
 * <p>
 * 其他模组作者可以通过此类的静态方法与浪漫扩展进行联动。
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 1. 增加两位玩家之间的好感度
 * RomanticExpansionAPI.getAffectionManager().addAffection(playerA, playerB, 10);
 *
 * // 2. 手动触发一个浪漫事件
 * RomanticExpansionAPI.triggerRomanticEvent(playerA, playerB, "heart_to_heart");
 *
 * // 3. 注册自定义浪漫事件
 * RomanticExpansionAPI.registerCustomEvent(new MyCustomEvent());
 *
 * // 4. 监听浪漫事件（在Mod主类构造函数中注册）
 * NeoForge.EVENT_BUS.addListener(this::onRomanticEvent);
 *
 * // 5. 检查玩家婚姻状态
 * boolean married = RomanticExpansionAPI.getRelationshipManager().isMarried(player);
 * }</pre>
 *
 * @since API 1.0.0
 */
public final class RomanticExpansionAPI {

    private RomanticExpansionAPI() {}

    private static final String API_VERSION = "1.0.0";
    private static final Map<String, IRomanticEvent> customEvents = new LinkedHashMap<>();
    private static IAffectionManager affectionManagerImpl;
    private static IRelationshipManager relationshipManagerImpl;

    /**
     * @return 当前API版本号
     */
    public static String getAPIVersion() {
        return API_VERSION;
    }

    // ======================== 好感度管理 API ========================

    /**
     * 获取好感度管理器实例。
     * 用于读取、增加、设置玩家之间的好感度。
     */
    public static IAffectionManager getAffectionManager() {
        if (affectionManagerImpl == null) {
            affectionManagerImpl = new IAffectionManager() {
                @Override
                public int getAffection(Player player, Player target) {
                    return AffectionManager.getAffection(player, target);
                }

                @Override
                public void addAffection(Player player, Player target, int amount) {
                    AffectionManager.addAffection(player, target, amount);
                }

                @Override
                public void setAffection(Player player, Player target, int value) {
                    AffectionManager.setAffection(player, target, value);
                }

                @Override
                public void handleInteraction(IAffectionManager.InteractionType type, Player player, Player target) {
                    AffectionManager.InteractionType internalType =
                            AffectionManager.InteractionType.valueOf(type.name());
                    AffectionManager.handleInteraction(internalType, player, target);
                }
            };
        }
        return affectionManagerImpl;
    }

    // ======================== 关系状态查询 API ========================

    /**
     * 获取关系状态管理器实例。
     * 用于查询玩家的婚姻、订婚、怀孕等状态。
     */
    public static IRelationshipManager getRelationshipManager() {
        if (relationshipManagerImpl == null) {
            relationshipManagerImpl = new IRelationshipManager() {
                @Override
                public boolean isMarried(Player player) {
                    if (player instanceof ServerPlayer sp) {
                        var data = net.conczin.mca.server.world.data.PlayerSaveData.get(sp);
                        return data.getRelationshipState() == net.conczin.mca.entity.ai.relationship.RelationshipState.MARRIED_TO_PLAYER;
                    }
                    return false;
                }

                @Override
                public boolean isEngaged(Player player) {
                    if (player instanceof ServerPlayer sp) {
                        var data = net.conczin.mca.server.world.data.PlayerSaveData.get(sp);
                        return data.getRelationshipState() == net.conczin.mca.entity.ai.relationship.RelationshipState.ENGAGED_TO_PLAYER;
                    }
                    return false;
                }

                @Override
                public Optional<UUID> getPartnerUUID(Player player) {
                    if (player instanceof ServerPlayer sp) {
                        var data = net.conczin.mca.server.world.data.PlayerSaveData.get(sp);
                        return data.getPartnerUUID();
                    }
                    return Optional.empty();
                }

                @Override
                public boolean isPregnant(Player player) {
                    PregnancyManager.PregnancyData data = PregnancyManager.getPregnancyData(player.getUUID());
                    return data != null && data.isActive();
                }

                @Override
                public boolean isInSharedUmbrella(Player player) {
                    return SharedUmbrellaManager.isInSharedUmbrella(player);
                }

                @Override
                public Optional<Player> getSharedUmbrellaPartner(Player player) {
                    return Optional.ofNullable(SharedUmbrellaManager.getSharedPartner(player));
                }

                @Override
                public boolean isSameGenderMarriageAllowed(Player player) {
                    if (player instanceof ServerPlayer sp) {
                        return MarriageConfig.isSameGenderMarriageAllowed(sp);
                    }
                    return MarriageConfig.isGlobalAllowSameGenderMarriage();
                }
            };
        }
        return relationshipManagerImpl;
    }

    // ======================== 浪漫事件 API ========================

    /**
     * 注册一个自定义浪漫事件。
     * 注册后该事件会出现在随机触发池中，并可通过 {@link #triggerRomanticEvent(ServerPlayer, ServerPlayer, String)} 手动触发。
     *
     * @param event 自定义事件实现
     * @return 如果注册成功返回true；如果已有相同ID的事件则返回false
     */
    public static boolean registerCustomEvent(IRomanticEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
        Objects.requireNonNull(event.id(), "event.id() cannot be null");
        if (customEvents.containsKey(event.id())) {
            return false;
        }
        customEvents.put(event.id(), event);
        return true;
    }

    /**
     * 取消注册一个自定义浪漫事件。
     *
     * @param eventId 事件ID
     * @return 如果找到并移除返回true
     */
    public static boolean unregisterCustomEvent(String eventId) {
        return customEvents.remove(eventId) != null;
    }

    /**
     * 获取所有已注册的自定义事件ID。
     */
    public static Set<String> getCustomEventIds() {
        return Collections.unmodifiableSet(customEvents.keySet());
    }

    static Collection<IRomanticEvent> getCustomEvents() {
        return customEvents.values();
    }

    /**
     * 手动触发一个浪漫事件（可以是内置事件或自定义事件）。
     * <p>
     * 会同时触发 {@link RomanticEventTriggeredEvent} 到 NeoForge 事件总线。
     *
     * @param player   玩家A
     * @param partner  玩家B
     * @param eventId  事件ID（内置ID或自定义注册的ID）
     * @return 如果事件存在并成功触发返回true
     */
    public static boolean triggerRomanticEvent(ServerPlayer player, ServerPlayer partner, String eventId) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(partner, "partner cannot be null");
        Objects.requireNonNull(eventId, "eventId cannot be null");

        IRomanticEvent custom = customEvents.get(eventId);
        if (custom != null) {
            triggerCustomEvent(custom, player, partner);
            return true;
        }

        for (RomanticEventManager.RomanticEvent builtin : RomanticEventManager.RomanticEvent.values()) {
            if (builtin.id().equals(eventId)) {
                RomanticEventManager.triggerEventById(player, partner, eventId);
                return true;
            }
        }
        return false;
    }

    /**
     * 触发两位玩家之间的随机浪漫事件（同共伞时的机制）。
     * <p>
     * 会同时触发 {@link RomanticEventTriggeredEvent}。
     *
     * @return 如果随机到并触发了事件返回true
     */
    public static boolean triggerRandomRomanticEvent(ServerPlayer player, ServerPlayer partner) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(partner);

        List<IRomanticEvent> pool = new ArrayList<>();
        for (RomanticEventManager.RomanticEvent e : RomanticEventManager.RomanticEvent.values()) {
            pool.add(wrapBuiltin(e));
        }
        pool.addAll(customEvents.values());

        if (pool.isEmpty()) return false;

        double totalWeight = pool.stream().mapToDouble(IRomanticEvent::weight).sum();
        double rnd = new Random().nextDouble() * totalWeight;
        double acc = 0;
        for (IRomanticEvent ev : pool) {
            acc += ev.weight();
            if (rnd <= acc) {
                if (ev instanceof BuiltinWrapper bw) {
                    RomanticEventManager.triggerEvent(bw.delegate, player, partner);
                } else {
                    triggerCustomEvent(ev, player, partner);
                }
                return true;
            }
        }
        return false;
    }

    // ======================== 求婚/结婚操作 API ========================

    /**
     * 向目标玩家发送求婚GUI请求（等同于手持订婚戒指右键的效果）。
     * <p>
     * 会触发 {@link ProposalSentEvent}。
     *
     * @param proposer 求婚者（必须有订婚戒指且不在冷却中）
     * @param target   被求婚者
     * @return 如果请求被成功发送返回true；否则（冷却中/缺少戒指等）返回false
     */
    public static boolean sendProposalRequest(ServerPlayer proposer, ServerPlayer target) {
        Objects.requireNonNull(proposer);
        Objects.requireNonNull(target);
        if (proposer == target) return false;

        if (CooldownManager.isOnCooldown(proposer.getUUID(), "proposal")) {
            return false;
        }
        if (!hasEngagementRing(proposer)) {
            return false;
        }

        ProposalSentEvent event = new ProposalSentEvent(proposer, target);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return false;
        }

        CooldownManager.setCooldown(proposer.getUUID(), "proposal");
        sendPacket(target, proposer);
        return true;
    }

    // ======================== 共伞 API ========================

    /**
     * 向目标玩家发送共伞邀请。
     * <p>
     * 会触发 {@link SharedUmbrellaRequestEvent}。
     */
    public static boolean sendSharedUmbrellaRequest(ServerPlayer initiator, ServerPlayer target) {
        SharedUmbrellaRequestEvent event = new SharedUmbrellaRequestEvent(initiator, target);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return false;
        }
        return SharedUmbrellaManager.sendRequest(initiator, target);
    }

    // ======================== 冷却管理 API ========================

    /**
     * 检查玩家是否处于指定操作的冷却中。
     *
     * @param playerId 玩家UUID
     * @param type     操作类型："proposal" | "bouquet" | "marriage"
     */
    public static boolean isOnCooldown(UUID playerId, String type) {
        return CooldownManager.isOnCooldown(playerId, type);
    }

    /**
     * 获取冷却剩余毫秒数。
     */
    public static long getRemainingCooldown(UUID playerId, String type) {
        return CooldownManager.getRemainingCooldown(playerId, type);
    }

    /**
     * 清除玩家所有操作冷却。
     */
    public static void clearCooldown(UUID playerId) {
        CooldownManager.clearCooldown(playerId);
    }

    // ======================== 内部辅助 ========================

    private static void triggerCustomEvent(IRomanticEvent event, ServerPlayer player, ServerPlayer partner) {
        RomanticEventTriggeredEvent forgeEvent = new RomanticEventTriggeredEvent(
                player, partner, event.id(), true, event.affectionBonus()
        );
        if (NeoForge.EVENT_BUS.post(forgeEvent).isCanceled()) {
            return;
        }
        event.triggerEffect(player, partner);
        if (event.getPlayerMessage() != null) {
            player.sendSystemMessage(event.getPlayerMessage());
        }
        if (event.getPartnerMessage() != null) {
            partner.sendSystemMessage(event.getPartnerMessage());
        }
        AffectionManager.addAffection(player, partner, event.affectionBonus());
        AffectionManager.addAffection(partner, player, event.affectionBonus());
    }

    private static boolean hasEngagementRing(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof net.conczin.mca.item.EngagementRingItem) {
                return true;
            }
        }
        return false;
    }

    private static void sendPacket(ServerPlayer target, ServerPlayer proposer) {
        var packet = new com.xiaoshi2022.mcaromanticexpansion.network.OpenProposalGUIPacket(
                proposer.getUUID(), proposer.getName().getString()
        );
        target.connection.send(packet);
    }

    private static IRomanticEvent wrapBuiltin(RomanticEventManager.RomanticEvent e) {
        return new IRomanticEvent() {
            @Override public String id() { return e.id(); }
            @Override public double weight() { return e.weight(); }
            @Override public int affectionBonus() { return e.affectionBonus(); }
            @Override public void triggerEffect(ServerPlayer player, ServerPlayer partner) { e.triggerEffect(player, partner); }
            @Override public net.minecraft.network.chat.Component getPlayerMessage() { return e.getPlayerMessage(); }
            @Override public net.minecraft.network.chat.Component getPartnerMessage() { return e.getPartnerMessage(); }
        };
    }

    private record BuiltinWrapper(RomanticEventManager.RomanticEvent delegate) implements IRomanticEvent {
        @Override public String id() { return delegate.id(); }
        @Override public double weight() { return delegate.weight(); }
        @Override public int affectionBonus() { return delegate.affectionBonus(); }
        @Override public void triggerEffect(ServerPlayer player, ServerPlayer partner) { delegate.triggerEffect(player, partner); }
    }
}
