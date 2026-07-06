# 职业履历融合 MOD（Career Chronicle）开发总纲

> 版本：v2.0（2026-07-02）
> 地位：**后续开发的唯一指导文档**。合并并取代《职业履历融合MOD_综合开发指导文档.md》v1.0 与《职业履历融合MOD_资产生成方案.md》v1.0（两份旧文档建议移入 `docs/archive/` 归档）。与任何旧 Checklist 冲突时以本文档为准。
> 覆盖维度：代码架构、产品、美术、音效、粒子、动画 + 资产生产工具链。
> 子任务详细设计文档按 CLAUDE.md 流程（设计 → 测试用例 → 编码）另行产出，但不得违背第一章基线。

---

## 第一章 不可违背的设计基线（宪法层）

以下条目均为已定案决策，后续任何设计不得违背；如需变更必须先修订本章。

### 1.1 玩法与架构基线（B1-B6）

| # | 基线 | 定案内容 |
|---|---|---|
| B1 | **等级封顶** | 50 级 / 5 个职业段为**永久上限**。删除一切"100级/10段"预留设计。所有数值曲线、UI、融合规则按封顶设计。 |
| B2 | **无序一致原则** | 履历结算无序：最终能力只取决于选择的多重集合（class_counts + tag_scores + hidden_flags），与选择顺序无关。顺序只影响解锁时机与过渡体验。**任何新系统（含技能升级）设计时必须自证满足本条。** |
| B3 | **技能升级采用方案 A（履历池派生）** | 技能等级不是存储值，是**运行时派生值**：`level(skill) = f(class_counts, tag_scores)`，由技能 JSON 声明派生规则。天然满足 B2，capability 无需新增字段。 |
| B4 | **服务端权威** | 一切技能释放、装配、升级判定在服务端完成，客户端仅做展示与预测。现有校验链（存活→解锁→装配→冷却→装备→资源）不得削弱。 |
| B5 | **数据驱动到效果层** | 技能效果必须组件化（第二章），新增技能/调数值原则上零 Java 改动。手写 executor 仅保留给"确实无法组件表达"的少数特殊技能。 |
| B6 | **世界观隐喻：编年史** | 世界会"铭记"灵魂的经历。职业段=篇章（Chapter），技能=铭文（Inscription），融合=共鸣（Resonance），隐藏职业=被抹除的禁忌篇章（Forbidden Chapter），种族起源=第一篇章。全部原创表达，不使用 Overlord 专有名词。**六个维度的所有产出必须能用这套隐喻解释。** |

### 1.2 资产生产基线（D1-D3，2026-07-02 定案）

| # | 基线 | 定案内容 |
|---|---|---|
| D1 | **90 张体系图标 = 程序生成闭环** | 技能/职业/种族图标由 Pillow 管线程序生成，Claude 多模态自审迭代，用户只终审 contact sheet。 |
| D2 | **17 张关键图标 = 用户 AI 自生成** | 11 职业 + 6 种族的高质量版由**用户使用 AI 图像工具自行生成**；Claude 负责：① 产出逐图标 AI 生成规格书（prompt + 色板 + 风格约束）；② 对产出做程序化后处理（降采样、色板对齐、统一描边与形状框）；③ 多模态视觉 QA。 |
| D3 | **音效 = S2 优先，S3 兜底** | 全部音效**先走供给线 S2（CC0 素材包 + ffmpeg 加工）**；经用户耳测，若某层（预期风险最高为事件层品牌音）迭代 2 轮仍不达标，**该层升级到 S3（ElevenLabs 等 AI 音效生成，届时再注册 API key）**。S1（程序合成）降级为 S2 覆盖不到时的补充工具。 |

### 1.3 技能获取节奏（B2/B3 的具体化，已确认）

