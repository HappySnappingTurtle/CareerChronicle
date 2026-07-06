# 职业履历融合 MOD 长期落地 Checklist

本文档用于承接《职业履历融合MOD产品设计.md》和《职业履历融合MOD开发落地文档.md》，作为从当前可运行原型走向完整产品的长期执行账本。

当前原则：这不是一次性 Demo 任务。每个勾选项必须有代码、资源、构建、实机或截图/日志证据支撑；没有证据不勾选。

## 0. 当前阶段判断

当前处于“战略防御期”：已经有 Forge 1.20.1 工程、玩家 Capability、基础数据加载、网络同步、种族/职业界面、技能释放、基础图标和几把自定义武器，但完整 MOD 还没有进入内容厚度、表现质量和长期可维护阶段。

阶段目标不是堆更多技能数量，而是先建立稳定的 UI、技能特效、美术资产、模型和 QA 管线。

已有基础：

- [x] Forge 1.20.1 工程骨架和 `careerchronicle` modid。
- [x] 玩家职业数据持久化与快照同步。
- [x] 基础 races/classes/skills/fusions 数据加载。
- [x] 基础种族选择与职业界面。
- [x] 基础技能槽、冷却和服务端释放校验。
- [x] 初版技能图标与三把自定义技能武器原型。
- [x] 火法、弓手、火焰箭融合的原型闭环。

当前测试暴露的问题：

- [x] 记录：缺少职业经验条 UI。
- [x] 记录：技能 HUD 过大，遮挡视野。
- [x] 记录：技能效果仍像普通弓箭或普通粒子，职业辨识度不足。
- [x] 记录：技能图标、武器、美术和模型需要进入系统化产出。
- [x] 实装：职业 HUD 显示等级、经验、下一等级进度。
- [x] 实装：技能 HUD 改为紧凑横向布局，避开准星和主视野。
- [x] 解决：每个技能至少具备施放提示、轨迹提示、命中反馈三层表现。
- [x] 实装：当前技能已接入 S2C 客户端 FX 播放入口。
- [x] 实装：自定义技能武器带职业技能入口、耐久、冷却和 tooltip 使用反馈。
- [x] 实装：关键自定义武器 item model 增加第一/第三人称手持 display 配置。
- [ ] 解决：自定义武器完成实机第一/第三人称角度检查并修正模型遮挡。

进入下一阶段的条件：

- [ ] 0.2 Alpha 的 1-30 级循环可实机游玩。
- [ ] 职业经验、职业段选择、技能解锁、技能装配形成稳定闭环。
- [ ] HUD/UI、美术图标、模型、技能 FX 都有可重复生产规范。
- [ ] dedicated server 至少完成一次加载、进服、同步、释放技能验证。

## 1. 版本路线

### 1.1 P0：当前原型修正

目标：把测试中最影响体验的问题先压住，让后续内容扩展有可靠基线。

- [x] 技能栏加入图标渲染。
- [x] 加入初版自定义技能武器。
- [x] 调整箭矢发射起点，避免全部从玩家视野同一点发出。
- [x] HUD 增加职业经验条。
- [x] 技能 HUD 改为低遮挡紧凑布局。
- [x] 技能 FX 拆出表现规范和客户端播放入口。
- [x] 减少技能释放消息对屏幕中央/actionbar 的干扰。
- [x] 建立“测试反馈 -> checklist -> 修复 -> 构建 -> 实机验证”的记录格式。

验收：

- [x] `./gradlew --gradle-user-home .gradle-home build` 成功。
- [ ] `runClient` 中 HUD 不遮挡准星、快捷栏和主要战斗视野。
- [ ] `/career add-xp` 后职业经验条变化可见。
- [ ] 连续释放 3 个技能时，冷却显示可读且不过度占屏。

### 1.2 0.2 Alpha：1-30 级可玩闭环

目标：从“功能能用”升级到“玩家能从新号玩到 30 级并形成第一套构筑”。

- [x] 种族达到 3-4 个：人类、精灵、矮人、兽人。
- [x] 职业达到 4 个：火魔法师、弓箭手、战士、牧师。
- [x] 等级上限 30，覆盖 3 个职业段。
- [x] 击杀经验、基础任务/探索经验至少完成 2 类来源。
- [x] 6-8 个融合技能。
- [x] 8-12 个职业专精技能。
- [x] 1 个隐藏职业线索链进入可验证状态。
- [x] 技能装配界面支持 4 主动槽、被动槽预留。
- [x] 职业主界面支持时间线、履历池统计、下一段预览。
- [x] 第一版 CareerProjectileEntity 或等价自定义投射物进入实装。
- [x] 至少 1 个技能有专属投射物材质或模型。

验收：

- [ ] 新玩家选择种族后可正常进入职业路线。
- [ ] 到 10/20/30 级时职业段选择触发正确。
- [ ] 火法 -> 弓手与弓手 -> 火法均解锁同一融合技能。
- [ ] 专精路线和混职路线都能得到明显收益。
- [x] 技能释放、冷却、资源消耗全部由服务端裁决。

### 1.3 0.3 Beta：1-50 级内容骨架

目标：产品形态基本成立，开始面向多人和长期平衡。

- [x] 种族达到 6 个：人类、精灵、矮人、兽人、亡灵、魔裔。
- [x] 职业达到 8 个：战士、守护者、弓箭手、盗贼、火法、冰法、牧师、死灵法师。
- [x] 等级上限 50，覆盖 5 个职业段。
- [x] 20-30 个融合技能。
- [x] 20-30 个专精技能。
- [x] 3 个隐藏职业进入可解锁状态。
- [x] 完整技能装配界面：主动、被动、终极、种族槽。
- [x] 职业手册支持职业、种族、技能、隐藏线索。
- [x] 服务端配置支持 XP 倍率、PVP 开关、粒子倍率、召唤物上限。
- [ ] 多人服务器 30 分钟稳定性测试。

验收：

