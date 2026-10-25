package com.the_backrooms_complex.worldgen;

import com.the_backrooms_complex.worldgen.level0.BackroomsAvenue;
import com.the_backrooms_complex.worldgen.level0.BackroomsMaze;
import com.the_backrooms_complex.worldgen.level0.BackroomsRooms;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.the_backrooms_complex.ModBiomes;
import com.the_backrooms_complex.ModBalance;
import com.the_backrooms_complex.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 后室 Level 0 自定义区块生成器。
 *
 * <h2>多层结构（每层周期 7 格）</h2>
 * <pre>
 * Y 偏移   内容
 * +0       地板（floor_tile，整层满铺）
 * +1       踢脚线（wall_*_plinth，仅墙壁列） / 通道空气
 * +2..+5   墙壁主体（wall_*，仅墙壁列） / 通道空气
 * +6       天花板（ceiling_tile，整层满铺；间隔位置替换为荧光灯）
 * </pre>
 * 每层 = 地板 1 + 墙壁 5 + 天花板 1 = 7 格；净空 5 格（+1..+5）。
 * 从 Y=0 向上堆叠 36 层，顶部残层（Y≥252）用天花板封顶。
 *
 * <h2>走廊布局（可变宽度 1~7 格）</h2>
 * <p>由 {@link BackroomsMaze} 计算：逻辑单元格间距 8 格，先生成迷宫拓扑（整个平面上的随机
 * 生成树），再按概率分布给每条走廊段分配宽度（1→1%、2→1.5%、3→10%、4→75%、5→10%、
 * 6→1.5%、7→1%），最后按宽度铺设通道。连通性在逻辑单元格层面用并查集复核。</p>
 * <p>本区块的 16×16 布局由 {@link BackroomsMaze#generateWallGrid(int, int)} 独立计算，
 * 结果只依赖区块 XZ 坐标，因此任何时刻生成结果一致。</p>
 *
 * <h2>3×3 竖井</h2>
 * <p>由 {@link BackroomsMaze#shaftMask(int, int)} 计算。竖井 3×3 范围内：</p>
 * <ul>
 *   <li>Y+1..Y+5 的墙壁<b>全部跳过</b>（上下层对应位置均不生成墙壁）；</li>
 *   <li>地板与天花板在 3×3 范围内打通（仅最底层地板保留，避免露出虚空）；</li>
 *   <li>荧光灯不生成（该处无天花板）。</li>
 * </ul>
 * <p>竖井锚点平均每 120 个区块 1 个（约每 175×175 格 1 个），满足"每 200×200 格至少 1 个"。</p>
 *
 * <h2>确定性</h2>
 * <p>所有随机性均由坐标哈希决定，不依赖世界种子与区块加载顺序，完全可复现。</p>
 *
 * <p><b>本模组不生成任何战利品箱、物资或战利品表。</b></p>
 */
public class BackroomsChunkGenerator extends ChunkGenerator {

    // ==================== 常量 ====================
    /** 每层周期高度（格）：地板 1 + 墙壁 5 + 天花板 1。 */
    public static final int LAYER_HEIGHT = 8;
    /**
     * 实际生成的层数。
     *
     * <p>世界高度是 256，但没必要都填满 —— 只生成 {@value #LEVEL_LAYERS} 层就够玩家活动，
     * 剩下的空气层不参与逐方块放置，可显著降低区块生成开销。</p>
     */
    public static final int LEVEL_LAYERS = 5;
    /** 世界高度。 */
    private static final int WORLD_HEIGHT = 256;
    /** 世界最小 Y。 */
    private static final int MIN_Y = 0;
    /** 完整层数（0..35），第 36 层为顶部 4 格残层。 */
    /**
     * 实际可用层数 —— <b>按维度高度动态计算</b>。
     *
     * <p>原来写死 32 层，而 Level 1 的高度是 288（36 层）⇒ 访问第 33 层时越界，
     * 区块生成抛异常，玩家看到的就是虚空。现在改为 {@code getGenDepth() / LAYER_HEIGHT}，
     * 任何高度的维度都能正确工作。</p>
     */
    /**
     * 当前区块实际可用的层数。
     *
     * <p><b>故意不用 {@link #getGenDepth()}</b>：那个方法返回的是本类硬编码的
     * {@link #WORLD_HEIGHT}（256），与维度 JSON 里的真实高度无关。
     * Level 1 的高度是 288（36 层），如果按 256 算就只有 32 层 ⇒ 第 33 层越界。
     * 这里改为读取<b>区块自身</b>的高度，任何维度都能正确工作。</p>
     */
    private static int fullLayers(net.minecraft.world.level.LevelHeightAccessor heightAccessor) {
        return Math.min(LEVEL_LAYERS, Math.max(1, heightAccessor.getHeight() / LAYER_HEIGHT));
    }

    /** 荧光灯间隔（格）。 */
    private static final int LAMP_SPACING = 5;

    // ==================== 编解码器 ====================
    /** 静态 CODEC，供维度 JSON 反序列化（"type": "backrooms_complex:backrooms"）。 */
    public static final MapCodec<BackroomsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BackroomsChunkGenerator::getBiomeSource)
            ).apply(instance, BackroomsChunkGenerator::new));

    // ==================== 字段 ====================
    private final BackroomsBiomeSource backroomsBiomeSource;

    // 缓存的方块状态（避免每格重复分配）
    private final BlockState floorState;
    private final BlockState ceilingState;
    private final BlockState lampState;
    private final BlockState airState = Blocks.AIR.defaultBlockState();

    private final BlockState yellowWall;
    private final BlockState yellowPlinth;
    private final BlockState concreteWall;
    private final BlockState concretePlinth;
    private final BlockState dampWall;
    private final BlockState dampPlinth;

    // ===== 混凝土大厅群系专用方块状态 =====
    private final BlockState strippedCarpetFloor;
    private final BlockState ruinedCeiling;
    private final BlockState ruinedVent;
    private final BlockState crackedWall;
    private final BlockState warningWall;
    private final BlockState myceliumCarpetState;
    private final BlockState dampFloor;
    private final BlockState dampCeiling;

    public BackroomsChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
        // 本模组的维度 JSON 始终使用 BackroomsBiomeSource，因此此处强制转换安全
        this.backroomsBiomeSource = (BackroomsBiomeSource) biomeSource;

        this.floorState = ModBlocks.FLOOR_TILE.get().defaultBlockState();
        this.ceilingState = ModBlocks.CEILING_TILE.get().defaultBlockState();
        this.lampState = ModBlocks.FLUORESCENT_LAMP.get().defaultBlockState();
        this.yellowWall = ModBlocks.WALL_YELLOW.get().defaultBlockState();
        this.yellowPlinth = ModBlocks.WALL_YELLOW_PLINTH.get().defaultBlockState();
        this.concreteWall = ModBlocks.WALL_CONCRETE.get().defaultBlockState();
        this.concretePlinth = ModBlocks.WALL_CONCRETE_PLINTH.get().defaultBlockState();
        this.dampWall = ModBlocks.WALL_DAMP.get().defaultBlockState();
        this.dampPlinth = ModBlocks.WALL_DAMP_PLINTH.get().defaultBlockState();
        this.strippedCarpetFloor = ModBlocks.STRIPPED_CARPET_FLOOR.get().defaultBlockState();
        this.ruinedCeiling = ModBlocks.RUINED_CEILING.get().defaultBlockState();
        this.ruinedVent = ModBlocks.RUINED_VENT.get().defaultBlockState();
        this.crackedWall = ModBlocks.CRACKED_CONCRETE_WALL.get().defaultBlockState();
        this.warningWall = ModBlocks.WARNING_CONCRETE_WALL.get().defaultBlockState();
        this.dampFloor = ModBlocks.DAMP_FLOOR.get().defaultBlockState();
        this.myceliumCarpetState = ModBlocks.MYCELIUM_CARPET.get().defaultBlockState();
        this.dampCeiling = ModBlocks.DAMP_CEILING.get().defaultBlockState();
    }

    // ==================== ChunkGenerator 核心：填充方块与群系 ====================

    /**
     * 填充噪声阶段（对应规格中的 populateNoise + populateBiomes）。
     */
    /** 供客户端染色器使用：直接按 (x, z, 层) 查群系，与生成器完全一致。 */
    public BackroomsBiomeSource biomeSource() {
        return this.backroomsBiomeSource;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        this.populateNoise(chunk);
        this.populateBiomes(chunk, randomState);
        return CompletableFuture.completedFuture(chunk);
    }

    /**
     * 生成地牢结构方块：地板、踢脚线、墙壁、天花板、荧光灯，以及 3×3 竖井。
     * 不生成任何战利品箱或物资。
     */
    private void populateNoise(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        int minY = chunk.getMinBuildHeight();
        // 只生成前 LEVEL_LAYERS 层，其余留空（降低算力）
        int maxY = Math.min(chunk.getMaxBuildHeight(), LEVEL_LAYERS * LAYER_HEIGHT);

        // 1. 走廊布局：逐层独立种子（每层一套完全不同的布局），只依赖区块坐标 + 层索引
        boolean[][][] wall = BackroomsMaze.generateWallGrids(chunkX, chunkZ, fullLayers(chunk));

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        // 2. 逐列生成
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int worldX = chunkX * 16 + lx;
                int worldZ = chunkZ * 16 + lz;
                // 列级常量：只依赖 (worldX, worldZ)，提到 y 循环外避免每个方块重算
                final boolean colInHallRoom = inHallRoom(worldX, worldZ);
                final boolean colOnGiantPillar = onGiantPillar(worldX, worldZ);
                final boolean colIsLamp = isLamp(worldX, worldZ);


                for (int y = minY; y < maxY; y++) {
                    int lh = LAYER_HEIGHT;
                    int layer = y / lh;
                    int offset = y - layer * lh;   // 层内偏移
                // 三维群系：本层的群系决定本层的墙壁花纹 / 地板 / 天花板 / 通风口
                // biome = 未抖动的原始群系；踢脚线用它，避免墙脚出现黄/黑斑块
                Holder<Biome> biome = backroomsBiomeSource.biomeAt(worldX, worldZ, layer);
                boolean hall = backroomsBiomeSource.isConcreteHalls(biome);
                boolean damp = backroomsBiomeSource.isDampCorridors(biome);
                BlockState plinthState = plinthForBiome(biome);   // 踢脚线也参与渐变
                BlockState columnFloor = hall ? strippedCarpetFloor : (damp ? dampFloor : floorState);
                BlockState columnCeiling = hall ? ruinedCeiling : (damp ? dampCeiling : ceilingState);
                BlockState columnLamp = hall ? ruinedVent : lampState;
                // 大厅房间（较大空地）与独立柱子（1x1，不贴墙）
                boolean inHall = hall && colInHallRoom;
                // 先判断层号是否在有效范围内，再访问 wall[layer]
                boolean layerValid = layer < fullLayers(chunk);
                boolean pillar = layerValid && hall && !inHall && colOnGiantPillar
                        // 柱子四邻必须都是通道，避免出现"贴着墙角的突出柱状拐角"
                        && lx > 0 && lx < 15 && lz > 0 && lz < 15
                        && !wall[layer][lx - 1][lz] && !wall[layer][lx + 1][lz]
                        && !wall[layer][lx][lz - 1] && !wall[layer][lx][lz + 1];

                    if (!layerValid) {
                        // 顶部残层（Y >= 252）：用天花板封顶，闭合维度
                        chunk.setBlockState(mutable.set(worldX, y, worldZ), ceilingState, false);
                        continue;
                    }

                    // 本层自己的布局（逐层种子不同）
                    boolean isWall = wall[layer][lx][lz];

                    // 大厅房间：整块清空（大厅状空地）
                    // 独立柱子：在没有墙的通道列上立起 1x1 柱
                    if (inHall) {
                        isWall = false;
                    } else if (pillar && !isWall) {
                        isWall = true;
                    }

                    // ===== 房间（长宽 11~49 的矩形空间，由走廊相连）=====
                    // 宽阔走廊地带（主干道 >16 格宽、>50 格长；两侧每 8 格一条 3 格宽支廊）
                    if (!inHall && BackroomsAvenue.isWideArea(worldX, worldZ, layer)) {
                        isWall = false;
                    }

                    BackroomsRooms.Room room = BackroomsRooms.roomAt(worldX, worldZ, layer, hall);
                    if (room != null) {
                        if (BackroomsRooms.isInterior(room, worldX, worldZ)) {
                            // 房间内部整块清空
                            isWall = false;
                            // 大房间内的 1x1 柱子：用同样的踢脚线 + 墙壁格式
                            if (BackroomsRooms.isPillar(room, worldX, worldZ)) {
                                isWall = true;
                            }
                        } else if (BackroomsRooms.isDoorway(room, worldX, worldZ)) {
                            // 四边中点各开一个 3 格宽门洞
                            isWall = false;
                    } else if (BackroomsRooms.isPerimeter(room, worldX, worldZ)) {
                        // 房间边界强制为墙（门洞除外）=> 房间内部成为规整矩形
                        isWall = true;
                    }
                    }

                    if (offset == 0) {
                        // 地板：整层满铺（层间不再有竖井连接）
                        chunk.setBlockState(mutable.set(worldX, y, worldZ), columnFloor, false);
                    } else if (offset <= lh - 2) {
                        // 墙壁区（+1..+5）：墙壁列放踢脚线/墙，通道列留空气
                        if (isWall) {
                            // offset == 1 使用踢脚线方块（四边凸出裙边），其余为普通墙壁
                            BlockState wallState = wallForBiome(biome, worldX, y, worldZ);
                            BlockState state = (offset == 1) ? plinthState : wallState;
                            chunk.setBlockState(mutable.set(worldX, y, worldZ), state, false);
                        } else {
                    chunk.setBlockState(mutable.set(worldX, y, worldZ), airState, false);
                    // ===== 混凝土大厅的地物：紧贴地面的熄灭篝火 / 蜘蛛网 =====
                    if (hall && offset == 1) {
                        long decorSeed = (long) worldX * 341873128712L
                                + (long) worldZ * 132897987541L
                                + (long) layer * 9187L;
                        double r = net.minecraft.util.RandomSource.create(decorSeed).nextDouble();
                        if (r < ModBalance.DECOR_COBWEB_CHANCE) {   // 蜘蛛网
                            // 0.01% 蜘蛛网
                            chunk.setBlockState(mutable.set(worldX, y, worldZ),
                                    net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState(), false);
                        } else if (r < ModBalance.DECOR_GRAVEL_CHANCE) {
                            // 0.003% 贴墙的燧石堆：原版没有燧石方块，用"砂砾"表现（燧石即从砂砾中获得）
                            net.minecraft.world.level.block.state.BlockState wallProbe =
                                    chunk.getBlockState(mutable.set(worldX + 1, y, worldZ));
                            boolean nextToWall = chunk.getBlockState(mutable.set(worldX + 1, y, worldZ)).isSolidRender(null, null)
                                    || chunk.getBlockState(mutable.set(worldX - 1, y, worldZ)).isSolidRender(null, null)
                                    || chunk.getBlockState(mutable.set(worldX, y, worldZ + 1)).isSolidRender(null, null)
                                    || chunk.getBlockState(mutable.set(worldX, y, worldZ - 1)).isSolidRender(null, null);
                            if (nextToWall) {
                                net.minecraft.world.level.block.state.BlockState pile =
                                        net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState();
                                chunk.setBlockState(mutable.set(worldX, y, worldZ), pile, false);
                                // 第二块堆在斜上方，形成"自然堆积"感
                                if (r < 0.00001D) {
                                    chunk.setBlockState(mutable.set(worldX, y + 1, worldZ), pile, false);
                                }
                            }
                        } else if (r < ModBalance.DECOR_CAMPFIRE_CHANCE) {
                            // 0.005% 熄灭的篝火
                            chunk.setBlockState(mutable.set(worldX, y, worldZ),
                                    net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState()
                                            .setValue(net.minecraft.world.level.block.CampfireBlock.LIT, Boolean.FALSE),
                                    false);
                        }
                    }
                        }
                        // ===== 潮湿房间地板的菌毯：平滑噪声场，形成不规则的自然斑块 =====
                        if (damp) {
                            double n = carpetNoise(worldX, worldZ, layer);
                            // 阈值 0.67：约 10% 的地板被覆盖，斑块更小更分散
                            if (n > 0.67D
                                    && chunk.getBlockState(mutable.set(worldX, y - 1, worldZ)) == dampFloor
                                    && chunk.getBlockState(mutable.set(worldX, y, worldZ)).isAir()) {
                                chunk.setBlockState(mutable.set(worldX, y, worldZ),
                                        this.myceliumCarpetState, false);
                            }
                        }
                    } else {
                        // offset == 6：天花板（间隔处放荧光灯）
                        if (colIsLamp) {
                            chunk.setBlockState(mutable.set(worldX, y, worldZ), columnLamp, false);
                        } else {
                            chunk.setBlockState(mutable.set(worldX, y, worldZ), columnCeiling, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * 混凝土墙壁的差分变种：破败(10%) / 警示(1%)。
     *
     * <p>用 {@code (x + 2z) mod 10 == 0} 与 {@code mod 100 == 5} 判定，
     * 相邻列（相差 1 或 2）不可能同时命中，因此<b>同种变种永不相邻</b>，同时概率精确。</p>
     */
    private BlockState wallForBiome(Holder<Biome> biome, int x, int y, int z) {
        if (backroomsBiomeSource.isConcreteHalls(biome)) {
            // 三轴都参与：相邻方块（x±1 / z±1 / y±1）的 k 值分别相差 1 / 2 / 3，
            // 不可能同时命中同一余数 => 同种变种在三维上永不相邻。
            // 加入 y 之后变种以"单个方块"散布，而不是整条 5 格高的竖柱。
            int k = Math.floorMod(x + 2 * z + 3 * y, 10000);
            // 警示墙只出现在"每层地板与天花板之间的 Y 轴中点"（offset == 3）；仍在混凝土大厅、概率仍为 0.01%
            int lh2 = this.getGenDepth() >= 288 ? 18 : LAYER_HEIGHT;
            int offsetInLayer = y - Math.floorDiv(y, lh2) * lh2;
            if (k == 5 && offsetInLayer == 3) {
                return warningWall;
            }
            if (k % 10 == 0) {
                return crackedWall;
            }
        }
        return wallForBiome(biome);
    }

    /** 大厅房间：每 32 格一个单元，命中则清出 11x11 的空地。 */
    private static boolean inHallRoom(int x, int z) {
        int cx = Math.floorDiv(x, 32);
        int cz = Math.floorDiv(z, 32);
        if (!hallHash(cx, cz)) {
            return false;
        }
        int ox = Math.floorMod(x, 32) - 16;
        int oz = Math.floorMod(z, 32) - 16;
        return Math.abs(ox) <= 5 && Math.abs(oz) <= 5;
    }

    /**
     * 独立柱子 / 巨柱判定：每 48 格一个网格，柱体占 {@code 22..25}（4×4）。
     */
    private static boolean onGiantPillar(int x, int z) {
        // 48 格网格，每格中心放一根 4x4 柱
        int cx = Math.floorDiv(x, 48);
        int cz = Math.floorDiv(z, 48);
        int ox = Math.floorMod(x, 48);
        int oz = Math.floorMod(z, 48);
        // 柱心在 (22..25, 22..25) => 4x4
        if (ox < 22 || ox > 25 || oz < 22 || oz > 25) {
            return false;
        }
        return true;   // 每个 48 格网格都有一根巨柱（停车楼式排列）
    }

    private static boolean hallHash(int cx, int cz) {
        long h = (long) cx * 0x9E3779B97F4A7C15L ^ (long) cz * 0xC2B2AE3D27D4EB4FL ^ 0x5DEECE66DL;
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL; h ^= h >>> 33;
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.55;
    }

/**
     * 为每个方块列分配群系。
     *
     * <p>1.21.1 中 {@link ChunkAccess} 没有公开的 {@code setBiome} 方法，群系填充应使用
     * {@link ChunkAccess#fillBiomesFromNoise(net.minecraft.world.level.biome.BiomeResolver,
     * net.minecraft.world.level.biome.Climate.Sampler)}。本群系源实现了 {@code BiomeResolver}，
     * 其 {@code getNoiseBiome} 忽略采样器、只按 XZ 坐标确定性返回。</p>
     */
    private void populateBiomes(ChunkAccess chunk, RandomState randomState) {
        chunk.fillBiomesFromNoise(this.getBiomeSource(), randomState.sampler());
    }

    // ==================== 荧光灯 ====================

    /** 荧光灯：每 {@link #LAMP_SPACING} 格一个（间隔 4~6 格）。 */
    private boolean isLamp(int worldX, int worldZ) {
        return Math.floorMod(worldX, LAMP_SPACING) == 2 && Math.floorMod(worldZ, LAMP_SPACING) == 2;
    }

    // ==================== 群系路径按层缓存 ====================

// ==================== 群系 → 方块映射 ====================

    private BlockState wallForBiome(Holder<Biome> biome) {
        ResourceLocation key = biomeKey(biome);
        String path = key.getPath();
        // ---- Level 0 ----
        if (ModBiomes.CONCRETE_HALLS.location().equals(key)) return concreteWall;
        if (ModBiomes.DAMP_CORRIDORS.location().equals(key)) return dampWall;
        return yellowWall;
    }

    private BlockState plinthForBiome(Holder<Biome> biome) {
        ResourceLocation key = biomeKey(biome);
        String path = key.getPath();
        if (ModBiomes.CONCRETE_HALLS.location().equals(key)) return concretePlinth;
        if (ModBiomes.DAMP_CORRIDORS.location().equals(key)) return dampPlinth;
        return yellowPlinth;
    }

    private ResourceLocation biomeKey(Holder<Biome> biome) {
        return biome.unwrapKey().map(ResourceKey::location).orElse(ModBiomes.YELLOW_ROOMS.location());
    }

    // ==================== 其余 ChunkGenerator 抽象方法 ====================

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /**
     * 菌毯分布的平滑噪声场。
     *
     * <p>做法：把世界坐标按 {@value #CARPET_CELL} 格划分成网格，网格顶点处用一个
     * 确定性哈希生成 0~1 的值，再在格子内做<b>平滑双线性插值</b>（smoothstep）。
     * 这样得到的等高线是自然的不规则团块，而不是方块状的矩形。</p>
     *
     * @param x     世界 X
     * @param z     世界 Z
     * @param layer 层索引（不同层的斑块形状不同）
     * @return 0~1 的噪声值
     */
    /** 菌毯噪声网格边长（格）；越小斑块越碎、越分散。 */
    private static final double CARPET_CELL = 4.5D;

    private static double carpetNoise(int x, int z, int layer) {
        double fx = (double) x / CARPET_CELL;
        double fz = (double) z / CARPET_CELL;
        int x0 = (int) Math.floor(fx);
        int z0 = (int) Math.floor(fz);
        double tx = smoothStep(fx - x0);
        double tz = smoothStep(fz - z0);

        double v00 = carpetHash(x0, z0, layer);
        double v10 = carpetHash(x0 + 1, z0, layer);
        double v01 = carpetHash(x0, z0 + 1, layer);
        double v11 = carpetHash(x0 + 1, z0 + 1, layer);

        double a = v00 + (v10 - v00) * tx;
        double b = v01 + (v11 - v01) * tx;
        return a + (b - a) * tz;
    }

    /** smoothstep 缓动：让插值在格点处导数为 0，边界更圆润。 */
    private static double smoothStep(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    /** 网格顶点确定性哈希（0~1）。 */
    private static double carpetHash(int gx, int gz, int layer) {
        long h = (long) gx * 0x9E3779B97F4A7C15L
                + (long) gz * 0xC2B2AE3D27D4EB4FL
                + (long) layer * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) / (double) (1L << 53);
    }
    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
                             RandomState random, ChunkAccess chunk) {
        // 地表已在 fillFromNoise 中构建，无需额外处理
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
        return 0; // 基高为 0（地板位于 Y=0）
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        int minY = height.getMinBuildHeight();
        int maxY = height.getMaxBuildHeight();
        BlockState[] column = new BlockState[maxY - minY];
        Arrays.fill(column, this.airState);
        column[0] = floorState; // Y=0 处放一层地板，其余为空气
        return new NoiseColumn(minY, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        // 读客户端当前世界的真实高度（Level 0 世界高 256，实际只生成 LEVEL_LAYERS 层）
        int height = this.getGenDepth();
        try {
            var cl = net.minecraft.client.Minecraft.getInstance().level;
            if (cl != null) {
                height = cl.getHeight();
            }
        } catch (Throwable ignored) {
            // 专用服务器上拿不到客户端世界，退回 getGenDepth()
        }
        info.add("Backrooms Level 0  (" + LAYER_HEIGHT + " 格每层 / 生成 "
                + LEVEL_LAYERS + " 层 / 世界高 " + height + ")");
    }
}