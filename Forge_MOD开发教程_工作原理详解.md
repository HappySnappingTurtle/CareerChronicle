# Minecraft Forge MOD 开发工作原理详解

> 本文档以 Career Chronicle MOD（Forge 1.20.1）为实例，说明 Minecraft 和 Forge 的工作机制以及 MOD 代码如何介入。

---

## 一、整体架构总览

```
                    玩家启动 Minecraft
                          |
                    Forge 引导加载器
                          |
              +-----------|------------+
              |                        |
         发现 MOD JAR              加载原版 MC
         (mods.toml)               (minecraft)
              |                        |
         创建 @Mod 实例            初始化世界/实体
              |                        |
         注册阶段 (MOD Bus)        运行阶段 (FORGE Bus)
         - 物品注册                - 玩家事件
         - 实体注册                - 世界事件
         - 配置注册                - Tick 事件
         - 能力注册                - 渲染事件
         - 网络注册
              |                        |
              +-----------|------------+
                          |
                   游戏运行中
                   MOD 代码通过事件和注册
                   与原版行为交织运行
```

---

## 二、Forge 的两条事件总线

这是理解 Forge MOD 最核心的概念。Forge 有两条独立的事件总线：

### 2.1 MOD Bus（模组总线）

**用途**：MOD 加载阶段的生命周期事件。只在游戏启动时触发一次。

```
游戏启动
  |
  ├─ RegisterCapabilitiesEvent  ← 注册能力类型
  ├─ RegisterKeyMappingsEvent   ← 注册按键绑定（仅客户端）
  ├─ EntityRenderersEvent       ← 注册实体渲染器（仅客户端）
  ├─ FMLCommonSetupEvent        ← 通用初始化（注册网络通道）
  └─ FMLClientSetupEvent        ← 客户端初始化
```

**我们的代码如何接入**：

```java
// 方式1：在构造函数中手动注册监听器
@Mod("careerchronicle")
public class CareerChronicleMod {
    public CareerChronicleMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();      // 获取 MOD Bus
        CareerItems.ITEMS.register(modEventBus);                // 物品注册
        CareerEntities.ENTITY_TYPES.register(modEventBus);      // 实体注册
        modEventBus.addListener(this::commonSetup);             // 通用初始化
    }
}

// 方式2：用注解自动注册（推荐用于静态方法）
@Mod.EventBusSubscriber(modid = "careerchronicle", bus = Bus.MOD)  // ← 指定 MOD Bus
public class CareerCapabilityEvents {
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ICareerData.class);  // 告诉 Forge 我们有一个自定义能力
    }
}
```

### 2.2 FORGE Bus（游戏事件总线）

**用途**：游戏运行时的实时事件。每帧/每 Tick/每个动作都会触发。

```
游戏运行中（每时每刻）
  |
  ├─ TickEvent.PlayerTickEvent        ← 每 Tick 触发（20次/秒）
  ├─ PlayerEvent.PlayerLoggedInEvent  ← 玩家登录
  ├─ PlayerEvent.Clone               ← 玩家死亡重生/跨维度
  ├─ AttachCapabilitiesEvent          ← 实体创建时附加能力
  ├─ LivingDeathEvent                 ← 生物死亡
  ├─ ProjectileImpactEvent            ← 投射物命中
  ├─ RenderGuiOverlayEvent           ← 每帧渲染 HUD
  ├─ AddReloadListenerEvent           ← 数据包加载
  └─ ServerStartingEvent              ← 服务器启动
```

**我们的代码如何接入**：

```java
// 默认不指定 bus = 就是 FORGE Bus
@Mod.EventBusSubscriber(modid = "careerchronicle")
public class CareerPlayerEvents {

    @SubscribeEvent  // 每创建一个玩家实体时触发
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            // 给玩家附加我们的职业数据
            event.addCapability(CareerDataCapability.ID, new CareerDataProvider());
        }
    }

    @SubscribeEvent  // 每秒触发 20 次
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 在这里更新冷却、回复法力、应用种族被动等
    }
}
```

### 两条总线对比表