- 首次选择某职业：获得该职业**全部基础技能**。
- 重复选择同一职业：解锁该职业 1-2 个进阶技能（JSON `unlock_at_count: 2/3` 声明），同时相关技能因 class_counts/tag_scores 上升**自动升级**（方案 A 派生）。
- 选择不同职业：获得新职业全部基础技能，同时已有技能中共享标签者自动升级。
- 玩家实际战力由装配槽（4 主动 + 终极 + 种族）封顶；技能池扩大只增加构筑广度，不增加瞬时功率。

---

## 第二章 代码架构篇

### 2.1 技能效果组件化（0.4 核心任务，一切内容扩展的地基）

**现状**：73 个技能效果硬编码于 `SkillExecutorRegistry`（1864 行），数值写死。
**目标**：技能 JSON 声明效果组件序列，Java 只提供组件解释器。

**技能 JSON 目标格式（Schema v2）：**

```json
{
  "id": "careerchronicle:fireball",
  "type": "active",
  "cost": { "mana": 20 },
  "cooldown_ticks": 120,
  "upgrade": { "source": "tag:fire", "max_level": 5 },
  "effects": [
    { "op": "projectile", "entity": "careerchronicle:career_fireball", "speed": 1.5 },
    { "op": "damage", "amount": 5.0, "per_level": 1.0, "on": "projectile_hit" },
    { "op": "ignite", "seconds": 3, "per_level": 1, "on": "projectile_hit" }
  ],
  "fx": {
    "cast_sound": "careerchronicle:skill.cast.fire",
    "cast_particle": "careerchronicle:ember_rune",
    "hit_sound": "careerchronicle:skill.hit.fire",
    "hit_particle": "minecraft:lava",
    "camera_shake": { "strength": 0.4, "ticks": 6 },
    "cast_circle": true
  }
}
```

**首批效果组件（op）清单**——覆盖现有 73 技能约 85% 的行为：

| op | 参数 | 说明 |
|---|---|---|
| `damage` | amount, per_level, damage_type | 对目标造成伤害 |
| `apply_effect` | effect, duration, amplifier, per_level_duration | 施加药水效果（增益给自己/减益给目标） |
| `heal` | amount, per_level, target(self/ally/area) | 治疗 |
| `knockback` | strength, direction | 击退/击飞 |
| `projectile` | entity, speed, count, spread | 发射投射物（复用现有 3 种投射物实体） |
| `aoe` | radius, per_level_radius, filter | 范围选取目标，后续 op 作用于选中集 |
| `dash` | distance, direction | 位移 |
| `shield` | absorb_amount, duration | 吸收护盾（映射到 absorption） |
| `resource` | type, amount | 回复/消耗法力·体力 |
| `summon` | entity, count, lifetime_ticks, cap | 召唤（0.5+ 落地，schema 先预留） |

**解释器架构：**
- `skill/effect/EffectOp` 接口：`void apply(EffectContext ctx, JsonObject params)`；`EffectContext` 携带 caster、target 集、skill level、触发时机（cast / projectile_hit / tick）。
- `EffectOpRegistry`：`Map<String, EffectOp>`，Forge 事件注册，**允许其他 MOD/附属注册自定义 op**——对整合包生态的关键开放点。
- `CareerRegistryValidator` 扩展：校验每个技能的 op 名存在、参数类型正确；reload 原子性沿用现有机制。
- 兼容路径：JSON 保留可选 `"executor"` 字段，存在则走旧硬编码路径。迁移期两轨并行，迁移完成后 `SkillExecutorRegistry` 收缩到 ≤10 个特殊技能（预计 <300 行）。

**方案 A 升级的实现**：`SkillLevelService.levelOf(ICareerData, SkillDef)` 纯函数——解析 `upgrade.source`（`tag:fire` / `class:fire_mage`），从履历池读取计数，`level = min(max_level, 1 + 计数超出解锁需求的部分)`。**不存储、不迁移、无 DATA_VERSION 变更**；快照同步沿用现有 S2C snapshot（客户端用同一纯函数本地计算显示）。

