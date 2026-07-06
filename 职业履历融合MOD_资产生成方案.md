# 资产生成方案（美术 + 音效的具体生产工具链）

> 版本：v1.0（2026-07-02）
> 定位：《综合开发指导文档》第四、五章的**执行层补充**——回答"每类资产由谁、用什么工具生成"。
> 本机工具链现状（已探查）：ffmpeg ✅、afconvert ✅、python3+Pillow ✅、uvx/npx ✅（可运行 MCP）；numpy ❌（pip 可装）、sox ❌、aseprite ❌。当前会话未连接任何 MCP 服务器。

## 0. 能力边界（决定方案形态的两个事实）

| 资产 | Claude 能生成 | Claude 能审查 | 结论 |
|---|---|---|---|
| 图像（PNG） | ✅ Python/Pillow 程序生成 | ✅ **多模态可直接看图** | 图标可全闭环自主迭代，无需外部工具 |
| 音频（OGG） | ✅ 合成/加工/AI 调用 | ❌ 不能"听"；仅能 ffprobe 技术校验 | 生成自动化，**审美 QA 必须由用户耳测**，按反馈调参迭代 |

---

## 1. 美术生成方案

### 1.1 技能/职业/种族图标（90 张，32×32）——纯本地闭环，无需任何外部服务

**工具**：Python + Pillow（已装，替换现有 generate_icons.py 的 stdlib 手写 PNG 方式，能力强一个量级：抗锯齿绘制、渐变、图层合成、批量导出）。

**工作流（Claude 全自主）**：
1. 重写 `generate_icons.py` → `tools/icon_pipeline_v2.py`：读取《综合开发指导文档》4.1 的 10 家族色板 + 6 形状语言作为配置表，每图标 = 形状模板 × 家族渐变 × 图形 motif（火焰/冰晶/箭/盾等 20 个基础 motif 函数）× 2px 描边 × 高光。
2. 生成后自动拼 **contact sheet**（90 图标拼成一张 10×9 大图，含 id 标注）。
3. Claude 用 Read 查看 contact sheet → 逐个评审（辨识度/色板一致性/形状语言正确性）→ 修改 motif 参数 → 重新生成。循环直到达标。
4. 融合技能的对角渐变、等级罗马刻痕 I-V 徽标同管线产出。
5. `tools/verify_resources.py` 做数量/命名/尺寸兜底校验。

**用户参与**：只需终审 contact sheet（一张图看全部），点名不满意的图标。

### 1.2 GUI 底图/九宫格（羊皮纸、墨线框，256×256 级别）——AI 图像生成的最佳用武之地

小图标不适合 AI 生成（1024² 输出下采样到 32×32 一致性差），但**大尺寸纹理正好相反**。两条路线按投入选：

- **路线 A（零成本，先做）**：Pillow 程序生成——Perlin 类噪声做羊皮纸肌理 + 程序化墨线边框 + 家族色烫金点缀。效果"够用"，风格可控。
- **路线 B（更高质感，可选）**：接入 AI 图像生成 MCP，生成羊皮纸/古书纹理原图，Claude 下载后用 Pillow 后处理（裁切九宫格、色调对齐色板、无缝化）。候选（均需用户注册 API key）：
  - Replicate MCP（模型多，含 SDXL/FLUX，按量计费）：`claude mcp add replicate -e REPLICATE_API_TOKEN=xxx -- npx -y replicate-mcp`
  - Recraft（有 pixel-art / 材质风格控制）：官方 API + MCP
  - 注：具体包名/接入命令以官方文档为准，接入前先验证。
- **判定**：0.5 的 GUI 重皮先走路线 A；Beta 反馈若集中吐槽观感，再上路线 B。

### 1.3 手绘 17 张关键图标（11 职业 + 6 种族，1.0 前）——不可自动化，人力约稿

- 渠道：itch.io 像素画师约稿版块 / Fiverr（搜 "32x32 pixel art icons"，17 张预算约 $50-150）/ 国内爱发电、米画师。
- Claude 的角色：产出**约稿规格书**（色板 hex 值、形状语言图示、每图标的 motif 描述、参考图=程序生成版），验收时看图比对规格。
- 免费替代：用户自绘可用 LibreSprite / Pixelorama（免费开源，替代付费的 Aseprite）。

---

## 2. 音效生成方案（28 个 OGG，三条供给线按层分配）

### 2.1 供给线 S1：程序合成（零外部依赖，立即可做）

**工具**：`pip3 install numpy` → Python 合成 WAV → `ffmpeg -i x.wav -ac 1 -ar 44100 -c:a libvorbis x.ogg`。

