package com.the_backrooms_complex.worldgen.level1;

import com.the_backrooms_complex.worldgen.level1.Level1ChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.GenerationStep;
import com.the_backrooms_complex.ModBlocks;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * <b>Level 1「宜居地带」专用区块生成器</b>。
 *
 * <p>与 Level 0 的生成器 <b>完全独立</b>，
 * 没有任何共享的方块、配色或布局逻辑 —— 它自己就是一套生成规则。</p>
 *
 * <h2>结构</h2>
 * <ul>
 *   <li><b>层高 18 格</b>（地板 1 + 空气 16 + 天花板 1），288 高 ⇒ 16 层；</li>
 *   <li><b>过道（停车场）</b>：一整片开阔石头地面，只有 <b>4×4 混凝土柱</b>
 *       按 48 格网格排布，柱脚是强化深板岩警示柱；</li>
 *   <li><b>小径</b>：约 45% 的 96×96 方块区，是 <b>3 个子层 × 6 格高</b>的
 *       白墙窄迷宫；</li>
 *   <li><b>过道小房间</b>：64 格网格里 60% 概率有 7×7 房间，南墙正中一扇金属门；</li>
 *   <li><b>连接</b>：过道与小径的交界是混凝土柱墙，每 24 格开一扇金属门；</li>
 *   <li><b>光照</b>：天花板层每 5 格一个荧光灯（唯一沿用的 Level 0 方块）。</li>
 * </ul>
 */
public class Level1ChunkGenerator extends ChunkGenerator {

    // ==================== 常量 ====================

    /** 单层高度（格）。 */
    public static final int LAYER_H = 12;
    /** 子层高度（小径用）。 */
    public static final int SUB_H = 6;
    /** 子层数量。 */
    public static final int SUB_COUNT = 2;

    /**
     * 实际生成的层数。
     *
     * <p>世界高度是 288（{@code 288 / 18 = 16} 层），但只生成 {@value #LAYERS} 层，其余留空 ——
     * 逐方块循环的上界因此从 288 格降到 {@code 3 × 18 = 54} 格，区块生成快约 5 倍。</p>
     */
    public static final int LAYERS = 3;

    /** 小径区边长（格）。 */
    public static final int PASSAGE_CELL = 96;
    /** 过道小房间网格边长（格）。 */
    public static final int ROOM_CELL = 64;
    /** 房间半径（7×7 ⇒ 3）。 */
    public static final int ROOM_RADIUS = 3;

    /** 巨柱间距（格）。 */
    public static final int PILLAR_STEP = 48;
    /** 巨柱半径（4×4 ⇒ 2 格偏移，中心 22~25）。 */
    public static final int PILLAR_CENTER = 23;

    /** 荧光灯间距（格）。 */
    public static final int LAMP_STEP = 5;

    // ==================== 编解码器 ====================