**迁移步骤与测试要求（TDD）：**
1. 先写 JUnit（见 2.2）：解释器对 mock context 的每个 op 行为、`SkillLevelService` 派生公式、**无序性回归测试**（同一多重集不同顺序 → 逐技能等级一致）。
2. 迁移 5 个试点技能（火球/治疗/冲锋/箭雨/护盾各代表一类），GameTest 增加"组件技能实际造成伤害"用例。
3. 批量迁移剩余模板化技能；`allExecutorsExist` GameTest 改造为"executor 或 effects 二选一必须合法"。

### 2.2 测试与工程基建（0.4 内完成）

- **JUnit 下沉**：`src/test/java` 建立，覆盖纯逻辑——`CareerProgressionMath`、融合规则匹配、tag_scores 聚合、`SkillLevelService`、效果组件参数解析。目标：不启动 MC 即可秒级验证核心规则。
- **datagen 接入**：`GatherDataEvent`，先迁移 recipes（25 个）与 item model JSON；`verify_resources.py` 保留作为兜底但不再是唯一防线。
- **卫生项**：删除 `com.example.examplemod` 空目录；网络协议版本与 MOD 版本联动（`0.4.0`），协议不匹配拒绝连接并给出明确提示。
- **多人验证（P0，不等 0.4）**：确认 EULA，dedicated server + 2 客户端 30 分钟：并发施放、特效互见、断线重连、数据持久化、/reload。

### 2.3 世界内载体的技术选型（0.5）

- **战利品成书**：Forge Global Loot Modifiers（GLM）注入原版 loot table（地牢/要塞/下界堡垒箱子），成书内容走 lang key（保证中英双语）。
- **成就树**：`data/careerchronicle/advancements/` 纯 JSON，"编年史"页签，按篇章推进；触发器用现有事件（选种族/选职业/融合解锁）补发自定义 criteria。
- **融合预览**：服务端在 snapshot 中附带"可达融合"计算结果（复用融合规则引擎，输入=假设选择后的履历池），客户端只渲染——保持 B4。
- **结构/仪式（1.0）**：小型结构用 1.20.1 原生 structure/jigsaw JSON；巫妖命匣仪式用"特定物品 + 特定方块交互"实现，不做多方块结构。

---

## 第三章 产品篇

### 3.1 版本路线图

| 版本 | 代号 | 主题 | 内容 |
|---|---|---|---|
| **0.3-alpha** | — | **立即公开**（P0，两周内） | 现有内容清理后直接发 Modrinth：藏掉被动槽、玩家一页指南、反馈渠道（Discord/GitHub Issues）。目的：真实构筑行为数据 |
| **0.4** | 共鸣 Resonance | 架构版 | 效果组件化 + 方案 A 升级 + JUnit/datagen + 世界观圣经与全量风味文案 + 音效 v1（S2 线）+ 图标管线 v2 |
| **0.5** | 篇章 Chapters | 内容版 | 隐藏职业线索成书 + 编年史成就树 + 融合预览 UI + 编年符文粒子 + GUI 编年史化重皮 + 17 张关键图标（D2 流程）+ 多人稳定性达标 → **公开 Beta** |
| **1.0** | 编年史 Chronicle | 发布版 | Beta 反馈驱动的平衡调整 + 石碑结构与命匣仪式 + 召唤系统 + 三份指南（玩家/服主/数据包作者）+ 正式发布 |

### 3.2 Alpha 发布与反馈闭环（0.3-alpha）

- 发布物：Modrinth 项目页（首段文案直击卖点："你的每一段职业履历都参与构筑——顺序不重要，组合才重要"）、明确标注 Alpha、已知问题清单。
- 反馈指标（决定 0.4/0.5 资源分配）：① 玩家平均触发首个融合的时长（目标 2-4h）；② 最常见构筑 TOP5（判断 26 个融合覆盖是否够）；③ 差评关键词分布（美术/引导/内容量/bug）。
- 收集方式：反馈表单 + Discord 提问模板，不做遥测。

### 3.3 融合预览 UI 规格（0.5）

