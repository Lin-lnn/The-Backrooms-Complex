# 板条箱 GUI 实现方案

> 目标：右键打开容器界面（27 格）；离开视线 10~30 秒后箱子消失；玩家放置的箱子永久存在。

## 一、文件清单

| # | 文件 | 作用 |
|---|---|---|
| 1 | `block/entity/CrateBlockEntity.java` | 容器数据 + 战利品填充 + 视线计时 |
| 2 | `block/CrateBlock.java` | 改 `BaseEntityBlock`，右键开菜单，掉落物品 |
| 3 | `menu/CrateMenu.java` | `AbstractContainerMenu`，27 + 36 槽位 |
| 4 | `client/CrateScreen.java` | `AbstractContainerScreen<CrateMenu>` |
| 5 | `ModBlockEntities.java` | `DeferredRegister<BlockEntityType<?>>` |
| 6 | `ModMenus.java` | `DeferredRegister<MenuType<?>>` |
| 7 | `BackroomsComplex` | 两个注册表挂到 mod 总线 |
| 8 | `BackroomsClient` | `RegisterMenuScreensEvent` 绑定 Screen |

## 二、关键设计决策

### 2.1 菜单如何拿到容器（客户端同步的关键）

`MenuType` 的客户端工厂只收到 `(windowId, inventory)`，**拿不到 BlockPos** ✗
必须用 `IMenuTypeExtension.create(...)` 并把 `BlockPos` 写进 `FriendlyByteBuf`：

```java
public static final DeferredHolder<MenuType<?>, MenuType<CrateMenu>> CRATE =
        MENUS.register("crate", () -> IMenuTypeExtension.create(
                (windowId, inv, data) -> new CrateMenu(windowId, inv, data.readBlockPos())));
```

`CrateMenu(int, Inventory, BlockPos)` 在客户端从 `level.getBlockEntity(pos)` 取容器 ✓
服务端用 `player.openMenu(new SimpleMenuProvider(
        (id, inv, p) -> new CrateMenu(id, inv, pos), displayName))` ✓

### 2.2 战利品填充时机

不在生成时填（会让区块数据膨胀），而是**首次打开时**填：

```java
public void ensureFilled() {
    if (this.filled) {
        return;
    }
    this.filled = true;
    // 从 ModLootTables 或硬编码池里随机 2~4 件
    ...
    setChanged();
}
```

### 2.3 视线计时消失

`BlockEntity#tick`（服务端）：

```java
public void serverTick() {
    if (!this.natural) {           // 玩家放置的箱子：不计时
        return;
    }
    Player witness = level.getNearestPlayer(center, WATCH_RADIUS);
    if (witness != null) {
        this.unseenTicks = 0;      // 被看见 ⇒ 计时归零
        return;
    }
    if (++this.unseenTicks > this.lifetimeTicks) {
        level.removeBlock(worldPosition, false);
    }
}
```

`lifetimeTicks` 在 `ensureFilled` 里随机为 **200 ~ 600 tick**（10~30 秒）✓ 写入 NBT ✓

### 2.4 玩家放置的箱子

`natural` 属性已经是 `false` ⇒ BlockEntity 创建时从 blockstate 读取并存进字段 ✓ 永不计时 ✓

## 三、NBT 字段

| 键 | 类型 | 说明 |
|---|---|---|
| `Items` | List | 27 格物品 |
| `Filled` | boolean | 是否已填充 |
| `Natural` | boolean | 是否自然生成 |
| `Unseen` | int | 连续未被看见的 tick |
| `Lifetime` | int | 消失阈值（200~600）|

## 四、注册顺序（重要）

```
ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
ModMenus.MENUS.register(modEventBus);
```

必须**早于** `ModBlocks.BLOCKS.register(...)` 之后的任何 `get()` 调用 ✓
目前 `ModBlocks.CRATE` 用的是 `new CrateBlock(...)`，改 `BaseEntityBlock` 后不能在没有
BlockEntityType 的情况下调用 `.get()` ✗ 需要把 worldgen 里的 `crateState` 初始化
挪到 `FMLCommonSetupEvent` 或改用延迟求值 ✓

## 五、风险点

| 风险 | 说明 |
|---|---|
| **`crateState` 初始化时机** | `Level1ChunkGenerator` 构造器里直接 `ModBlocks.CRATE.get()`，若注册未完成会崩 ✗ 需改为惰性 |
| 客户端方块实体不同步 | 27 格需 `Container` 数据包，用 `AbstractContainerMenu` 的 `broadcastChanges` 即可 |
| `BlockEntityType` 的 `validBlocks` | 必须传 `ModBlocks.CRATE.get()`，同样是注册顺序问题 |
| `MenuType` 数量上限 | 无，放心 |
| 音效/纹理 | 用原版 `generic_54` 纹理 + `BARREL_OPEN` 音效即可，无需新贴图 |

## 六、预计工作量

| 步骤 | 行数 |
|---|---|
| CrateBlockEntity | ~180 |
| CrateMenu | ~80 |
| CrateScreen | ~40 |
| CrateBlock 改造 | ~80 |
| 注册（2 个类 + 2 处挂载 + 客户端事件）| ~60 |
| **合计** | **~440 行 + 4 处注册** |

## 七、验收标准

1. 右键自然生成的箱子 ⇒ 打开 27 格界面，里面有 2~4 件物资
2. 取走物品后箱子保留（可继续当储物箱用）或按设计消失
3. 离开 24 格且 **10~30 秒**后再回来 ⇒ 箱子已消失
4. 玩家放置的箱子 ⇒ 24 格/几十分钟都不消失
5. `/data get block` 能看到 `Filled/Natural/Unseen/Lifetime` 五个字段

## 八、当前状态（本文件写入时）

- 刷新率：跃金 0.12%，其他区段 0.012% ✅
- 右键搜刮：**已移除**（不再误删箱子）✅
- GUI：**未实现** ✗
- 计时消失：**未实现** ✗（当前仍是 24 格内无玩家即 `randomTick` 删除）
- HEAD：`5b5346c`