| 特征 | MOD Bus | FORGE Bus |
|---|---|---|
| 触发时机 | 游戏启动时（一次性） | 游戏运行中（持续） |
| 用途 | 注册物品/实体/能力/网络 | 处理玩家/世界/渲染事件 |
| 注解指定 | `bus = Bus.MOD` | 不指定（默认） |
| 典型事件 | `FMLCommonSetupEvent` | `TickEvent.PlayerTickEvent` |
| 客户端限定 | `value = Dist.CLIENT` | `value = Dist.CLIENT` |

---

## 三、DeferredRegister — 物品/实体注册机制

Minecraft 的所有物品、方块、实体类型都存在一个全局 Registry（注册表）。Forge 提供 `DeferredRegister` 来安全地在启动阶段注册自定义内容。

### 流程图

```
CareerChronicleMod 构造函数
        |
        v
CareerItems.ITEMS.register(modEventBus)  ← 告诉 Forge "我有东西要注册"
        |
        v
  Forge 启动阶段自动调用
        |
        v
  每个 ITEMS.register("name", supplier) 被执行
        |
        v
  物品被写入 Minecraft 的全局物品注册表
        |
        v
  游戏中可以通过 /give @s careerchronicle:ember_staff 获得
```

### 代码实例

```java
public class CareerItems {
    // 1. 创建一个延迟注册器，关联到我们的 MOD ID
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "careerchronicle");

    // 2. 注册一个物品（此时物品还没创建，只是预约）
    public static final RegistryObject<Item> EMBER_STAFF = ITEMS.register(
            "ember_staff",                                    // 注册名
            () -> new SkillWeaponItem(                       // 物品工厂（延迟创建）
                    new Item.Properties().stacksTo(1).durability(192),
                    skill("fireball")
            )
    );

    // 3. 在主类构造函数中：
    //    CareerItems.ITEMS.register(modEventBus);
    //    这一行把预约提交给 Forge，Forge 在正确时机真正创建物品
}
```

**为什么要"延迟"**：Minecraft 的注册表有严格的时序要求。如果你在错误的时间点创建物品（比如在类加载时），会导致崩溃。`DeferredRegister` 确保在 Forge 准备好后才创建。

---

## 四、Capability 系统 — 给玩家附加自定义数据

Minecraft 原版的 Player 类没有"职业等级"、"技能列表"这些字段。Forge 的 Capability 系统让我们能**无侵入地**给任何实体附加自定义数据。

### 流程图

```
玩家实体被创建（登录/重生）
        |
        v
Forge 触发 AttachCapabilitiesEvent
        |
        v
我们的代码监听这个事件：
  if (entity instanceof Player) {
      event.addCapability(ID, new CareerDataProvider());
  }
        |
        v
CareerDataProvider 内部持有 CareerData 对象
  ├─ CareerData 包含：race, level, xp, attributes, skills, loadout...
  ├─ 实现 serializeNBT() → 存入存档
  └─ 实现 deserializeNBT() → 从存档读取
        |
        v
之后任何地方都可以读取：
  CareerDataAccess.get(player).ifPresent(data -> {
      int level = data.getCareerLevel();
      data.addCareerXp(100);
  });
```

### 代码实例

```java
// 1. 定义数据接口
public interface ICareerData {
    ResourceLocation getRace();
    void setRace(ResourceLocation race);
    int getCareerLevel();
    // ...更多方法
}

// 2. 实现数据类（包含 NBT 序列化）
public class CareerData implements ICareerData {
    private ResourceLocation race = UNSELECTED_RACE;
    private int careerLevel = 1;

    // 保存到存档
    public CompoundTag serializePersistentData() {
        CompoundTag tag = new CompoundTag();
        tag.putString("race", race.toString());
        tag.putInt("careerLevel", careerLevel);
        return tag;
    }

    // 从存档读取
    public void deserializePersistentData(CompoundTag tag) {
        race = ResourceLocation.tryParse(tag.getString("race"));
        careerLevel = tag.getInt("careerLevel");
    }
}

// 3. 附加到玩家（通过事件）
@SubscribeEvent
public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
    if (event.getObject() instanceof Player) {
        event.addCapability(ID, new CareerDataProvider());
    }
}

// 4. 玩家死亡时复制数据（否则重生后数据丢失）
@SubscribeEvent
public static void onPlayerClone(PlayerEvent.Clone event) {
    // 把旧玩家的数据复制到新玩家
    CareerDataAccess.get(event.getOriginal()).ifPresent(oldData ->
        CareerDataAccess.get(event.getEntity()).ifPresent(newData ->
            newData.copyPersistentFrom(oldData)));
}
```