- 位置：职业选择界面右侧新增"共鸣预览"栏。
- 逻辑：悬停候选职业 → 列出该选择将**点亮/推进**的融合：已解锁→彩色图标+名称；条件将满足→高亮边框"即将共鸣"；差 1 个条件→剪影+谜语提示（文案出自世界观圣经）；差 ≥2 条件→不显示（保留探索感）。
- 服务端计算可达集，客户端渲染（见 2.3）。

### 3.4 内容供给红线

- 每个新职业入库标准：3 基础技能 + 2 进阶技能（`unlock_at_count`）+ 至少 2 个与现有职业的融合 + 风味文案 + 图标 + 视听规格表行（第八章）。缺一不入库。
- 融合覆盖目标（1.0）：8 基础职业两两组合 28 种中至少 24 种有融合技能（现 26 个需核对分布是否均匀而非堆在热门组合上）。

---

## 第四章 美术篇（视觉规范 + 生产工具链）

> 本机工具链现状：python3 + Pillow ✅、ffmpeg ✅、uvx/npx ✅；numpy ❌（pip 可装）。
> 能力边界：**图像 Claude 可多模态直接查看**——图标可"生成→看图→迭代"全闭环自主完成。

### 4.1 视觉语言体系（形状 = 技能类型，颜色 = 标签家族）

**形状语言（现有体系保留并正式化）：** 主动=圆角方、融合=菱形、隐藏=六边形、被动=方形、终极=双框、种族=角框。新增：**升级等级用底部罗马数字刻痕 I-V**（"篇章刻度"隐喻），不用数字角标。

**色彩体系（20 标签归并为 10 个家族，全项目唯一色板）：**

| 家族 | 涵盖标签 | 主色 | 辅色 | 隐喻 |
|---|---|---|---|---|
| 烈焰 | fire | #E25822 橙红 | #F5A623 金黄 | 燃烧的墨迹 |
| 寒霜 | frost | #7FD4E8 冰蓝 | #E8F6FA 霜白 | 凝固的书页 |
| 神圣 | holy, heal | #F5D76E 圣金 | #FFFDF2 辉白 | 烫金铭文 |
| 幽暗 | dark, undead | #6B4A9E 幽紫 | #1D1526 墨黑 | 被抹除的字 |
| 锋刃 | melee, crit | #9AA0A6 铁灰 | #C0392B 猩红 | 划破纸面的刻痕 |
| 壁垒 | defense, shield | #B08D57 青铜 | #6E5B3E 褐 | 封印的书扣 |
| 飞羽 | projectile, bow, precision | #6FA35C 橄榄绿 | #EFE6D0 羽白 | 疾书的笔锋 |
| 诡影 | stealth, mobility | #3E5C61 暗青 | #8C9EA3 烟灰 | 页间的空白 |
| 秘法 | spell, control | #4A69BD 靛蓝 | #9B7EDE 淡紫 | 流动的墨水 |
| 缚灵 | summon, alchemy, support | #2E8B6E 翡翠 | #A3D9C5 淡青 | 页上走出的插画 |

融合技能图标 = 两个家族主色的**对角渐变**（无序原则的视觉表达：渐变无方向主次）。

### 4.2 生产线一：90 张体系图标（D1，程序生成全闭环，0.4）

**工具**：Python + Pillow（替换现有 stdlib 手写 PNG 方式：抗锯齿、渐变、图层合成、批量导出）。

**工作流（Claude 全自主）：**
1. 重写 `generate_icons.py` → `tools/icon_pipeline_v2.py`：读取 4.1 色板 + 形状语言作为配置表，每图标 = 形状模板 × 家族渐变 × motif（火焰/冰晶/箭/盾等 20 个基础 motif 函数）× 2px 描边 × 左上高光。
2. 自动拼 **contact sheet**（90 图标 + id 标注拼成一张大图）。
3. Claude 用多模态查看 contact sheet → 逐个评审（辨识度/色板一致性/形状语言正确性）→ 调 motif 参数 → 重生成，循环到达标。
4. 融合对角渐变、罗马刻痕 I-V 徽标同管线产出。
5. `verify_resources.py` 做数量/命名/尺寸兜底校验。

