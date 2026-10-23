package com.the_backrooms_complex.structure;

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

    /** 允许的最高层索引：0..32，对应 Y = 0 .. 32*7+6 = 230。 */
    public static final int MAX_LAYER_INDEX = 31;

    /** 结构可占据的最高 Y 坐标。 */
    public static final int MAX_STRUCTURE_Y = MAX_LAYER_INDEX * LAYER_PERIOD + 6;

    /** 可用结构模板数量（level0_1 .. level0_5）。 */
    public static final int TEMPLATE_COUNT = 5;

    private static final long SALT_TEMPLATE = 0x9E3779B97F4A7C15L;
    private static final long SALT_LAYER = 0xC2B2AE3D27D4EB4FL;

    public Level0Structure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        final boolean DIAG = true;   // 临时诊断开关，排查完删掉
        if (DIAG) {
            System.out.println("[BC-DIAG] findGenerationPoint chunk=" + chunkPos.x + "," + chunkPos.z);
        }

        // 1) 反推本区块对应的 90×90 网格单元（与放置规则 Level0StructurePlacement 完全一致）
        int[] cell = Level0StructurePlacement.cellForChunk(chunkPos.x, chunkPos.z);
        if (cell == null) {
            if (DIAG) System.out.println("[BC-DIAG]   cell == null -> 不是锚点区块，跳过");
            return Optional.empty();
        }
        int gx = cell[0];
        int gz = cell[1];

        StructureTemplateManager templateManager = context.structureTemplateManager();

        // 2) 确定性选择模板（只依赖网格单元坐标，与逐层迷宫种子无关）
        int templateIndex = 1 + Math.floorMod(mix(gx, gz, 0, SALT_TEMPLATE), TEMPLATE_COUNT);
        ResourceLocation templateId = ResourceLocation.fromNamespaceAndPath(
                BackroomsComplex.MODID, "level0_" + templateIndex);
        StructureTemplate template = templateManager.getOrCreate(templateId);
        Vec3i size = template.getSize();
        if (DIAG) System.out.println("[BC-DIAG]   cell=" + gx + "," + gz + " template=level0_" + templateIndex + " size=" + size);
        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
            if (DIAG) System.out.println("[BC-DIAG]   模板为空/缺失！");
            return Optional.empty(); // 模板缺失或为空
        }

        // 3) 目标层：底部对齐 floorY = layerIndex * 7，且结构整体不超过 Y 上限
        int maxLayerByHeight = (MAX_STRUCTURE_Y + 1 - size.getY()) / LAYER_PERIOD;
        if (maxLayerByHeight < 0) {
            return Optional.empty(); // 结构过高，放不进允许范围
        }
        int maxLayer = Math.min(MAX_LAYER_INDEX, maxLayerByHeight);

        // 逐层独立判定：每层 5% 概率（52429 / 1048576 ≈ 5%），从最高层往下找第一个命中的层
        int layerIndex = -1;
        for (int l = maxLayer; l >= 0; l--) {
            if ((mix(gx, gz, l, SALT_LAYER) & 0xFFFFFL) < 52429L) {
                layerIndex = l;
                break;
            }
        }
        if (layerIndex < 0) {
            if (DIAG) System.out.println("[BC-DIAG]   所有层的 5% 判定都没命中（maxLayer=" + maxLayer + "）");
            return Optional.empty();
        }
        if (DIAG) System.out.println("[BC-DIAG]   命中层=" + layerIndex + " floorY=" + (layerIndex * LAYER_PERIOD));

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