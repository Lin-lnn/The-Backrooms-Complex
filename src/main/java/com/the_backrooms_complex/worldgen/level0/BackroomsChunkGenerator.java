package com.the_backrooms_complex.worldgen.level0;

import com.the_backrooms_complex.ModBlocks;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Level 0「黄色房间」专用区块生成器。
 *
 * <p>布局：哈希抖动的拓扑主干（{@link RoomGraph}）+ L 形折线走廊
 * （{@link TopologyWalls}），再叠加宽阔街道（{@link BackroomsAvenue}）与
 * 矩形房间（{@link BackroomsRooms}）。所有随机性都由坐标与层号确定，
 * 不使用运行时事件或全局状态。</p>
 *
 * <p>方块只有一套黄色房间配色：地板 / 天花板 / 荧光灯 / 黄色墙壁与踢脚线。</p>
 */
public class BackroomsChunkGenerator extends ChunkGenerator {

    // ==================== 常量 ====================

    /** 实际生成的层数。 */
    public static final int LEVEL_LAYERS = 5;
    /** 世界高度。 */
    private static final int WORLD_HEIGHT = 256;
    /** 世界最小 Y。 */
    private static final int MIN_Y = 0;

    /** 单层高度（格）：地板 1 + 墙壁/空气 6 + 天花板 1。 */
    public static final int LAYER_HEIGHT = 8;
    /**
     * 当前区块实际可用的层数。
     *
     * <p>不依赖 {@link #getGenDepth()} 的硬编码世界高度，而是读取区块自身高度，
     * 这样即使维度高度变化也不会越界。</p>
     */
    private static int fullLayers(net.minecraft.world.level.LevelHeightAccessor heightAccessor) {
        return Math.min(LEVEL_LAYERS, Math.max(1, heightAccessor.getHeight() / LAYER_HEIGHT));
    }

    // ==================== 编解码器 ====================