**用户参与**：仅终审 contact sheet，点名不满意的图标。

### 4.3 生产线二：17 张关键图标（D2，用户 AI 自生成 + Claude 规格书与后处理，0.5）

11 职业 + 6 种族的高质量版，流程三段式：

1. **Claude 产出《AI 生成规格书》**（逐图标一节）：中英文 prompt（含主体 motif 描述、"dark fantasy chronicle/manuscript theme"风格词、家族色 hex 约束、负面词）；建议用户以 512-1024px 生成（AI 不擅长直接出 32×32）。
2. **用户用任意 AI 图像工具生成**，产出放入约定目录（如 `art_input/`），每图标可放多张候选。
3. **Claude 后处理 + QA**：Pillow 批处理——降采样到 32×32（缩放+色彩量化到家族色板）、统一加 2px 描边与形状语言外框（保证与 90 张程序图标体系一致）→ 多模态视觉评审 → 拼 contact sheet 供用户终审。不达标的单张打回重新生成。

### 4.4 生产线三：GUI 底图/九宫格（羊皮纸主题，0.5）

- **默认路线**：Pillow 程序生成——噪声肌理羊皮纸 + 程序化墨线边框 + 家族色烫金点缀，裁九宫格。
- **升级路线（可选）**：若 Beta 反馈集中吐槽观感，用户可用 AI 图像工具生成羊皮纸/古书纹理原图（同 4.3 流程），Claude 后处理成九宫格。
- 交付规范：图标 32×32、GUI 底图 256×256，命名沿用 `textures/gui/skill/<skill_id>.png` 现状。

---

## 第五章 音效篇（音效体系 + 生产工具链）

> 能力边界：**Claude 不能"听"音频**——可生成、加工、ffprobe 技术校验（声道/采样率/时长），但审美 QA 必须由用户耳测，按反馈迭代。

### 5.1 分层音效体系（0.4 落地 v1，共约 28 个 .ogg）

| 层 | 事件 | 数量 | 规格 |
|---|---|---|---|
| 施放层 | `skill.cast.<家族>`（10 家族各 1） | 10 | mono 44.1kHz（3D 定位），0.5-1.2s |
| 命中层 | `skill.hit.fire/frost/holy/dark/physical` | 5 | mono，0.2-0.5s，各 2-3 变体防重复感 |
| UI 层 | `ui.chronicle_open`（翻页）、`ui.skill_equip`（笔尖落纸）、`ui.tab_flip`、`ui.deny`（干墨刮纸） | 4 | stereo |
| 事件层 | `event.level_up`、`event.segment_choice`（重音，每 10 级的仪式感）、`event.fusion_unlock`（**品牌音**：两个音阶交叠共鸣）、`event.hidden_unlock`（低语+纸张撕裂）、`event.skill_upgrade` | 5 | stereo，fusion_unlock 2-3s |
| 环境层（1.0） | 命匣仪式、石碑激活 | ~4 | — |

### 5.2 生产供给线（D3 定案：S2 优先 → S3 兜底，S1 补充）

**主线 S2：CC0 素材包 + ffmpeg 加工（全部 24 条 0.4/0.5 音效的第一供给线）**
- 素材源：**Kenney.nl 全 CC0 音效包**（Impact Sounds / UI Audio / RPG Audio / Digital Audio 等，zip 可 curl 直接下载，无需账号）；备用 freesound.org（质量上限更高，需注册免费账号取 API key 后由 Claude 调 REST API 按 `license:CC0` 搜索下载——Kenney 不够用再启用）。
- 加工：ffmpeg 单工具完成（无需 sox）——变调 `asetrate+aresample`、叠层 `amix`、混响 `aecho`、倒放 `areverse`、裁切淡出 `afade`。事件层品牌音用**多层叠加**逼近（如融合解锁 = 钟鸣 + 倒放 whoosh + 翻页声三层错位混合）。
- 产物统一 mono/44.1k/ogg（UI/事件层 stereo），ffprobe 自动校验规格，来源与许可逐条写入 CREDITS.txt。**禁止使用许可不明素材。**