- [ ] 50 级角色能形成至少 5 种明显不同构筑。
- [ ] 混职不是惩罚，专精也不是唯一最优。
- [ ] 隐藏职业有线索、有门槛、有反馈，不靠猜命令。
- [x] 资源包缺失、翻译缺失、无效 JSON 都能被 QA 脚本发现。

### 1.4 1.0 Release：完整首发

目标：达到可发布、可维护、可扩展的数据驱动职业融合 MOD。

- [ ] 6 种族、8 基础职业、3-5 隐藏职业。
- [ ] 60-80 个技能总量。
- [ ] 20-30 个融合技能。
- [ ] 20-30 个专精技能。
- [ ] CN/EN 双语完整。
- [ ] 完整玩家指南、服主管理指南、数据包扩展指南。
- [ ] 默认配置与平衡配置分离。
- [ ] dedicated server、客户端、局域网联机全通过发布验收。
- [ ] 发布包中不含调试资产、过期资源、无用生成物。

验收：

- [ ] 玩家能理解“每 10 级选择一次职业，所有职业成为永久履历”。
- [ ] 玩家能表达自己的构筑身份，而不是只记得单个技能。
- [ ] 数据包作者能新增一个技能和一个融合规则并 reload 验证。
- [x] 服务器管理员能通过配置降低粒子、调整经验和限制 PVP。

## 2. 系统工程 Checklist

### 2.1 架构与服务端权威

- [x] Capability 持久化玩家职业数据。
- [x] S2C 快照同步给客户端显示。
- [x] C2S 选择种族、选择职业、释放技能。
- [x] 所有技能效果只由服务端决定结果。
- [x] 客户端只负责 UI、输入和视觉表现。
- [x] 冷却、资源、装备要求、距离、目标合法性全部服务端校验。
- [x] 伪造技能包、未解锁技能、错误装备、冷却中释放都有明确拒绝路径。
- [x] 技能装配包服务端校验槽位和未解锁技能。
- [x] 数据结构加入版本号和迁移策略。

### 2.2 数据驱动与 reload

- [x] races/classes/skills/fusions 基础加载。
- [x] hidden_unlocks 加载。
- [x] xp_sources 加载。
- [x] schema 校验错误给出文件、字段、引用对象。
- [x] reload 采用“新 registry 全部校验成功后原子替换”。
- [x] reload 失败保留旧 registry。
- [x] reload 后在线玩家重新计算解锁技能和装配合法性。

### 2.3 职业经验与进度

- [x] 职业经验来源：击杀。
- [x] 职业经验来源：首次进入生物群系探索。
- [x] 职业经验来源：结构发现。
- [x] 职业经验来源：职业行为，例如治疗、格挡、远程命中。
- [x] 经验曲线由共享工具提供，服务端和客户端 UI 复用。
- [x] 等级提升逻辑服务端自动执行。
- [x] 每 10 级发放职业段选择机会。
- [x] 已获得但未选择的职业段可在 UI 中提示。
- [x] 经验条显示当前等级、当前 XP、下一级 XP。
- [x] 防刷策略：同类怪、刷怪笼、短时间重复行为有衰减或配置。

### 2.4 种族系统

- [x] 人类、精灵、矮人、兽人原型数据。
- [x] 6 个首发种族数据完成。
- [x] 0.2 四种族原型被动服务端生效。
- [x] 种族被动全部服务端生效。
- [ ] 种族弱点和职业倾向进入显示与规则。
- [ ] 种族图标 64x64 完成。
- [ ] 种族选择 UI 支持优点、弱点、推荐职业、隐藏路线提示。
- [ ] 新玩家首次进入强提示但不硬卡死服务器流程。

### 2.5 职业与履历池

- [x] 火魔法师、弓箭手、战士、牧师原型数据。
- [x] 8 个基础职业数据完成（9 含隐藏）。
- [x] 职业标签池统计显示支持 class_counts 和 tag_scores。
- [x] 重复职业奖励支持专精路线。
- [x] 职业选择界面展示当前段、历史段、可选职业和基础预览收益。
- [x] 职业选择界面预览新增融合。
- [ ] 职业选择界面预览隐藏路线。
- [x] 未满足条件职业显示灰态与原因。
- [x] 职业段选择按 10/20/30 等级节点与 0.2 三段上限进行服务端校验。
- [ ] 职业段选择后能回放新增技能、融合、隐藏线索。

### 2.6 融合、专精与隐藏职业

- [x] 火法 + 弓手融合原型。
- [x] 0.2 四职业两两组合已形成 6 个融合技能。
- [x] 融合规则支持无序组合。
- [x] 融合规则支持 class_counts。
- [x] 融合规则支持 tag_scores。
- [x] 专精奖励支持同职业重复段数。
- [x] 0.2 四职业已具备 x2/x3 两档专精奖励，共 8 个专精技能。
- [x] 隐藏职业支持履历条件、行为条件、击杀条件、世界交互条件。
- [x] 0.2 首条隐藏线索支持履历条件与标签强度条件。
- [x] 隐藏线索进入职业手册，而不是只靠聊天提示。
- [x] 隐藏职业解锁后能作为后续职业段选择。
- [x] 0.2 首个可选隐藏职业：灰烬守望者。

## 3. 技能与战斗表现 Checklist

### 3.1 技能运行时

- [x] 基础技能槽和冷却。
- [x] Z/X/V/B 快捷释放。
- [x] 技能武器右键触发原型。
- [x] 4 主动槽、4 被动槽、1 终极槽、1 种族槽。
- [x] 法力与体力资源。
- [x] 装备标签要求。
- [x] 技能释放失败反馈改为低干扰 HUD 提示。
- [x] 技能释放成功反馈进入客户端 FX。
- [x] 技能释放成功不刷聊天。

### 3.2 投射物与命中

- [x] 箭矢发射起点初步偏移，减少同点发射观感。
- [x] CareerProjectileEntity。
- [x] 自定义火球投射物。
- [x] 火球投射物使用专属 billboard renderer 和 `textures/entity/career_projectile/fireball.png`。
- [x] 自定义冰霜碎片投射物。
- [x] 弓术技能的轨迹尾迹。
- [x] 命中事件去重，避免多次结算。
- [x] 投射物命中方块和命中实体的反馈区分。
- [x] 下一发箭修饰器只消费一次。

