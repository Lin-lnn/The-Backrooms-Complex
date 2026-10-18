# BackroomsComplex（后室综合体）— NeoForge 1.21.1 模组

一个基于 **Minecraft 1.21.1 + NeoForge 21.1.x** 的自定义维度模组：无限延伸、昏暗压抑的"后室 Level 0"阈限空间。
核心是自定义 `ChunkGenerator`，按区块确定性生成 **1~7 格可变宽度走廊**的迷宫地牢，多层堆叠，并在其中生成用户提供的 **NBT 特定结构**（`backrooms_complex:level0`）承担跨层连通与房间内容。

---

## 一、目录结构

```
backrooms_complex-1.0.0/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── gradle/wrapper/gradle-wrapper.properties      # Gradle 9.1.0
└── src/main/
    ├── java/com/the_backrooms_complex/
    │   ├── BackroomsLevel0.java                   # 模组主类（@Mod）
    │   ├── ModBlocks.java                         # 方块注册（生存不可破坏 / 创造可破坏）
    │   ├── ModItems.java                          # 物品 + 创造模式物品栏
    │   ├── ModDimensions.java                     # 维度 ResourceKey + BootstrapContext 注册维度类型
    │   ├── ModBiomes.java                         # 群系 ResourceKey + BootstrapContext 注册群系
    │   ├── ModStructures.java                     # 结构系统注册（StructureType/PieceType/ProcessorType）
    │   ├── ModEvents.java                         # 维度进入提示（不再拦截方块破坏）
    │   ├── ModConfig.java                         # 内存配置（消耗密钥 / 维度纯净模式）
    │   ├── block/FluorescentLampBlock.java        # 荧光灯（光照 15）
    │   ├── block/HubDoorBlock.java                # 枢纽之门（DoorBlock + 密钥传送交互）
    │   ├── item/ThresholdKeyItem.java             # 阈界的层级密钥（普通 Item + tooltip）
    │   ├── structure/BedrockSafeProcessor.java    # 结构处理器：基岩不覆盖、其余强制覆盖
    │   ├── structure/Level0Structure.java         # 特定结构：128×128 网格 / 层级对齐 / 确定性
    │   ├── structure/Level0StructurePiece.java    # 结构片段（TemplateStructurePiece）
    │   ├── command/BackroomsCommands.java         # /backrooms check
    │   ├── datagen/DataGenerators.java            # 数据生成入口（RegistrySetBuilder）
    │   └── worldgen/
    │       ├── BackroomsChunkGenerator.java       # 核心：多层结构 / 地板墙壁天花板荧光灯
    │       ├── BackroomsMaze.java                 # 可变宽度走廊布局（1~7 格，分块完美迷宫）
    │       ├── Level0Teleporter.java              # 传送目标搜索（螺旋 + 有效性校验）
    │       ├── BackroomsBiomeSource.java          # 群系源（确定性值噪声）
    │       └── UnionFind.java                     # 并查集（路径压缩 + 按秩合并）
    └── resources/
        ├── META-INF/neoforge.mods.toml
        ├── assets/backrooms_complex/
        │   ├── blockstates/  (9 个方块状态文件)
        │   ├── models/block/ (9 个方块模型，含踢脚线双元素模型)
        │   ├── models/item/  (9 个物品模型)
        │   ├── textures/block/(9 张贴图，来自你提供的资源 + 荧光灯占位图)
        │   └── lang/ (en_us.json、zh_cn.json)
        └── data/backrooms_complex/
            ├── dimension/level_0.json             # 维度（LevelStem）
            ├── dimension_type/level_0.json        # 维度类型（数据驱动）
            ├── worldgen/biome/ (3 个群系 JSON)
            └── loot_tables/chests/ (3 张战利品表)
```

---

## 二、贴图映射说明

你提供的贴图已复制并重命名到 `src/main/resources/assets/backrooms_complex/textures/block/`：