**兜底 S3：AI 音效生成（升级条件明确）**
- 触发条件：某层经用户耳测**迭代 2 轮仍不达标**（预期风险最高：事件层品牌音），该层升级 S3。
- 首选 ElevenLabs 音效 API（有官方 MCP，免费档有月度额度）：届时注册 API key → `claude mcp add elevenlabs -e ELEVENLABS_API_KEY=xxx -- uvx elevenlabs-mcp`（具体命令以官方文档为准，接入前先验证）→ 按 5.1 的音效描述写 prompt 批量生成候选 → 用户耳测挑选 → ffmpeg 规格化。备选：Stability AI Stable Audio API（curl 直调）。确认所用档位可商用/再分发并记入 CREDITS.txt。

**补充 S1：程序合成（S2 素材覆盖不到时的填缝工具）**
- `pip3 install numpy` → `tools/sfx_synth.py` 合成 WAV（噪声整形+ADSR 做 whoosh/翻页/冰裂；加法合成做钟鸣；FM 做暗系低鸣）→ ffmpeg 转 ogg。典型用途：UI 层的"笔尖落纸"这类 CC0 包里找不到的特定音。

**迭代协议（三条线通用）**：Claude 每条音效出 3-5 个候选 → 用户耳测（每批约 10-15 分钟）→ 反馈用"编号+形容词"（如"cast.frost 第 2 个太尖锐、尾音长一点"）→ Claude 调参重出。

### 5.3 技术接入

- `assets/careerchronicle/sounds.json` + `DeferredRegister<SoundEvent>`；施放/命中音 id 写入技能 JSON 的 `fx` 字段——**音效跟数据走，不跟代码走**。
- `subtitle` key 全部配齐（无障碍 + 中英双语，`subtitles.careerchronicle.*`）。
- 迁移：67 处 `SoundEvents.*` 原版调用中，技能相关的随组件化迁入 JSON；原版音仅保留给通用交互。

---

## 第六章 粒子篇（视觉特效）

### 6.1 粒子身份：编年符文（Chronicle Runes）

全 MOD 粒子的统一识别符号：**发光的符文字符碎片**（呼应 B6"技能=铭文"）。

- **自定义 ParticleType（0.5，共 3 个）**：
  - `chronicle_rune`：8-12 帧符文字符 sprite sheet，颜色经 `ParticleOptions` 传 RGB 参数化——**一个粒子类型服务 10 个家族**，不做 10 个类型。
  - `resonance_ring`：融合技能施放时脚下扩散的双色符文环（两家族色对角对称——无序原则的动效表达）。
  - `forbidden_wisp`：隐藏职业专用，墨黑拖尾+紫边（"被抹除的字在燃烧"）。
- 注册：`DeferredRegister<ParticleType<?>>` + `RegisterParticleProvidersEvent`；sprite 走 `assets/particles/*.json`（sprite sheet 本身由图标管线 v2 同工具产出）。

### 6.2 使用规则与性能预算

- 组件化 JSON 的 `fx.cast_particle` / `fx.hit_particle` 声明，`SkillFxRenderer` 从"每技能手写"收缩为"按声明播放"的通用播放器（742 行预计减半）。
- 分层预算：施放 ≤20 粒子、命中 ≤12/目标、持续性光环 ≤4/tick；沿用 `skillParticleMultiplier`（0-2）与 `maxTargetHitFxPerSkillCast` 限流、`PacketDistributor.NEAR` 32 格——**新粒子不得绕过这两个既有阀门**。
- 原版粒子继续用于填充层（烟/火星/水花），自定义符文只出现在"施放起手"和"融合/隐藏"两个高价值时刻——稀缺性即辨识度。

---

## 第七章 动画篇

### 7.1 决策：1.0 前不引入骨骼动画库

