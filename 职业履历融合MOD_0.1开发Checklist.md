# 职业履历融合 MOD 0.1 开发 Checklist

文档版本：v0.1  
日期：2026-06-02  
目标版本：Minecraft 1.20.1 + Forge  
对应开发文档：[职业履历融合MOD开发落地文档.md](/Users/hongyuwu/Documents/MC_MOD/职业履历融合MOD开发落地文档.md)

## 0.1 总目标

0.1 不是完整 RPG Mod，而是验证“职业履历无序融合”能不能在真实 Minecraft Forge 环境中稳定运行。

当前状态：本文件是开发执行与监督 checklist，不代表 0.1 代码已经完成。所有未勾选任务都必须在后续实际开发、运行验证和 SubAgent 监督后才能标记完成。

必须完成的玩家闭环：

```text
进入世界 -> 选择种族 -> 获取职业经验 -> 10级选择职业 -> 20级选择第二职业
-> 履历池重算 -> 火魔法师 + 弓箭手 解锁火焰箭
-> 释放火焰箭 -> 下一发普通箭被打标 -> 命中后触发火焰效果
```

核心验收：

```text
火魔法师 -> 弓箭手 = 火焰箭
弓箭手 -> 火魔法师 = 火焰箭
```

顺序不能改变同一职业组合的最终融合结果。

## 完成判定规则

任何任务只有同时满足以下条件，才能勾选完成：

- 代码或资源已经落地到仓库。
- 通过对应构建、运行或游戏内验证。
- 验证证据记录在本 checklist 的“验收记录”区域。
- 如果任务涉及客户端和服务端同步，必须至少在本地单人世界验证。
- 如果任务涉及网络包、Screen、按键或 C2S/S2C，同一里程碑或 M8 必须验证客户端连接本地专用服务端。
- 如果任务涉及持久化，必须验证退出重进或死亡复制。

禁止只因为“代码写完了”就标记完成。

### 验收证据模板

每个里程碑的“验收记录”必须按下面格式填写。没有证据路径或命令输出摘要的任务，不允许勾选完成。

```text
- 验证日期：
- 验证人：
- Git/文件状态摘要：
- 执行命令：
- 日志文件路径：
- 截图/录屏路径：
- debug 命令输出摘要：
- 游戏内测试步骤：
- 实际结果：
- 失败重试记录：
- 遗留问题：
```

## 监督机制

SubAgent 监督者职责：

- 审计 checklist 是否足以驱动真实开发。
- 每个里程碑完成后，检查验收证据是否充分。
- 对“未运行、未截图、未提供日志、未进游戏验证”的任务保持未完成。
- 重点检查 Forge 1.20.1 的玩家数据、reload、网络同步、箭矢打标和运行验证。

主线程每完成一个里程碑后，应请求 SubAgent 做一次监督审计，并把审计结果追加到本文档“监督记录”。

## 全局验收命令

后续建立 Forge 工程后，至少需要保留以下命令作为验收入口：

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
```

如果工程结构不同，需要在本节更新实际命令。

## 里程碑 M0：工程骨架

目标：仓库从纯文档变成可启动的 Forge 1.20.1 Mod 工程。

### M0 Checklist

- [x] M0-01 创建 Forge 1.20.1 MDK 工程。
  - 产物：`build.gradle`、`settings.gradle`、`gradlew`、`src/main/java`、`src/main/resources`。
  - 必须记录：Minecraft 版本、Forge 版本、Java 版本、Gradle 版本。
  - 验收：`./gradlew build` 成功。

- [x] M0-02 设置基础 Mod 信息。
  - modid：`careerchronicle`。
  - 名称：`Career Chronicle` 或中文名占位。
  - 产物：`mods.toml`、主 Mod 类。
  - 验收：`runClient` 的 Mods 列表中出现该 Mod。

- [ ] M0-03 注册基础事件总线、配置、网络入口。
  - 产物：`CareerChronicleMod`、`ModConfig`、`NetworkHandler` 空骨架。
  - 验收：客户端启动无报错，服务端启动无报错。

- [x] M0-04 建立包结构。
  - 需要包：
    - `careerchronicle.player`
    - `careerchronicle.data`
    - `careerchronicle.career`
    - `careerchronicle.skill`
    - `careerchronicle.network`
    - `careerchronicle.command`
    - `careerchronicle.client`
  - 验收：包结构存在，后续任务按包归档。

- [x] M0-05 添加中文和英文语言文件。
  - 产物：`zh_cn.json`、`en_us.json`。
  - 验收：至少包含 Mod 名称、基础命令反馈文本。

### M0 验收记录

```text
M0-01 / M0-02 / M0-04 / M0-05 已通过后台验证；M0-03 因服务端 EULA 未确认，暂不勾选。