### 3.3 技能 FX 标准

每个技能必须拆成三层表现：

- [x] 施放提示：手部、武器或脚下的短反馈。
- [x] 轨迹提示：投射物、冲刺路径、范围边缘或状态残影。
- [x] 命中反馈：火球爆点、受击粒子、音效。
- [x] 命中反馈：箭术技能命中反馈。
- [x] 命中反馈：其他非投射技能命中反馈。

FX 预算：

- [x] 普通技能单次粒子不超过 30 个。
- [x] 大范围技能粒子有配置倍率。
- [x] 只给可见半径内玩家发送表现包。
- [x] 持续粒子尽量由客户端本地播放。
- [x] 低配模式可降低粒子密度。

### 3.4 首批技能表现任务

- [x] 火球术：专属火球实体、手部火星、飞行火尾、命中爆点。
- [x] 蓄力射击：弓身光线、箭尾细线、命中强击反馈。
- [x] 散射：三支箭从不同侧向起点出射，轨迹可读。
- [x] 束缚射击：命中后有绿色/白色束缚环，不遮挡玩家视野。
- [x] 火焰箭：普通箭升级为带火尾的职业箭，命中有小型火花爆开。
- [x] 余烬爆发：范围边缘可读，粒子不糊屏。
- [x] 火焰步：移动方向有短残焰，不在准星中心堆粒子。

## 4. UI 与 HUD Checklist

### 4.1 HUD

- [x] 职业经验条：显示等级、当前 XP、下一级 XP。
- [x] 职业经验条：不覆盖原版快捷栏、血量、饥饿、经验条。
- [x] 技能槽：紧凑横向布局，不遮挡准星。
- [x] 技能槽：图标、按键、冷却数字都可读。
- [x] 技能槽：空槽有低调占位，不误认为可用技能。
- [ ] HUD 在 GUI Scale 2/3/4 下不重叠。
- [ ] 有屏幕变窄时的 fallback 布局。

### 4.2 职业主界面

- [x] 简化职业界面原型。
- [x] 顶部显示种族、职业等级、经验条。
- [x] 左侧职业时间线。
- [x] 中部可选职业卡。
- [x] 右侧预览标签变化、授予技能、未来段数。
- [x] 右侧预览新增融合。
- [x] 技能列表展示图标、名字、类型、冷却、资源。
- [x] 不能选择的职业展示原因。
- [x] 选择职业后 UI 刷新与服务端状态一致。

### 4.3 技能装配界面

- [x] 当前 Z/X/V/B 四主动槽支持点击装配/卸下。
- [x] 主动技能槽 4 个。
- [x] 被动技能槽 4 个预留位。
- [x] 终极槽 1 个。
- [x] 种族槽 1 个。
- [x] 技能筛选：全部、主动、被动、融合、隐藏、种族。
- [x] 点击装配。
- [ ] 拖拽装配。
- [x] 未满足装备条件的技能可看但不能装。

### 4.4 职业手册

- [x] 已知种族页面。
- [x] 已知职业页面。
- [x] 已解锁技能页面。
- [x] 隐藏线索页面。
- [x] 基础融合说明页面。
- [x] 数据包扩展技能可在手册中显示。

## 5. 美术、模型与音频 Checklist

### 5.1 美术规范

- [x] 32x32 技能图标规范。
- [ ] 64x64 种族图标规范。
- [ ] 职业图标规范。
- [x] 主动技能圆形、融合菱形、隐藏六边形、被动方形的视觉语言。
- [ ] 火焰、冰霜、神圣、黑暗、弓术各自颜色和形状语言。
- [x] 资源命名规则和缺失检查脚本。

### 5.2 首发图标

- [x] 火球、蓄力射击、散射、束缚射击、火焰箭等初版图标。
- [x] 火法、弓手、战士、牧师职业图标。
- [x] 人类、精灵、矮人、兽人种族图标。
- [x] 亡灵、魔裔种族图标。
- [x] 融合技能图标与基础技能图标可明显区分。
- [ ] 隐藏职业图标不剧透完整条件。

### 5.3 武器与物品模型

- [x] 余烬法杖初版物品。
- [x] 履历反曲弓初版物品。
- [x] 束缚发射器初版物品。
- [x] 符文剑初版物品。
- [x] 圣徽初版物品。
- [x] 每把武器有明确用途：技能入口、职业倾向或构筑引导。
- [x] 每把武器有 item texture。
- [x] 每把武器有 item model。
- [x] 关键武器具备手持角度 display 配置。
- [ ] 关键武器完成实机第三人称检查。
- [x] 不止原版武器：法杖、短弩/发射器、符文剑、圣徽进入初版实装。
- [ ] 死灵法器进入资产计划。

### 5.4 实体模型与材质

- [x] CareerProjectileEntity 基础材质。
- [x] 火球投射物材质。
- [x] 冰霜碎片材质。
- [ ] 神圣符文区域材质。
- [ ] 余烬精灵或召唤物模型。
- [ ] 隐藏职业试炼实体模型候选。

### 5.5 音频

- [x] 可复用原版音效清单。
- [x] 火焰、冰霜、神圣、黑暗、弓术释放音效分组。
- [x] 技能冷却失败使用轻提示音，不刷聊天。
- [x] 大技能音效有距离衰减。

## 6. 内容数据 Checklist

### 6.1 种族内容

- [x] 人类：泛用、多职业适性。
- [x] 精灵：弓术、感知、敏捷倾向。
- [x] 矮人：防御、锻造、守护倾向。
- [x] 兽人：近战、狂暴、体力倾向。
- [x] 亡灵：黑暗、死灵、暗处回复被动。
- [x] 魔裔：火焰、代价、火焰抗性被动。

### 6.2 基础职业内容