原版 1.20.1 无玩家骨骼动画；GeckoLib/PlayerAnimator 引入成本高且收益集中在第三人称。**1.0 前用"屏幕空间 + 世界空间贴片"达成 80% 打击感：**

| 手段 | 现状 | 0.4-0.5 计划 |
|---|---|---|
| 镜头震动 | `CameraShakeManager` 已有 | 参数入 `fx` JSON（强度/时长），重技能重震、轻技能微震 |
| 受击反馈 | `HitFlashOverlay` 已有 | 保留 |
| 施法阵 | 无 | 施放起手脚下渲染符文法阵贴片（2D quad，家族色，0.3-0.5s 旋转淡出）——最廉价的"施法感"来源 |
| 手部动作 | 无 | 复用原版 `swing` + 施法物品 `use` 动画（弓拉弦式蓄力），零新增依赖 |
| 投射物 | 3 种实体已有 | 增加旋转 + 符文拖尾粒子 |
| GUI 动画 | 无 | 翻页过渡（5 帧）、技能解锁"墨迹晕开"显现（alpha+scale 插值）、融合解锁全屏"双色共鸣"一次性动效（配品牌音——玩家截图/录屏的时刻） |

### 7.2 召唤系统的动画预留（1.0）

召唤物落地时评估：若 ≤3 种且形态简单（漂浮灵体类），用原版 `EntityModel` 手写关键帧，不引库；若 1.0 后扩展多种类召唤物，届时引 GeckoLib 且仅用于召唤物实体，不碰玩家动画。

---

## 第八章 内容生产流水线：技能视听规格表

**六个维度在"每个技能"上的交汇点。** 建立 `docs/技能视听规格表.md`，每技能一行，作为内容入库的强制检查单：

| 列 | 说明 | 责任维度 |
|---|---|---|
| skill_id / 中英文名 | 命名遵守世界观术语表 | 产品/世界观 |
| 家族（色板） | 10 家族之一 | 美术 |
| 形状 | 6 形状语言之一 | 美术 |
| effects 组件序列 | op 列表与数值 | 架构 |
| upgrade 规则 | `source` + `max_level` | 架构/产品 |
| cast/hit 音效 id | 5.1 清单内 | 音效 |
| cast/hit 粒子 | 6.1 清单内 + 预算内 | 粒子 |
| 震动/法阵参数 | 7.1 | 动画 |
| 风味文案 key | 出自世界观圣经 | 产品 |

新技能 = 填一行规格表 + 写一个 JSON + 跑一遍 GameTest。**这张表是"内容供给速度"问题的最终答案**：0.4 完成后，一个技能的入库成本从"写 Java + 编译"降到"半小时填表写 JSON"。存量 73 技能在组件化迁移时同步补齐。

---

## 第九章 里程碑、依赖与验收

### 9.1 执行顺序

```
P0（立即，2周）: 文档大扫除 → EULA+多人30min → 藏被动槽 → 0.3-alpha 发布
0.4（架构）:  [JUnit先行] → 效果组件解释器 → 方案A升级 → 试点迁移5技能 → 批量迁移
              ∥ 并行：世界观圣经 → 风味文案 → 音效v1(S2线) → 图标管线v2(全自主)
0.5（内容）:  线索成书+成就树 → 融合预览UI → 符文粒子 → GUI重皮
              ∥ 并行：17张关键图标（用户AI生成→Claude后处理QA）→ 多人达标 → 公开Beta
1.0（发布）:  Beta反馈平衡 → 石碑+命匣仪式 → 召唤 → 三份指南 → 发布
```

**关键依赖**：方案 A 升级、音效入 JSON、粒子入 JSON、法阵参数入 JSON 全部依赖**组件化 Schema v2 先定稿**——0.4 第一个子任务必须是 `0.4-01 Schema v2 与解释器`（按 CLAUDE.md 流程：设计文档 → 测试用例文档 → 编码）。

### 9.2 各版本验收标准（不达标不进入下一版本）