- 验证日期：2026-06-02
- 验证人：Codex 主线程；待 Wegener SubAgent 审计
- Git/文件状态摘要：当前目录暂无 git 仓库；已生成 Forge MDK 工程、主 Mod 类、配置、网络骨架、包结构、语言文件和构建产物。
- Minecraft/Forge/Java/Gradle 版本：
  - Minecraft：1.20.1
  - Forge：47.4.20
  - Gradle Wrapper：8.8
  - Gradle 运行 JVM：Java 21.0.10 aarch64
  - Forge runClient/runServer toolchain：Java 17.0.15 x86_64
- 执行命令：
  - ./gradlew build
  - /private/tmp/gradle-8.8/bin/gradle runServer
  - /private/tmp/gradle-8.8/bin/gradle runClient
  - 所有下载/运行命令均使用用户提供代理：`https_proxy=http://127.0.0.1:7890 http_proxy=http://127.0.0.1:7890 all_proxy=socks5://127.0.0.1:7891`
  - 额外加入 Java 显式代理参数：`-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890 -DsocksProxyHost=127.0.0.1 -DsocksProxyPort=7891`
- 日志文件路径：
  - /Users/hongyuwu/Documents/MC_MOD/run/logs/latest.log
  - /Users/hongyuwu/Documents/MC_MOD/run/logs/debug.log
  - /Users/hongyuwu/Documents/MC_MOD/run/logs/2026-06-02-1.log.gz
  - /Users/hongyuwu/Documents/MC_MOD/run/logs/debug-1.log.gz
- 截图/录屏路径：
  - 未作为有效证据使用。`/private/tmp/careerchronicle-runclient.png` 只截到 Codex 前台窗口，不是 Minecraft 客户端窗口。
  - 旧会话的视觉探针 MCP 已定位到 `/Users/hongyuwu/IdeaProjects/MythicAgeMod/tools/mc_visual_probe_mcp.py` 与 `VisualProbeServer.java`；当前 M0 采用后台日志验证，暂不迁移游戏内视觉探针。
- build 结果：
  - `./gradlew build` 通过，输出 `BUILD SUCCESSFUL in 1m 21s`。
  - 产物：`/Users/hongyuwu/Documents/MC_MOD/build/libs/careerchronicle-0.1.0.jar`。
- runClient 结果：
  - `runClient` 完整启动并最终正常停止，输出 `BUILD SUCCESSFUL in 55m 22s`。
  - 日志确认 Forge 发现并加载 `careerchronicle`：`Found valid mod file main with {careerchronicle} mods - versions {0.1.0}`。
  - 日志确认主类实例化：`Creating FMLModContainer instance for com.hongyuwu.careerchronicle.CareerChronicleMod`。
  - 日志确认 common setup：`Career Chronicle common setup complete.`。
  - 日志确认网络入口：`Career Chronicle network channel registered.`。
- runServer 结果：
  - 第二次 `runServer` 补齐下载资源后 Gradle 任务成功，Forge 服务端发现 `careerchronicle` 0.1.0。
  - 服务端停在 EULA 门槛：`You need to agree to the EULA in order to run the server.`
  - 因未替用户确认 EULA，未进入完整服务端启动，M0-03 暂不勾选。
- debug 命令输出摘要：
  - `run/logs/debug.log` 行 81-82：读取到 `careerchronicle` mod coordinates。
  - `run/logs/debug.log` 行 94/108：发现有效 Mod 文件 `{careerchronicle}` 版本 `0.1.0`。
  - `run/logs/debug.log` 行 333：创建 `CareerChronicleMod` 的 FML 容器。
  - `run/logs/debug.log` 行 511：生成 `mod:careerchronicle` PackInfo。
  - `run/logs/latest.log` 行 52-53：common setup 与 network channel 注册完成。
  - `run/logs/latest.log` 行 73：客户端正常停止。
  - `jar tf build/libs/careerchronicle-0.1.0.jar`：包含 `META-INF/mods.toml`、`CareerChronicleMod.class`、`ModConfig.class`、`NetworkHandler.class`、`S2CCareerSnapshotPacket.class`、`en_us.json`、`zh_cn.json`。
- 失败重试记录：
  - 第一次 `downloadAssets` 有 3 个音效资源 SSL 握手失败；加入 Java 显式代理参数后重跑成功。
  - 普通沙箱运行 `./gradlew build` 因网络权限失败；授权后 Wrapper 下载 Gradle 8.8 并构建成功。
  - 截图辅助脚本因本机 Swift/SDK 版本不匹配与 Screen Recording 权限未授予失败；`screencapture` 成功但未抓到 Minecraft 前台窗口。
