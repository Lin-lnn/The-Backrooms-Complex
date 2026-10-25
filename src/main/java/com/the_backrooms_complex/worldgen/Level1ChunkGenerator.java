package com.the_backrooms_complex.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import com.the_backrooms_complex.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * <b>Level 1「宜居地带」专用区块生成器</b>。
 *
 * <p>与 Level 0 的 {@link BackroomsChunkGenerator} <b>完全独立</b>，
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
    public static final int LAYER_H = 18;
    /** 子层高度（小径用）。 */
    public static final int SUB_H = 6;
    /** 子层数量。 */
    public static final int SUB_COUNT = 3;

    /** 小径区边长（格）。 */
    public static final int PASSAGE_CELL = 96;
    /** 小径墙间距（格）。 */
    public static final int PASSAGE_WALL_STEP = 5;
    /** 小径墙缺口周期（格）。 */
    public static final int PASSAGE_GAP_PERIOD = 7;

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
    private final BlockState lampState;
    private final BlockState lampOffState;

    public Level1ChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
        this.floorState = ModBlocks.CONCRETE_FLOOR.get().defaultBlockState();
        this.ceilingState = ModBlocks.CONCRETE_CEILING.get().defaultBlockState();
        this.pillarState = ModBlocks.CONCRETE_PILLAR.get().defaultBlockState();
        this.warningPillarState = ModBlocks.CONCRETE_WARNING_PILLAR.get().defaultBlockState();
        this.whiteWallState = ModBlocks.WHITE_PLASTERED_WALL.get().defaultBlockState();
        this.lampState = ModBlocks.FLUORESCENT_LAMP.get().defaultBlockState();
        this.lampOffState = ModBlocks.LAMP_OFF.get().defaultBlockState();
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

    /**
     * 小径内部的墙体判定。
     *
     * <p>交叉点<b>一律打通</b>，保证每个 5×5 格子四周至少有一个开口，
     * 不会出现玩家进不去的密闭空腔。</p>
     */
    public static boolean isPassageWall(int x, int z) {
        boolean vWall = Math.floorMod(x, PASSAGE_WALL_STEP) == 0;
        boolean hWall = Math.floorMod(z, PASSAGE_WALL_STEP) == 0;
        if (vWall && hWall) {
            return false;   // 交叉点必通
        }
        if (!vWall && !hWall) {
            return false;   // 走廊内部
        }
        if (vWall) {
            return Math.floorMod(z, PASSAGE_GAP_PERIOD) > 1;
        }
        return Math.floorMod(x, PASSAGE_GAP_PERIOD) > 1;
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
        long h = (long) cx * 0x94D049BB133111EBL ^ (long) cz * 0x2545F4914F6CDD1DL ^ 0x726F6F6DL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        if (((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) >= 0.60) {
            return -1;
        }
        int ox = Math.floorMod(x, ROOM_CELL) - ROOM_CELL / 2;
        int oz = Math.floorMod(z, ROOM_CELL) - ROOM_CELL / 2;
        int ax = Math.abs(ox);
        int az = Math.abs(oz);
        if (ax <= ROOM_RADIUS && az <= ROOM_RADIUS) {
            if (oz == ROOM_RADIUS && ox == 0) {
                return 2;   // 南墙正中 = 门
            }
            if (ax == ROOM_RADIUS || az == ROOM_RADIUS) {
                return 1;   // 墙
            }
            return 0;
        }
        return -1;
    }

    // ==================== 区块生成 ====================

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager,
                                                        ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        int chunkX = cp.x;
        int chunkZ = cp.z;
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = chunkX * 16 + lx;
                int wz = chunkZ * 16 + lz;

                // ---- 列级常量（只依赖 XZ）----
                final boolean colPassage = isPassageArea(wx, wz);
                final boolean colBoundary = isPassageBoundary(wx, wz);
                final boolean colGate = colBoundary && Math.floorMod(wx + wz, 24) == 0;
                final boolean colPassageWall = !colPassage && colBoundary;
                final boolean colPillar = onPillar(wx, wz);
                final boolean colLamp = isLamp(wx, wz);
                final int colRoom = roomKind(wx, wz);

                for (int y = minY; y < maxY; y++) {
                    int layer = y / LAYER_H;
                    int offset = y - layer * LAYER_H;

                    // ===== 小径：3 个子层，每个 6 格 =====
                    if (colPassage && offset < SUB_COUNT * SUB_H) {
                        int sub = offset / SUB_H;
                        int subOff = offset - sub * SUB_H;
                        if (subOff == 0) {
                            chunk.setBlockState(m.set(wx, y, wz), floorState, false);
                        } else if (subOff == SUB_H - 1) {
                            chunk.setBlockState(m.set(wx, y, wz), ceilingState, false);
                        } else if (colGate) {
                            // 门洞：1~2 格高处放金属门
                            chunk.setBlockState(m.set(wx, y, wz),
                                    (subOff == 1 || subOff == 2)
                                            ? ModBlocks.METAL_DOOR.get().defaultBlockState()
                                            : airState, false);
                        } else {
                            chunk.setBlockState(m.set(wx, y, wz),
                                    isPassageWall(wx, wz) ? whiteWallState : airState, false);
                        }
                        continue;
                    }

                    // ===== 过道小房间 =====
                    if (!colPassage && colRoom >= 0 && offset < SUB_COUNT * SUB_H) {
                        int sub = offset / SUB_H;
                        int subOff = offset - sub * SUB_H;
                        if (subOff == 0) {
                            chunk.setBlockState(m.set(wx, y, wz), floorState, false);
                        } else if (subOff == SUB_H - 1) {
                            chunk.setBlockState(m.set(wx, y, wz), ceilingState, false);
                        } else if (colRoom == 2) {
                            chunk.setBlockState(m.set(wx, y, wz),
                                    (subOff == 1 || subOff == 2)
                                            ? ModBlocks.METAL_DOOR.get().defaultBlockState()
                                            : airState, false);
                        } else {
                            chunk.setBlockState(m.set(wx, y, wz),
                                    colRoom == 1 ? whiteWallState : airState, false);
                        }
                        continue;
                    }

                    // ===== 过道：开阔停车场 =====
                    if (offset == 0) {
                        chunk.setBlockState(m.set(wx, y, wz), floorState, false);
                    } else if (offset < LAYER_H - 1) {
                        boolean wall = colPillar || (colPassageWall && !colGate);
                        if (wall) {
                            // 柱脚（层内第 1 格）用警示柱，其余用混凝土柱
                            chunk.setBlockState(m.set(wx, y, wz),
                                    (offset == 1) ? warningPillarState : pillarState, false);
                        } else {
                            chunk.setBlockState(m.set(wx, y, wz), airState, false);
                        }
                    } else {
                        // 天花板层：每 5 格一个荧光灯
                        chunk.setBlockState(m.set(wx, y, wz),
                                colLamp ? lampState : ceilingState, false);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    // ==================== 其余覆写 ====================

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
        java.util.Arrays.fill(column, Blocks.AIR.defaultBlockState());
        column[0] = this.floorState;
        return new NoiseColumn(minY, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Backrooms Level 1  (288 高 / " + LAYER_H + " 格每层 / " + (288 / LAYER_H) + " 层)");
    }
}