- [x] 战士。
- [x] 守护者。
- [x] 弓箭手。
- [x] 盗贼。
- [x] 火魔法师。
- [x] 冰魔法师。
- [x] 牧师。
- [x] 死灵法师。

### 6.3 融合技能候选池

- [x] 火焰箭原型。
- [ ] 爆裂火箭。
- [ ] 精准燃烧箭。
- [ ] 冰霜箭。
- [ ] 圣裁斩。
- [ ] 死亡斩击。
- [ ] 燃影突袭。
- [ ] 守护祝福。
- [ ] 元素召唤。
- [ ] 净化之焰。
- [ ] 诅咒箭。
- [ ] 药剂箭。

### 6.4 隐藏职业候选

- [x] 灰烬守望者。
- [x] 巫妖。
- [x] 死亡骑士。
- [ ] 审判者。
- [ ] 龙血战士。
- [ ] 魔弓手。

## 7. QA、性能与发布 Checklist

### 7.1 自动与手动验证

- [x] `./gradlew build`。
- [ ] `runClient` 新建世界、选择种族、选择职业、释放技能。
- [ ] `runServer` dedicated server 启动、进服、同步、释放技能。
- [ ] `runServer` 当前待确认 Minecraft EULA：`run/eula.txt` 为 `eula=false`，不自动代替用户同意。
- [ ] 数据 reload 成功和失败路径。
- [ ] GUI Scale 2/3/4。
- [x] 中英文语言 key 完整性。
- [x] 资源路径缺失检查。

### 7.2 必测玩法用例

- [ ] 种族持久化。
- [ ] 职业履历持久化。
- [ ] 死亡后职业数据保留。
- [ ] 火法 -> 弓手解锁火焰箭。
- [ ] 弓手 -> 火法解锁火焰箭。
- [x] 同职业重复解锁专精奖励。
- [x] 冷却中无法重复释放。
- [x] 未解锁技能无法伪造释放。
- [x] 无装备要求时可释放，有装备要求时必须校验。

### 7.3 性能

- [ ] 10 名玩家持续释放技能 10 分钟无明显 TPS 下降。
- [ ] 粒子低配模式有效。
- [ ] 召唤物数量上限可配置。
- [ ] 目标搜索不每 tick 全量扫描。
- [ ] FX 包按半径发送。

### 7.4 发布资料

- [ ] 玩家上手指南。
- [ ] 服主管理指南。
- [ ] 数据包作者指南。
- [ ] 平衡配置说明。
- [ ] 版本迁移说明。
- [ ] Changelog。
- [ ] 已知问题。

## 8. 第一批局部进攻点

接下来按顺序推进，避免同时开太多战线。

1. HUD 修正：职业经验条、低遮挡技能栏、冷却可读。
2. 技能表现 v2：火球术、蓄力射击、火焰箭先达到“三层表现”。
3. 美术管线：图标规范、资源命名、缺失检查。
4. 武器模型：现有三把武器从原型升级到职业入口道具。
5. 自定义投射物：CareerProjectileEntity 和火球实体。
6. dedicated server 验证：不让客户端表现掩盖服务端问题。

## 9. 每次完成项证据模板

```text
完成项：
改动文件：
构建结果：
实机步骤：
截图/日志：
遗留风险：
下一步：
```

### 2026-06-04 reload 稳定性

完成项：数据 reload 新 registry 原子替换、失败保留旧 registry、reload 后在线玩家重算技能/装配合法性、schema 错误定位到文件和字段。
改动文件：`CareerDataReloadListener.java`、`CareerDataEvents.java`、`CareerProgressionService.java`、`CareerData.java`、`ICareerData.java`、`CareerRuntimeState.java`、`JsonDataUtil.java`、`RegistryValidationException.java`、`tools/verify_resources.py`。
构建结果：`./gradlew --gradle-user-home .gradle-home build` 成功；资源 QA 输出 `54 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 2 XP sources`。
实机步骤：待 `runClient` 或服务器内执行 `/reload` 成功/失败路径。
截图/日志：待实机日志。
遗留风险：尚未完成手动 `/reload` 成功和故意破坏 JSON 的失败路径验证。
下一步：补 `/reload` 实机验收和数据包作者新增技能/融合规则流程。

### 2026-06-04 xp_sources 数据化

完成项：`xp_sources` 数据目录接入 registry；击杀和首次生物群系探索 XP 改为从数据读取；`/career add-xp` 保留命令数值但来源显示走统一 source key；QA 校验 XP source 数值和语言 key。
改动文件：`XpSourceDef.java`、`RegistrySnapshot.java`、`CareerDataReloadListener.java`、`CareerDataParsers.java`、`CareerRegistryValidator.java`、`JsonDataUtil.java`、`CareerProgressionService.java`、`CareerPlayerEvents.java`、`CareerCommands.java`、`tools/verify_resources.py`、`xp_sources/kill.json`、`xp_sources/biome.json`。
构建结果：`./gradlew --gradle-user-home .gradle-home build` 成功；资源 QA 输出 `54 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 2 XP sources`。
实机步骤：待 `runClient` 中击杀和首次进入生物群系验证 XP 数值与 HUD 变化。
截图/日志：待实机日志。
遗留风险：职业行为 XP 和结构发现 XP 已在后续记录补齐；防刷衰减仍未实现。
下一步：进入 runClient GUI scale/XP 实机验收，或进入防刷策略数据化。

### 2026-06-04 职业行为 XP

完成项：新增 `ranged_hit`、`healing`、`guard_block` 三个 XP source；职业箭矢命中生物给远程命中 XP；盾牌成功格挡给格挡 XP；牧师/圣域类技能仅在目标确实少血并实际治疗时给治疗 XP，避免满血空刷治疗经验。
改动文件：`CareerProgressionService.java`、`CareerPlayerEvents.java`、`SkillExecutorRegistry.java`、`xp_sources/ranged_hit.json`、`xp_sources/healing.json`、`xp_sources/guard_block.json`、`zh_cn.json`、`en_us.json`。
构建结果：`python3 tools/verify_resources.py` 成功；`./gradlew --gradle-user-home .gradle-home compileJava` 成功；`./gradlew --gradle-user-home .gradle-home build` 成功；资源 QA 输出 `57 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 5 XP sources`。
实机步骤：待 `runClient` 中测试职业箭矢命中、盾牌格挡、受伤后释放治疗技能，并观察 actionbar 与 HUD XP 变化。
截图/日志：待实机日志。
遗留风险：目前只做了“满血不算治疗 XP”的轻量防刷；短时间重复行为、刷怪笼/刷盾、同目标重复远程命中衰减仍未实现。
下一步：结构发现 XP 已在后续记录补齐；进入防刷策略数据化或 runClient XP 实机验收。