---

## 五、网络通信 — 客户端与服务端的数据同步

Minecraft 是客户端-服务端架构。即使是单人游戏，也有一个内置服务端在运行。所有游戏逻辑（伤害、经验、技能效果）都在服务端执行，客户端只负责显示。

### 流程图：玩家释放技能

```
客户端                                    服务端
  |                                         |
  | 玩家按下 Z 键                            |
  |                                         |
  v                                         |
CareerClientEvents.onClientTick()           |
  → USE_SKILL_1.consumeClick()              |
  → 发送 C2SUseSkillPacket ──────────────→  |
                                            v
                                  C2SUseSkillPacket.handle()
                                    → CareerSkillService.useSkill()
                                      → 校验：已解锁？冷却？资源？装备？
                                      → SkillExecutorRegistry.execute()
                                        → 造成伤害、应用效果
                                      → 扣除资源、设置冷却
                                      → CareerDataAccess.sync(player)
                                            |
                                            v
                              S2CCareerSnapshotPacket ──────→ 客户端
                                                              |
                                                              v
                                                  ClientCareerData.update()
                                                    → HUD 刷新冷却显示
                                                    → 技能槽状态更新
                                            |
                                            v
                              S2CPlaySkillFxPacket ──────→ 客户端
                                                              |
                                                              v
                                                  SkillFxRenderer.play()
                                                    → 播放粒子特效
                                                    → 播放音效
```

### 代码实例

```java
// 注册网络通道
public class NetworkHandler {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("careerchronicle", "main"), ...);

    public static void register() {
        // 注册客户端→服务端的包
        CHANNEL.registerMessage(id++,
                C2SUseSkillPacket.class,         // 包类
                C2SUseSkillPacket::encode,        // 序列化
                C2SUseSkillPacket::decode,        // 反序列化
                C2SUseSkillPacket::handle,        // 处理逻辑
                PLAY_TO_SERVER);                  // 方向

        // 注册服务端→客户端的包
        CHANNEL.registerMessage(id++,
                S2CCareerSnapshotPacket.class,
                ...
                PLAY_TO_CLIENT);
    }
}

// 客户端发送
CHANNEL.sendToServer(new C2SUseSkillPacket(skillId));

// 服务端广播
CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
        new S2CPlaySkillFxPacket(skillId, "cast", origin, target));
```

---

## 六、数据驱动 — JSON 数据包系统

Minecraft 1.13+ 引入了数据包系统。我们利用这个机制实现"不改代码就能修改游戏内容"。

### 流程图

```
游戏启动 / 执行 /reload 命令
        |
        v
Forge 触发 AddReloadListenerEvent
        |
        v
我们注册 CareerDataReloadListener
        |
        v
Minecraft 扫描 data/careerchronicle/careerchronicle/ 目录
        |
        ├─ races/human.json      → 解析为 RaceDef
        ├─ races/elf.json        → 解析为 RaceDef
        ├─ classes/fire_mage.json → 解析为 ClassDef
        ├─ skills/fireball.json  → 解析为 SkillDef
        ├─ fusions/fire_mage_archer_flame_arrow.json → FusionDef
        └─ ...
        |
        v
CareerDataReloadListener.apply()
  → 解析所有 JSON 为 Java 对象
  → 交叉验证引用完整性
  → 构建 RegistrySnapshot（不可变快照）
  → 原子替换到 CareerRegistry
        |
        v
如果解析失败 → 保留旧 Registry，日志报错
如果成功 → 在线玩家重新计算技能解锁
```

### 代码实例