| 模组方块/贴图 | 来源文件 |
| --- | --- |
| `wall_yellow.png` | `贴图/threshold_classic_wall.png`（黄色经典壁纸） |
| `wall_concrete.png` | `贴图/threshold_smooth_walls.png`（平滑，用于混凝土） |
| `wall_damp.png` | `贴图/threshold_speckled_wall.png`（斑点，用于潮湿发霉） |
| `wall_yellow_plinth.png` | `实体模型/模型贴图/threshold_classic_basewalls_texture.png`（踢脚线深色纹理） |
| `wall_concrete_plinth.png` | `实体模型/模型贴图/threshold_smooth_basewalls_texture.png` |
| `wall_damp_plinth.png` | `实体模型/模型贴图/threshold_speckled_basewalls_texture.png` |
| `floor_tile.png` | `贴图/threshold_deepyellow_floor.png` |
| `ceiling_tile.png` | `贴图/threshold_deepyellow_ceiling.png` |
| `fluorescent_lamp.png` | **程序生成的占位图（明亮暖白）**，建议替换为你的明亮灯管贴图 |

> **关于"basewalls"纹理的假设**：我假设 `*_basewalls_texture.png` 就是踢脚线（baseboard）的深色纹理。若实际颜色/含义不符，请直接替换 `wall_*_plinth.png` 即可，代码与模型无需改动。
>
> **未使用**：`threshold_striped_wall.png`、`threshold_striped_basewalls_texture.png`、`threshold_basewall.png` 以及 `.bbmodel` 实体模型文件。规格要求 3 个群系，故条纹（striped）纹理暂未使用；如需第 4 种群系，可参照 `ModBiomes`/`BackroomsBiomeSource` 增加即可。

---

## 三、环境要求

- **JDK 21 —— 必须 21.0.3 或更高（推荐 Temurin/Adoptium 21.0.8+）**
  - ⚠️ **不要用 JDK 21.0.0（build 35，2023 年 GA 版）**：它的 javac 存在已知 bug，会导致
    NeoForm 反编译源码重编译失败（报错 `null in package-info.java`）。
- **Gradle 8.10+**（本项目按 **ModDevGradle 2.0.107** 配置）
- NeoForge **21.1.190**（在 `gradle.properties` 的 `neo_version` 中配置）
- 首次构建需联网（下载 Gradle / NeoForge / Minecraft 依赖，约 800MB+）

> ✅ **本模组已实际构建成功并产出 JAR**，验证环境：
> `Temurin 21.0.8` + `Gradle 9.1` + `ModDevGradle 2.0.107` + `NeoForge 21.1.190`。
> 产物：`backrooms_complex-1.0.0.jar`（约 216 KB）。

> ℹ️ 本项目使用 **ModDevGradle 2.x**（插件 id `net.neoforged.moddev`），
> 而非旧的 `net.neoforged.gradle.userdev`；`neoForge { version = ... }` 会自动引入 NeoForge 依赖。

---

## 四、构建与启动
### 1. 构建

仓库已随附 Gradle Wrapper，**无需本机安装 Gradle**：

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

产物在 `build/libs/backrooms_complex-1.0.0.jar`。

> 首次构建会自动下载 Gradle 9.1.0 与 NeoForge 依赖，约需数分钟。


### 2. 构建（编译）并打包为 JAR

**一键打包（Windows）**：双击项目根目录的 [`build.bat`](build.bat) 即可（使用仓库自带 wrapper 构建，并把 JAR 复制到项目根目录）。

或手动执行：

```bash
gradlew build
```

产物在 `build/libs/backrooms_complex-1.0.0.jar`，这是**可直接使用**的模组 JAR。
把它复制到游戏目录的 `mods/` 文件夹即可（游戏需已安装 NeoForge 21.1.x for Minecraft 1.21.1）。

> 首次构建需联网下载 Gradle、NeoForge、Minecraft 依赖与 JDK 21 工具链（约 800MB+），耗时 10–30 分钟。
>
> 若构建时出现 `null in package-info.java` 错误，请检查 JDK 21 的版本（需 21.0.3+，见上文）。
>
> `neoforge.mods.toml` 中的 `${mod_id}` 等占位符由 `build.gradle` 的 `processResources` 任务自动替换，
> 已在构建产物中验证替换正确（`modId="backrooms_complex"`、`version="1.0.0"`）。

