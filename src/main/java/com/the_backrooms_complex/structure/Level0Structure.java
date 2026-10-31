package com.the_backrooms_complex.structure;

import com.the_backrooms_complex.worldgen.level0.BackroomsChunkGenerator;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * 后室 Level 0 的"特定结构"生成（替换原先的 3×3 跨层竖井）。
 *
 * <h2>放置规则</h2>
 * <ul>
 *   <li><b>网格</b>：水平面按 <b>90×90 格</b>划分为逻辑网格（由自定义
 *       {@link Level0StructurePlacement} 实现 —— 原版 {@code minecraft:random_spread}
 *       的 {@code spacing}/{@code separation} 单位是<b>区块</b>，无法精确表达 90 格），
 *       每个单元最多 1 个结构，即平均每 90 格一个；</li>
 *   <li><b>偏移</b>：锚点在单元内按坐标哈希确定性偏移 0..28 格，不会总落在网格正中心；
 *       相邻单元锚点最小间距 = 90 - 28 = <b>62 格</b>；</li>
 *   <li><b>层级对齐</b>：结构最低 Y 对齐目标层地板 {@code floorY = layerIndex * 7}；</li>
 *   <li><b>层范围</b>：仅在层索引 0..{@link #MAX_LAYER_INDEX}（Y 0..{@link #MAX_STRUCTURE_Y}）内生成，
 *       且结构整体不得超出该 Y 上限；</li>
 *   <li><b>确定性</b>：模板、层索引与锚点全部由网格单元坐标哈希决定，<b>与世界种子无关</b>，
 *       同一位置任何时刻结果一致。</li>
 * </ul>
 *
 * <h2>生成优先级（高于种子）</h2>
 * <p>走廊地形在 {@code fillFromNoise}（NOISE 阶段）按<b>逐层种子</b>生成，
 * 而结构在更晚的 FEATURES 阶段放置，并通过 {@link BedrockSafeProcessor} 强制覆盖，
 * 因此<b>结构的生成优先级高于逐层种子</b>：无论某一层的迷宫种子把该处生成为墙还是通道、
 * 地板还是天花板，结构内容都会照常覆盖上去。</p>
 * <p>此外，结构自身的<b>模板选择、层索引与锚点都不使用逐层种子</b>（只依赖网格单元坐标），
 * 所以逐层布局的差异不会影响结构的位置与内容。</p>
 *
 * <h2>覆盖规则</h2>
 * <p>放置时使用 {@link BedrockSafeProcessor}：世界中的 {@code minecraft:bedrock} 不被覆盖，
 * 其余方块（含后室模组自己的墙壁、踢脚线、天花板、地板、荧光灯，以及空气）一律强制覆盖。
 * 因此结构可以跨层，覆盖中间的天花板与地板，但永远不会穿过基岩。</p>
 */
public class Level0Structure extends Structure {

    public static final MapCodec<Level0Structure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(settingsCodec(instance)).apply(instance, Level0Structure::new));

    /** 每层周期（格），与 {@code BackroomsChunkGenerator.LAYER_HEIGHT} 一致。 */
    public static final int LAYER_PERIOD = 8;   // 必须等于 BackroomsChunkGenerator.LAYER_HEIGHT

    /**
     * 允许的最高层索引。
     *
     * <p>必须与生成器实际生成的层数一致：生成器只填 0..{@code LEVEL_LAYERS-1} 层，
     * 超出这个范围的层是空气 —— 之前写成 31，结构就会刷在 Y=248 的天空里。</p>
     */
    public static final int MAX_LAYER_INDEX = BackroomsChunkGenerator.LEVEL_LAYERS - 1;

    /**
     * 「马尼拉结构」的独立刷新率：<b>0.005%</b>（1 / 20000）。
     *
     * <p>用独立哈希判定，命中时整座结构改用 {@code level_manila} 模板，
     * 不再走普通模板的加权选择。</p>
     */
    public static final long MANILA_CHANCE_DENOM = 1333L;   // 0.075%（原 0.005% × 15）

    /**
     * 结构只在<b>最下面这几层</b>生成。
     *
     * <p>原来结构可以在 32 层里任意一层出现，玩家在底层活动时只有 1/32 的结构能被看到，
     * 这正是"结构很难找到"的原因。收窄到 4 层后，底层遇到结构的概率提升约 8 倍。</p>
     */
    public static final int LAYER_SPREAD = BackroomsChunkGenerator.LEVEL_LAYERS;   // 仅在实际存在的层内分配
    /** 马尼拉结构专用盐值。 */
    private static final long SALT_MANILA = 0x4D414E494C41L;

    /**
     * 结构可占据的最高 Y 坐标。
     *
     * <p>留两格余量，保证结构整体位于最上层天花板（{@code LEVEL_LAYERS * LAYER_PERIOD - 1}）
     * 之下，不会顶穿或悬空。</p>
     */
    public static final int MAX_STRUCTURE_Y =
            BackroomsChunkGenerator.LEVEL_LAYERS * LAYER_PERIOD - 2;

    /** 旧模板数量（level0_1..level0_5），权重 1。 */
    public static final int OLD_TEMPLATE_COUNT = 5;
    /** 新模板数量（level0_6..level0_9），权重 2。 */
    public static final int NEW_TEMPLATE_COUNT = 4;
    /** 加权总数 = 5×1 + 4×2 = 13。 */
    public static final int TEMPLATE_WEIGHT_TOTAL = OLD_TEMPLATE_COUNT + NEW_TEMPLATE_COUNT * 2;

    private static final long SALT_TEMPLATE = 0x9E3779B97F4A7C15L;
    private static final long SALT_LAYER = 0xC2B2AE3D27D4EB4FL;

    public Level0Structure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        // 诊断已关闭

        // 1) 反推本区块对应的 90×90 网格单元（与放置规则 Level0StructurePlacement 完全一致）
        int[] cell = Level0StructurePlacement.cellForChunk(chunkPos.x, chunkPos.z);
        if (cell == null) {
            return Optional.empty();
        }
        int gx = cell[0];
        int gz = cell[1];

        StructureTemplateManager templateManager = context.structureTemplateManager();

        // 2) 确定性选择模板（只依赖网格单元坐标，与逐层迷宫种子无关）
        // 先判定极低概率的「马尼拉结构」（0.005%）：命中则整座结构都用 level_manila
        boolean manila = Math.floorMod(mix(gx, gz, 0, SALT_MANILA), MANILA_CHANCE_DENOM) == 0L;
        ResourceLocation templateId;
        if (manila) {
            templateId = ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "level_manila");
        } else {
            // 加权选择：0~4 -> 旧模板 1..5（权重 1）；5~12 -> 新模板 6..9（权重 2）
            int roll = (int) Math.floorMod(mix(gx, gz, 0, SALT_TEMPLATE), TEMPLATE_WEIGHT_TOTAL);
            int templateIndex;
            if (roll < OLD_TEMPLATE_COUNT) {
                templateIndex = 1 + roll;
            } else {
                templateIndex = OLD_TEMPLATE_COUNT + 1 + (roll - OLD_TEMPLATE_COUNT) / 2;
            }
            templateId = ResourceLocation.fromNamespaceAndPath(
                    BackroomsComplex.MODID, "level0_" + templateIndex);
        }
        StructureTemplate template = templateManager.getOrCreate(templateId);
        Vec3i size = template.getSize();
        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
            return Optional.empty(); // 模板缺失或为空
        }

        // 3) 目标层：底部对齐 floorY = layerIndex * 7，且结构整体不超过 Y 上限
        int maxLayerByHeight = (MAX_STRUCTURE_Y + 1 - size.getY()) / LAYER_PERIOD;
        if (maxLayerByHeight < 0) {
            return Optional.empty(); // 结构过高，放不进允许范围
        }
        int maxLayer = Math.min(MAX_LAYER_INDEX, maxLayerByHeight);

        // 层选择：从<b>最高层往下</b>找第一个命中的层。
        //
        // 关键修正：原来 32 层都能被选中，导致结构有 31/32 的概率生成在玩家头顶上方，
        // 玩家在底层走廊里几乎看不到 —— 这就是"结构很难找到"的真正原因。
        // 现在把可生成的层收窄到<b>最下面 4 层</b>（LAYER_SPREAD = 4），
        // 使结构集中在玩家主要活动的底层区域，可遇到率提升约 8 倍。
        // 每个网格单元必定分配到一个结构；具体落在哪一层由单元哈希决定。
        // 这样在足够大的范围内，每一层都能分配到结构，且不同单元的层互不相同。
        int topLayer = Math.min(maxLayer, LAYER_SPREAD - 1);
        int layerIndex = -1;
        int spread = topLayer + 1;
        if (spread > 0) {
            layerIndex = (int) Math.floorMod(mix(gx, gz, 0, SALT_LAYER), spread);
        }
        if (false) {
            for (int l = topLayer; l >= 0; l--) {
                layerIndex = l;
                break;
            }
        }
        if (layerIndex < 0) {
            return Optional.empty();
        }

        // 群系过滤由 has_structure/level0 标签完成（只含 yellow_rooms）


        int floorY = layerIndex * LAYER_PERIOD;

        // 4) 锚点：完全由网格单元坐标决定（与逐层迷宫种子、世界种子均无关）
        int centerX = Level0StructurePlacement.anchorX(gx, gz);
        int centerZ = Level0StructurePlacement.anchorZ(gx, gz);
        // 关键：把结构"原点"也吸附到迷宫的 8 格单元格网格上。
        // 模板尺寸是 7/5/12，若直接用 中心-半宽 会落在奇数偏移上，
        // 导致结构自带的墙壁/地板与迷宫墙体错位（家具插进墙里）。
        int snap = Level0StructurePlacement.GRID_SNAP;
        int originX = Math.floorDiv(centerX - size.getX() / 2, snap) * snap;
        int originZ = Math.floorDiv(centerZ - size.getZ() / 2, snap) * snap;
        BlockPos origin = new BlockPos(originX, floorY, originZ);

        StructurePlaceSettings settings = Level0StructurePiece.defaultSettings();
        return Optional.of(new Structure.GenerationStub(origin, (StructurePiecesBuilder builder) ->
                builder.addPiece(new Level0StructurePiece(templateManager, templateId, settings, origin))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.LEVEL0_STRUCTURE.get();
    }

    /** 坐标 + 盐值的确定性混合（splitmix64 风格）。 */
    private static long mix(int a, int b, int c, long salt) {
        long h = (long) a * 0x9E3779B97F4A7C15L
                ^ (long) b * 0xC2B2AE3D27D4EB4FL
                ^ (long) c * 0x165667B19E3779F9L
                ^ salt;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }
}