- 发现问题：
  - M0-03 剩余服务端完整启动验证：需要用户确认是否允许修改 `run/eula.txt` 为 `eula=true` 后重跑 `./gradlew runServer`。
  - M0-02 当前采用后台日志验证替代 Mods UI 截图；如用户自行视觉确认 Mods 列表，可补充截图路径。
```

## 里程碑 M1：玩家数据与同步

目标：实现 Forge Capability 持久化，保证种族、等级、职业履历和技能解锁不会丢。

### M1 Checklist

- [ ] M1-01 实现 `ICareerData`。
  - 字段：race、careerLevel、careerXp、classHistory、unlockedSkills、skillLoadout、hiddenFlags。
  - 验收：单元级序列化/反序列化方法可调用。

- [ ] M1-02 实现 `CareerDataCapability` 和 provider。
  - 使用 `AttachCapabilitiesEvent<Entity>` 附加到玩家。
  - 验收：玩家登录后能通过命令读取到非空职业数据。

- [ ] M1-03 实现 NBT 持久化。
  - 必须保存：种族、等级、经验、职业履历、已解锁技能、装配。
  - 不持久化普通短冷却。
  - 验收：设置种族后退出重进，数据仍在。

- [ ] M1-04 实现 `PlayerEvent.Clone` 数据复制。
  - 死亡后复制职业数据。
  - 非死亡 clone 完整复制。
  - 验收 A：玩家死亡后种族、等级、职业履历、已解锁技能不丢。
  - 验收 B：`event.isWasDeath() == false` 场景不丢数据，至少验证末地返回或可控模拟 clone。

- [ ] M1-05 实现服务端到客户端快照同步。
  - 包：`S2CCareerSnapshotPacket`。
  - 触发：登录、跨维度、重生、职业选择、技能解锁。
  - 验收：客户端 UI 或 debug overlay 能显示服务端同步来的数据。

- [ ] M1-06 实现短期战斗状态。
  - 内容：冷却、法力、体力、下一发投射物 modifier。
  - 不进入长期 NBT。
  - 验收：退出重进后普通短冷却清空，职业数据不丢。

### M1 验收记录

```text
当前状态：M1 基础代码已落地并通过构建，但尚未进行游戏内持久化、死亡复制、非死亡 clone、登录同步、跨维度同步验收；所有 M1 checkbox 保持未完成。

- 验证日期：2026-06-02
- 执行命令：`./gradlew build`
- 日志文件路径：终端构建输出；后续游戏内验证需补 `run/logs/latest.log` 和 `debug.log`
- 截图/录屏路径：待补
- 种族持久化测试：待补
- 死亡复制测试：待补
- 非死亡 clone 测试：待补
- 登录同步测试：待补
- 跨维度同步测试：待补
- debug 命令输出摘要：
  - 已实现 `ICareerData`、`CareerData`、`CareerDataSnapshot`、`CareerRuntimeState`、NBT 序列化/反序列化。
  - 已实现 `CareerDataCapability`、`CareerDataProvider`、`CareerCapabilityEvents`、`CareerPlayerEvents`。
  - 已实现登录、跨维度、重生后的 `S2CCareerSnapshotPacket` 同步入口和客户端缓存 `ClientCareerData`。
  - 已实现 `/career debug`、`/career set-race`、`/career add-class`、`/career add-xp` 作为早期验收命令。
  - `./gradlew build` 通过，输出 `BUILD SUCCESSFUL in 22s`。
- 失败重试记录：
  - 初次构建发现 `RegisterCapabilitiesEvent` 包名错误；已改为 `net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent` 并拆到 MOD bus。
  - 增补 provider invalidate，避免 Capability LazyOptional 生命周期残留。
  - 增补 `DistExecutor` 包装 S2C 客户端缓存更新，降低 dedicated server classloading 风险。
- 发现问题：
  - M1 不能标记完成，必须补单人世界/本地专服内的持久化、死亡复制、非死亡 clone、登录同步和跨维度同步证据。
  - 根据 Wegener 监督意见，M1 完成前必须先由用户确认 EULA 后补跑完整 `runServer`。
