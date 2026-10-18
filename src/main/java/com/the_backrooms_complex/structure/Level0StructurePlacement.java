package com.the_backrooms_complex.structure;

import com.mojang.datafixers.Products.P2;
import com.mojang.datafixers.Products.P5;
import com.mojang.datafixers.Products.P7;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import com.the_backrooms_complex.ModStructures;
import com.the_backrooms_complex.worldgen.BackroomsMaze;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

/**
 * 「特定结构」的自定义放置规则：以 <b>方块</b>为单位的网格（默认 {@link #DEFAULT_SPACING_BLOCKS} 格）。
 *
 * <p>为什么不用原版 {@code minecraft:random_spread}：它的 {@code spacing} / {@code separation}
 * 单位是<b>区块</b>，只能表达 16 的整数倍，无法精确表达 90 格。本类直接以方块坐标划分网格。</p>
 *
 * <h2>规则</h2>
 * <ul>
 *   <li>水平面按 {@code spacingBlocks × spacingBlocks}（默认 90×90）划分逻辑网格，
 *       <b>每个单元最多生成 1 个结构</b>；</li>
 *   <li>单元内的锚点由 (gx, gz) 哈希确定性偏移 {@code 0..offsetRange}（默认 0..28），
 *       因此不会总落在网格正中心；</li>
 *   <li>相邻单元锚点最小间距 = {@code spacingBlocks - offsetRange} = 90 - 28 = <b>62 格</b>
 *       （远大于区块宽度 16，故每个单元恰好对应唯一一个"放置区块"）；</li>
 *   <li>只依赖坐标哈希，<b>与世界种子无关</b>，同一位置结果恒定。</li>
 * </ul>
 */
public class Level0StructurePlacement extends StructurePlacement {

    /** 默认网格边长（格）。 */
    public static final int DEFAULT_SPACING_BLOCKS = 90;

    /** 默认单元内确定性偏移上界（格）。 */
    public static final int DEFAULT_OFFSET_RANGE = 28;