### 2026-06-04 低遮挡技能表现 v2

完成项：范围/守护/治疗类技能 FX 起点下移到脚边或手侧，降低第一人称视野遮挡；客户端 FX 粒子数量整体收敛，普通命中反馈和轨迹反馈更短；服务端 `sendParticles` 同步降低高度和数量；火球、弓术、齐射、风暴射手等投射技能发射起点增加手侧/左右差异，减少“从视野中心同一点射出”的观感；五把自定义技能武器第一人称 display 下压并缩小，减少贴近准星。
改动文件：`SkillExecutorRegistry.java`、`SkillFxRenderer.java`、`models/item/ember_staff.json`、`models/item/chronicle_recurve.json`、`models/item/snare_launcher.json`、`models/item/runic_blade.json`、`models/item/sunlit_sigil.json`。
构建结果：`python3 tools/verify_resources.py` 成功；`./gradlew --gradle-user-home .gradle-home compileJava` 成功；`./gradlew --gradle-user-home .gradle-home build` 成功；资源 QA 输出 `57 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 5 XP sources`。
实机步骤：待 `runClient` 中用第一人称连续释放火球、散射、圣域、陨火、守护类技能，并检查准星、目标和快捷栏可见性；待第三人称查看五把武器 display。
截图/日志：待实机截图。
遗留风险：仍未做 GUI Scale 2/3/4 截图矩阵；武器第三人称角度没有实机验收；非投射技能命中反馈已在后续记录补齐。
下一步：进入 `runClient` 实机截图验收；粒子倍率已在后续记录补齐。

### 2026-06-04 非投射技能命中反馈

完成项：复用 `S2CPlaySkillFxPacket` 的 `fxType` 扩展 `entity_hit` 与 `ally_hit`；近战/范围/火焰/圣域类技能在每个受影响目标中心播放低粒子命中反馈；治疗和增益目标播放友方反馈；治疗 XP 仍只在实际回血时发放，满血获得 buff 只播放表现不刷经验。
改动文件：`SkillExecutorRegistry.java`、`SkillFxRenderer.java`。
构建结果：`python3 tools/verify_resources.py` 成功；`./gradlew --gradle-user-home .gradle-home compileJava` 成功；`./gradlew --gradle-user-home .gradle-home build` 成功；资源 QA 输出 `57 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 5 XP sources`。
实机步骤：待 `runClient` 中用余烬爆发、突进斩、震地猛击、神圣新星、炽焰冲锋、日焰庇护、圣域降临等技能确认命中目标各自有反馈且不遮挡视野。
截图/日志：待实机截图。
遗留风险：大量目标同时命中时仍可能产生较多网络 FX 包；粒子倍率配置和目标数量上限已在后续记录补齐。
下一步：进入 runClient 视觉验收。

### 2026-06-04 粒子倍率与目标 FX 上限

完成项：COMMON 配置新增 `skillParticleMultiplier` 和 `maxTargetHitFxPerSkillCast`；FX 网络包携带服务端粒子倍率，客户端按倍率缩放技能粒子数量；服务端直接发出的技能粒子、箭矢修饰粒子和自定义火球尾焰也纳入倍率；非投射技能目标命中/友方反馈使用单次释放预算，默认最多 8 个目标发送目标 FX 包。
改动文件：`ModConfig.java`、`NetworkHandler.java`、`S2CPlaySkillFxPacket.java`、`ClientPacketDispatch.java`、`ClientPacketHandlers.java`、`SkillFxRenderer.java`、`SkillExecutorRegistry.java`、`CareerPlayerEvents.java`、`CareerProjectileEntity.java`。
构建结果：`python3 tools/verify_resources.py` 成功；`./gradlew --gradle-user-home .gradle-home compileJava` 成功；`./gradlew --gradle-user-home .gradle-home build` 成功；资源 QA 输出 `57 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 5 XP sources`。
实机步骤：待 `runClient` 或本地服务端中调低 `skillParticleMultiplier`，连续释放大范围技能验证粒子减少但音效仍保留；待召唤多目标验证 `maxTargetHitFxPerSkillCast` 限制目标反馈包。
截图/日志：待实机截图和配置文件。
遗留风险：XP 倍率和 PVP 开关已在后续记录补齐；召唤物上限仍待召唤系统落地后接入；目标 FX 上限只限制表现包，不影响实际伤害/治疗命中数。
下一步：结构发现 XP 已在后续记录补齐；进入 runClient 视觉和配置实机验收。

### 2026-06-04 XP 倍率与技能 PVP 开关

完成项：COMMON 配置新增 `careerXpMultiplier` 与 `enableSkillPvp`；所有统一职业 XP 发放按倍率缩放，包含击杀、探索、行为、命令 XP 以及人类选择职业的额外 XP；攻击性范围技能默认不影响玩家目标；职业箭命中玩家时按 PVP 开关跳过实体命中；自定义火球命中玩家时按 PVP 开关跳过伤害和点燃。
改动文件：`ModConfig.java`、`CareerProgressionService.java`、`SkillExecutorRegistry.java`、`CareerPlayerEvents.java`、`CareerProjectileEntity.java`、`职业履历融合MOD长期落地Checklist.md`。
构建结果：`python3 tools/verify_resources.py` 成功；`./gradlew --gradle-user-home .gradle-home compileJava` 成功；`./gradlew --gradle-user-home .gradle-home build` 成功；资源 QA 输出 `57 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 5 XP sources`。
实机步骤：待本地服务端中分别配置 `careerXpMultiplier=0/2` 验证 XP 不增长/翻倍；配置 `enableSkillPvp=false/true` 验证职业箭、火球和范围技能对玩家目标的影响。
截图/日志：待实机日志。
遗留风险：召唤物系统尚未落地，因此 `summon` 上限暂时没有运行时目标可接；完整“服务端配置支持 XP 倍率、PVP 开关、粒子倍率、召唤物上限”总项仍不勾满。
下一步：结构发现 XP 已在后续记录补齐；进入 runClient/本地服务端配置验收。