```java
// 1. 数据目录结构
// data/careerchronicle/careerchronicle/
//   ├─ races/human.json
//   ├─ classes/fire_mage.json
//   ├─ skills/fireball.json
//   └─ fusions/fire_mage_archer_flame_arrow.json

// 2. JSON 格式示例 (skills/fireball.json)
{
    "display_key": "careerchronicle.skill.fireball",
    "type": "active",
    "resource": "mana",
    "resource_cost": 18,
    "executor": "careerchronicle:fireball",
    "cooldown_ticks": 80,
    "requirements": {
        "equipment_tags": ["careerchronicle:arcane_focus"]
    }
}

// 3. 监听数据加载事件
@SubscribeEvent
public static void onAddReloadListener(AddReloadListenerEvent event) {
    event.addListener(new CareerDataReloadListener());
}

// 4. 继承 Minecraft 的 JSON 资源加载器
public class CareerDataReloadListener extends SimpleJsonResourceReloadListener {
    public CareerDataReloadListener() {
        super(new Gson(), "careerchronicle");
        // "careerchronicle" 对应目录 data/<namespace>/careerchronicle/
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonElements, ...) {
        // 这里接收到的 jsonElements 就是所有 JSON 文件
        // key = 文件路径（如 careerchronicle:races/human）
        // value = 解析后的 JSON 对象
    }
}
```

---

## 七、客户端渲染 — HUD 和 GUI

### 7.1 HUD 叠加层（不打断游戏的信息显示）

```
每一帧渲染
    |
    v
Forge 触发 RenderGuiOverlayEvent.Post
    |
    v
我们检查：是否是 HOTBAR 之后的渲染阶段？
    |
    v
是 → 渲染职业经验条、技能槽、资源条
    |
    └─ graphics.fill(x, y, w, h, color)    ← 画矩形
    └─ graphics.drawString(font, text, ...) ← 画文字
    └─ graphics.blit(texture, ...)          ← 画图标
```

```java
@SubscribeEvent
public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
    // 只在 HOTBAR 渲染完成后画我们的 HUD
    if (!VanillaGuiOverlay.HOTBAR.id().equals(event.getOverlay().id())) return;

    int width = event.getWindow().getGuiScaledWidth();   // 屏幕逻辑宽度
    int height = event.getWindow().getGuiScaledHeight();  // 屏幕逻辑高度
    renderCareerHud(event.getGuiGraphics(), width, height);
}
```

### 7.2 GUI 界面（全屏菜单）

```
玩家按 C 键 / 右键职业手册
    |
    v
客户端创建 Screen 实例
    Minecraft.getInstance().setScreen(new CareerScreen())
    |
    v
Screen.init() 被调用
    → 创建按钮（Button.builder(...)）
    → 注册为可渲染组件（addRenderableWidget）
    |
    v
每帧调用 Screen.render()
    → 画背景面板
    → 画文字/图标
    → 父类 render() 会自动渲染按钮
    |
    v
玩家点击按钮
    → Button 回调触发
    → 发送 C2S 网络包到服务端
    → 服务端处理后同步快照回客户端
```

---

## 八、完整的技能释放流程（端到端）

以"玩家按 Z 释放火球术"为例：

