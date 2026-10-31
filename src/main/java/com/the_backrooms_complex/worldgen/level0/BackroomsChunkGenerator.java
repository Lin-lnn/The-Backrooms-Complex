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
    /** 荧光灯间隔（格）。 */
    private static final int LAMP_SPACING = 5;

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
    }

    /** 生成一整列（从 {@code minY} 到 {@code maxY}）。 */
    private void fillColumn(ChunkAccess chunk, BlockPos.MutableBlockPos mutable,
                            boolean[][][] wall, int worldX, int worldZ,
                            int lx, int lz, int minY, int maxY) {
        boolean colIsLamp = isLamp(worldX, worldZ);
        int layers = fullLayers(chunk);

        // 墙体判定与层无关的循环变量，每列每层只算一次（原来每个方块算一次，且每次都分配 Room）
        boolean[] colWall = new boolean[layers];
        for (int l = 0; l < layers; l++) {
            colWall[l] = resolveWall(wall, lx, lz, worldX, worldZ, l);
        }

        for (int y = minY; y < maxY; y++) {
            int layer = y / LAYER_HEIGHT;
            int offset = y - layer * LAYER_HEIGHT;

            if (layer >= layers) {
                // 顶部残层（Y >= 252）：用天花板封顶，闭合维度
                chunk.setBlockState(mutable.set(worldX, y, worldZ), ceilingState, false);
                continue;
            }

            boolean isWall = colWall[layer];

            if (offset == 0) {
                chunk.setBlockState(mutable.set(worldX, y, worldZ), floorState, false);
            } else if (offset <= LAYER_HEIGHT - 2) {
                // 墙体状态在这里一次性取好，避免每个方块都做一次三元判断
                if (isWall) {
                    chunk.setBlockState(mutable.set(worldX, y, worldZ),
                            (offset == 1) ? this.yellowPlinth : this.yellowWall, false);
                }
                // 非墙 = 空气：区块从全空气开始，这里<b>不写入</b>。
                // 每层有 6 格墙体空间，其中大部分是空气，跳过可省掉约 2/3 的方块写入。
            } else {
                chunk.setBlockState(mutable.set(worldX, y, worldZ),
                        colIsLamp ? lampState : ceilingState, false);
            }
        }
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

    /** 荧光灯：每 {@link #LAMP_SPACING} 格一个。 */
    private static boolean isLamp(int worldX, int worldZ) {
        return Math.floorMod(worldX, LAMP_SPACING) == 2
                && Math.floorMod(worldZ, LAMP_SPACING) == 2;
    }

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
