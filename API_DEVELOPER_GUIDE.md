# MCA浪漫扩展 开发者联动文档

**API 版本**: `1.0.0`
**适用游戏版本**: Minecraft 1.21.1 (NeoForge)
**最后更新**: 2026-06-28

---

## 一、简介

MCA浪漫扩展（MCARomanticExpansion）为「Minecraft Comes Alive (MCA)」模组增加了玩家与玩家之间的浪漫互动：好感度系统、求婚结婚、共伞、随机浪漫事件、怀孕育儿等功能。本模组暴露一套完整的公共 API，允许其他模组作者进行联动：读取好感度、查询婚姻状态、监听或取消浪漫事件、注册自定义浪漫事件等。

所有 API 入口位于：
```java
com.xiaoshi2022.mcaromanticexpansion.api.RomanticExpansionAPI
```

---

## 二、前置与依赖配置

### 2.1 前置模组
- **NeoForge** (建议版本 21.1.228+)
- **Minecraft Comes Alive (MCA)** Reborn 版

### 2.2 在你的模组中加入依赖

在你的 `build.gradle` 中添加对 MCA浪漫扩展 的依赖（请根据发布渠道调整）：

```groovy
repositories {
    // 若发布在 CurseMaven：
    maven { url 'https://cursemaven.com' }
}

dependencies {
    // 仅编译期依赖，运行时由玩家安装
    compileOnly fg.deobf('curse.maven:mca-romantic-expansion-xxxxx:yyyyyy')
}
```

在你的 `mods.toml` 中声明可选/必选依赖：

```toml
[[dependencies.mymod]]
modId = "mcaromanticexpansion"
mandatory = false     # true 为必选依赖，false 为可选联动
versionRange = "[1.0.0,)"
ordering = "NONE"
side = "BOTH"
```

为了避免「未安装 MCA浪漫扩展 时类找不到」，建议用反射或 `ModList` 检查后再调用：

```java
public static final String ROMANTIC_EXPANSION_ID = "mcaromanticexpansion";

public static boolean isRomanticExpansionLoaded() {
    return net.neoforged.fml.loading.FMLEnvironment.mods.stream()
            .anyMatch(m -> m.getModId().equals(ROMANTIC_EXPANSION_ID));
}
```

---

## 三、API 入口：RomanticExpansionAPI

`RomanticExpansionAPI` 是调用一切功能的静态入口：

| 方法 | 说明 |
|---|---|
| `getAPIVersion()` | 获取当前 API 版本号，如 `"1.0.0"` |
| `getAffectionManager()` | 获取好感度管理器 `IAffectionManager` |
| `getRelationshipManager()` | 获取关系状态查询器 `IRelationshipManager` |
| `registerCustomEvent(IRomanticEvent)` | 注册自定义浪漫事件 |
| `unregisterCustomEvent(String)` | 取消注册自定义浪漫事件 |
| `getCustomEventIds()` | 获取所有已注册自定义事件的 ID 集合 |
| `getCustomEvents()` | 获取所有已注册自定义事件实例 |
| `triggerRomanticEvent(player, partner, eventId)` | 手动触发指定 ID 的浪漫事件（内置或自定义） |
| `triggerRandomRomanticEvent(player, partner)` | 按权重随机触发一个浪漫事件（内置+自定义池） |
| `sendProposalRequest(proposer, target)` | 等同持戒指向：向 target 发送求婚 GUI |
| `sendSharedUmbrellaRequest(initiator, target)` | 向 target 发送共伞邀请 |
| `isOnCooldown(uuid, type)` / `getRemainingCooldown(uuid, type)` / `clearCooldown(uuid)` | 冷却管理 |

---

## 四、好感度管理（IAffectionManager）

每位玩家对其他玩家都有独立的好感度数值（范围 `-100` 到 `+∞`，`setAffection` 最高限制到 100）。

```java
IAffectionManager am = RomanticExpansionAPI.getAffectionManager();

// 1. 查询：玩家A 对 玩家B 的好感度
int value = am.getAffection(playerA, playerB);

// 2. 增加 / 减少
am.addAffection(playerA, playerB, 10);  // +10
am.addAffection(playerA, playerB, -5);  // -5

// 3. 直接设置（会被限制最大 100）
am.setAffection(playerA, playerB, 80);

// 4. 使用内置交互类型（预设好感加值）
am.handleInteraction(IAffectionManager.InteractionType.KISS, playerA, playerB);
```