```
┌─────────────────── 客户端 ───────────────────┐
│                                               │
│  1. CareerClientEvents.onClientTick()         │
│     → USE_SKILL_1.consumeClick()              │
│     → skillId = snapshot.skillLoadout[0]      │
│        = "careerchronicle:fireball"           │
│                                               │
│  2. NetworkHandler.CHANNEL.sendToServer(      │
│        new C2SUseSkillPacket(skillId))         │
│                                               │
│     ──────── 网络包发送 ────────               │
│                                               │
└───────────────────────────────────────────────┘
                    |
                    v
┌─────────────────── 服务端 ───────────────────┐
│                                               │
│  3. C2SUseSkillPacket.handle()                │
│     → CareerSkillService.useSkill(player, id) │
│                                               │
│  4. 校验链：                                   │
│     ✓ 玩家存活？                               │
│     ✓ 技能已解锁？                             │
│     ✓ 冷却结束？                               │
│     ✓ 类型可释放？(active/fusion/hidden)       │
│     ✓ 持有正确装备？(arcane_focus tag)          │
│     ✓ 法力充足？(≥18 mana)                     │
│                                               │
│  5. SkillExecutorRegistry.execute()           │
│     → fireball(player)                        │
│       → 创建 CareerProjectileEntity           │
│       → 设置 skillId=fireball, damage=6       │
│       → entity.shoot(方向, 速度)               │
│       → level.addFreshEntity(projectile)      │
│                                               │
│  6. 扣除资源 & 设置冷却                        │
│     → data.consumeResource("mana", 18)        │
│     → data.setCooldown("fireball", 80 ticks)  │
│     → CareerDataAccess.sync(player)           │
│       → 发送 S2CCareerSnapshotPacket          │
│                                               │
│  7. 发送特效包                                 │
│     → NetworkHandler.playSkillFx(             │
│         player, "fireball", "cast",           │
│         origin, target)                       │
│     → 发送 S2CPlaySkillFxPacket               │
│                                               │
└───────────────────────────────────────────────┘
                    |
                    v
┌─────────────────── 客户端 ───────────────────┐
│                                               │
│  8. 收到 S2CCareerSnapshotPacket              │
│     → ClientCareerData 更新本地缓存            │
│     → HUD 显示冷却倒计时 (4秒)                │
│     → 法力条减少                               │
│                                               │
│  9. 收到 S2CPlaySkillFxPacket                 │
│     → SkillFxRenderer.play("fireball", ...)   │
│     → 播放火焰粒子                             │
│     → 播放音效 SoundEvents.FIRECHARGE_USE     │
│                                               │
│  10. 火球投射物在世界中飞行                     │
│      → CareerProjectileRenderer 渲染火球贴图   │
│      → tick() 产生火焰尾迹粒子                 │
│                                               │
│  11. 火球命中目标                              │
│      → onHitEntity() 造成 6 点伤害             │
│      → onHit() 播放命中特效 + discard()       │
│                                               │
└───────────────────────────────────────────────┘
```

---

## 九、文件结构与 Minecraft 的对应关系

```
src/main/
├── java/com/hongyuwu/careerchronicle/    ← Java 代码
│   ├── CareerChronicleMod.java           ← @Mod 入口，Forge 发现并实例化这个类
│   ├── career/                           ← 职业逻辑（纯计算，不依赖 MC API）
│   ├── client/                           ← 客户端专用（GUI、HUD、特效渲染）
│   ├── command/                          ← /career 命令
│   ├── config/                           ← 配置系统
│   ├── data/                             ← 数据驱动（JSON 解析、Registry）
│   ├── item/                             ← 自定义物品
│   ├── network/                          ← 网络包（C2S、S2C）
│   ├── player/                           ← 玩家数据（Capability、NBT）
│   ├── registry/                         ← 物品/实体注册
│   ├── skill/                            ← 技能系统（执行器、装配）
│   └── world/entity/                     ← 自定义实体（投射物）
│
├── resources/
│   ├── META-INF/mods.toml                ← MOD 元数据（Forge 读取这个文件发现 MOD）
│   ├── pack.mcmeta                       ← 资源包格式声明
│   ├── assets/careerchronicle/           ← 客户端资源
│   │   ├── lang/zh_cn.json               ← 中文翻译（key→显示文本）
│   │   ├── lang/en_us.json               ← 英文翻译
│   │   ├── textures/gui/skill/*.png      ← 技能图标
│   │   ├── textures/gui/class/*.png      ← 职业图标
│   │   ├── textures/gui/race/*.png       ← 种族图标
│   │   ├── textures/item/*.png           ← 物品贴图
│   │   ├── textures/entity/              ← 实体贴图（投射物）
│   │   └── models/item/*.json            ← 物品 3D 模型定义
│   │
│   └── data/careerchronicle/             ← 服务端数据
│       ├── careerchronicle/              ← 自定义数据目录
│       │   ├── races/*.json              ← 种族定义
│       │   ├── classes/*.json            ← 职业定义
│       │   ├── skills/*.json             ← 技能定义
│       │   ├── fusions/*.json            ← 融合规则
│       │   ├── hidden_unlocks/*.json     ← 隐藏职业条件
│       │   └── xp_sources/*.json         ← 经验来源
│       ├── recipes/*.json                ← 合成配方（Minecraft 原生格式）
│       └── tags/items/*.json             ← 物品标签（装备分类）
```