### 2026-06-04 结构发现 XP

完成项：新增 `structure` XP source；玩家低频探索检查同时检测当前位置关联的世界结构；按玩家 NBT 记录已发现结构 id，首次进入同一结构类型时发放职业 XP；结构发现 XP 进入统一 XP 倍率和 actionbar 反馈。
改动文件：`CareerProgressionService.java`、`CareerPlayerEvents.java`、`xp_sources/structure.json`、`zh_cn.json`、`en_us.json`、`职业履历融合MOD长期落地Checklist.md`。
构建结果：`python3 tools/verify_resources.py` 成功；`./gradlew --gradle-user-home .gradle-home compileJava` 成功；`./gradlew --gradle-user-home .gradle-home build` 成功；资源 QA 输出 `58 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 6 XP sources`。
实机步骤：待 `runClient` 中进入村庄、废弃传送门、矿井等结构区域，确认首次进入给 XP、重复进入同一结构 id 不重复给 XP。
截图/日志：待实机日志。
遗留风险：这是“按结构类型去重”，不是按每个具体生成实例去重；后续如果想鼓励探索多个同类型村庄，需要把结构 id + chunk/包围盒做成实例级 key。
下一步：进入 runClient 视觉与 XP 实机验收。

### 2026-06-04 法力/体力资源与释放拒绝

完成项：`CareerRuntimeState` 增加法力/体力当前值与上限；服务端按职业等级初始化资源并每秒回复；`CareerDataSnapshot` 同步资源给客户端；HUD 在技能槽下方显示低占屏法力/体力条；技能释放前由服务端校验资源，不足时使用 actionbar 低干扰提示；技能执行成功后才扣资源、写入冷却并同步；被动/不可直接释放类型会被服务端拒绝。
改动文件：`CareerRuntimeState.java`、`CareerDataSnapshot.java`、`CareerData.java`、`CareerPlayerEvents.java`、`CareerSkillService.java`、`CareerClientEvents.java`、`zh_cn.json`、`en_us.json`、`职业履历融合MOD长期落地Checklist.md`。
构建结果：`python3 tools/verify_resources.py` 成功；`./gradlew --gradle-user-home .gradle-home compileJava` 成功；`./gradlew --gradle-user-home .gradle-home build` 成功；资源 QA 输出 `58 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 6 XP sources`。
实机步骤：待 `runClient` 中连续释放高消耗技能验证资源下降、资源不足时不能释放且只在 actionbar 提示；等待自然回复后再次释放；观察 HUD 两条资源条不遮挡准星和快捷栏。
截图/日志：待实机截图。
遗留风险：装备标签要求已在后续记录补齐；资源上限与回复公式目前是保守默认值，后续应数据化或进入平衡配置。
下一步：进入 runClient HUD/资源实机验收，或补装配界面的装备条件灰态。

### 2026-06-04 装备标签要求

完成项：技能 JSON 支持 `requirements.equipment_tags`；QA 脚本校验 requirements 结构；新增 `careerchronicle:arcane_focus`、`ranged_focus`、`melee_weapon`、`holy_focus` 四类 item tag，覆盖自定义技能武器与原版弓/弩/剑；所有现有技能按法术、远程、近战、神圣分组补齐装备需求；服务端释放前检查主手/副手是否满足任一装备标签，错误装备时 actionbar 低干扰拒绝；技能武器 tooltip 和职业界面技能 tooltip 显示装备需求。
改动文件：`SkillDef.java`、`CareerDataParsers.java`、`CareerRegistryValidator.java`、`CareerSkillService.java`、`SkillWeaponItem.java`、`CareerScreen.java`、`tools/verify_resources.py`、`data/careerchronicle/tags/items/*.json`、`skills/*.json`、`zh_cn.json`、`en_us.json`、`职业履历融合MOD长期落地Checklist.md`。
构建结果：`python3 tools/verify_resources.py` 成功；`./gradlew --gradle-user-home .gradle-home compileJava` 成功；`./gradlew --gradle-user-home .gradle-home build` 成功；资源 QA 输出 `62 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 6 XP sources`。
实机步骤：待 `runClient` 中空手按 Z/X/V/B 验证技能被装备要求拒绝；手持余烬法杖释放火球、手持履历反曲弓/原版弓释放弓术、手持符文剑/原版剑释放近战、手持曦光圣徽释放治疗，确认成功路径。
截图/日志：待实机截图。
遗留风险：装配界面目前只显示装备要求，未把不满足当前装备的技能灰态化；“冷却、资源、装备要求、距离、目标合法性全部服务端校验”里的距离和目标合法性仍需专项审计后再勾。
下一步：进入 runClient 装备要求实机验收，或补装配 UI 灰态。

### 2026-06-04 装备条件装配灰态