**方法**：参数化合成脚本 `tools/sfx_synth.py`——噪声整形（白/粉噪声 + 带通滤波 + ADSR 包络）做 whoosh/翻页/冰裂；加法合成（正弦泛音列 + 指数衰减）做钟鸣/共鸣音阶；FM 合成做奥术/暗系低鸣。每条音效生成 3-5 个参数变体供挑选。

**适用**：UI 层 4 条（翻页/落笔/切页/拒绝音）+ 施放层中"合成感"可接受的家族（寒霜/秘法/幽暗/诡影）。

**迭代协议**：Claude 批量生成变体 → 用户耳测（在 MC 内或直接播放）→ 反馈用形容词+编号（"cast.frost 第 2 个太尖锐、尾音长一点"）→ Claude 调滤波/包络参数重出。

### 2.2 供给线 S2：CC0 素材包加工（免费路线里质量最优）

**素材源**：
- **Kenney.nl**（全 CC0，zip 可 curl 直接下载，无需账号）：Impact Sounds、UI Audio、RPG Audio、Digital Audio 等包——命中层的钝响/金属/入肉底料基本齐全。
- **freesound.org**（质量上限更高，CC0 需筛选）：需注册免费账号取 API key，之后 Claude 可用 curl 调其 REST API 按 `license:CC0` 搜索并下载。可选项，Kenney 不够用再上。

**加工**：ffmpeg 单工具完成（无需 sox）——变调 `asetrate+aresample`、叠层 `amix`、混响 `aecho`、倒放 `areverse`、裁切淡出 `afade`。Claude 写批处理脚本，产物统一 mono/44.1k/ogg，ffprobe 自动校验规格，来源与许可逐条写入 CREDITS.txt。

**适用**：命中层 5 条（含 2-3 变体）+ 事件层的底料层。

### 2.3 供给线 S3：AI 音效生成（事件层 5 条"品牌音"的最佳路线）

事件层（升级/段落抉择/融合解锁/隐藏解锁/技能升级）是描述性、独特性要求最高的，正是 text-to-SFX 的强项——"两个音阶由远及近交叠成共鸣，纸张与钟声质感，2.5 秒"这种需求写 prompt 比合成参数容易得多。

- **首选：ElevenLabs 音效 API**（有官方 MCP，免费档每月有额度）。接入方式（以官方文档为准）：注册取 API key → `claude mcp add elevenlabs -e ELEVENLABS_API_KEY=xxx -- uvx elevenlabs-mcp` → Claude 通过 MCP 的 text-to-sound-effects 工具按第五章的音效描述批量生成候选 → 用户耳测挑选 → ffmpeg 规格化。
- **备选**：Stability AI Stable Audio API（curl 直调，无需 MCP）；本地 AudioCraft/AudioGen（Meta 开源，免费但需下载模型 + 本地算力，成本高不推荐首选）。
- 许可注意：确认所用档位的生成内容可商用/再分发，记入 CREDITS.txt。

### 2.4 分配总表（28 条 → 供给线）

| 层 | 条数 | 供给线 | 用户需做的事 |
|---|---|---|---|
| UI 层 | 4 | S1 合成 | 耳测反馈 |
| 施放层 | 10 | S1（寒霜/秘法/幽暗/诡影等 6 条）+ S2 底料叠加（烈焰/锋刃/壁垒/飞羽 4 条） | 耳测反馈 |
| 命中层 | 5（×2-3 变体） | S2 Kenney 加工 | 耳测反馈 |
| 事件层 | 5 | S3 ElevenLabs（需注册 API key）；不接 MCP 则降级 S1+S2 叠层 | **注册 ElevenLabs key**、耳测挑选 |
| 环境层（1.0） | ~4 | S3 为主 | 同上 |

---

## 3. 需要用户决策/动手的事项清单（Claude 无法代办）

1. 【0.4 前】是否接入 ElevenLabs MCP（注册 API key）？不接则事件层降级 S1+S2，品牌音质感受损。
2. 【0.4 中】音效耳测：每批次约 10-15 分钟听样反馈。
3. 【0.5 可选】GUI 底图是否上 AI 图像生成（Replicate/Recraft key）？默认先走程序生成。
4. 【1.0 前】17 张关键图标约稿：选渠道、定预算（约 $50-150 或人民币等价）。
5. 【随时】freesound API key（可选，Kenney 不够用时）。

## 4. 落地顺序（并入 0.4-04/0.4-05 子任务）

```
0.4-05 图标管线v2: pip无需 → icon_pipeline_v2.py → contact sheet 自审迭代 → 用户终审   [全自主]
0.4-04 音效v1:    pip3 install numpy → sfx_synth.py(S1) ∥ curl Kenney(S2)
                  → 首批 UI+施放层出样 → 用户耳测 → 迭代
                  → [用户决策1后] S3 事件层品牌音 → 耳测挑选 → 全量入库 sounds.json
```