---

## 十、MOD 加载的完整生命周期

```
时间线 ──────────────────────────────────────────────────→

[1] Forge 发现 JAR
    └─ 读取 META-INF/mods.toml
    └─ 找到 modId="careerchronicle"

[2] 实例化 @Mod 类
    └─ new CareerChronicleMod(context)
    └─ DeferredRegister 提交注册请求
    └─ 配置文件初始化

[3] MOD Bus 事件阶段
    ├─ RegisterCapabilitiesEvent → 注册 ICareerData
    ├─ RegisterKeyMappingsEvent  → 注册 C/Z/X/V/B 按键
    ├─ EntityRenderersEvent      → 注册投射物渲染器
    └─ FMLCommonSetupEvent       → 注册网络通道

[4] 物品/实体注册完成
    └─ 所有 RegistryObject 现在可以 .get()

[5] 世界加载
    ├─ AddReloadListenerEvent    → 注册 JSON 数据加载器
    ├─ ServerStartingEvent       → 服务端启动日志
    └─ 数据包加载完成             → 128 个 JSON 解析入 Registry

[6] 玩家加入
    ├─ AttachCapabilitiesEvent   → 给玩家附加 CareerData
    ├─ PlayerLoggedInEvent       → 同步数据 + 弹出种族选择
    └─ TickEvent.PlayerTickEvent → 持续：冷却、资源回复、被动效果

[7] 游戏运行中
    ├─ 按键触发 → C2S 包 → 服务端处理 → S2C 包 → 客户端更新
    ├─ 渲染事件 → HUD/GUI 更新
    └─ /reload  → 数据包热重载，不重启
```

---

## 十一、关键概念速查表

| 概念 | Forge API | 我们的用法 |
|---|---|---|
| 注册物品 | `DeferredRegister<Item>` | 38 个物品（武器、材料、手册） |
| 注册实体 | `DeferredRegister<EntityType>` | 1 个自定义投射物 |
| 附加数据 | `Capability` + `AttachCapabilitiesEvent` | 玩家职业数据 |
| 存档持久化 | `ICapabilitySerializable<CompoundTag>` | NBT 自动存取 |
| 网络同步 | `SimpleChannel` + C2S/S2C Packet | 8 个网络包 |
| 客户端 GUI | `Screen` + `GuiGraphics` | 职业界面、种族选择 |
| HUD 渲染 | `RenderGuiOverlayEvent` | 经验条、技能栏 |
| 数据驱动 | `SimpleJsonResourceReloadListener` | 172 个 JSON 定义 |
| 配置系统 | `ForgeConfigSpec` | 5 个可配置项 |
| 按键绑定 | `KeyMapping` + `RegisterKeyMappingsEvent` | C/Z/X/V/B |
| 合成配方 | `data/recipes/*.json` | 25 个配方 |
| 物品标签 | `data/tags/items/*.json` | 4 个装备分类标签 |

---

## 十二、常见问题

### Q: 为什么不能在客户端直接修改玩家数据？
A: Minecraft 是服务端权威架构。客户端只能发送"请求"（C2S 包），服务端验证后执行，再把结果同步回客户端。这防止了作弊。

### Q: DeferredRegister 和直接 new Item() 有什么区别？
A: 直接创建会在错误的时间点触发，导致崩溃。DeferredRegister 确保在 Forge 的注册阶段才创建物品。

### Q: 为什么需要两条事件总线？
A: MOD Bus 处理一次性的启动事件（注册），FORGE Bus 处理持续的运行时事件。分开是为了防止注册阶段的事件在运行时被误触发。

### Q: JSON 数据包修改后需要重启游戏吗？
A: 不需要。执行 `/reload` 命令就会重新加载所有 JSON。如果新数据有错误，会保留旧的 Registry 不受影响。

### Q: Capability 数据在玩家死亡后会丢失吗？
A: 默认会丢失。我们通过监听 `PlayerEvent.Clone` 事件，在死亡重生时手动复制数据到新的玩家实体。