### 3. 启动客户端（进入游戏）

```bash
gradlew runClient
```

### 4. 启动服务端（可选）

```bash
gradlew runServer
```

### 5. 数据生成（可选，重新生成 dimension_type / biome JSON）

```bash
gradlew runData
```

生成结果在 `src/generated/resources/data/backrooms_complex/`。注意：本模组已直接在 `src/main/resources/data/` 提供了等价 JSON（运行时加载的就是这些），
`runData` 只用于按代码重新生成；**不要**让同一注册表条目同时存在于 `src/main/resources` 与 `src/generated/resources`（会重复）。

---

## 五、进入维度

游戏内（单人世界输入，或服务端执行）：

```
/execute in backrooms_complex:level_0 run tp @s ~ 64 ~
```

- `backrooms_complex:level_0` 是维度 ID（即 `data/backrooms_complex/dimension/level_0.json` 定义的 LevelStem）。
- `~ 64 ~`：推荐传送到 Y=64（位于中下层）；该维度是迷宫，**任意坐标可能是墙壁**，若卡在墙里请换坐标或使用创造飞行移动。
- 进入后会收到一条聊天提示，说明"所有方块不可破坏"。

> 也可以结合 `/execute in backrooms_complex:level_0 run tp @s <x> <y> <z>` 精确定位。

---

## 六、验证连通性

```
/backrooms check
```

该命令（需 OP）会扫描玩家周围 32×32×32 区域，用**并查集**统计空气连通分量数量：
- 输出 `1` → 该区域空气连通正常；
- 输出 `> 1` → 存在不连通区域（正常情况下不会发生，因为递归回溯算法天然生成生成树，且区块边界有确定性门洞）。

临时切换"允许破坏"（用于调试/编辑地图）：

```
/backrooms toggleBreak
```

每次执行翻转一次开关，并提示当前状态（开启/关闭）。

---

## 七、关键设计说明

### 7.1 多层结构（周期 7 格）
每层：`地板(+0) → 踢脚线/墙(+1..+5) → 天花板(+6)`，净空 5 格；从 Y=0 堆叠 36 层，顶部 4 格（252–255）用天花板封顶。
**荧光灯**位于每层最顶部（天花板 Y，即 +6），按 5 格间隔替换天花板方块。规格表中"+7"与我采用的"7 格周期"存在 1 格出入，本实现以"周期 7 格、灯在天花板层"为准（见 `BackroomsChunkGenerator` 注释）。

### 7.2 走廊布局与连通性（核心算法）
- **逻辑单元格间距 8 格**（刚好容纳最宽 7 格走廊 + 1 格墙），节点处开 3×3 房间。
- **逐层独立种子**：瓦片生成树、瓦片间门洞、走廊宽度三者的哈希都**把层索引作为输入**，因此**每一层的迷宫布局完全不同**（实测完整瓦片尺度下层间差异最小 18.4%、平均 25.0%），且每层各自满足连通性与宽度分布约束。
- **迷宫拓扑 = 分块完美迷宫**：平面按 8×8 个单元格（64×64 格）划分为"瓦片"，每个瓦片内部用递归回溯（迭代 + 显式栈，**零分配实现**）生成生成树；相邻瓦片之间按坐标哈希在共享边界开一个**确定性门洞**，保证该层整个平面连通。这避免了"父节点向原点收敛"方案会在坐标轴上退化成无限长笔直走廊的问题。
- **走廊宽度按楼层分三档，底部偏狭窄、顶部偏宽敞**。宽度由"起点单元格 + 方向 + 层索引"的坐标哈希决定，实测分布误差 < 0.05%：

  | 楼层 | 层索引 | 宽度范围 | 1格 | 2格 | 3格 | 4格 | 5格 | 6格 | 7格 |
  |---|---|---|---|---|---|---|---|---|---|
  | 1~12 | 0–11 | 1~7 格 | 1% | 1.5% | 10% | **75%** | 10% | 1.5% | 1% |
  | 13~24 | 12–23 | 1~7 格 | 1% | 1.5% | 20% | 30% | 16% | 1.5% | **30%** |
  | 25~36 | 24–35 | **2~7 格**（无 1 格） | — | 1.5% | 10% | 11% | **46%** | 1.5% | 30% |

  实测空气占比：第 1~12 层约 51%，第 13~24 层约 60~63%，第 25~36 层约 63~64%，整体越往上越开阔。
  **空气占比整体上调约 20%**：在基础宽度之上再统一加宽（BackroomsMaze.WIDTH_BONUS_LOW/MID/HIGH，默认 1/2/2，上限 7 格）。
  三档分别给不同加成，是因为上两层基础宽度本就大、容易顶到 7 格上限而"吃不满"。
  加宽只多挖墙壁、不会破坏连通性。实测（4×4 区块，封闭区域全部为 0）：
  第 1~12 层 51%→61%，第 13~24 层 60~63%→72~74%，第 25~36 层 63~64%→75~77%，**增幅 +16%~+20%**。