内置交互类型与加值：

| 枚举 | 好感加值 | 说明 |
|---|---|---|
| `GIFT` | +5 | 赠送礼物 |
| `BOUQUET` | +8 | 送花束 |
| `SHARED_UMBRELLA` | +1 | 共伞期间 |
| `HUG` | +6 | 拥抱 |
| `WHISPER` | +3 | 悄悄话 |
| `DANCE` | +10 | 跳舞 |
| `KISS` | +15 | 亲吻 |
| `PROPOSAL_ACCEPT` | +20 | 接受求婚 |
| `MARRIAGE` | +30 | 结婚 |

---

## 五、关系状态查询（IRelationshipManager）

用于查询婚姻、怀孕、共伞等状态：

```java
IRelationshipManager rm = RomanticExpansionAPI.getRelationshipManager();

// 婚姻、订婚
boolean married = rm.isMarried(player);          // 已婚
boolean engaged = rm.isEngaged(player);          // 已订婚
Optional<UUID> partnerId = rm.getPartnerUUID(player);  // 配偶/未婚夫 UUID

// 怀孕、共伞
boolean pregnant = rm.isPregnant(player);
boolean inUmbrella = rm.isInSharedUmbrella(player);
Optional<Player> umbrellaPartner = rm.getSharedUmbrellaPartner(player);

// 同性结婚配置（按玩家）
boolean canSameGender = rm.isSameGenderMarriageAllowed(player);
```

---

## 六、浪漫事件系统

### 6.1 手动触发浪漫事件

```java
// 触发一个内置/自定义事件
boolean ok = RomanticExpansionAPI.triggerRomanticEvent(player, partner, "mcaromanticexpansion:heart_to_heart");

// 从池子里按权重随机抽一个（含自定义事件）
boolean fired = RomanticExpansionAPI.triggerRandomRomanticEvent(player, partner);
```

### 6.2 注册自定义浪漫事件

实现 `IRomanticEvent` 接口，然后调用 `registerCustomEvent`：

```java
public class StarlightDanceEvent implements IRomanticEvent {

    // 必须全局唯一，建议用 "你的modid:事件名" 格式
    @Override
    public String id() { return "mymod:starlight_dance"; }

    // 随机抽取权重，越大越容易被抽到。内置参考：分享故事 1.0、轻吻 0.4、告白 0.2
    @Override
    public double weight() { return 0.5; }

    // 触发时双方获得的好感度加成
    @Override
    public int affectionBonus() { return 20; }

    // 在服务端执行，可自由发挥：粒子、药水、成就、物品奖励……
    @Override
    public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
        ServerLevel level = player.serverLevel();
        // 例：星光粒子
        for (int i = 0; i < 100; i++) {
            double x = player.getX() + (Math.random() - 0.5) * 6;
            double y = player.getY() + 2 + Math.random() * 4;
            double z = player.getZ() + (Math.random() - 0.5) * 6;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    x, y, z, 1, 0, 0, 0, 0);
        }
    }

    // 返回 null 则不发送消息
    @Override
    public net.minecraft.network.chat.Component getPlayerMessage() {
        return net.minecraft.network.chat.Component.literal("§d你与伴侣在星光下翩翩起舞！");
    }

    @Override
    public net.minecraft.network.chat.Component getPartnerMessage() {
        return net.minecraft.network.chat.Component.literal("§d你与伴侣在星光下翩翩起舞！");
    }
}
```

在你的 Mod 主类构造函数中注册：

```java
public MyMod() {
    // IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
    // ... 其他注册 ...
    if (isRomanticExpansionLoaded()) {
        RomanticExpansionAPI.registerCustomEvent(new StarlightDanceEvent());
    }
}
```

取消注册：
```java
RomanticExpansionAPI.unregisterCustomEvent("mymod:starlight_dance");
```

---

## 七、监听 / 取消浪漫事件

所有自定义事件均实现 `ICancellableEvent`，可在监听中 `setCanceled(true)` 取消后续操作。事件全部发布在 `NeoForge.EVENT_BUS` 上。

### 7.1 AffectionChangedEvent —— 好感度即将变化时