- **0.4**：GameTest 全绿 + JUnit 全绿 + 无序性回归测试通过 + 迁移后技能实机行为与迁移前比对无差异 + `SkillExecutorRegistry` ≤300 行 + 音效 v1 用户耳测通过（或已按 D3 触发 S3 升级流程）。
- **0.5**：新玩家不看外部资料 2-4h 内触发首个融合（≥2 名未接触过本 MOD 的玩家实测）+ 多人 30min 零异常 + 融合预览对 26 个融合全部可达 + 17 张关键图标入库。
- **1.0**：Beta 期收集 ≥30 份反馈且 TOP3 问题已处理 + 三份指南 + 发布包无 example 残留、协议版本对齐、CREDITS.txt 许可完整。

### 9.3 用户参与事项清单（Claude 无法代办的全部事项）

| 时点 | 事项 | 预计投入 |
|---|---|---|
| P0 | 确认 EULA、注册 Modrinth 账号发布 0.3-alpha | 1h |
| 0.4 | 音效每批出样后耳测反馈 | 每批 10-15min，预计 3-4 批 |
| 0.4 末 | 若事件层 2 轮不达标 → 决定注册 ElevenLabs key（D3 兜底） | 15min |
| 0.5 | 按规格书用 AI 工具生成 17 张关键图标原图（每张 2-3 候选） | 2-4h |
| 0.5 | 图标/GUI contact sheet 终审 | 30min |
| 0.5 | 找 2 名新玩家做首融合实测 | 协调成本 |
| 1.0 | Beta 反馈渠道运营、约稿/许可等对外事务 | 持续 |

---

## 附录 A：标签家族 → 视听映射总表（速查）

| 家族 | 主色 | 施放音 | 命中音 | 粒子基调 | 法阵 |
|---|---|---|---|---|---|
| 烈焰 | #E25822 | 火焰爆燃+纸张燃烧 | 灼烧噼啪 | 橙红符文+火星 | 橙红旋转 |
| 寒霜 | #7FD4E8 | 冰晶凝结+墨水冻结 | 碎冰 | 冰蓝符文+雪雾 | 冰蓝缓旋 |
| 神圣 | #F5D76E | 钟鸣泛音 | 柔和钟声 | 金色符文+光尘 | 金色绽放 |
| 幽暗 | #6B4A9E | 低语+纸页翻动倒放 | 闷响+吸入感 | 紫黑符文+墨雾 | 紫黑收缩 |
| 锋刃 | #9AA0A6 | 金属出鞘 | 利刃入肉 | 灰白刻痕划痕 | 无（近战不出阵） |
| 壁垒 | #B08D57 | 书扣锁合+盾鸣 | 钝响 | 青铜符文环绕 | 青铜稳定环 |
| 飞羽 | #6FA35C | 笔锋疾书+弓弦 | 箭簇入靶 | 绿白流线 | 无 |
| 诡影 | #3E5C61 | 纸页轻响 | 短促刺音 | 烟灰淡出符文 | 一闪即逝 |
| 秘法 | #4A69BD | 墨水流动+泛音上行 | 奥术爆鸣 | 靛蓝符文轨迹 | 靛蓝多层 |
| 缚灵 | #2E8B6E | 书页翻动+生灵苏醒 | — | 翡翠符文成形 | 翡翠描边 |

## 附录 B：待产出的子任务清单（按 CLAUDE.md 流程逐个走"设计→测试用例→编码"）

1. `0.4-01 效果组件 Schema v2 与解释器`（**下一个任务**）
2. `0.4-02 方案A技能升级派生服务`
3. `0.4-03 世界观圣经`（纯文档，无测试环节）
4. `0.4-04 音效体系 v1（S2 线）`
5. `0.4-05 图标管线 v2`（可与 0.4-01 并行，全自主）
6. `0.5-01 融合预览 UI`
7. `0.5-02 线索成书与成就树`
8. `0.5-03 编年符文粒子`
9. `0.5-04 17张关键图标 AI 生成规格书 + 后处理管线`