- **确定性**：所有随机性只依赖坐标哈希/瓦片种子，与世界种子、区块加载顺序均无关（同层重复生成 0 处不一致）。
- **并查集验证**：逐层在逻辑单元格层面复核连通性，若出现异常分量则按 1 格宽重新打通作为兜底修复（实测 36 层全部无封闭房间）。
- **性能**：瓦片生成树带全局缓存 + 零分配实现，生成"1 个区块的全部 36 层布局"实测仅 **0.479 ms/区块**。
- **层间不再有竖井**：跨层连通改由特定结构承担（见 7.4）。

### 7.3 踢脚线
墙壁列 Y+1 使用 `wall_*_plinth`。采用用户提供的 Blockbench 模型（`threshold_*_basewalls.bbmodel`）：墙壁主体用各变体的 64×64 `*_basewalls_texture.png` UV 图集，底部 3 像素高裙边**四边各凸出 1 像素**，使用共享的 `plinth_skirt.png`。
因模型四边对称，方块**不需要 `facing` 属性**。**模型元素不参与碰撞**，碰撞箱仍为完整方块，故凸出部分不会卡住玩家。

### 7.4 特定结构（替换原跨层竖井）
- 结构 ID `backrooms_complex:level0`，模板为用户提供的 `data/backrooms_complex/structure/level0_1..5.nbt`（尺寸 7×14×7 / 5×14×5 / 12×14×12 / 12×7×12 / 12×21×12，高度均为 7 的倍数）。
- **放置机制**：标准 `Structure` / `StructureType` / `StructurePiece`（`TemplateStructurePiece`，其 `postProcess` 内部调用 `StructureTemplate.placeInWorld` 逐方块放置，天然支持跨区块）。
- **网格与密度**：自定义 `Level0StructurePlacement`，直接以**方块坐标**划分 **90×90 格**网格（原版 `random_spread` 单位是区块，无法表达 90），每个单元 1 个结构 ⇒ **平均每 90 格一个**。
- **偏移与间距**：锚点在单元内按坐标哈希确定性偏移 0~28 格；相邻单元锚点最小间距 **62 格**（实测 14641 个单元 0 冲突）。
- **确定性**：锚点、模板、层索引全部只依赖网格单元坐标，**与世界种子无关**。
- **层级对齐**：结构最低 Y 对齐 `floorY = layerIndex × 7`；层索引只在 `0..32`（Y ≤ 230）内选取，且结构整体不得超出 Y=230。
- **生成优先级高于种子**：走廊地形在 NOISE 阶段按逐层种子生成，结构在更晚的 FEATURES 阶段放置并强制覆盖 ⇒ 无论某层种子把该处生成为墙还是通道，结构都会覆盖上去；且结构自身的选择**不使用**逐层种子，故逐层布局差异不影响结构位置与内容。
- **覆盖规则**：`BedrockSafeProcessor` 在放置每个方块前检查 `info.pos()` 处的现有方块 —— 是 `minecraft:bedrock` 则返回 `null` 跳过（保留基岩），其余一律强制覆盖（含墙壁、踢脚线、天花板、地板、荧光灯与空气）。注意该方法的第 3 个参数 `pos` 是种子位置，**方块绝对坐标在 `info.pos()`**。
- **群系**：通过标签 `backrooms_complex:has_structure/level0` 指定（1.21.1 的群系 JSON 不含 `structures` 字段）。