```java
@SubscribeEvent
public void onAffectionChanged(AffectionChangedEvent event) {
    ServerPlayer changer = event.getEntity();      // 主体玩家
    ServerPlayer target  = event.getTarget();      // 目标玩家
    int oldVal = event.getOldValue();
    int newVal = event.getNewValue();
    ChangeReason reason = event.getReason();       // ADD / SET / INTERACTION / ROMANTIC_EVENT

    // 例：敌对关系时禁止互加好感
    if (newVal > oldVal && reason == ChangeReason.INTERACTION) {
        event.setCanceled(true);  // 取消本次好感度变动
        return;
    }

    // 例：情人节双倍加成（修改 newValue）
    if (isValentinesDay()) {
        event.setNewValue(oldVal + (newVal - oldVal) * 2);
    }
}
```

### 7.2 ProposalSentEvent —— 求婚请求即将发送时

```java
@SubscribeEvent
public void onProposalSent(ProposalSentEvent event) {
    ServerPlayer proposer = event.getProposer();
    ServerPlayer target   = event.getTarget();

    if (!isInSacredChurch(proposer.blockPosition())) {
        proposer.sendSystemMessage(Component.literal("§c必须在神圣教堂内求婚！"));
        event.setCanceled(true);  // 不打开求婚GUI
    }
}
```

### 7.3 ProposalRespondedEvent —— 求婚得到回应（接受/拒绝）

```java
@SubscribeEvent
public void onProposalResponded(ProposalRespondedEvent event) {
    ServerPlayer responder = event.getResponder();   // 做出回应的人
    ServerPlayer proposer  = event.getProposer();
    boolean accepted = event.isAccepted();

    if (accepted) {
        // 例：送成就 / 礼物
        giveProposalAcceptanceGift(proposer, responder);
    }
}
```

### 7.4 MarriageChangedEvent —— 结婚 或 离婚

```java
@SubscribeEvent
public void onMarriageChanged(MarriageChangedEvent event) {
    ServerPlayer playerA = event.getEntity();
    ServerPlayer playerB = event.getSpouse();
    ChangeType type = event.getChangeType();   // MARRIED / DIVORCED

    if (type == ChangeType.MARRIED) {
        grantMarriageAchievement(playerA, playerB);
    } else {
        // 离婚也可以取消哦……
        // event.setCanceled(true);
    }
}
```

### 7.5 RomanticEventTriggeredEvent —— 任意浪漫事件触发时

无论内置事件还是自定义事件，都会触发此事件：

```java
@SubscribeEvent
public void onRomanticEvent(RomanticEventTriggeredEvent event) {
    ServerPlayer player  = event.getEntity();
    ServerPlayer partner = event.getPartner();
    String eventId = event.getEventId();        // 如 "mymod:starlight_dance"
    boolean isCustom = event.isCustomEvent();   // 是否自定义事件
    int affectionBonus = event.getAffectionBonus();

    // 例：给事件加额外奖励
    if ("mcaromanticexpansion:confession".equals(eventId)) {
        firework(player.serverLevel(), player.position());
    }
}
```

### 7.6 SharedUmbrellaRequestEvent / SharedUmbrellaEstablishedEvent

```java
// 共伞请求即将发送（可取消）
@SubscribeEvent
public void onUmbrellaRequest(SharedUmbrellaRequestEvent event) {
    ServerPlayer initiator = event.getInitiator();
    ServerPlayer target    = event.getTarget();
    // if (天气不佳) event.setCanceled(true);
}

// 共伞已成功建立（可取消，取消后将不进入共伞状态）
@SubscribeEvent
public void onUmbrellaEstablished(SharedUmbrellaEstablishedEvent event) {
    ServerPlayer a = event.getPlayerA();
    ServerPlayer b = event.getPlayerB();
}
```

---

## 八、主动操作 API

### 8.1 发起求婚
等同于玩家手持订婚戒指右键另一位玩家：
```java
boolean ok = RomanticExpansionAPI.sendProposalRequest(proposer, target);
// 返回 false 可能原因：冷却中、没戒指、ProposalSentEvent 被取消
```

### 8.2 发起共伞邀请
```java
boolean ok = RomanticExpansionAPI.sendSharedUmbrellaRequest(initiator, target);
```

---

## 九、冷却管理 API

