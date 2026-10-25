package com.the_backrooms_complex.worldgen.level1;

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
import com.the_backrooms_complex.worldgen.level1.Level1Sectors.Region;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.the_backrooms_complex.worldgen.level1.Level1Sectors.*;
import static com.the_backrooms_complex.worldgen.level1.Level1Geometry.*;

/**
 * <b>Level 1「宜居地带」专用区块生成器</b>。
 *
 * <p>与 Level 0 的生成器 <b>完全独立</b>，
 * 没有任何共享的方块、配色或布局逻辑 —— 它自己就是一套生成规则。</p>
 *
 * <h2>结构</h2>
 * <ul>
 *   <li><b>层高 12 格</b>（2 个 6 格子层：地板 1 + 空气 4 + 天花板 1），
 *       288 高最多容纳 24 层，实际只生成前 3 层（总高 36）；</li>
 *   <li><b>过道（停车场）</b>：一整片开阔地面，只有 <b>2×2 混凝土柱</b>
 *       按 16 格网格排布；</li>
 *   <li><b>小径</b>：约 45% 的 96×96 方块区，是 <b>2 个子层 × 6 格高</b>的
 *       白墙窄迷宫；</li>
 *   <li><b>过道小房间</b>：64 格网格里每层 5~10 个 7×7 房间，南墙正中一扇门；</li>
 *   <li><b>连接</b>：区段交界处用本区段墙体封边，每 32 格开一条 4 格宽通道；
 *       两层区段的上下两个子层都开通道；</li>
 *   <li><b>光照</b>：天花板层按各区段灯距嵌入荧光灯（唯一沿用的 Level 0 方块）。</li>
 * </ul>
 */
public class Level1ChunkGenerator extends ChunkGenerator {