### 7.5 群系
三种群系 `yellow_rooms` / `concrete_halls` / `damp_corridors` 由 `BackroomsBiomeSource` 用确定性二维值噪声按 XZ 分配，仅影响墙壁/踢脚线纹理。
**本模组不生成任何战利品表、箱子或物资刷新。**

### 7.5 方块不可破坏（双重保障）
1. 属性层：`strength(-1.0F, 3600000.0F)`（生存不可挖掘，类基岩）。
2. 事件层：`BlockEvent.BreakEvent` 服务端取消（覆盖创造模式）。
`ModConfig.allowOpBreak`（默认 false）允许 OP 破坏；`/backrooms toggleBreak` 临时开关。
可选 `ModConfig.enforcePureDimension`（默认 false）禁止在维度内放置非本模组方块。

---

## 八、需要验证的 API（1.21.1 精确签名已尽量核对）

以下签名已通过 1.21.1 NeoForge javadoc 核对，但请你编译时留意（若 IDE 报错即为版本差异，需微调）：

1. `ChunkGenerator` 的 11 个抽象方法签名（`buildSurface(..., ChunkAccess)`、`fillFromNoise(Blender, RandomState, StructureManager, ChunkAccess)`、`getBaseColumn(...)`、`getGenDepth()`、`getMinY()`、`getSeaLevel()`、`applyCarvers(...)`、`spawnOriginalMobs(...)`、`addDebugScreenInfo(...)` 等）——已逐一实现。
2. `ChunkAccess.fillBiomesFromNoise(BiomeResolver, Climate.Sampler)` + `RandomState.sampler()` —— 用于群系填充（1.21.1 无公开 `setBiome`）。
3. `BiomeSource` 抽象方法 `getNoiseBiome(int,int,int,Climate.Sampler)`、`collectPossibleBiomes()`、`codec()`。
4. `Registries.CHUNK_GENERATOR` / `Registries.BIOME_SOURCE`（编解码器注册表键）。
5. `BootstrapContext` 位于 `net.minecraft.data.worldgen.BootstrapContext`（数据生成用）。
6. `DimensionType` 构造参数第 13 位 `effectsLocation` 是 `ResourceLocation`，直接传 `BuiltinDimensionTypes.OVERWORLD_EFFECTS`。
7. **datagen**：`DatapackBuiltinEntriesProvider(PackOutput, CompletableFuture<HolderLookup.Provider>, RegistrySetBuilder, Set<String>)` 与 `GatherDataEvent#getLookupProvider()`——该部分为可选功能，若版本签名有出入可整体删除 `DataGenerators.java`（不影响运行时）。
8. **战利品表** `rolls` / `set_count` 的 `NumberProvider` JSON（`minecraft:uniform` + `min`/`max`）——若游戏日志提示战利品表解析失败，请把 `min`/`max` 改为 `{ "type": "minecraft:constant", "value": N }` 嵌套形式。

---

## 九、需要你补充的信息清单

1. **荧光灯贴图**：`fluorescent_lamp.png` 当前是程序生成的明亮占位图，请替换为你想要的灯管/灯罩贴图（16×16 PNG）。
2. **踢脚线贴图确认**：请确认 `*_basewalls_texture.png` 是否确为踢脚线纹理；若不是，请提供深色踢脚线贴图并替换 `wall_*_plinth.png`。
3. **群系↔贴图配色确认**：我按 `classic=黄 / smooth=灰 / speckled=潮湿` 映射，若你的贴图实际配色不同，告诉我即可调整映射。
4. **Gradle Wrapper**：已随附 `gradlew` / `gradlew.bat` / `gradle-wrapper.jar`，可直接构建。
5. （可选）若希望用 **TerraBlender** 控制群系分布，请在 `build.gradle` 取消依赖注释并改为 TerraBlender 的 Region/SurfaceRule 方案；当前实现为"纯代码群系 + 简单噪声"，无需额外依赖。