| Type 标识 | 说明 |
|---|---|
| `"proposal"` | 求婚冷却 |
| `"bouquet"`  | 送花束冷却 |
| `"marriage"` | 婚礼冷却 |

```java
UUID playerId = player.getUUID();

boolean onCd = RomanticExpansionAPI.isOnCooldown(playerId, "proposal");
long remainMs = RomanticExpansionAPI.getRemainingCooldown(playerId, "proposal");
RomanticExpansionAPI.clearCooldown(playerId);   // 清除该玩家所有操作冷却
```

---

## 十、完整示例：条件化好感加成 & 自定义星光事件

```java
package com.example.mylovelyaddon;

import com.xiaoshi2022.mcaromanticexpansion.api.IAffectionManager;
import com.xiaoshi2022.mcaromanticexpansion.api.IRomanticEvent;
import com.xiaoshi2022.mcaromanticexpansion.api.RomanticExpansionAPI;
import com.xiaoshi2022.mcaromanticexpansion.api.event.AffectionChangedEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@Mod("mylovelyaddon")
public class MyLovelyAddon {

    public static final String MOD_ID = "mylovelyaddon";

    public MyLovelyAddon() {
        if (isRomanticExpansionLoaded()) {
            // 注册自定义浪漫事件
            RomanticExpansionAPI.registerCustomEvent(new StarlightDanceEvent());
        }
    }

    private static boolean isRomanticExpansionLoaded() {
        return net.neoforged.fml.loading.FMLEnvironment.mods.stream()
                .anyMatch(m -> m.getModId().equals("mcaromanticexpansion"));
    }

    // 注册到 NeoForge 事件总线
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static class ForgeEvents {

        @SubscribeEvent
        public static void onAffection(AffectionChangedEvent event) {
            // 夜晚的浪漫互动：好感度增加效果 +50%
            ServerLevel level = event.getEntity().serverLevel();
            if (level != null && level.isNight()
                    && event.getNewValue() > event.getOldValue()
                    && event.getReason() == AffectionChangedEvent.ChangeReason.INTERACTION) {
                int delta = event.getNewValue() - event.getOldValue();
                event.setNewValue(event.getOldValue() + (int)(delta * 1.5));
            }
        }
    }

    // 自定义浪漫事件
    public static class StarlightDanceEvent implements IRomanticEvent {
        @Override public String id() { return MOD_ID + ":starlight_dance"; }
        @Override public double weight() { return 0.5; }
        @Override public int affectionBonus() { return 20; }

        @Override
        public void triggerEffect(ServerPlayer player, ServerPlayer partner) {
            ServerLevel level = player.serverLevel();
            if (!level.dimensionType().hasSkyLight()) return;  // 只在露天生效
            for (int i = 0; i < 200; i++) {
                double x = (player.getX() + partner.getX()) / 2.0 + (Math.random() - 0.5) * 8;
                double y = Math.max(player.getY(), partner.getY()) + 2 + Math.random() * 5;
                double z = (player.getZ() + partner.getZ()) / 2.0 + (Math.random() - 0.5) * 8;
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
            }
        }

        @Override public Component getPlayerMessage()  { return Component.literal("§d繁星闪烁，你与伴侣共舞一曲！"); }
        @Override public Component getPartnerMessage() { return Component.literal("§d繁星闪烁，你与伴侣共舞一曲！"); }
    }
}
```

---

## 十一、常见问题

**Q1：为什么直接调用 `NeoForge.EVENT_BUS.post(event).isCanceled()` 编译不通过？**
A1：NeoForge 21.1 的 `EventBus.post()` 返回的是事件对象本身（泛型 T），不是 boolean。标准用法：
```java
NeoForge.EVENT_BUS.post(event);
if (event.isCanceled()) { ... }
```

**Q2：如何判断玩家是否已婚到「另一名玩家」而不是 MCA 的村民？**
A2：`IRelationshipManager#isMarried` / `getPartnerUUID` 底层使用 MCA 的 `RelationshipState.MARRIED_TO_PLAYER`，因此只会匹配玩家-玩家婚姻。

**Q3：自定义浪漫事件和内置事件的触发关系？**
A3：自定义事件与内置事件共同参与权重随机；`triggerRomanticEvent` 可直接按 ID 触发两者。

---

祝你在 MCA 世界中创造更多甜蜜的联动功能！
