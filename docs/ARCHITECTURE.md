# 代码结构说明

> 一句话：**每个 Level 一个包，互不引用；共享的只有注册表与群系源。**

## 目录结构

```
com.the_backrooms_complex
├── BackroomsComplex          模组入口（注册 codec / 群系源 / 事件）
├── ModBlocks / ModItems      方块与物品注册表
├── ModBiomes / ModDimensions 群系 key 与维度 key
├── ModEvents                 环境音与首次进入提示
├── ModBalance                ★ 数值调参常量（饮水、物品效果）
├── ModConfig                 配置项
├── ModNetwork / ModPlayerData 饮水值同步与结算
│
├── block/                    方块行为（门、家具、刮纸墙…）
├── client/                   纯客户端（HUD、维度的雾、板条箱界面）
├── command/                  命令
├── entity/                   实体（座位）
├── item/                     物品行为
├── structure/                Level 0 的封闭结构（小屋 + 门）
│
└── worldgen/
    ├── BackroomsBiomeSource   ★ 两个维度共用的三维群系源（primary + 可选区段群系 + 层高）
    ├── UnionFind              通用并查集
    ├── level0/                ★ Level 0 专用，不引用 level1
    │   ├── BackroomsChunkGenerator  生成器（层高 8，生成 LEVEL_LAYERS = 5 层，单黄色群系）
    │   ├── BackroomsMaze            确定性布局入口
    │   ├── BackroomsAvenue          宽阔街道
    │   ├── BackroomsRooms           房间（矩形 + 门洞 + 内柱）
    │   ├── Level0Teleporter         坠落 / 传送逻辑
    │   ├── RoomGraph                确定性拓扑图
    │   └── TopologyWalls            拓扑图 → 墙体网格
    └── level1/                ★ Level 1 专用，不引用 level0
        ├── Level1Sectors            层高 / Region / 权重表 / 区段边界与通道（纯计算）
        ├── Level1Geometry           小径迷宫 / 过道房间 / 柱网 / 楼梯（带各自缓存）
        ├── Level1Decor              灯位 / 板条箱 / 草藤 / 碎屑 / 管道（判定与状态）
        └── Level1ChunkGenerator     写方块：BlockState 字段、fillColumn / placeAisle / placeSubFloor、codec
```

## 两条硬规则

| 规则 | 说明 |
|---|---|
| **L0 与 L1 不互相引用** | 两个生成器、各自的常量、各自的逻辑，改一边不会影响另一边 |
| **共享只有两样** | `ModBlocks` / `ModBiomes` 等注册表，以及参数化的 `BackroomsBiomeSource` |

## 数值都在 `ModBalance`

| 分组 | 例子 |
|---|---|
| 饮水值 | `THIRST_BASE_INTERVAL`、`THIRST_SPRINT_INTERVAL`、`DEHYDRATION_DAMAGE` |
| 物品效果 | `FIBER_DAMAGE`、`DAMP_CAN_*`、`PRISTINE_CAN_*` |

## 关键常量速查

| 常量 | 位置 | 值 | 含义 |
|---|---|---|---|
| `LEVEL_LAYERS` | `level0/BackroomsChunkGenerator` | 5 | L0 实际生成层数 |
| `LAYER_HEIGHT` | `level0/BackroomsChunkGenerator` | 8 | L0 单层高度 |
| `LAYERS` | `level1/Level1Sectors` | 3 | L1 实际生成层数 |
| `LAYER_H` | `level1/Level1Sectors` | 12 | L1 单层高度 |
| `SUB_H` / `SUB_COUNT` | `level1/Level1Sectors` | 6 / 2 | L1 子层（两层 6 格） |
| `PASSAGE_CELL` | `level1/Level1Geometry` | 96 | 小径区边长 |
| `MAZE_CELL` | `level1/Level1Geometry` | 10 | 小径房间网格 |
| `PILLAR_STEP` | `level1/Level1Geometry` | 16 | 混凝土柱网间距（2×2 柱） |

## 新增内容时的落点

| 你要加的东西 | 放到哪 |
|---|---|
| Level 0 的生成规则 | `worldgen/level0/BackroomsChunkGenerator` |
| Level 1 的区段 / 层高 | `worldgen/level1/Level1Sectors` |
| Level 1 的平面几何 | `worldgen/level1/Level1Geometry` |
| Level 1 的地物判断 | `worldgen/level1/Level1Decor` |
| Level 1 的写方块逻辑 | `worldgen/level1/Level1ChunkGenerator` |
| 方块 / 物品 | `block/` / `item/`，然后到 `ModBlocks` / `ModItems` 注册 |
| 调参数值 | **只放 `ModBalance`**，别写魔法数字 |