```

## 里程碑 M2：数据包与 Registry

目标：职业、种族、技能、融合规则由 JSON 加载，并支持安全 reload。

### M2 Checklist

- [ ] M2-01 定义数据模型。
  - `RaceDef`
  - `ClassDef`
  - `SkillDef`
  - `FusionDef`
  - `RegistrySnapshot`
  - 验收：默认 JSON 可解析成模型。

- [ ] M2-02 实现数据目录。
  - `data/careerchronicle/careerchronicle/races/*.json`
  - `data/careerchronicle/careerchronicle/classes/*.json`
  - `data/careerchronicle/careerchronicle/skills/*.json`
  - `data/careerchronicle/careerchronicle/fusions/*.json`
  - 说明：第二个 `careerchronicle` 是 `SimpleJsonResourceReloadListener("careerchronicle")` 的目录名。
  - 验收：默认数据随 Mod 启动加载。

- [ ] M2-03 实现 reload listener。
  - 使用 `AddReloadListenerEvent` 注册。
  - 解析到临时 map。
  - 校验通过后一次性替换 immutable registry。
  - 失败时保留旧 registry。
  - registry 必须带版本号或 debug version。
  - 验收 A：故意写错一个技能引用，reload 失败但旧数据仍可用。
  - 验收 B：失败 reload 后 registry version 不变。
  - 验收 C：成功 reload 后 registry version 递增。

- [ ] M2-04 实现引用校验。
  - 职业引用技能必须存在。
  - 融合规则引用技能必须存在。
  - 技能引用 executor 必须存在。
  - 图标缺失只警告。
  - 验收：错误日志能指出具体 JSON 和字段。

- [ ] M2-05 实现 `/career reload`。
  - 管理员权限。
  - reload 后重算在线玩家履历和已装配技能。
  - 验收 A：新增融合规则后无需重启即可生效。
  - 验收 B：玩家释放技能期间执行 reload 不崩溃、不读到半更新数据。

### M2 默认数据 Checklist

- [ ] M2-D01 种族：人类。
- [ ] M2-D02 种族：精灵。
- [ ] M2-D03 职业：火魔法师。
- [ ] M2-D04 职业：弓箭手。
- [ ] M2-D05 技能：火球。
- [ ] M2-D06 技能：蓄力射击。
- [ ] M2-D07 技能：火焰箭。
- [ ] M2-D08 融合：`fire >= 1 && bow >= 1 -> flame_arrow`。
- [ ] M2-D09 0.2 占位，不进入 0.1：`fire_mage x2 -> fire_burst`。
- [ ] M2-D10 0.2 占位，不进入 0.1：`archer x2 -> piercing_shot`。

### M2 验收记录

```text
当前状态：M2 数据模型、reload listener、引用校验和默认 JSON 已落地并通过构建；尚未进行游戏内 reload、错误 JSON 保留旧 registry、registry version 变化、技能释放期间 reload、在线玩家重算验收。

- 验证日期：2026-06-02
- 执行命令：`./gradlew build`
- 日志文件路径：终端构建输出；后续游戏内 reload 需补 `run/logs/latest.log` 与 `debug.log`
- 截图/录屏路径：待补
- 正常 reload：待补
- 错误 JSON reload：待补
- registry version 变化：待补
- 技能释放期间 reload：待补
- 在线玩家重算：待补
- 已落地代码：
  - `RaceDef`、`ClassDef`、`SkillDef`、`FusionDef`、`RegistrySnapshot`
  - `CareerRegistry`、`CareerDataReloadListener`、`CareerRegistryValidator`
  - `SkillExecutorRegistry`
  - `/career registry` 调试命令
- 默认数据：
  - `data/careerchronicle/careerchronicle/races/human.json`
  - `data/careerchronicle/careerchronicle/races/elf.json`
  - `data/careerchronicle/careerchronicle/classes/fire_mage.json`
  - `data/careerchronicle/careerchronicle/classes/archer.json`
  - `data/careerchronicle/careerchronicle/skills/fireball.json`
  - `data/careerchronicle/careerchronicle/skills/charged_shot.json`
  - `data/careerchronicle/careerchronicle/skills/flame_arrow.json`
  - `data/careerchronicle/careerchronicle/fusions/fire_mage_archer_flame_arrow.json`
- debug 命令输出摘要：
  - `./gradlew build` 通过，输出 `BUILD SUCCESSFUL in 22s`。
  - 融合规则以 `required_class_counts` 表达，因此 `fire_mage + archer -> flame_arrow` 天然无序。
- 失败重试记录：无构建失败；补充了技能 executor 引用校验，未知 executor 会导致 reload 失败并保留旧 registry。
- 发现问题：
  - M2 checkbox 仍不可勾选，需要在游戏内验证 reload 成功、错误 JSON 失败但旧 registry 保留、version 递增/不变。
```

## 里程碑 M3：职业等级、职业段与履历池

目标：职业等级和履历融合规则能通过命令和实际经验驱动。

### M3 Checklist

- [ ] M3-01 实现职业经验与等级曲线。
  - 0.1 只做击杀经验。
  - 验收：击杀怪物后职业经验增加。

- [ ] M3-02 实现等级段节点。
  - 10 级解锁第 1 个职业段。
  - 20 级解锁第 2 个职业段。
  - 30 级作为 0.1 上限。
  - 验收：达到 10/20 级后出现待选择职业段。

- [ ] M3-03 实现职业选择逻辑。
  - 服务端校验是否有未选择职业段。
  - 服务端校验职业是否存在、是否可选。
  - 写入 `classHistory`。
  - 验收：重复提交同一 slot 会被拒绝。

- [ ] M3-04 实现履历池计算。
  - 统计 `class_counts`。
  - 统计 `tag_scores`。
  - 加入种族 tag bonus。
  - 验收：debug 命令能打印职业次数和标签强度。

- [ ] M3-05 预留专精技能数据结构，但 0.1 不实现专精技能运行时。
  - `repeat_rewards` 字段可以存在。
  - `fire_burst`、`piercing_shot` 只作为 0.2 占位。
  - 验收：0.1 发布门槛不要求重复选择同职业解锁可释放专精技能。

- [ ] M3-06 实现无序融合技能解锁。
  - `fire >= 1 && bow >= 1 -> flame_arrow`。
  - 验收 A：`fire_mage -> archer` 解锁 `flame_arrow`。
  - 验收 B：`archer -> fire_mage` 解锁 `flame_arrow`。

### M3 调试命令

- [ ] M3-C01 `/career debug <player>` 显示完整快照。
- [ ] M3-C02 `/career xp add <player> <amount>` 添加职业经验。
- [ ] M3-C03 `/career level set <player> <level>` 设置职业等级。
- [ ] M3-C04 `/career race set <player> <race>` 设置种族。
- [ ] M3-C05 `/career class choose <player> <slot> <class>` 选择职业段。
- [ ] M3-C06 `/career reset <player>` 清空 0.1 测试数据。

### M3 验收记录

```text
待填写：
- fire_mage -> archer 结果：
- archer -> fire_mage 结果：
- fire_mage -> fire_mage 占位结果：
- debug 输出摘要：
- 失败重试记录：
- 发现问题：
```

## 里程碑 M4：投射物与箭矢打标

目标：火焰箭不只是 UI 解锁，而是真正影响下一发普通箭。

### M4 Checklist

- [ ] M4-01 实现 `CareerProjectileEntity`。
  - 用于火球。
  - 字段：owner、skillId、damage、lifeTicks、element。
  - 验收：投射物生命周期结束自动消失。

- [ ] M4-02 实现 `EntityJoinLevelEvent` 箭矢打标。
  - 判断新实体是否 `AbstractArrow`。
  - 判断 owner 是否 `ServerPlayer`。
  - 读取并消费 owner 的 `next_projectile_modifiers`。
  - 写入箭实体 PersistentData。
  - 验收：释放火焰箭后射出普通箭，箭实体含 modifier。

- [ ] M4-03 实现命中结算。
  - 通过 `ProjectileImpactEvent` 或伤害事件读取箭实体 PersistentData。
  - 造成额外火焰伤害。
  - 点燃目标。
  - 播放命中特效。
  - 验收 A：带 modifier 的箭命中才触发火焰效果。
  - 验收 B：同一支箭命中实体只触发一次 modifier。
  - 验收 C：命中方块不造成实体伤害。
  - 验收 D：如果同时监听 `ProjectileImpactEvent` 和伤害事件，必须有去重标记，不能重复伤害、重复点燃、重复粒子。

- [ ] M4-04 验证只影响下一发箭。
  - 第一发触发火焰效果。
  - 第二发不触发。
  - 验收：modifier 被正确消费。

- [ ] M4-05 明确 0.1 兼容范围。
  - 0.1 完整支持普通弓 + 普通箭。
  - 弩、多重射击、烟花弩进入 0.2。
  - 验收：文档和配置提示清楚。

### M4 验收记录

```text
待填写：
- 验证日期：
- 执行命令：
- 日志文件路径：
- 截图/录屏路径：
- EntityJoinLevelEvent 打标结果：
- ProjectileImpactEvent/伤害事件结算结果：
- 单次结算测试：
- 命中方块测试：
- 第二发箭消费测试：
- debug 命令输出摘要：
- 失败重试记录：
- 发现问题：
```

## 里程碑 M5：技能运行时

目标：技能能在服务端权威下释放、消耗资源、进入冷却、产生效果。

### M5 Checklist

- [ ] M5-01 实现 `SkillRuntime`。
  - 服务端校验技能是否已解锁。
  - 服务端校验技能是否已装配或在固定槽中。
  - 服务端校验冷却、资源、装备。
  - 验收：客户端伪造释放未解锁技能会被拒绝。

- [ ] M5-02 实现法力/体力。
  - 0.1 可使用简单固定上限。
  - 需要自然恢复。
  - 验收：资源不足时不能释放技能。

- [ ] M5-03 实现短冷却。
  - 存在 `ICareerCombatState`。
  - 使用 game time 计算 ready time。
  - 验收：连续释放火球，第二次被拒绝。

- [ ] M5-04 实现 `spawn_projectile` executor。
  - 用于火球。
  - 验收：火球能发射、命中、消失。

- [ ] M5-05 实现 `mark_next_projectile` executor。
  - 写入玩家下一发投射物 modifier。
  - 超时未射出则失效。
  - 验收：释放火焰箭后状态存在，射出普通箭后状态被消费。

- [ ] M5-06 实现 `damage` executor。
  - 服务端造成技能伤害。
  - 0.1 不做复杂伤害类型也可以，但要预留。
  - 验收：火球和火焰箭命中能造成额外伤害。

- [ ] M5-07 实现 `play_fx` executor。
  - 只向半径内玩家发送 S2C 表现包。
  - 验收：32 格外玩家不收到普通技能 FX。

### M5 技能 Checklist

- [ ] M5-S01 火球。
  - 来源：火魔法师。
  - 效果：发射火球，命中造成火焰伤害。
  - 验收：命中怪物造成伤害并播放火焰粒子。

- [ ] M5-S02 蓄力射击。
  - 来源：弓箭手。
  - 效果：标记下一发普通箭，增加伤害。
  - 验收：只影响下一发箭。

- [ ] M5-S03 火焰箭。
  - 来源：火魔法师 + 弓箭手。
  - 效果：标记下一发普通箭，命中造成额外火焰伤害并点燃。
  - 验收：未解锁时不能释放，解锁后能释放。

### M5 验收记录

```text
待填写：
- 火球测试：
- 蓄力射击测试：
- 火焰箭测试：
- 冷却/资源测试：
- 伪造请求测试：
- 发现问题：
```

## 里程碑 M6：UI 与 HUD

目标：玩家不靠命令也能完成 0.1 的核心流程。

### M6 Checklist

- [ ] M6-01 首次种族选择 Screen。
  - 可选：人类、精灵。
  - 确认后发送 C2S 请求。
  - 服务端校验未选过种族。
  - 验收：首次进世界弹出；确认后不再重复弹出。

- [ ] M6-02 职业选择 Screen。
  - 显示未选择职业段。
  - 可选：火魔法师、弓箭手。
  - 显示选择后获得技能和可能解锁融合。
  - 验收：20 级选择第二职业时能预览火焰箭。

- [ ] M6-03 简化 HUD。
  - 显示法力/体力。
  - 显示固定技能槽。
  - 显示冷却遮罩。
  - 显示下一发箭 modifier 状态。
  - 验收：释放火焰箭后 HUD 显示下一发箭状态。

- [ ] M6-04 键位。
  - 打开职业界面。
  - 释放当前技能。
  - 切换固定技能槽。
  - 验收：按键可在设置中修改。

- [ ] M6-05 UI 同步容错。
  - 客户端显示只使用 `CareerClientSnapshot`。
  - 服务端拒绝非法选择时，客户端显示错误消息。
  - 验收：客户端不能通过 UI 选择不存在职业。

- [ ] M6-06 分辨率、GUI Scale 和语言检查。
  - 1280x720，GUI Scale 2。
  - 1920x1080，GUI Scale 3。
  - `zh_cn` 和 `en_us` 不缺 key。
  - 验收：种族界面、职业界面、HUD 不遮挡、不溢出、不显示缺失翻译 key。

### M6 验收记录

```text
待填写：
- 验证日期：
- 执行命令：
- 日志文件路径：
- 截图/录屏路径：
- 种族界面：
- 职业界面：
- HUD：
- 键位：
- 服务端拒绝非法 UI 请求：
- 分辨率/GUI Scale：
- 中英文语言 key：
- 失败重试记录：
- 发现问题：
```

## 里程碑 M7：美术与特效

目标：0.1 表现足够识别，但不追求完整美术量。

### M7 Checklist

- [ ] M7-01 技能图标。
  - `fireball.png`
  - `charged_shot.png`
  - `flame_arrow.png`
  - 验收：UI 中图标不缺失。

- [ ] M7-02 种族图标。
  - `human.png`
  - `elf.png`
  - 验收：种族选择界面不缺图。

- [ ] M7-03 职业图标。
  - `fire_mage.png`
  - `archer.png`
  - 验收：职业选择界面不缺图。

- [ ] M7-04 GUI 基础贴图。
  - `skill_slot.png`
  - `skill_slot_selected.png`
  - `cooldown_overlay.png`
  - `race_card.png`
  - `class_card.png`
  - 验收：UI 没有紫黑缺失贴图。

- [ ] M7-05 粒子与音效。
  - 火球飞行粒子。
  - 火焰箭箭尾粒子。
  - 命中火花。
  - 释放音效可先复用原版。
  - 验收：10 名玩家或测试替代方案持续释放 10 分钟，平均 TPS 不低于 19，客户端无 crash，日志无连续 error/warn 刷屏。

### M7 验收记录

```text
待填写：
- 验证日期：
- 执行命令：
- 日志文件路径：
- 截图/录屏路径：
- 缺失贴图检查：
- 粒子表现：
- 音效表现：
- 平均 TPS：
- error/warn 统计：
- 失败重试记录：
- 发现问题：
```

## 里程碑 M8：构建、运行与回归测试

目标：保证 0.1 真能启动、能运行、能完成核心流程。

### M8 Checklist

- [ ] M8-01 `./gradlew build` 成功。
  - 验收：生成 jar。

- [ ] M8-02 `./gradlew runClient` 成功。
  - 验收：进入单人世界无崩溃。

- [ ] M8-03 `./gradlew runServer` 成功。
  - 验收：服务端启动无崩溃，Mod 加载成功。

- [ ] M8-04 单人完整流程测试。
  - 新建世界。
  - 选择种族。
  - 设置或获取经验到 10 级。
  - 选择火魔法师。
  - 到 20 级选择弓箭手。
  - 解锁火焰箭。
  - 释放并命中怪物。
  - 验收：全流程成功。

- [ ] M8-05 反向顺序测试。
  - 新建或重置玩家。
  - 先弓箭手，后火魔法师。
  - 验收：同样解锁火焰箭。

- [ ] M8-06 持久化回归。
  - 选择种族和职业后退出重进。
  - 死亡后重生。
  - 验收：数据不丢。

- [ ] M8-07 reload 回归。
  - 正确 JSON reload。
  - 错误 JSON reload。
  - 修复后再次 reload。
  - 验收：旧 registry 不被错误数据污染。

- [ ] M8-08 性能冒烟。
  - 10 名玩家或测试替代方案持续释放技能 10 分钟。
  - 验收：平均 TPS 不低于 19，10 分钟内无 crash，无连续 error/warn 刷屏，FX 包半径限制生效。

- [ ] M8-09 本地专用服务端连接完整流程测试。
  - 启动 `./gradlew runServer`。
  - 启动客户端连接本地专服。
  - 完成种族选择。
  - 完成职业选择。
  - 正向或反向顺序解锁火焰箭。
  - 释放火焰箭并命中怪物。
  - 执行 `/career reload`。
  - 测试死亡复制。
  - 验收：专服环境下 C2S/S2C、Screen、按键、reload、持久化全流程通过，无 classloading 崩溃。

### M8 验收记录

```text
待填写：
- 验证日期：
- 执行命令：
- 日志文件路径：
- 截图/录屏路径：
- build：
- runClient：
- runServer：
- 正向顺序：
- 反向顺序：
- 持久化：
- reload：
- 性能冒烟：
- 本地专服连接完整流程：
- 平均 TPS：
- error/warn 统计：
- debug 命令输出摘要：
- 失败重试记录：
- 发现问题：
```

## 0.1 不做清单

以下内容必须保持未进入 0.1，除非重新审计范围：

- [ ] 召唤物 AI。
- [ ] 隐藏职业。
- [ ] 牧师、战士完整职业线。
- [ ] 完整技能装配 UI。
- [ ] 结构探索经验。
- [ ] 治疗、范围、召唤、buff 通用 executor。
- [ ] 弩、多重射击、烟花弩完整兼容。
- [ ] 大型 Boss。
- [ ] 自定义维度。
- [ ] 100 级终局内容。

这些 checkbox 保持未勾选是正确状态。

## 监督记录

### 监督记录 1：Checklist 初版审计

```text
审计时间：2026-06-02
是否可执行：方向正确，M0-M8 主线基本合理，但初版不足以完全保证“写完且正常运行”。
P0 问题：
- 缺少客户端连接本地专用服务端的完整流程验证。
- 验收记录没有强制填写证据类型和路径。
P1 问题：
- 非死亡 PlayerEvent.Clone 缺少明确验收。
- reload 线程边界、registry version 和技能释放期间 reload 验收不足。
- M4/M5 存在依赖倒置。
- 箭矢命中缺少只结算一次验收。
- fire_burst、piercing_shot 专精内容轻微带回 0.1。
P2 问题：
- 性能验收偏主观。
- UI 缺少分辨率和语言检查。
- M0 缺少 Forge/Java/Gradle 版本锁定证据。
已修订：
- 增加验收证据模板。
- 增加 M8-09 本地专用服务端连接完整流程测试。
- 增加最终发布门槛：专服连接通过、M0-M8 证据完整。
- 增加非死亡 clone 验收。
- 增加 reload registry version、失败不变、成功递增、释放期间 reload 验收。
- 将 M4 改为投射物与箭矢打标，M5 改为技能运行时。
- 增加箭矢 modifier 单次结算、命中方块不造成实体伤害验收。
- 将 fire_burst、piercing_shot 改为 0.2 占位，不进入 0.1 发布门槛。
- 增加 TPS、error/warn、UI 分辨率、GUI Scale、语言 key 检查。
结论：初版审计意见已采纳，当前 checklist 可作为 0.1 开发与监督基线。
```

### 监督记录 2：M0 工程骨架审计

```text
审计时间：2026-06-02
审计者：Wegener SubAgent

结论：
- M0-01、M0-02、M0-04、M0-05 可以认可完成。
- M0-03 暂不能算完成，保持未勾选是正确的。

证据认可：
- 工程文件、源码、资源、jar 内容和日志证据足够证明 Forge 1.20.1 + Forge 47.4.20 工程骨架存在。
- `gradle.properties` 锁定 Minecraft `1.20.1`、Forge `47.4.20`、modid `careerchronicle`。
- jar 内包含 `mods.toml`、主类、`ModConfig`、`NetworkHandler` 和中英文语言文件。
- `runClient` 日志证明 FML 发现、实例化并加载 `careerchronicle 0.1.0`。

P0：
- 无。当前没有发现阻止继续 M1 基础开发的工程级错误。

P1：
- `runServer` 未完整启动，M0-03 未完成。当前只到 EULA 门槛，不能替代完整 dedicated server 启动。
- 视觉证据不足。M0 可以暂用日志替代截图，但 UI/HUD/Screen/专服连接流程不能继续只靠后台日志。

监督要求：
- 允许进入 M1 基础代码开发。
- 不允许把 M0 全部标为完成，直到用户确认 EULA 后补跑完整 `runServer`。
- M1 标记完成前必须补跑完整 `runServer`，看到服务端启动完成、`Career Chronicle common setup complete.`、`Career Chronicle network channel registered.` 等日志。
- checklist 必须明确记录 EULA 是用户确认后修改，不是自动代替用户同意。
```

### 监督记录 3：M1 玩家数据基础审计

```text
审计时间：2026-06-02
审计者：Wegener SubAgent

结论：
- 代码层面无 P0 阻断问题。
- Capability 注册、Provider、NBT 序列化、Clone 复制入口、登录/跨维度/重生同步入口符合 Forge 1.20.1 基本做法。
- M1 全部 checkbox 都不能勾选；大部分是“代码落地但未验收”。

P1：
- `/career set-race/add-class` 的 ID 解析会污染后续验收：裸 `human` 不应变成 `minecraft:human`，并且参数应支持带冒号的完整 ID。
- S2C 同步缺少客户端可观察证据入口。
- 专服完整启动仍未补证，M1 完成前必须补 M0-03。

已修订：
- `CareerCommands` 将 `set-race/add-class` 参数改为 `StringArgumentType.string()`，并对无 namespace 输入补 `careerchronicle`。
- `ClientCareerData.update` 增加客户端日志，后续可用 `Client career snapshot updated` 作为 S2C 到达证据。

M1 Checklist 状态：
- M1-01：代码落地，未游戏/单元验收。
- M1-02：代码落地，未登录命令验收。
- M1-03：代码落地，未退出重进验收。
- M1-04：代码落地，未死亡/非死亡 clone 验收。
- M1-05：部分落地，已有客户端日志观察入口，但未游戏内验收。
- M1-06：部分落地，`CareerRuntimeState` 不进 NBT，但缺设置/检查冷却的验收入口。

最低成本验证建议：
- `./gradlew runClient`
- 新建可作弊世界。
- `/career debug`
- `/career set-race careerchronicle:human`
- `/career add-class careerchronicle:fire_mage`
- `/career add-xp 123`
- `/save-all` 后退出重进，再 `/career debug`
- `/kill @s` 后重生，再 `/career debug`
- 跨维度传送后再 `/career debug`
- 同时在用户确认 EULA 后补完整 `./gradlew runServer`。
```

## 最终 0.1 发布门槛

只有以下全部满足，才能称为 0.1 完成：

- [ ] M0-M8 全部完成。
- [ ] 所有 M0-M8 验收记录证据完整，包含命令、日志路径、截图/录屏路径或 debug 输出摘要。
- [ ] 正向和反向职业顺序都能解锁火焰箭。
- [ ] 火焰箭能真实影响下一发普通箭并命中结算。
- [ ] 玩家职业数据退出重进和死亡后不丢。
- [ ] 错误 JSON reload 不污染旧 registry。
- [ ] `build`、`runClient`、`runServer` 全部成功。
- [ ] 客户端连接本地专用服务端完整流程通过。
- [ ] SubAgent 最终监督审计没有 P0 阻断问题。
