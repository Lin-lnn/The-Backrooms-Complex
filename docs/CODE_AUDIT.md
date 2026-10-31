# 代码质量审计报告（历史快照）

> 自动生成于仓库初始化阶段，用于记录当时的代码状态与后续优化方向。
> **1.0.4 的实际情况**：68 个 Java 文件、7,685 行；
> 最新静态审计请运行 `python tools/audit.py`（当前 6/6 通过），
> 资源自检请运行 `python tools/selfcheck.py`。下文数字仅作历史对照。
>
> **1.0.4 已删除幻视 / 理智 / `BiomeTint` / 随机事件与两套 Level 0 群系**；
> 下文提到这些内容的段落只用于记录当时的审计结果，不代表当前代码。

## 一、总览

| 指标 | 数值 |
|---|---|
| Java 文件 | **52 个** |
| Java 总行数 | 约 **5,600 行** |
| 资源文件 | **152 个**（约 84 KB）|
| 未使用 import | **0 个**（已清理）|
| Javadoc 覆盖 | 全部 52 个文件均有类级注释 |

## 二、各包规模

| 包 | 文件数 | 说明 |
|---|---|---|
| `worldgen` | 8 | 区块生成、迷宫、房间、群系源、传送 |
| `client` | 11 | 渲染与特效（着色、模糊、HUD、幻视）|
| 根包 | 13 | 注册入口、方块/物品、网络、玩家数据 |
| `command` | 2 | `/backrooms` 指令 |
| `structure` | 4 | NBT 结构的两阶段放置 |
| `item` | 7 | 工具与可食用物品 |
| `block` | 7 | 自定义方块行为 |
| `datagen` | 1 | 数据生成 |

### 最大的三个文件

| 文件 | 行数 | 备注 |
|---|---|---|
| `BackroomsMaze.java` | 534 | 迷宫算法，注释 71 行，逻辑高度内聚 |
| `BackroomsChunkGenerator.java` | 454 | 逐方块生成，注释 51 行 |
| `FakeBlockHallucination.java` / `ShadowFigure.java` | 254 / 251 | 幻视特效，结构相似 |

## 三、已完成清理

| 项 | 处理 |
|---|---|
| `SanityFakePlayer.java`（170 行）| **删除** —— 客户端无法渲染实体，功能无效 |
| `biomeAtWithBlend()`（47 行）| **删除** —— 取消哈希抖动后无调用 |
| `wall_*_plinth.png` × 3 | **删除** —— 踢脚线模型已改为复用墙壁贴图 |
| `plinth_skirt_dark_gray.png` | **删除** —— 混凝土踢脚线统一使用 `plinth_skirt` |
| `dehydration_blur.png` / `dehydration_vignette.png` / `red_vignette.png` | **删除** —— 相关特效已重做或移除 |
| 诊断日志（6 行）| **删除** —— 正式构建不应打印 |
| 未使用 import（2 处）| **清理** |

## 四、待优化项

### 1. 全限定名过多（约 150 处）

**现状**：多处直接写完整包路径，例如

```java
com.yourname.backrooms_complex.ModNetwork.sendTo(player);
net.minecraft.world.item.Items.GLASS_BOTTLE
```

**建议**：改为常规 `import`，提升可读性。

**风险**：机械替换易误伤同名类，建议逐文件人工确认。

### 2. 常量分散

**现状**：数值常量散落在各业务类中，例如

| 常量 | 位置 |
|---|---|
| `SEWAGE_THIRST_RESTORE = 35` | `ModPlayerData` |
| `DRINK_THIRST_RESTORE = 30` | `ModEvents` |
| `EAT_DAMAGE = 6.0F` | `CarpetFiberItem` |
| `HUNGER_TICKS = 200` | `CarpetFiberItem` |

**建议**：抽到 `ModBalance`（数值平衡表）统一管理，便于调参。

### 3. 幻视类结构相似

`ShadowFigure` / `FakeBlockHallucination` / `SoundHallucination` / `ItemHallucination`
都实现了「触发概率 + 生成 + 计时 + 模糊」的相同骨架。

**建议**：抽取 `AbstractHallucination` 基类，子类只实现 `spawn()` / `render()`。

### 4. 着色性能

`BiomeTint.getBlockColor()` 对每个方块面执行 **5×5 = 25 次**群系查询。

**现状**：结果被烘焙进区块网格，只在重建时计算，运行时无开销。

**可选优化**：为 `(cellX, cellZ, layer)` 加一个小型 LRU 缓存，
在大型区块重建时可减少约 90% 的重复查询。

## 五、性能特征

| 模块 | 频率 | 开销 | 评价 |
|---|---|---|---|
| `BiomeTint` | 区块重建时，每面 25 次查询 | 中 | 可缓存优化 |
| `DehydrationBlur` | 每帧 1 次 post-process | 低 | 原版 blur，已优化 |
| `ModPlayerData` 结算 | 每秒 1 次 | 极低 | ✅ |
| `ShadowFigure` / `FakeBlockHallucination` | 每 tick 1 次随机数 | 极低 | ✅ |
| 结构放置 | 每网格单元 1 次 | 低 | ✅ |
| 地物生成（篝火/蛛网/砂砾）| 每方块 1 次哈希 | 低 | ✅ |

## 六、结论

代码整体**结构清晰、注释充分、无未使用代码**。
主要改进空间在**可读性**（全限定名）与**可维护性**（常量集中、幻视类抽象），
而非功能性或性能问题。