    public static final MapCodec<BackroomsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source")
                            .forGetter(BackroomsChunkGenerator::getBiomeSource)
            ).apply(instance, BackroomsChunkGenerator::new));

    // ==================== 字段 ====================

    private final BlockState floorState;
    private final BlockState ceilingState;
    private final BlockState lampState;
    private final BlockState yellowWall;
    private final BlockState yellowPlinth;
    private final BlockState airState = Blocks.AIR.defaultBlockState();

    public BackroomsChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
        this.floorState = ModBlocks.FLOOR_TILE.get().defaultBlockState();
        this.ceilingState = ModBlocks.CEILING_TILE.get().defaultBlockState();
        this.lampState = ModBlocks.FLUORESCENT_LAMP.get().defaultBlockState();
        this.yellowWall = ModBlocks.WALL_YELLOW.get().defaultBlockState();
        this.yellowPlinth = ModBlocks.WALL_YELLOW_PLINTH.get().defaultBlockState();
    }

    // ==================== 区块生成 ====================

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager,
                                                        ChunkAccess chunk) {
        this.populateNoise(chunk);
        this.populateBiomes(chunk, randomState);
        return CompletableFuture.completedFuture(chunk);
    }

    /** 生成地板、踢脚线、墙壁、天花板与荧光灯。 */
    private void populateNoise(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        int minY = chunk.getMinBuildHeight();
        int maxY = Math.min(chunk.getMaxBuildHeight(), LEVEL_LAYERS * LAYER_HEIGHT);

        boolean[][][] wall = BackroomsMaze.generateWallGrids(chunkX, chunkZ, fullLayers(chunk));
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                fillColumn(chunk, mutable, wall, chunkX * 16 + lx, chunkZ * 16 + lz,
                        lx, lz, minY, maxY);
            }
        }
        // 第二遍：墙壁已全部就位，此时再决定灯位（走廊取中线 / 房间按 3 格网格）
        placeLamps(chunk, wall, chunkX, chunkZ, minY, maxY);
    }

    /**
     * 生成一整列（从 {@code minY} 到 {@code maxY}）。
     *
     * <p>三项优化都在这里：
     * <ol>
     *   <li><b>按层循环</b>：不再对每个方块做 {@code y / LAYER_HEIGHT} 与取模，
     *       层号与偏移直接由循环变量给出；</li>
     *   <li><b>合并连续墙段</b>：踢脚线与墙身是连续的一段，一次遍历写完；</li>
     *   <li><b>section 直写</b>：墙身位于天花板之下，不影响高度图，
     *       因此跳过 {@code ChunkAccess#setBlockState}，直接写所属
     *       {@link LevelChunkSection}，省掉每次写入的索引与回调开销。</li>
     * </ol>
     * 地板与天花板仍走正常写入通道，保证高度图与光照正确。</p>
     */
    private void fillColumn(ChunkAccess chunk, BlockPos.MutableBlockPos mutable,
                            boolean[][][] wall, int worldX, int worldZ,
                            int lx, int lz, int minY, int maxY) {
        int layers = fullLayers(chunk);

        // 墙体判定按层预先算好（每列每层一次）
        boolean[] colWall = new boolean[layers];
        for (int l = 0; l < layers; l++) {
            colWall[l] = resolveWall(wall, lx, lz, worldX, worldZ, l);
        }

        int lx15 = worldX & 15;
        int lz15 = worldZ & 15;

        for (int layer = 0; layer < (maxY - minY) / LAYER_HEIGHT + 1; layer++) {
            int baseY = minY + layer * LAYER_HEIGHT;
            if (baseY >= maxY) {
                break;
            }
            if (layer >= layers) {
                // 顶部残层：用天花板封顶，闭合维度
                for (int y = baseY; y < maxY; y++) {
                    chunk.setBlockState(mutable.set(worldX, y, worldZ), ceilingState, false);
                }
                break;
            }

            // 地板
            chunk.setBlockState(mutable.set(worldX, baseY, worldZ), floorState, false);

            // 踢脚线 + 墙身：连续一段，合并写入
            if (colWall[layer]) {
                setSectionBlock(chunk, lx15, baseY + 1, lz15, yellowPlinth);
                for (int offset = 2; offset <= LAYER_HEIGHT - 2; offset++) {
                    setSectionBlock(chunk, lx15, baseY + offset, lz15, yellowWall);
                }
            }

            // 天花板（灯具留到第二遍再放，见 placeLamps）
            chunk.setBlockState(mutable.set(worldX, baseY + LAYER_HEIGHT - 1, worldZ),
                    ceilingState, false);
        }
    }

    /**
     * 第二遍：在天花板上放荧光灯。
     *
     * <p>规则分两类：</p>
     * <ul>
     *   <li><b>走廊</b>：只有当该格是 1 格宽走廊的<b>正中</b>时放灯，保证灯居中照亮；</li>
     *   <li><b>房间 / 其它随机区域</b>：按全局 3 格网格整齐排布（{@code x%3==0 && z%3==0}），
     *       形状规整、间距一致。</li>
     * </ul>
     */
    private void placeLamps(ChunkAccess chunk, boolean[][][] wall,
                            int chunkX, int chunkZ, int minY, int maxY) {
        int layers = fullLayers(chunk);
        for (int layer = 0; layer < layers; layer++) {
            int ceilingY = minY + layer * LAYER_HEIGHT + LAYER_HEIGHT - 1;
            if (ceilingY >= maxY) {
                break;
            }
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int worldX = chunkX * 16 + lx;
                    int worldZ = chunkZ * 16 + lz;
                    if (!lampWanted(wall, layer, lx, lz, worldX, worldZ)) {
                        continue;
                    }
                    chunk.setBlockState(
                            new BlockPos(worldX, ceilingY, worldZ),
                            lampState, false);
                }
            }
        }
    }

    /**
     * 该格天花板是否放灯。
     *
     * <p>1 格宽走廊取正中；其余（房间、街道、随机区域）按 3 格网格，保证整齐。</p>
     */
    private static boolean lampWanted(boolean[][][] wall, int layer, int lx, int lz,
                                      int worldX, int worldZ) {
        boolean here = !wallAt(wall, layer, lx, lz);
        if (!here) {
            return false;
        }
        boolean west = wallAt(wall, layer, lx - 1, lz);
        boolean east = wallAt(wall, layer, lx + 1, lz);
        boolean north = wallAt(wall, layer, lx, lz - 1);
        boolean south = wallAt(wall, layer, lx, lz + 1);
        // 1 格宽走廊：某一轴两侧都是墙、另一轴两侧都通 => 该格为走廊正中
        boolean centredAlongX = west && east && !north && !south;
        boolean centredAlongZ = north && south && !west && !east;
        if (centredAlongX || centredAlongZ) {
            return true;
        }
        // 房间 / 其它区域：3 格整齐网格
        return Math.floorMod(worldX, 3) == 0 && Math.floorMod(worldZ, 3) == 0;
    }

    /** 越界视为墙（走廊在区块边界处不会误判为正中）。 */
    private static boolean wallAt(boolean[][][] wall, int layer, int lx, int lz) {
        if (lx < 0 || lx > 15 || lz < 0 || lz > 15) {
            return true;
        }
        return wall[layer][lx][lz];
    }

    /**
     * 直接写所属区块段（不改高度图、不走回调）。
     *
     * <p>仅用于天花板之下的墙身：那里不参与高度图，光照由光照引擎稍后统一处理。
     * 越界或段不存在时安全跳过。</p>
     */
    private static void setSectionBlock(ChunkAccess chunk, int x15, int y, int z15,
                                       BlockState state) {
        int index = chunk.getSectionIndex(y);
        if (index < 0 || index >= chunk.getSectionsCount()) {
            return;
        }
        LevelChunkSection section = chunk.getSection(index);
        if (section == null) {
            return;
        }
        section.setBlockState(x15, y & 15, z15, state, false);
    }


    /**
     * 判定某一格是否放墙：迷宫网格 → 宽阔街道 → 矩形房间。
     */
    private static boolean resolveWall(boolean[][][] wall, int lx, int lz,
                                       int worldX, int worldZ, int layer) {
        boolean isWall = wall[layer][lx][lz];

        // 宽阔走廊地带（主干道 >16 格宽、>50 格长；两侧每 8 格一条 3 格宽支廊）
        if (BackroomsAvenue.isWideArea(worldX, worldZ, layer)) {
            isWall = false;
        }

        BackroomsRooms.Room room = BackroomsRooms.roomAt(worldX, worldZ, layer);
        if (room != null) {
            if (BackroomsRooms.isInterior(room, worldX, worldZ)) {
                // 房间内部整块清空
                isWall = false;
                // 大房间内的 1×1 柱子
                if (BackroomsRooms.isPillar(room, worldX, worldZ)) {
                    isWall = true;
                }
            } else if (BackroomsRooms.isDoorway(room, worldX, worldZ)) {
                // 四边中点各开一个 3 格宽门洞
                isWall = false;
            } else if (BackroomsRooms.isPerimeter(room, worldX, worldZ)) {
                // 房间边界强制为墙（门洞除外）
                isWall = true;
            }
        }
        return isWall;
    }

    /** 为每个方块列分配群系。 */
    private void populateBiomes(ChunkAccess chunk, RandomState randomState) {
        chunk.fillBiomesFromNoise(this.getBiomeSource(), randomState.sampler());
    }

    // ==================== 荧光灯 ====================

    // ==================== 其余 ChunkGenerator 抽象方法 ====================

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
                             RandomState random, ChunkAccess chunk) {
        // 地表已在 fillFromNoise 中构建
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // 不雕刻洞穴/峡谷
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // 无怪物生成
    }

    @Override
    public int getGenDepth() {
        return WORLD_HEIGHT;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type,
                             LevelHeightAccessor level, RandomState random) {
        return 0; // 地板位于 Y=0
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        int minY = height.getMinBuildHeight();
        int maxY = height.getMaxBuildHeight();
        BlockState[] column = new BlockState[maxY - minY];
        Arrays.fill(column, this.airState);
        column[0] = floorState;
        return new NoiseColumn(minY, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Backrooms Level 0  (" + LAYER_HEIGHT + " 格每层 / 生成 "
                + LEVEL_LAYERS + " 层)");
    }
}