    public static final MapCodec<Level0StructurePlacement> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codec(instance).apply(instance, Level0StructurePlacement::new));

    /**
     * 组合"公共放置字段（locate_offset / frequency / salt / exclusion_zone）"与两个自定义字段。
     * 写法与原版 {@code ConcentricRingsStructurePlacement} 一致：公共部分来自
     * {@link StructurePlacement#placementCodec}，额外字段单独用 {@code instance.group(...)}，
     * 最后手工拼成 {@code P7}。
     */
    private static P7<Mu<Level0StructurePlacement>, Vec3i, StructurePlacement.FrequencyReductionMethod,
            Float, Integer, Optional<StructurePlacement.ExclusionZone>, Integer, Integer> codec(
            RecordCodecBuilder.Instance<Level0StructurePlacement> instance) {
        P5<Mu<Level0StructurePlacement>, Vec3i, StructurePlacement.FrequencyReductionMethod,
                Float, Integer, Optional<StructurePlacement.ExclusionZone>> p5 = placementCodec(instance);
        P2<Mu<Level0StructurePlacement>, Integer, Integer> p2 = instance.group(
                Codec.intRange(16, 4096).fieldOf("spacing_blocks")
                        .forGetter(Level0StructurePlacement::spacingBlocks),
                Codec.intRange(0, 512).optionalFieldOf("offset_range", DEFAULT_OFFSET_RANGE)
                        .forGetter(Level0StructurePlacement::offsetRange)
        );
        return new P7<>(p5.t1(), p5.t2(), p5.t3(), p5.t4(), p5.t5(), p2.t1(), p2.t2());
    }

    private static final long SALT_ANCHOR_X = 0xDB4F0B9175AE2165L;
    private static final long SALT_ANCHOR_Z = 0xBBE0563303A4615FL;

    private final int spacingBlocks;
    private final int offsetRange;

    public Level0StructurePlacement(Vec3i locateOffset, StructurePlacement.FrequencyReductionMethod method,
                                    float frequency, int salt,
                                    Optional<StructurePlacement.ExclusionZone> exclusionZone,
                                    int spacingBlocks, int offsetRange) {
        super(locateOffset, method, frequency, salt, exclusionZone);
        this.spacingBlocks = spacingBlocks;
        this.offsetRange = offsetRange;
    }

    public int spacingBlocks() {
        return this.spacingBlocks;
    }

    public int offsetRange() {
        return this.offsetRange;
    }

    // ==================== 网格数学（静态，供放置规则与结构本体共用） ====================

    /**
     * 结构锚点必须吸附到的格距 —— 与 {@link BackroomsMaze#CELL_PITCH} 一致。
     *
     * <p><b>为什么要吸附：</b>迷宫自身的墙体/通道是以 8 格为周期的逻辑单元格网格排布的。
     * 如果结构锚点落在任意偏移上，结构自带的墙壁、地板与内部家具就会与迷宫墙体错位，
     * 出现"竹楼梯插进墙里、房间被墙切开"的观感。把锚点吸附到 8 的整数倍后，
     * 结构的方块与迷宫单元格对齐，房间才能完整地嵌进迷宫。</p>
     */
    public static final int GRID_SNAP = BackroomsMaze.CELL_PITCH;

    /** 网格单元 (gx,gz) 的锚点方块 X 坐标（确定性、已吸附到 8 格网格，与世界种子无关）。 */
    public static int anchorX(int gx, int gz) {
        int steps = DEFAULT_OFFSET_RANGE / GRID_SNAP + 1;
        return gx * DEFAULT_SPACING_BLOCKS
                + (int) (Math.floorMod(BackroomsMaze.hash3(gx, gz, 0, SALT_ANCHOR_X), (long) steps)
                        * GRID_SNAP);
    }

    /** 网格单元 (gx,gz) 的锚点方块 Z 坐标（确定性、已吸附到 8 格网格，与世界种子无关）。 */
    public static int anchorZ(int gx, int gz) {
        int steps = DEFAULT_OFFSET_RANGE / GRID_SNAP + 1;
        return gz * DEFAULT_SPACING_BLOCKS
                + (int) (Math.floorMod(BackroomsMaze.hash3(gx, gz, 0, SALT_ANCHOR_Z), (long) steps)
                        * GRID_SNAP);
    }

    /**
     * 由"放置区块"反推它对应的网格单元。
     *
     * <p>{@code Structure.generate} 只拿到区块坐标，结构本体需要凭此反推出锚点，
     * 因此这里提供与 {@link #isPlacementChunk} 完全一致的判定逻辑。</p>
     *
     * @return {@code {gx, gz}}；若该区块不是任何单元的放置区块则返回 {@code null}
     */
    public static int[] cellForChunk(int chunkX, int chunkZ) {
        int gx0 = Math.floorDiv(chunkX * 16 - DEFAULT_SPACING_BLOCKS, DEFAULT_SPACING_BLOCKS);
        int gx1 = Math.floorDiv(chunkX * 16 + 15 + DEFAULT_SPACING_BLOCKS, DEFAULT_SPACING_BLOCKS);
        int gz0 = Math.floorDiv(chunkZ * 16 - DEFAULT_SPACING_BLOCKS, DEFAULT_SPACING_BLOCKS);
        int gz1 = Math.floorDiv(chunkZ * 16 + 15 + DEFAULT_SPACING_BLOCKS, DEFAULT_SPACING_BLOCKS);
        for (int gx = gx0; gx <= gx1; gx++) {
            for (int gz = gz0; gz <= gz1; gz++) {
                if (Math.floorDiv(anchorX(gx, gz), 16) == chunkX
                        && Math.floorDiv(anchorZ(gx, gz), 16) == chunkZ) {
                    return new int[]{gx, gz};
                }
            }
        }
        return null;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        // 概率判定完全交给 Level0Structure.findGenerationPoint（逐层 0.005%），
        // 因此这里对所有区块返回 true，让它有机会执行。
        return true;
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModStructures.LEVEL0_PLACEMENT.get();
    }
}