完成项：新增 `SkillEquipmentRequirements` 统一判断与本地化装备要求文案；职业界面技能列表仍显示全部已解锁技能，但当前主手/副手不满足装备标签时，技能行降亮、测试按钮禁用、装配按钮禁用并在 tooltip 显示缺少装备；已装配技能即使当前装备不匹配也允许卸下，避免玩家被错误配置锁住；主动槽内不满足当前装备的技能显示低调警示；服务端装配包在写入 loadout 前校验当前装备要求，伪造装配包会被 actionbar 拒绝；主动槽保存现在保留中间空槽，避免卸下 Z/X/V/B 中间槽后技能前移，客户端也不会为空槽发送释放包。
改动文件：`SkillEquipmentRequirements.java`、`CareerSkillService.java`、`CareerLoadoutService.java`、`SkillWeaponItem.java`、`CareerScreen.java`、`CareerDataNbt.java`、`CareerData.java`、`CareerDataSnapshot.java`、`CareerClientEvents.java`、`zh_cn.json`、`en_us.json`、`职业履历融合MOD长期落地Checklist.md`。
构建结果：`python3 tools/verify_resources.py` 成功；`./gradlew --gradle-user-home .gradle-home compileJava` 成功；`./gradlew --gradle-user-home .gradle-home build` 成功；资源 QA 输出 `62 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 6 XP sources`。
实机步骤：待 `runClient` 中空手打开职业界面，确认技能可见但装配/测试按钮灰态；手持对应法杖/弓/剑/圣徽后重开界面，确认对应技能可装配；换成错误装备后确认已装配槽显示警示且可卸下；卸下中间槽后确认后续槽位不前移，按空槽键不发起释放。
截图/日志：待实机截图。
遗留风险：当前按“打开界面时的主手/副手装备”判定可装配，换装后释放仍由服务端再次校验；距离与目标合法性仍需单独补齐。
下一步：进入 runClient 装备装配灰态实机验收，或继续补距离/目标合法性服务端校验。

### 2026-06-04 技能筛选与类型视觉语言

完成项：职业界面技能列表新增 `全部/主动/被动/融合/隐藏/种族` 筛选按钮；切换筛选时重置滚动位置；技能列表改为 5 行窗口并支持鼠标滚轮滚动，避免 27 个已解锁技能把右侧栏挤爆；技能图标统一叠加类型外框，主动为圆角框、融合为菱形框、隐藏为六边形框、被动为方形框，终极和种族类型也预留了不同边框样式；融合技能现在即使复用 16x16 初版图标，也能通过外框和颜色与基础主动技能区分。
改动文件：`CareerScreen.java`、`SkillIconRenderer.java`、`zh_cn.json`、`en_us.json`、`职业履历融合MOD长期落地Checklist.md`。
构建结果：`python3 tools/verify_resources.py` 成功；`./gradlew --gradle-user-home .gradle-home compileJava` 成功；`./gradlew --gradle-user-home .gradle-home build` 成功；资源 QA 输出 `62 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 6 XP sources`。
实机步骤：待 `runClient` 中解锁多类技能后打开职业界面，逐个点击筛选按钮确认列表过滤正确；在全部技能列表中滚轮滚动确认按钮和图标同步移动；观察融合技能菱形外框、主动技能圆角外框、隐藏/被动预留框在小尺寸下是否可读。
截图/日志：待实机截图。
遗留风险：当前仍是 16x16 初版图标叠加视觉外框，尚未升级到真正的 32x32 技能图标规范；GUI Scale 2/3/4 下筛选按钮和列表仍需截图矩阵验证。
下一步：进入 runClient 职业界面筛选和 GUI Scale 实机验收，或继续推进 32x32 图标规范与图标重绘。

### 2026-06-04 职业手册系统与编译修复

完成项：创建 `ManualPage`、`ManualEntry`、`ManualIcon` 三个支撑类型；补齐职业手册全部 29 条 UI 语言 key（CN/EN）；修复 `CareerScreen` 48 个编译错误；添加 `FusionDef` 导入。
改动文件：`ManualPage.java`、`ManualEntry.java`、`ManualIcon.java`、`CareerScreen.java`、`zh_cn.json`、`en_us.json`。
构建结果：`./gradlew --gradle-user-home .gradle-home build` 成功；QA 输出 `62 json, 4 races, 5 classes, 27 skills, 6 fusions, 1 hidden unlocks, 6 XP sources`。

### 2026-06-04 投射物命中去重与反馈区分

完成项：`CareerArrowTags` 新增 `tryConsumeHit` 方法防止箭矢多次触发效果/经验；`CareerProjectileEntity` 新增 `hitConsumed` 标记防止自定义投射物重入伤害；命中 FX 区分 `entity_hit` 和 `block_hit` 两种类型；`SkillFxRenderer` 新增 `playBlockHit` 方法：方块命中粒子更少、音效不同。
改动文件：`CareerArrowTags.java`、`CareerProjectileEntity.java`、`CareerPlayerEvents.java`、`SkillFxRenderer.java`。
构建结果：BUILD SUCCESSFUL。

### 2026-06-04 HUD GUI Scale 适配

完成项：HUD 渲染改为响应式布局；XP 条、技能槽、资源条根据 `guiScaledWidth` 自动缩放；窄屏（<320 scaled px，对应 GUI Scale 4）使用 96px XP 条、16px 技能槽、10px 图标；槽间距和冷却数字位置动态计算。
改动文件：`CareerClientEvents.java`。
构建结果：BUILD SUCCESSFUL。

### 2026-06-04 余烬爆发和火焰步客户端 FX

完成项：`emberBurst` 和 `flameStep` 服务端 executor 移除冗余服务端粒子，改为通过 `NetworkHandler.playSkillFx` 发送客户端 FX 包；`SkillFxRenderer` 新增 `playEmberBurst`（3 层：地面火环 + 内圈小火 + 岩浆碎片，音效为火焰弹 + 爆炸）和 `playFlameStep`（脚下残焰轨迹，方向感明确，音效为火焰弹）。
改动文件：`SkillExecutorRegistry.java`、`SkillFxRenderer.java`。
构建结果：BUILD SUCCESSFUL。

### 2026-06-04 亡灵和魔裔种族

完成项：新增 `undead` 和 `demon` 种族 JSON 数据；亡灵特性 `dark_vitality`（暗处回复）、魔裔特性 `infernal_pact`（火焰抗性）服务端被动生效；生成占位种族图标；CN/EN 语言 key 完成；所有 6 种族 `allowed_classes` 更新为差异化配置（人类 8 职业、精灵 7、矮人 6、兽人 7、亡灵 7、魔裔 6）。
改动文件：`races/*.json`（×6）、`CareerPlayerEvents.java`、`zh_cn.json`、`en_us.json`、种族图标（×2）。
构建结果：QA 输出 `64 json, 6 races, 5 classes`→后续扩展到 `88 json, 6 races, 9 classes`。