    // ==================== 常量 ====================

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
    /** 各墙体的踢脚线（立体裙边）。 */
    private final BlockState whiteSkirtState;
    private final BlockState yuejinSkirtState;
    private final BlockState gardenSkirtState;
    private final BlockState legendSkirtState;
    /** 板条箱方块状态（惰性取，避免注册顺序问题）。 */
    private BlockState crateState;
    /** 花园段地面的草簇。 */
    private final BlockState grassTuftState;
    /** 传说段墙面的磨砂玻璃格。 */
    private final BlockState frostedGlassState;
    /** 衔尾段的破损补丁（砂砾）。 */
    private final BlockState rubbleState;
    /** 衔尾段碎屑里的两种混凝土粉末。 */
    private final BlockState powderLightState;
    private final BlockState powderWhiteState;
    /** 两层之间的爬升台阶。 */
    private final BlockState stairState;
    /** 花园段从天花板垂下的藤蔓。 */
    private final BlockState vineState;
    /** 传说段的深色木门。 */
    private final BlockState woodDoorState;
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
        this.whiteSkirtState = ModBlocks.WHITE_PLASTERED_WALL_SKIRTING.get().defaultBlockState();
        this.yuejinSkirtState = ModBlocks.YELLOWED_CONCRETE_WALL_SKIRTING.get().defaultBlockState();
        this.gardenSkirtState = ModBlocks.GARDEN_WALL_SKIRTING.get().defaultBlockState();
        this.legendSkirtState = ModBlocks.LEGEND_WALL_SKIRTING.get().defaultBlockState();
        // 注意：不能在这里取 CRATE 的方块状态。
        // CrateBlock 现在是 BaseEntityBlock，其 BlockEntityType 需要 ModBlocks.CRATE.get()，
        // 在注册完成前取值会触发空指针；改为生成时按需取值（见 crateState()）。
        this.grassTuftState = net.minecraft.world.level.block.Blocks.SHORT_GRASS.defaultBlockState();
        this.frostedGlassState = net.minecraft.world.level.block.Blocks.WHITE_STAINED_GLASS.defaultBlockState();
        this.rubbleState = net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState();
        this.powderLightState = net.minecraft.world.level.block.Blocks.LIGHT_GRAY_CONCRETE_POWDER
                .defaultBlockState();
        this.powderWhiteState = net.minecraft.world.level.block.Blocks.WHITE_CONCRETE_POWDER
                .defaultBlockState();
        // 楼梯沿 +Z 方向爬升，因此台阶朝向南（高的一侧在后方）
        this.stairState = ModBlocks.CONCRETE_FLOOR_STAIRS.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING,
                        net.minecraft.core.Direction.SOUTH);
        // 普通绿色藤蔓；贴顶时置 UP，贴墙时置对应方向
        this.vineState = net.minecraft.world.level.block.Blocks.VINE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.VineBlock.UP, Boolean.TRUE);
        this.woodDoorState = net.minecraft.world.level.block.Blocks.DARK_OAK_DOOR.defaultBlockState();
        this.lampState = ModBlocks.FLUORESCENT_LAMP.get().defaultBlockState();
        this.lampOffState = ModBlocks.LAMP_OFF.get().defaultBlockState();
        this.metalDoorState = ModBlocks.METAL_DOOR.get().defaultBlockState();
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /** 灯位判定。 */
    public static boolean isLamp(int x, int z) {
        return Math.floorMod(x, LAMP_STEP) == 2 && Math.floorMod(z, LAMP_STEP) == 2;
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
    private static Column columnAt(int wx, int wz, int layer) {
        boolean passage = isPassageArea(wx, wz);
        boolean boundary = isPassageBoundary(wx, wz);
        int room = roomKind(wx, wz, layer);
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
                onPillar(wx, wz, layer),
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
        Column c = null;
        boolean subFloor = false;
        // 柱子列优先：柱子上绝不叠加小径/房间的子层结构（否则会长出木板+石板的混合柱）
        // 区段与配色按「层」计算：每一层的区段布局不同
        Region region = null;
        SectorProfile prof = null;
        int lastLayer = Integer.MIN_VALUE;

        for (int y = minY; y < maxY; y++) {
            int offset = y - (y / LAYER_H) * LAYER_H;
            // 每层用不同的区段布局
            int layer = y / LAYER_H;
            if (region == null || layer != lastLayer) {
                lastLayer = layer;
                region = regionAt(wx, wz, layer);
                prof = profileOf(region);
                // 柱子 / 房间 / 小径判定同样按层，三层布局才真正不同
                c = columnAt(wx, wz, layer);
                subFloor = !c.pillar() && c.passage();
            }
            // 包络墙：边界列完全由这里负责，地物不得抢占。
            // 每隔 32 格打通一条 4 格宽的通道，高度取两侧「单层高度」中较小的一个。
            if (offset > 0 && offset < LAYER_H - 1 && isSectorEdge(wx, wz, layer)) {
                Region other = otherRegionAcross(wx, wz, layer, region);
                // 通道高度取两侧单层高度的较小值，且永远不凿穿本层天花板
                int openHeight = Math.min(Math.min(storeyHeight(region), storeyHeight(other)),
                        LAYER_H - 1);
                boolean tunnel = isTunnelOpening(wx, wz, layer);
                // 两侧都是两层区段时，上层子层也开同一条通道；两层的开口上下对齐，
                // 中间保留 offset == SUB_H 的楼板，因此是两条独立隧道而不是一个挑空。
                boolean upperTunnel = tunnel
                        && isStackedRegion(region) && isStackedRegion(other)
                        && LAYER_H >= 2 * SUB_H
                        && offset > SUB_H && offset < LAYER_H - 1;
                if ((tunnel && offset < openHeight) || upperTunnel) {
                    set(chunk, m, wx, y, wz, this.airState);
                } else if (offset == 1) {
                    // 包络墙底部同样使用踢脚线
                    set(chunk, m, wx, y, wz, wallBaseOf(region));
                } else {
                    set(chunk, m, wx, y, wz, prof.wall());
                }
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
        return isYuejinArea(x, z, 0);
    }

    public static boolean isYuejinArea(int x, int z, int layer) {
        int cx = Math.floorDiv(x, YUEJIN_CELL);
        int cz = Math.floorDiv(z, YUEJIN_CELL);
        long h = mix(cx, cz, 0x7975656A696EL ^ (layer * 0x9E3779B97F4A7C15L));   // 'yuejin'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.30;
    }

    /** 跃金段灯位：每 2 格一盏（比过道的每 5 格更亮）。 */
    public static boolean isYuejinLamp(int x, int z) {
        return Math.floorMod(x, 2) == 0 && Math.floorMod(z, 2) == 0;
    }

    /** 跃金段板条箱密度（地面格比例）。 */
    public static final double CRATE_CHANCE_YUEJIN = 0.0012D;

    /** 其他区段的板条箱密度：跃金段的十分之一。 */
    public static final double CRATE_CHANCE_OTHER = CRATE_CHANCE_YUEJIN / 10.0D;

    /**
     * 板条箱落点。
     *
     * <p>跃金段最密（约 0.12%），其他区段只有它的十分之一（约 0.012%）。</p>
     */
    public static boolean isCrateSpot(int x, int z, Region region) {
        long h = mix(x, z, 0x637261746573L);   // 'crates'
        double roll = (h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL;
        return roll < (region == Region.YUEJIN ? CRATE_CHANCE_YUEJIN : CRATE_CHANCE_OTHER);
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
        return isGothicArea(x, z, 0);
    }

    public static boolean isGothicArea(int x, int z, int layer) {
        int cx = Math.floorDiv(x, GOTHIC_CELL);
        int cz = Math.floorDiv(z, GOTHIC_CELL);
        long h = mix(cx, cz, 0x676F74686963L ^ (layer * 0x9E3779B97F4A7C15L));   // 'gothic'
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
        return isOuroborosArea(x, z, 0);
    }

    public static boolean isOuroborosArea(int x, int z, int layer) {
        int cx = Math.floorDiv(x, OUROBOROS_CELL);
        int cz = Math.floorDiv(z, OUROBOROS_CELL);
        long h = mix(cx, cz, 0x6F75726F62L ^ (layer * 0x9E3779B97F4A7C15L));   // 'ourob'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.20;
    }

    /** 衔尾段灯位：每 6 格一盏，且只挂在天花板下（模拟裸露的灯管）。 */
    public static boolean isOuroborosLamp(int x, int z) {
        return Math.floorMod(x, 6) == 2 && Math.floorMod(z, 6) == 2;
    }

    /** 碎屑方块：砂砾为主，混入淡灰与白色混凝土粉末。 */
    private BlockState debrisState(int x, int y, int z) {
        long h = mix(x, y * 31 + z, 0x6465627269734CL);   // 'debris'
        int pick = (int) Math.floorMod(h >>> 7, 100L);
        if (pick < 62) {
            return this.rubbleState;
        }
        if (pick < 82) {
            return this.powderLightState;
        }
        return this.powderWhiteState;
    }

    /** 衔尾段的地面碎屑：用于后续在毛坯地面上撒杂物。 */
    public static boolean isOuroborosDebris(int x, int z) {
        long h = mix(x, z, 0x646562726973L);   // 'debris'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.03;
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
        return isGardenArea(x, z, 0);
    }

    public static boolean isGardenArea(int x, int z, int layer) {
        int cx = Math.floorDiv(x, GARDEN_CELL);
        int cz = Math.floorDiv(z, GARDEN_CELL);
        long h = mix(cx, cz, 0x67617264656EL ^ (layer * 0x9E3779B97F4A7C15L));   // 'garden'
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
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.06;
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
        return isLegendArea(x, z, 0);
    }

    public static boolean isLegendArea(int x, int z, int layer) {
        int cx = Math.floorDiv(x, LEGEND_CELL);
        int cz = Math.floorDiv(z, LEGEND_CELL);
        long h = mix(cx, cz, 0x6C6567656EL ^ (layer * 0x9E3779B97F4A7C15L));   // 'legen'
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
        int layer = y / LAYER_H;
        // 楼梯优先：楼梯井必须贯穿本子层的天花板和上一子层的地板，否则会撞头。
        // 台阶表面在 offset = step + 1，表面以下实心、表面以上留 2 格净空；
        // 最后一列的表面正好落在上一子层的地板上（offset = SUB_H），因此能真正走上去。
        // 只在空旷列上生成：柱子 / 房间 / 小径边界 / 区段边界都不允许被替换。
        if (prof.stacked() && allowsStairs(region) && offset > 0) {
            int step = rampStep(wx, wz, layer);
            boolean openColumn = !c.pillar() && c.room() < 0 && !c.boundary()
                    && !isSectorEdge(wx, wz, layer);
            if (step >= 0 && openColumn) {
                int surface = step + 1;
                if (offset <= surface + 2) {
                    if (offset < surface) {
                        set(chunk, m, wx, y, wz, prof.wall());      // 台阶下方的实心填充
                    } else if (offset == surface) {
                        set(chunk, m, wx, y, wz, this.stairState); // 台阶表面
                    } else {
                        set(chunk, m, wx, y, wz, this.airState);   // 头顶净空（含天花板 / 上层地板）
                    }
                    return;
                }
            }
        }
        // 花园段：地板上散布草簇（该格若不是墙才长）
        if (region == Region.GARDEN && offset % h == 1 && c.room() < 0
                && !c.pillar() && isGardenTuft(wx, wz)) {
            set(chunk, m, wx, y, wz, this.grassTuftState);
            return;
        }
        // 衔尾段：毛坯地面上的碎屑
        if (region == Region.OUROBOROS && offset % h == 1 && c.room() < 0
                && !c.pillar() && isOuroborosDebris(wx, wz)) {
            set(chunk, m, wx, y, wz, debrisState(wx, y, wz));
            return;
        }
        // 地面上偶有板条箱（跃金段最密，其他区段只有十分之一）
        if (offset % h == 1 && c.room() < 0 && isCrateSpot(wx, wz, region)) {
            set(chunk, m, wx, y, wz, crateState());
            return;
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
                        : prof.wall());
            } else if (c.room() == 1) {
                set(chunk, m, wx, y, wz, prof.wall());
            } else {
                set(chunk, m, wx, y, wz, this.airState);
            }
            return;
        }

        // 花园段藤蔓：只贴墙或贴天花板，不向下悬挂。
        if (region == Region.GARDEN && !c.pillar() && isGardenVine(wx, wz)) {
            int inSub = offset % h;
            // ① 贴天花板：紧贴天花板下一格，朝上的藤蔓
            if (inSub == h - 2) {
                set(chunk, m, wx, y, wz, this.vineState);
                return;
            }
            // ② 贴墙：某个水平邻居是区段边界墙，就在该侧生成藤蔓
            if (inSub >= 2 && inSub <= h - 3) {
                var face = vineFace(wx, wz, layer);
                if (face != null) {
                    set(chunk, m, wx, y, wz, this.vineState
                            .setValue(vineProperty(face), Boolean.TRUE));
                    return;
                }
            }
        }
        // 衔尾段(红) / 传说段(灰)：管道吊在天花板下方一格，交叉处带吊架
        if ((region == Region.OUROBOROS || region == Region.LEGEND)
                && offset % h == h - 2 && !c.pillar()) {
            BlockState pipe = pipeAt(region, wx, wz);
            if (pipe != null) {
                set(chunk, m, wx, y, wz, pipe);
                return;
            }
        }
        // 哥特段：拱顶外张「顶部最宽、向下收窄」=> 正确的起拱方向。
        // fromTop = 1 贴着天花板（半径 3），往下逐排收成 2、1。
        int fromTop = (h - 1) - (offset % h);
        if (region == Region.GOTHIC && fromTop >= 1 && fromTop <= 3
                && isGothicArchSpread(wx, wz, 4 - fromTop, layer)) {
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
                        (offset % h == 1) ? wallBaseOf(region) : prof.wall());
            } else {
                set(chunk, m, wx, y, wz, this.airState);
            }
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

    /** 藤蔓在当前格应贴的方向；没有可贴的墙时返回 {@code null}。 */
    private static net.minecraft.core.Direction vineFace(int x, int z, int layer) {
        if (isSectorEdge(x - 1, z, layer)) {
            return net.minecraft.core.Direction.WEST;
        }
        if (isSectorEdge(x + 1, z, layer)) {
            return net.minecraft.core.Direction.EAST;
        }
        if (isSectorEdge(x, z - 1, layer)) {
            return net.minecraft.core.Direction.NORTH;
        }
        if (isSectorEdge(x, z + 1, layer)) {
            return net.minecraft.core.Direction.SOUTH;
        }
        return null;
    }

    /** 方向 -> 藤蔓方块的方向属性。 */
    private static net.minecraft.world.level.block.state.properties.BooleanProperty
            vineProperty(net.minecraft.core.Direction direction) {
        return switch (direction) {
            case NORTH -> net.minecraft.world.level.block.VineBlock.NORTH;
            case SOUTH -> net.minecraft.world.level.block.VineBlock.SOUTH;
            case EAST -> net.minecraft.world.level.block.VineBlock.EAST;
            default -> net.minecraft.world.level.block.VineBlock.WEST;
        };
    }

    /** 花园段草簇落点：约 30%。 */
    public static boolean isGardenTuft(int x, int z) {
        long h = mix(x, z, 0x74756666L);   // 'tuff'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.30;
    }

    /**
     * 墙底（层内第 1 格）用的方块。
     *
     * <p>「混凝土警示柱」只在天鹰段使用；其余区段用各自墙体的踢脚线。</p>
     */
    private BlockState wallBaseOf(Region region) {
        return switch (region) {
            case AQUILA -> this.warningPillarState;          // 仅天鹰段用警示柱
            case YUEJIN -> this.yuejinSkirtState;
            case GARDEN -> this.gardenSkirtState;
            case LEGEND -> this.legendSkirtState;
            default -> this.whiteSkirtState;                 // 走廊 / 哥特 / 衔尾
        };
    }

    /** 衔尾段墙面剥落：约 12%。 */
    public static boolean isOuroborosPeeling(int x, int z, int offset) {
        long h = mix(x * 31 + z, offset, 0x7065656CL);   // 'peel'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.12;
    }

    /**
     * 衔尾段天花板管道：横向与纵向各每 8 格一条，交叉处四向连通。
     *
     * @return 该列应放的管道状态；不是管线时返回 {@code null}
     */
    private BlockState pipeAt(Region region, int x, int z) {
        // 衔尾段用红漆管道（每 8 格），传说段用灰色管道（每 12 格）
        int step;
        net.neoforged.neoforge.registries.DeferredBlock<?> block;
        if (region == Region.OUROBOROS) {
            step = 8;
            block = ModBlocks.RED_PIPE;
        } else if (region == Region.LEGEND) {
            step = 12;
            block = ModBlocks.BRASS_PIPE;
        } else {
            return null;
        }
        int offset = step / 2 + 1;
        boolean eastWest = Math.floorMod(z, step) == offset;
        boolean northSouth = Math.floorMod(x, step) == offset;
        if (!eastWest && !northSouth) {
            return null;
        }
        BlockState state = block.get().defaultBlockState();
        if (eastWest) {
            state = state.setValue(net.minecraft.world.level.block.PipeBlock.EAST, Boolean.TRUE)
                    .setValue(net.minecraft.world.level.block.PipeBlock.WEST, Boolean.TRUE);
        }
        if (northSouth) {
            state = state.setValue(net.minecraft.world.level.block.PipeBlock.NORTH, Boolean.TRUE)
                    .setValue(net.minecraft.world.level.block.PipeBlock.SOUTH, Boolean.TRUE);
        }
        // 交叉处向上接天花板，形成吊架（每 step 格一个）
        if (eastWest && northSouth) {
            state = state.setValue(net.minecraft.world.level.block.PipeBlock.UP, Boolean.TRUE);
        }
        return state;
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

    /** 惰性获取板条箱状态：首次使用时才查注册表。 */
    private BlockState crateState() {
        if (this.crateState == null) {
        this.crateState = ModBlocks.CRATE.get().defaultBlockState()
                    .setValue(com.the_backrooms_complex.block.CrateBlock.NATURAL, true);
        }
        return this.crateState;
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
        // 当前所处区段与分层方式（按玩家所在层判定，而不是固定第 0 层）
        int layer = Math.floorDiv(pos.getY(), LAYER_H);
        Region region = regionAt(pos.getX(), pos.getZ(), layer);
        SectorProfile prof = profileOf(region);
        info.add("第 " + layer + " 层  /  区段: " + region + "  /  "
                + (prof.stacked() ? "两层 6 格" : "12 格通高"));
        if (isPassageArea(pos.getX(), pos.getZ())) {
            info.add("小径区（" + SUB_COUNT + " 层 × " + SUB_H + " 格）");
        }
    }
}
