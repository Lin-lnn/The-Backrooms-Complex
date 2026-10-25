# 代码结构说明

> 一句话：**每个 Level 一个包，互不引用；共享的只有注册表与群系源。**

## 目录结构

```
com.the_backrooms_complex
├── BackroomsComplex          模组入口（注册 codec / 群系源 / 事件）
├── ModBlocks / ModItems      方块与物品注册表
├── ModBiomes / ModDimensions 群系 key 与维度 key
├── ModEvents                 全部事件钩子（tick / 登录 / 放置限制）
├── ModBalance                ★ 全部数值调参常量（改这里就生效）
├── ModConfig                 配置项
├── ModNetwork / ModPlayerData 精神值 / 饮水值同步
│
├── block/                    方块行为（门、家具、墙灰、刮纸墙…）
├── client/                   纯客户端（渲染层、着色、HUD、幻觉效果）
├── command/                  命令
├── entity/                   实体（细菌、座位）
├── item/                     物品行为
├── structure/                Level 0 的封闭结构（小屋 + 门）
│
└── worldgen/
    ├── BackroomsBiomeSource   ★ 两个维度共用的三维群系源（参数化：3 个群系 + 层高）
    ├── UnionFind              通用并查集
    ├── level0/                ★ Level 0 专用，不引用 level1
    │   ├── BackroomsChunkGenerator  生成器主干（层高 8，生成 LEVEL_LAYERS = 5 层）
    │   ├── BackroomsMaze            逐层墙体网格
    │   ├── BackroomsAvenue          宽阔街道
    │   ├── BackroomsRooms           房间（矩形 + 门洞 + 内柱）
    │   ├── Level0Teleporter         坠落/传送逻辑
    │   ├── RoomGraph                房间连通图
    │   └── Topology*                拓扑事件（开合门、封堵走廊、灯光闪烁）
    └── level1/                ★ Level 1 专用，不引用 level0
        ├── Level1ChunkGenerator     生成器（层高 18，3 个子层 × 6 格，生成 LAYERS = 3 层）
        └── Level1Flicker            「闪烁」现象（按层、逐组熄灭 / 亮起）
```

## 两条硬规则

| 规则 | 说明 |
|---|---|
| **L0 与 L1 不互相引用** | 两个生成器、各自的常量、各自的逻辑，改一边不会影响另一边 |
| **共享只有两样** | `ModBlocks`/`ModBiomes` 等**注册表**，以及参数化的 `BackroomsBiomeSource` |

## 数值都在 `ModBalance`

以前部分常量写了但没接线（改了不生效），现在**全部接通**：

| 分组 | 例子 |
|---|---|
| 精神值 / 饮水值 | `THIRST_BASE_INTERVAL`、`SANITY_DAMP_INTERVAL`、`DEHYDRATION_DAMAGE` |
| 物品效果 | `FIBER_DAMAGE`、`DAMP_CAN_*`、`PRISTINE_CAN_*` |
| 世界生成 | `DECOR_COBWEB_CHANCE`、`DECOR_GRAVEL_CHANCE`、`DECOR_CAMFIRE_CHANCE` |
| 恢复延迟 | `CARPET_RESTORE_TICKS` |

## 关键常量速查

| 常量 | 位置 | 值 | 含义 |
|---|---|---|---|
| `LEVEL_LAYERS` | `level0/BackroomsChunkGenerator` | 5 | L0 实际生成层数 |
| `LAYER_HEIGHT` | `level0/BackroomsChunkGenerator` | 8 | L0 单层高度 |
| `LAYERS` | `level1/Level1ChunkGenerator` | 3 | L1 实际生成层数 |
| `LAYER_H` | `level1/Level1ChunkGenerator` | 18 | L1 单层高度 |
| `SUB_H` / `SUB_COUNT` | `level1/Level1ChunkGenerator` | 6 / 3 | L1 小径子层 |
| `PASSAGE_CELL` | `level1/Level1ChunkGenerator` | 96 | 小径区边长 |
| `MAZE_CELL` | `level1/Level1ChunkGenerator` | 10 | 小径房间网格 |
| `PILLAR_STEP` | `level1/Level1ChunkGenerator` | 48 | 混凝土巨柱间距 |
| `GROUP_INTERVAL` | `level1/Level1Flicker` | 10 | 闪烁每组间隔（0.5 秒）|

## 新增内容时的落点

| 你要加的东西 | 放到哪 |
|---|---|
| Level 0 的生成规则 | `worldgen/level0/BackroomsChunkGenerator` |
| Level 1 的生成规则 | `worldgen/level1/Level1ChunkGenerator` |
| 只在某个维度生效的事件 | 对应 `levelN` 包里，并在入口判断 `level.dimension()` |
| 方块 / 物品 | `block/` / `item/`，然后到 `ModBlocks` / `ModItems` 注册 |
| 调参数值 | **只放 `ModBalance`**，别写魔法数字 |