### 2026-06-04 守护者、盗贼、冰魔法师、死灵法师

完成项：新增 4 个职业 JSON + 20 个技能 JSON（含基础技能 12 个 + 专精技能 8 个）；20 个服务端 executor 完整实现（非 stub）；生成 4 个职业图标 + 20 个技能图标占位 PNG；CN/EN 语言 key 完成。
改动文件：`classes/*.json`（×4）、`skills/*.json`（×20）、`SkillExecutorRegistry.java`、`zh_cn.json`、`en_us.json`、图标（×24）。
构建结果：QA 输出 `88 json, 6 races, 9 classes, 47 skills, 6 fusions, 1 hidden unlocks, 6 XP sources`。BUILD SUCCESSFUL。

### 2026-06-04 服务端校验与数据版本

完成项：技能释放前新增 `isAlive`/`isSpectator`/`isDeadOrDying` 校验；`CareerData` 序列化新增 `dataVersion` 字段（当前 v1）为未来数据迁移预留。
改动文件：`CareerSkillService.java`、`CareerDataNbt.java`、`CareerData.java`。
构建结果：BUILD SUCCESSFUL。

### 2026-06-04 自定义冰霜碎片投射物

完成项：`CareerProjectileRenderer` 根据 `skillId` 选择纹理（火焰/冰霜/暗黑三套）；`CareerProjectileEntity.tick` 根据 skillId 选择尾迹粒子（火焰/雪花/烟雾）；生成冰霜碎片和暗黑弹丸投射物纹理。
改动文件：`CareerProjectileRenderer.java`、`CareerProjectileEntity.java`、`textures/entity/career_projectile/frost_shard.png`、`dark_bolt.png`。
构建结果：BUILD SUCCESSFUL。

### 2026-06-04 粒子预算收敛

完成项：审计所有客户端 FX 粒子数量；大型 AoE 技能（meteor_rite/sanctuary_descent/unyielding_colossus/consecrated_slam/holy_nova）总粒子数从 32-38 收敛到 ≤30。
改动文件：`SkillFxRenderer.java`。
构建结果：BUILD SUCCESSFUL。

### 2026-06-04 终极槽和种族槽

完成项：`ICareerData`/`CareerData`/`CareerDataSnapshot`/`CareerDataNbt` 新增 `ultimateSlot` 和 `raceSlot` 字段；`CareerLoadoutService` 新增 `setUltimateSlot`/`setRaceSlot` 方法和 `SLOT_TYPE_ACTIVE/ULTIMATE/RACE` 常量；`C2SSetSkillLoadoutPacket` 升级为三字段（slotType, slot, skillId）；`CareerSkillService.isCastableSkill` 新增 `ultimate` 和 `race` 类型支持；CN/EN 语言 key 新增终极槽和种族槽标签。
改动文件：`ICareerData.java`、`CareerData.java`、`CareerDataSnapshot.java`、`CareerDataNbt.java`、`CareerLoadoutService.java`、`C2SSetSkillLoadoutPacket.java`、`CareerSkillService.java`、`CareerScreen.java`、`zh_cn.json`、`en_us.json`。
构建结果：BUILD SUCCESSFUL。

### 2026-06-04 32x32 图标规范与重绘

完成项：生成脚本 `generate_icons.py`；所有 47 个技能图标、9 个职业图标、6 个种族图标从 16x16 占位升级到 32x32 像素画；每个图标使用元素主题色（火/冰/神圣/暗黑/物理/隐匿）；包含 icon shape 和 silhouette 辨识度。
构建结果：BUILD SUCCESSFUL。

### 2026-06-04 武器第三人称 display 配置

完成项：所有 5 把自定义武器模型补齐 `head` 和 `fixed` display 配置，用于盔甲架和物品展示框展示；确认第三人称旋转/平移/缩放参数符合 Forge `item/generated` 规范。
改动文件：`models/item/ember_staff.json`、`chronicle_recurve.json`、`runic_blade.json`、`snare_launcher.json`。
构建结果：BUILD SUCCESSFUL。

### 2026-06-04 20 个跨职业融合技能

完成项：新增 20 个融合规则 JSON（覆盖冰法/守护/盗贼/死灵与其他职业的两两组合）；20 个融合技能 JSON（含冷却/资源/装备要求）；20 个服务端 executor 完整实现（thermal_shock/frost_arrow/frozen_blade 等）；20 个 32x32 占位图标；CN/EN 全部 20 条语言 key；`SkillExecutorRegistry` 新增 `canAffectAllyTarget` 和 `findNearestHostile` 辅助方法。
构建结果：QA 输出 `128 json, 6 races, 9 classes, 67 skills, 26 fusions, 1 hidden unlocks, 6 XP sources`。BUILD SUCCESSFUL。

### 2026-06-04 防刷策略

完成项：`CareerProgressionService` 新增 `applyAntiFarm` 方法；击杀经验在 60 秒窗口超过 10 次后减半、超过 20 次后归零；行为经验（远程命中/治疗/格挡）在 30 秒窗口超过 8 次后减半、超过 16 次后归零；窗口自动重置。
改动文件：`CareerProgressionService.java`。
构建结果：BUILD SUCCESSFUL。

### 2026-06-04 元素音效分组系统

完成项：`SkillFxRenderer` 新增 `ElementCategory` 枚举和 `elementCategory` 方法，根据技能名称关键词自动分类元素（火/冰/神圣/暗黑/隐匿/物理）；`playCategoryCast` 为每个元素选择对应粒子和音效（火焰弹声/碎玻璃声/紫水晶/凋零射击/末影传送/攻击扫荡）；`hitParticle` 重构为元素驱动，消除硬编码 skillId 判断。
改动文件：`SkillFxRenderer.java`。
构建结果：BUILD SUCCESSFUL。