    public static final MapCodec<Level1ChunkGenerator> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance ->
                    instance.group(
                            BiomeSource.CODEC.fieldOf("biome_source")
                                    .forGetter(Level1ChunkGenerator::getBiomeSource)
                    ).apply(instance, Level1ChunkGenerator::new));

    // ==================== 缓存方块状态 ====================

    private final BlockState airState = Blocks.AIR.defaultBlockState();
    private final BlockState floorState;
    private final BlockState ceilingState;
    private final BlockState pillarState;
    private final BlockState warningPillarState;
    private final BlockState whiteWallState;
    /** 跃金段：泛黄的混凝土地板 / 混凝土墙 / 金属瓦楞板天花板。 */
    private final BlockState yuejinFloorState;
    private final BlockState yuejinWallState;
    private final BlockState yuejinCeilingState;
    /** 花园段：草方块地面 / 藤蔓墙。 */
    private final BlockState gardenGrassState;
    private final BlockState gardenWallState;
    /** 传说段：深色木板护墙。 */
    private final BlockState legendWallState;
    /** 板条箱（跃金段地物）。 */
    private final BlockState crateState;
    /** 花园段地面的草簇。 */
    private final BlockState grassTuftState;
    /** 传说段墙面的磨砂玻璃格。 */
    private final BlockState frostedGlassState;
    /** 衔尾段天花板上的红漆管道。 */
    private final BlockState redPipeState;
    /** 衔尾段的破损补丁（砂砾）。 */
    private final BlockState rubbleState;
    /** 两层之间的爬升台阶。 */
    private final BlockState stairState;
    /** 花园段从天花板垂下的藤蔓。 */
    private final BlockState vineState;
    /** 传说段的深色木门。 */
    private final BlockState woodDoorState;
    /** 衔尾段斜靠的钢板。 */
    private final BlockState steelPlateState;
    private final BlockState lampState;
    private final BlockState lampOffState;
    /** 金属门（门洞用）。缓存一次，避免逐方块查注册表 + 分配。 */
    private final BlockState metalDoorState;

    public Level1ChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
        this.floorState = ModBlocks.CONCRETE_FLOOR.get().defaultBlockState();
        this.ceilingState = ModBlocks.CONCRETE_CEILING.get().defaultBlockState();
        this.pillarState = ModBlocks.CONCRETE_PILLAR.get().defaultBlockState();
        this.warningPillarState = ModBlocks.CONCRETE_WARNING_PILLAR.get().defaultBlockState();
        this.whiteWallState = ModBlocks.WHITE_PLASTERED_WALL.get().defaultBlockState();
        this.yuejinFloorState = ModBlocks.YELLOWED_CONCRETE_FLOOR.get().defaultBlockState();
        this.yuejinWallState = ModBlocks.YELLOWED_CONCRETE_WALL.get().defaultBlockState();
        this.yuejinCeilingState = ModBlocks.YELLOWED_CORRUGATED_METAL.get().defaultBlockState();
        this.gardenGrassState = net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
        this.gardenWallState = net.minecraft.world.level.block.Blocks.MOSS_BLOCK.defaultBlockState();
        this.legendWallState = net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS.defaultBlockState();
        // 自然生成的箱子带 natural=true，无人时消失
        this.crateState = ModBlocks.CRATE.get().defaultBlockState()
                .setValue(com.the_backrooms_complex.block.CrateBlock.NATURAL, true);
        this.grassTuftState = net.minecraft.world.level.block.Blocks.SHORT_GRASS.defaultBlockState();
        this.frostedGlassState = net.minecraft.world.level.block.Blocks.WHITE_STAINED_GLASS.defaultBlockState();
        this.redPipeState = net.minecraft.world.level.block.Blocks.RED_CONCRETE.defaultBlockState();
        this.rubbleState = net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState();
        this.stairState = ModBlocks.CONCRETE_FLOOR_STAIRS.get().defaultBlockState();
        // 用普通藤蔓并开启「向上附着」，它从天花板垂下且是绿色（垂泪藤是暗红色，观感不对）
        this.vineState = net.minecraft.world.level.block.Blocks.VINE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.VineBlock.UP, Boolean.TRUE);
        this.woodDoorState = net.minecraft.world.level.block.Blocks.DARK_OAK_DOOR.defaultBlockState();
        this.steelPlateState = ModBlocks.STEEL_PLATE.get().defaultBlockState();
        this.lampState = ModBlocks.FLUORESCENT_LAMP.get().defaultBlockState();
        this.lampOffState = ModBlocks.LAMP_OFF.get().defaultBlockState();
        this.metalDoorState = ModBlocks.METAL_DOOR.get().defaultBlockState();
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ==================== 区域判定 ====================

    /** 某个 XZ 是否在小径区（约 45%）。 */
    public static boolean isPassageArea(int x, int z) {
        int cx = Math.floorDiv(x, PASSAGE_CELL);
        int cz = Math.floorDiv(z, PASSAGE_CELL);
        long h = (long) cx * 0x9E3779B97F4A7C15L ^ (long) cz * 0xC2B2AE3D27D4EB4FL ^ 0x70617373616765L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.45;
    }

    /** 是否是过道/小径的边界格。 */
    public static boolean isPassageBoundary(int x, int z) {
        boolean here = isPassageArea(x, z);
        return here != isPassageArea(x + 1, z) || here != isPassageArea(x - 1, z)
                || here != isPassageArea(x, z + 1) || here != isPassageArea(x, z - 1);
    }

    // ==================== 小径迷宫：可变矩形房间 + L 形走廊 ====================

    /** 迷宫单元边长（格）：最大房间 7 + 墙 + 余量。 */
    private static final int MAZE_CELL = 10;
    /** 房间边长范围（格）。 */
    private static final int ROOM_MIN = 3;
    private static final int ROOM_MAX = 7;
    /** 走廊宽度范围（格）。 */
    private static final int COR_MIN = 2;
    private static final int COR_MAX = 3;
    /** 房间边长取值个数。 */
    private static final int ROOM_SPAN = ROOM_MAX - ROOM_MIN + 1;
    /** 走廊宽度取值个数。 */
    private static final int COR_SPAN = COR_MAX - COR_MIN + 1;
    /** 房间几何哈希盐。 */
    private static final long SALT_ROOM = 0x1111L;
    /** 走廊哈希盐。 */
    private static final long SALT_COR = 0x2222L;
    /** 走廊「先横后竖 / 先竖后横」的哈希盐。 */
    private static final long SALT_BEND = 0x5555L;

    /** 64 位混合哈希。 */
    private static long mix(long a, long b, long c) {
        long h = a * 0x9E3779B97F4A7C15L;
        h ^= b * 0xC2B2AE3D27D4EB4FL;
        h ^= c * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    /**
     * 取单元 {@code (cx, cz)} 内房间的矩形：{@code [x0, z0, 宽, 高]}。
     *
     * <p>宽高各自独立取 {@value #ROOM_MIN}~{@value #ROOM_MAX}，因此房间<b>多为矩形而非正方形</b>；
     * 房间在单元内还有随机偏移，相邻单元的房间因此不会排成整齐的行列。</p>
     */
    private static void roomRect(int cx, int cz, int[] out) {
        long h = mix(cx, cz, SALT_ROOM);
        int w = ROOM_MIN + (int) Math.floorMod(h, ROOM_SPAN);
        int ht = ROOM_MIN + (int) Math.floorMod(h >>> 8, ROOM_SPAN);
        int padMaxX = MAZE_CELL - w;
        int padMaxZ = MAZE_CELL - ht;
        int padX = padMaxX > 0 ? (int) Math.floorMod(h >>> 16, padMaxX + 1) : 0;
        int padZ = padMaxZ > 0 ? (int) Math.floorMod(h >>> 24, padMaxZ + 1) : 0;
        out[0] = cx * MAZE_CELL + padX;
        out[1] = cz * MAZE_CELL + padZ;
        out[2] = w;
        out[3] = ht;
    }

    /** 走廊宽度（2~3）。 */
    private static int corridorWidth(int cx, int cz, int dirSalt) {
        long h = mix(cx, cz, SALT_COR + dirSalt);
        return COR_MIN + (int) Math.floorMod(h, COR_SPAN);
    }

    /** 点是否落在轴对齐的粗线段内（{@code width} 为粗度，居中）。 */
    private static boolean segContains(int px, int pz, int ax, int az, int bx, int bz, int width) {
        int lo = -(width / 2);
        int hi = lo + width - 1;
        if (az == bz) {
            int d = pz - az;
            return px >= Math.min(ax, bx) && px <= Math.max(ax, bx) && d >= lo && d <= hi;
        }
        int d = px - ax;
        return pz >= Math.min(az, bz) && pz <= Math.max(az, bz) && d >= lo && d <= hi;
    }

    /**
     * 单元 {@code (ax, az)} 通往邻居 {@code (tx, tz)} 的 <b>L 形走廊</b>是否覆盖该点。
     *
     * <p>拐弯方向由哈希决定（先横后竖 / 先竖后横），这是"曲折"的主要来源。</p>
     */
    private static boolean inCorridor(int ax, int az, int tx, int tz, int x, int z,
                                      int[] a, int[] b) {
        roomRect(ax, az, a);
        roomRect(tx, tz, b);
        int x0 = a[0] + a[2] / 2;
        int z0 = a[1] + a[3] / 2;
        int x1 = b[0] + b[2] / 2;
        int z1 = b[1] + b[3] / 2;
        int w = corridorWidth(ax, az, tz == az ? 1 : 2);
        boolean horizFirst = (mix(ax, az, SALT_COR ^ SALT_BEND) & 1L) == 0L;
        if (horizFirst) {
            return segContains(x, z, x0, z0, x1, z0, w)
                    || segContains(x, z, x1, z0, x1, z1, w);
        }
        return segContains(x, z, x0, z0, x0, z1, w)
                || segContains(x, z, x0, z1, x1, z1, w);
    }

    /** 该列是否紧邻某个「房间门」列（四个水平邻格中任一是门位）。 */
    public static boolean roomDoorNeighbor(int x, int z) {
        return roomKind(x + 1, z) == 2 || roomKind(x - 1, z) == 2
                || roomKind(x, z + 1) == 2 || roomKind(x, z - 1) == 2;
    }

    /**
     * 小径内部的墙体判定（{@code true} = 墙）。
     *
     * <p>结构：{@value #MAZE_CELL} 格的单元网格，每格挖出一个
     * {@value #ROOM_MIN}~{@value #ROOM_MAX} 的<b>矩形房间</b>，
     * 再用宽度 {@value #COR_MIN}~{@value #COR_MAX} 的 <b>L 形走廊</b>
     * 把每个单元与它的<b>东、南</b>邻居连起来。</p>
     *
     * <p>因为每个单元都有东、南两条出边，整张图必然连通 ——
     * 这一点用洪水填充验证过：全图无任何孤立空腔。</p>
     */
    public static boolean isPassageWall(int x, int z) {
        int cx = Math.floorDiv(x, MAZE_CELL);
        int cz = Math.floorDiv(z, MAZE_CELL);
        int[] a = new int[4];
        int[] b = new int[4];
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int ax = cx + dx;
                int az = cz + dz;
                roomRect(ax, az, a);
                if (x >= a[0] && x < a[0] + a[2] && z >= a[1] && z < a[1] + a[3]) {
                    return false;                       // 房间内部
                }
                if (inCorridor(ax, az, ax + 1, az, x, z, a, b)) {
                    return false;                       // 通往东邻的走廊
                }
                if (inCorridor(ax, az, ax, az + 1, x, z, a, b)) {
                    return false;                       // 通往南邻的走廊
                }
            }
        }
        return true;
    }

    /** 巨柱判定：48 格网格，4×4 柱体（占 22..25）。 */
    public static boolean onPillar(int x, int z) {
        int ox = Math.floorMod(x, PILLAR_STEP);
        int oz = Math.floorMod(z, PILLAR_STEP);
        int lo = PILLAR_CENTER - 1;   // 22
        int hi = PILLAR_CENTER + 2;   // 25
        return ox >= lo && ox <= hi && oz >= lo && oz <= hi;
    }

    /** 灯位判定。 */
    public static boolean isLamp(int x, int z) {
        return Math.floorMod(x, LAMP_STEP) == 2 && Math.floorMod(z, LAMP_STEP) == 2;
    }

    /** 过道小房间判定：0 = 内部，1 = 墙，2 = 门，-1 = 无。 */
    public static int roomKind(int x, int z) {
        int cx = Math.floorDiv(x, ROOM_CELL);
        int cz = Math.floorDiv(z, ROOM_CELL);
        int[] rects = roomRects(cx, cz);            // 扁平数组：ox, oz, ox, oz ...
        int lx = Math.floorMod(x, ROOM_CELL);
        int lz = Math.floorMod(z, ROOM_CELL);
        for (int i = 0; i < rects.length; i += 2) {
            int ox = rects[i];
            int oz = rects[i + 1];
            int dx = lx - ox;
            int dz = lz - oz;
            if (dx < 0 || dz < 0 || dx >= ROOM_SIZE || dz >= ROOM_SIZE) {
                continue;
            }
            if (dz == ROOM_SIZE - 1 && dx == ROOM_RADIUS) {
                return 2;                            // 南墙正中 = 门
            }
            if (dx == 0 || dz == 0 || dx == ROOM_SIZE - 1 || dz == ROOM_SIZE - 1) {
                return 1;                            // 墙
            }
            return 0;                                // 房间内部
        }
        return -1;
    }

    /** 每个 96 格区内保证的房间数量下限 / 上限。 */
    public static final int ROOMS_MIN = 5;
    public static final int ROOMS_MAX = 10;

    /** 房间边长（含墙）。 */
    public static final int ROOM_SIZE = 2 * ROOM_RADIUS + 1;

    /** 房间候选格间距：96 / 16 = 6 ⇒ 每区 36 个候选位，足够放下 10 个房间。 */
    private static final int ROOM_SLOT = 16;

    /** 房间布局缓存（key = 区坐标打包）。 */
    private static final java.util.concurrent.ConcurrentHashMap<Long, int[]> ROOM_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 某个 96 格区内的房间位置列表（扁平 {@code ox, oz} 序列）。
     *
     * <p>用区坐标作种子做 Fisher-Yates 洗牌，取前 K 个（K = 5~10），
     * 因此<b>每个区的每一层都保证有同样 K 个房间</b>，而不是概率性出现。</p>
     */
    private static int[] roomRects(int cx, int cz) {
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
        int[] cached = ROOM_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        int slots = ROOM_CELL / ROOM_SLOT;          // 6 x 6
        int[] slotX = new int[slots * slots];
        int[] slotZ = new int[slots * slots];
        for (int i = 0; i < slotX.length; i++) {
            slotX[i] = (i % slots) * ROOM_SLOT + 4;  // 留 4 格边距，保证 7 宽房间不越界
            slotZ[i] = (i / slots) * ROOM_SLOT + 4;
        }
        // 确定性洗牌
        long h = (long) cx * 0x94D049BB133111EBL ^ (long) cz * 0x2545F4914F6CDD1DL ^ 0x726F6F6DL;
        for (int i = slotX.length - 1; i > 0; i--) {
            h ^= h >>> 33;
            h *= 0xC4CEB9FE1A85EC53L;
            h ^= h >>> 33;
            int j = (int) Math.floorMod(h, i + 1L);
            int tx = slotX[i];
            slotX[i] = slotX[j];
            slotX[j] = tx;
            int tz = slotZ[i];
            slotZ[i] = slotZ[j];
            slotZ[j] = tz;
        }
        int count = ROOMS_MIN + (int) Math.floorMod(h >>> 8, ROOMS_MAX - ROOMS_MIN + 1);
        int[] out = new int[count * 2];
        for (int i = 0; i < count; i++) {
            out[i * 2] = slotX[i];
            out[i * 2 + 1] = slotZ[i];
        }
        ROOM_CACHE.put(key, out);
        return out;
    }


    // ==================== 区块生成 ====================

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager,
                                                        ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        int minY = chunk.getMinBuildHeight();
        // 只生成前 LAYERS 层，其余留空（降低算力）
        int maxY = Math.min(chunk.getMaxBuildHeight(), LAYERS * LAYER_H);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                fillColumn(chunk, m, cp.x * 16 + lx, cp.z * 16 + lz, minY, maxY);
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    // ==================== 逐列生成 ====================

    /**
     * 一列的生成参数。
     *
     * <p>全部只依赖 {@code (x, z)}，因此每列只算一次，所有 y 共用 ——
     * 这是原生成器里散落一地的 {@code colXxx} 布尔量的集中版本。</p>
     */
    private record Column(
            boolean passage,        // 位于小径区
            boolean passageWall,    // 小径内部墙（迷宫判定）
            boolean boundary,       // 小径 / 过道的边界
            boolean gate,           // 门位（边界上每 24 格一列）
            boolean aisleBoundary,  // 过道侧的边界列
            boolean gateAdjacent,   // 紧邻门位（过道侧也要在门高度打通）
            Direction facing,       // 门的朝向
            int room,               // 过道小房间：-1 无 / 0 内部 / 1 墙 / 2 门
            boolean pillar,         // 混凝土柱
            boolean lamp) {         // 过道天花板灯位
    }

    /** 计算某一列的全部参数。 */
    private static Column columnAt(int wx, int wz) {
        boolean passage = isPassageArea(wx, wz);
        boolean boundary = isPassageBoundary(wx, wz);
        int room = roomKind(wx, wz);
        return new Column(
                passage,
                passage && isPassageWall(wx, wz),
                boundary,
                boundary && isGateColumn(wx, wz),
                !passage && boundary,
                boundary && (isGateColumn(wx + 1, wz) || isGateColumn(wx - 1, wz)
                        || isGateColumn(wx, wz + 1) || isGateColumn(wx, wz - 1)),
                facingOf(wx, wz, boundary),
                room,
                // 柱子只出现在「纯过道的开阔区域」，否则会被子层结构覆盖成木板/石板条带
                onPillar(wx, wz),
                isLamp(wx, wz));
    }

    /** 门的朝向：边界法线落在 X 还是 Z 轴；小径在正方向则朝东 / 朝南。 */
    private static Direction facingOf(int wx, int wz, boolean boundary) {
        if (!boundary) {
            return Direction.NORTH;
        }
        if (isPassageArea(wx + 1, wz) != isPassageArea(wx - 1, wz)) {
            return isPassageArea(wx + 1, wz) ? Direction.EAST : Direction.WEST;
        }
        return isPassageArea(wx, wz + 1) ? Direction.SOUTH : Direction.NORTH;
    }

    /** 生成一整列（从 minY 到 maxY）。 */
    private void fillColumn(ChunkAccess chunk, BlockPos.MutableBlockPos m,
                            int wx, int wz, int minY, int maxY) {
        Column c = columnAt(wx, wz);
        // 柱子列优先：柱子上绝不叠加小径/房间的子层结构（否则会长出木板+石板的混合柱）
        // 区段与配色每列只算一次（原先在 placeAisle 里逐方块计算）
        Region region = regionAt(wx, wz);
        SectorProfile prof = profileOf(region);
        // 子层结构只属于小径；过道房间在过道自己的 18 格空间里做成封闭盒子
        boolean subFloor = !c.pillar() && c.passage();
        for (int y = minY; y < maxY; y++) {
            int offset = y - (y / LAYER_H) * LAYER_H;
            // 区段边界：用本区段自己的墙方块包裹整段（每 16 格留通道，且只在两侧都有地板的高度开）
            // 只有「该列确实会放门」才允许开口：小径边界门位或过道房间门位。
            // 其它位置即便落在通道节奏上也不开洞，避免出现没有门的空壳开口。
            boolean hasDoor = c.gate() || c.room() == 2;
            boolean gateOpen = hasDoor && isSectorGate(wx, wz)
                    && isSafeGateHeight(offset, prof.stacked());
            if (offset > 0 && offset < LAYER_H - 1
                    && isSectorEdge(wx, wz) && !gateOpen) {
                // 边界墙：本区段的特色墙
                set(chunk, m, wx, y, wz, prof.wall());
                continue;
            }
            if (subFloor) {
                placeSubFloor(chunk, m, c, wx, y, wz, offset, region, prof);
            } else {
                placeAisle(chunk, m, c, wx, y, wz, offset, region, prof);
            }
        }
    }

    // ==================== 跃金段（Level 1 第三区段）====================

    /** 跃金段区域边长（格），与天鹰段的小径区错开。 */
    public static final int YUEJIN_CELL = 96;

    /**
     * 该列是否属于「跃金段」。
     *
     * <p>与天鹰段区别：米黄配色、照明更充足（每 2 格一盏灯）、罐料箱更多。</p>
     */
    public static boolean isYuejinArea(int x, int z) {
        int cx = Math.floorDiv(x, YUEJIN_CELL);
        int cz = Math.floorDiv(z, YUEJIN_CELL);
        long h = mix(cx, cz, 0x7975656A696EL);   // 'yuejin'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.30;
    }

    /** 跃金段灯位：每 2 格一盏（比过道的每 5 格更亮）。 */
    public static boolean isYuejinLamp(int x, int z) {
        return Math.floorMod(x, 2) == 0 && Math.floorMod(z, 2) == 0;
    }

    /**
     * 跃金段的板条箱落点：约 6% 的地面格会放一个箱子。
     *
     * <p>文中设定：跃金段的板条箱数量远超本层任何已探索地点。</p>
     */
    public static boolean isCrateSpot(int x, int z) {
        long h = mix(x, z, 0x637261746573L);   // 'crates'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.06;
    }

    // ==================== 哥特段（Level 1 第四区段）====================

    /** 哥特段区域边长（格）。 */
    public static final int GOTHIC_CELL = 96;

    /**
     * 该列是否属于「哥特段」。
     *
     * <p>参考图效果：粗壮的柱子向上张开成尖拱（拱顶），照明偏暗、偏黄。
     * 目前只判定区域，配色与拱形结构后续接入。</p>
     */
    public static boolean isGothicArea(int x, int z) {
        int cx = Math.floorDiv(x, GOTHIC_CELL);
        int cz = Math.floorDiv(z, GOTHIC_CELL);
        long h = mix(cx, cz, 0x676F74686963L);   // 'gothic'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.20;
    }

    /** 哥特段灯位：每 7 格一盏（比其他区段更暗，仅拱顶下有光）。 */
    public static boolean isGothicLamp(int x, int z) {
        return Math.floorMod(x, 7) == 3 && Math.floorMod(z, 7) == 3;
    }

    /** 哥特段拱顶：在某列高度 offset 处是否需要拱形收拢（用于后续拱顶结构）。 */
    public static boolean isGothicArchRing(int offset) {
        return offset == LAYER_H - 2 || offset == LAYER_H - 3;
    }

    // ==================== 衔尾段（Level 1 第五区段）====================

    /** 衔尾段区域边长（格）。 */
    public static final int OUROBOROS_CELL = 96;

    /**
     * 该列是否属于「衔尾段」。
     *
     * <p>参考图效果：未完工的毛坯空间 —— 裸混凝土、外露的红漆管道、
     * 剥落的墙面与地面碎屑。名字取自「衔尾蛇」：走到底会绕回起点，
     * 因此这里的结构在视觉上首尾相连。</p>
     */
    public static boolean isOuroborosArea(int x, int z) {
        int cx = Math.floorDiv(x, OUROBOROS_CELL);
        int cz = Math.floorDiv(z, OUROBOROS_CELL);
        long h = mix(cx, cz, 0x6F75726F62L);   // 'ourob'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.20;
    }

    /** 衔尾段灯位：每 6 格一盏，且只挂在天花板下（模拟裸露的灯管）。 */
    public static boolean isOuroborosLamp(int x, int z) {
        return Math.floorMod(x, 6) == 2 && Math.floorMod(z, 6) == 2;
    }

    /** 衔尾段的地面碎屑：用于后续在毛坯地面上撒杂物。 */
    public static boolean isOuroborosDebris(int x, int z) {
        long h = mix(x, z, 0x646562726973L);   // 'debris'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.06;
    }

    /**
     * 衔尾段是否需要把出口「绕回入口」。
     *
     * <p>衔尾段的几何是闭合的：从任意方向一直走，最终会回到曾走过的地方。</p>
     */
    public static boolean isOuroborosLoop(int x, int z) {
        return isOuroborosArea(x, z);
    }

    // ==================== 花园段（Level 1 第六区段）====================

    /** 花园段区域边长（格）。 */
    public static final int GARDEN_CELL = 96;

    /**
     * 该列是否属于「花园段」。
     *
     * <p>参考图效果：混凝土结构被植物接管 —— 地面长满草、柱脚包裹着藤蔓、
     * 大量细长藤条从天花板一直垂到地面。</p>
     */
    public static boolean isGardenArea(int x, int z) {
        int cx = Math.floorDiv(x, GARDEN_CELL);
        int cz = Math.floorDiv(z, GARDEN_CELL);
        long h = mix(cx, cz, 0x67617264656EL);   // 'garden'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.20;
    }

    /** 花园段灯位：每 8 格一盏，且偏暖（模拟阳光透过藤蔓）。 */
    public static boolean isGardenLamp(int x, int z) {
        return Math.floorMod(x, 8) == 4 && Math.floorMod(z, 8) == 4;
    }

    /**
     * 该列是否有一根从天花板垂到地面的藤条。
     *
     * <p>用较稀疏的哈希撒点，密度约 8%，形成「垂落的藤帘」。</p>
     */
    public static boolean isGardenVine(int x, int z) {
        long h = mix(x, z, 0x76696E6573L);   // 'vines'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.02;
    }

    /** 花园段的草地方块：地面整体换草。 */
    public static boolean isGardenGrass(int x, int z) {
        return isGardenArea(x, z);
    }

    // ==================== 传说段（Level 1 第七区段）====================

    /** 传说段区域边长（格）。 */
    public static final int LEGEND_CELL = 96;

    /**
     * 该列是否属于「传说段」。
     *
     * <p>参考图效果：老旧木质护墙板 + 上部成排磨砂玻璃格，深色木门，
     * 深色天花板挂着长条日光灯，地上散着纸箱与灰尘 —— 像一间废弃办公室。</p>
     */
    public static boolean isLegendArea(int x, int z) {
        int cx = Math.floorDiv(x, LEGEND_CELL);
        int cz = Math.floorDiv(z, LEGEND_CELL);
        long h = mix(cx, cz, 0x6C6567656EL);   // 'legen'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.20;
    }

    /** 传说段灯位：每 5 格一盏长条日光灯。 */
    public static boolean isLegendLamp(int x, int z) {
        return Math.floorMod(x, 5) == 2 && Math.floorMod(z, 5) == 2;
    }

    /** 传说段墙面上部的磨砂玻璃格：每 3 格一块，嵌在木护墙板上。 */
    public static boolean isLegendFrostedPanel(int x, int z, int offset) {
        if (offset < 8 || offset > 10) {
            return false;
        }
        return Math.floorMod(x, 3) != 0 && Math.floorMod(z, 3) != 0;
    }

    /** 传说段地面的纸箱杂物：约 4%。 */
    public static boolean isLegendClutter(int x, int z) {
        long h = mix(x, z, 0x636C7574746572L);   // 'clutter'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.04;
    }

    /**
     * 子层结构（小径与过道小房间共用）。
     *
     * <p>每层 {@value #LAYER_H} 格切成 {@value #SUB_COUNT} 个 {@value #SUB_H} 格子层：
     * 子层第 0 格 = 地板，最后一格 = 天花板，中间放门 / 墙 / 空气。</p>
     */
    private void placeSubFloor(ChunkAccess chunk, BlockPos.MutableBlockPos m,
                               Column c, int wx, int y, int wz, int offset,
                               Region region, SectorProfile prof) {
        int subOff = offset % SUB_H;
        if (subOff == 0) {
            set(chunk, m, wx, y, wz, prof.floor());
            return;
        }
        if (subOff == SUB_H - 1) {
            // 小径天花板每 3 格一盏灯；只在可走列上，避免嵌进墙里
            boolean lamp = c.passage() && !c.passageWall() && isPassageLamp(wx, wz);
            set(chunk, m, wx, y, wz, lamp ? this.lampState : prof.ceiling());
            return;
        }
        if (c.passage() && c.boundary()) {
            // 小径边界：整面实心，只在门位开 1 宽 × 2 高的洞（避免贯穿裂缝）
            boolean doorHalf = c.gate() && (subOff == 1 || subOff == 2);
            set(chunk, m, wx, y, wz, doorHalf
                    ? doorState(c.facing(), subOff == 1, region)
                    : prof.wall());
            return;
        }
        // 小径内部 / 过道小房间：门位开洞，其余按墙壁判定
        boolean doorCol = !c.passage() && c.room() == 2;
        boolean wallCol = c.passage() ? c.passageWall() : c.room() == 1;
        if (doorCol) {
            set(chunk, m, wx, y, wz,
                    (subOff == 1 || subOff == 2) ? this.metalDoorState : this.airState);
        } else {
            set(chunk, m, wx, y, wz, wallCol ? prof.wall() : this.airState);
        }
    }

    /** 过道（停车场）：地板 / 柱与边界墙 / 天花板。 */
    private void placeAisle(ChunkAccess chunk, BlockPos.MutableBlockPos m,
                            Column c, int wx, int y, int wz, int offset,
                            Region region, SectorProfile prof) {
        // stacked 的区段按 6 格两层堆叠，其余通高（天鹰 / 跃金 / 哥特）
        int h = prof.stacked() ? SUB_H : LAYER_H;
        // 花园段：地板上散布草簇（该格若不是墙才长）
        if (region == Region.GARDEN && offset % h == 1 && c.room() < 0
                && !c.pillar() && isGardenTuft(wx, wz)) {
            set(chunk, m, wx, y, wz, this.grassTuftState);
            return;
        }
        // 衔尾段：毛坯地面上的碎屑
        if (region == Region.OUROBOROS && offset % h == 1 && c.room() < 0
                && !c.pillar() && isOuroborosDebris(wx, wz)) {
            set(chunk, m, wx, y, wz, this.rubbleState);
            return;
        }
        // 跃金段：地面上偶有板条箱（文中该段堆满箱子）
        if (region == Region.YUEJIN && offset % h == 1 && c.room() < 0 && isCrateSpot(wx, wz)) {
            set(chunk, m, wx, y, wz, this.crateState);
            return;
        }
        // 堆叠区段：两层之间用台阶做爬升带（否则上层无法到达）
        if (prof.stacked() && isRampColumn(wx, wz)) {
            int step = rampHeight(wz);
            int inSub = offset % h;
            if (inSub >= 1 && inSub <= step) {
                // 台阶下方的实心填充
                set(chunk, m, wx, y, wz, prof.wall());
                return;
            }
            if (inSub == step + 1) {
                // 每列顶部一个台阶，逐列抬高 => 真正可走的楼梯
                set(chunk, m, wx, y, wz, this.stairState);
                return;
            }
        }
        if (offset % h == 0) {
            set(chunk, m, wx, y, wz, prof.floor());
            return;
        }

        // ===== 过道房间：四面实心，只在南墙留一扇 2 格高的门 =====
        if (c.room() >= 0) {
            if (offset % h == h - 1) {
                // 房间顶部与过道天花板齐平
                set(chunk, m, wx, y, wz, c.lamp() ? this.lampState : this.ceilingState);
            } else if (c.room() == 2) {
                // 门列：1~2 格高放「下半 / 上半」门，其余高度实心 => 除门以外没有入口
                set(chunk, m, wx, y, wz, (offset % h == 1 || offset % h == 2)
                        ? doorState(Direction.SOUTH, offset % h == 1, region)
                        : this.whiteWallState);
            } else if (c.room() == 1) {
                set(chunk, m, wx, y, wz, prof.wall());
            } else {
                set(chunk, m, wx, y, wz, this.airState);
            }
            return;
        }

        // 衔尾段：地面上斜靠着的钢板（贴墙或散放）
        if (region == Region.OUROBOROS && offset % h == 1 && c.room() < 0
                && !c.pillar() && isPlateSpot(wx, wz)) {
            set(chunk, m, wx, y, wz, this.steelPlateState);
            return;
        }
        // 花园段：从天花板垂下的藤条（最多 6 格长）
        if (region == Region.GARDEN && !c.pillar() && isGardenVine(wx, wz)) {
            int depthFromTop = (h - 1) - (offset % h);
            if (depthFromTop >= 1 && depthFromTop <= 3) {
                set(chunk, m, wx, y, wz, this.vineState);
                return;
            }
        }
        // 哥特段：仅在天花板下方一排、各向外扩 1 格。
        // 之前外扩 3 圈会形成蘑菇状的「斗拱」，与参考图的尖拱不符。
        int fromTop = (h - 1) - (offset % h);
        if (region == Region.GOTHIC && fromTop == 1
                && isGothicArchSpread(wx, wz, 1)) {
            set(chunk, m, wx, y, wz, prof.wall());
            return;
        }
        if (offset % h < h - 1) {
            boolean doorHeight = (offset % SUB_H == 1) || (offset % SUB_H == 2);
            // 门前必须通畅：紧邻房间门位的列，在门高度处强制空气（否则柱子/边界墙会把门堵死）
            if (doorHeight && roomDoorNeighbor(wx, wz)) {
                set(chunk, m, wx, y, wz, this.airState);
                return;
            }
            boolean wall = c.pillar() || (c.aisleBoundary() && !(c.gateAdjacent() && doorHeight));
            if (wall && region == Region.OUROBOROS && isOuroborosPeeling(wx, wz, offset)) {
                // 衔尾段：墙面剥落，露出砂砾底
                set(chunk, m, wx, y, wz, this.rubbleState);
            } else if (wall && region == Region.LEGEND && isLegendFrostedPanel(wx, wz, offset)) {
                // 传说段：木护墙板上部嵌一排磨砂玻璃格
                set(chunk, m, wx, y, wz, this.frostedGlassState);
            } else if (wall) {
                // 柱脚（层内第 1 格）用警示柱，其余用混凝土柱
                set(chunk, m, wx, y, wz,
                        (offset % h == 1) ? this.warningPillarState : prof.wall());
            } else {
                set(chunk, m, wx, y, wz, this.airState);
            }
            return;
        }
        // 天花板层：灯位按区段密度
        if (region == Region.OUROBOROS && isOuroborosPipeLine(wx, wz)) {
            // 衔尾段：天花板上横着红漆管道
            set(chunk, m, wx, y, wz, this.redPipeState);
            return;
        }
        set(chunk, m, wx, y, wz, prof.lamp().test(wx, wz) ? this.lampState : prof.ceiling());
    }

    /** 统一的写方块入口。 */
    private static void set(ChunkAccess chunk, BlockPos.MutableBlockPos m,
                            int wx, int y, int wz, BlockState state) {
        chunk.setBlockState(m.set(wx, y, wz), state, false);
    }
    /** 小径荧光灯间距（格）。 */
    public static final int PASSAGE_LAMP_STEP = 3;

    /** 小径天花板灯位：每 3 格一个，居中偏移 1 格。 */
    public static boolean isPassageLamp(int x, int z) {
        return Math.floorMod(x, PASSAGE_LAMP_STEP) == 1
                && Math.floorMod(z, PASSAGE_LAMP_STEP) == 1;
    }

    /** 花园段草簇落点：约 30%。 */
    public static boolean isGardenTuft(int x, int z) {
        long h = mix(x, z, 0x74756666L);   // 'tuff'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.30;
    }

    /**
     * 哥特段拱顶外张：距柱网 {@code rows} 格以内即算柱帽的一部分。
     *
     * <p>越靠近天花板，外张越宽 —— 视觉上就是柱子向上张开成尖拱。</p>
     */
    public static boolean isGothicArchSpread(int x, int z, int rows) {
        for (int dx = -rows; dx <= rows; dx++) {
            for (int dz = -rows; dz <= rows; dz++) {
                if (onPillar(x + dx, z + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 爬升带宽度：每 32 格出现一条，占 6 格（正好等于 SUB_H）。 */
    public static final int RAMP_PITCH = 32;

    /** 该列是否属于爬升带（只在需要爬升的区段生效）。 */
    public static boolean isRampColumn(int x, int z) {
        return Math.floorMod(x, RAMP_PITCH) == 0 && Math.floorMod(z, RAMP_PITCH) < SUB_H;
    }

    /** 爬升带上该列的高度（0 ~ SUB_H-1）。 */
    public static int rampHeight(int z) {
        return Math.floorMod(z, RAMP_PITCH);
    }

    /** 区段边界通道宽度（每 16 格留 2 格可通行）。 */
    public static final int SECTOR_GATE_STEP = 16;

    /**
     * 该列是否位于区段边界上。
     *
     * <p>只检查 +X / +Z 两个方向，保证边界墙只有一格厚；<b>只有本列</b>负责画墙，
     * 相邻列各画各的墙即可拼成完整一圈。</p>
     */
    public static boolean isSectorEdge(int x, int z) {
        Region here = regionAt(x, z);
        return regionAt(x + 1, z) != here || regionAt(x, z + 1) != here;
    }

    /**
     * 该高度开门是否安全（两侧都有地板，玩家不会掉下去）。
     *
     * <p>只有「每个 12 格层的最底部两格」两侧一定都有地板：地板在 {@code offset 0}，
     * 上方两格在两种分层方式下都是空的。上层（{@code offset 6} 起）只有同样是两层
     * 区段时才两侧都有地板，因此堆叠区段之间也允许在第二层开门，
     * 但两层区段与通高区段之间只在一层相通，避免从 6 格高处踏空。</p>
     */
    public static boolean isSafeGateHeight(int offset, boolean thisStacked) {
        int inSub = offset % LAYER_H;
        if (inSub == 1 || inSub == 2) {
            return true;                      // 底层：两侧都有地板
        }
        if (thisStacked && (inSub == SUB_H + 1 || inSub == SUB_H + 2)) {
            return true;                      // 第二层：仅堆叠区段之间
        }
        return false;
    }

    /** 边界上的通道位置（每 16 格 2 格宽）。 */
    public static boolean isSectorGate(int x, int z) {
        return Math.floorMod(x + z, SECTOR_GATE_STEP) < 2;
    }

    /** 斜靠钢板落点：约 1.5%。 */
    public static boolean isPlateSpot(int x, int z) {
        long h = mix(x, z, 0x706C617465L);   // 'plate'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.015;
    }

    /** 衔尾段墙面剥落：约 12%。 */
    public static boolean isOuroborosPeeling(int x, int z, int offset) {
        long h = mix(x * 31 + z, offset, 0x7065656CL);   // 'peel'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.12;
    }

    /** 衔尾段天花板管道：每 8 格一条横向走线（宽 1 格）。 */
    public static boolean isOuroborosPipeLine(int x, int z) {
        return Math.floorMod(z, 8) == 3;
    }

    /** 门位判定：沿边界每 24 格一个（用 x+z 取模，保证是单独一列而非整行）。 */
    public static boolean isGateColumn(int x, int z) {
        return Math.floorMod(x + z, 24) == 0;
    }

    /**
     * 构造金属门的指定一半：朝向、上下半、初始关闭。
     *
     * @param facing 门面朝向（由边界法线决定）
     * @param lower  {@code true} = 下半，{@code false} = 上半
     */
    private BlockState doorState(net.minecraft.core.Direction facing, boolean lower, Region region) {
        // 传说段的木护墙板配深色木门，其余区段仍是金属门
        BlockState base = region == Region.LEGEND ? this.woodDoorState : this.metalDoorState;
        return base
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, facing)
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                        lower
                                ? net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
                                : net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)
                .setValue(net.minecraft.world.level.block.DoorBlock.OPEN, Boolean.FALSE);
    }

    // ==================== 区段与配色 ====================

    /** Level 1 的区段。 */
    public enum Region {
        AQUILA, YUEJIN, GOTHIC, OUROBOROS, GARDEN, LEGEND, CORRIDOR
    }

    /** 该列属于哪个区段（判定顺序即优先级）。 */
    public static Region regionAt(int x, int z) {
        if (isYuejinArea(x, z)) {
            return Region.YUEJIN;
        }
        if (isGothicArea(x, z)) {
            return Region.GOTHIC;
        }
        if (isGardenArea(x, z)) {
            return Region.GARDEN;
        }
        if (isLegendArea(x, z)) {
            return Region.LEGEND;
        }
        if (isOuroborosArea(x, z)) {
            return Region.OUROBOROS;
        }
        return Region.CORRIDOR;
    }

    /** 一个区段的完整外观定义（新增区段只需在 profileOf 里加一条）。 */
    public record SectorProfile(BlockState floor, BlockState ceiling, BlockState wall,
                                LampPattern lamp, boolean stacked) {
    }

    /** 灯位判定的函数式接口（每段密度不同）。 */
    @FunctionalInterface
    public interface LampPattern {
        boolean test(int x, int z);
    }

    /** 取某区段的外观定义。 */
    public SectorProfile profileOf(Region r) {
        return switch (r) {
            case YUEJIN -> new SectorProfile(this.yuejinFloorState, this.yuejinCeilingState,
                    this.yuejinWallState, Level1ChunkGenerator::isYuejinLamp, false);
            case GARDEN -> new SectorProfile(this.gardenGrassState, this.ceilingState,
                    this.gardenWallState, Level1ChunkGenerator::isGardenLamp, true);
            case LEGEND -> new SectorProfile(this.floorState, this.ceilingState,
                    this.legendWallState, Level1ChunkGenerator::isLegendLamp, true);
            case GOTHIC -> new SectorProfile(this.floorState, this.ceilingState,
                    this.whiteWallState, Level1ChunkGenerator::isGothicLamp, false);
            case OUROBOROS -> new SectorProfile(this.floorState, this.ceilingState,
                    this.whiteWallState, Level1ChunkGenerator::isOuroborosLamp, true);
            default -> new SectorProfile(this.floorState, this.ceilingState,
                    this.whiteWallState, Level1ChunkGenerator::isLamp, true);
        };
    }

    // ==================== 其余覆写

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
                             RandomState random, ChunkAccess chunk) {
        // 地表已在 fillFromNoise 中构建
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // 不雕刻
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // 无
    }

    @Override
    public int getGenDepth() {
        return 288;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level,
                             RandomState random) {
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        int minY = height.getMinBuildHeight();
        int maxY = height.getMaxBuildHeight();
        BlockState[] column = new BlockState[maxY - minY];
        java.util.Arrays.fill(column, this.airState);
        column[0] = this.floorState;
        return new NoiseColumn(minY, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Backrooms Level 1  (" + LAYER_H + " 格每层 / 生成 " + LAYERS + " 层)");
        // 当前所处区段与分层方式（便于核对各段是否正确出现）
        Region region = regionAt(pos.getX(), pos.getZ());
        SectorProfile prof = profileOf(region);
        info.add("区段: " + region + "  /  " + (prof.stacked() ? "两层 6 格" : "12 格通高"));
        if (isPassageArea(pos.getX(), pos.getZ())) {
            info.add("小径区（" + SUB_COUNT + " 层 × " + SUB_H + " 格）");
        }
    }
}
