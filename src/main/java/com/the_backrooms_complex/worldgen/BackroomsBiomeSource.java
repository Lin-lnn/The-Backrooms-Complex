package com.the_backrooms_complex.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * 后室群系源。
 *
 * <p>根据 XZ 坐标的确定性二维值噪声分配 3 种群系。噪声完全由坐标决定，因此每个区块的
 * 群系分布独立可复现（无需跨区块全局状态）。</p>
 *
 * <p>群系之间的边界由双线性插值 + 平滑阶梯（smoothstep）产生，形成大块连续的群系斑块，
 * 而非随机散点。</p>
 */
public class BackroomsBiomeSource extends BiomeSource {

    /** 群系源编解码器：三个字段分别引用三种群系（以资源位置字符串序列化）。 */
    public static final MapCodec<BackroomsBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Biome.CODEC.fieldOf("yellow_rooms").forGetter(s -> s.yellowRooms),
                    Biome.CODEC.fieldOf("concrete_halls").forGetter(s -> s.concreteHalls),
                    Biome.CODEC.fieldOf("damp_corridors").forGetter(s -> s.dampCorridors)
            ).apply(instance, BackroomsBiomeSource::new));

    private final Holder<Biome> yellowRooms;
    private final Holder<Biome> concreteHalls;
    private final Holder<Biome> dampCorridors;

    public BackroomsBiomeSource(Holder<Biome> yellowRooms, Holder<Biome> concreteHalls, Holder<Biome> dampCorridors) {
        this.yellowRooms = yellowRooms;
        this.concreteHalls = concreteHalls;
        this.dampCorridors = dampCorridors;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        // 本群系源与高度无关，忽略 sampler
        // 注意：getNoiseBiome 的三个坐标都是"quart"（1/4 方块）分辨率，
        // 必须乘 4 还原成方块坐标，否则群系场会被拉伸 4 倍并与生成器错位。
        return biomeAt(x * 4, z * 4, Math.floorDiv(y * 4, BackroomsChunkGenerator.LAYER_HEIGHT));
    }

    /**
     * 按 XZ 坐标确定性返回群系（供 ChunkGenerator 直接调用，无需 Climate.Sampler）。
     */
    public Holder<Biome> biomeAt(int x, int z, int layer) {
        // 三个群系都以 256x256 格（面积 65536）为单元整体分配，远超"单个 >= 40000 格"。
        // 整单元分配 => 每个群系斑块都是完整的 65536 格。
        // 先把坐标吸附到 16 格区块网格，再划分群系单元：
        // 群系单元 256 = 16 x 16 区块，因此每个区块必然完整属于同一个群系，
        // 不会出现"区块内一半黄一半混凝土"的错位。
        int cx = Math.floorDiv(Math.floorDiv(x, 16), HALL_CELL_BLOCKS / 16);
        int cz = Math.floorDiv(Math.floorDiv(z, 16), HALL_CELL_BLOCKS / 16);
        if (hash01(cx, cz, layer) < 0.05) {
            return concreteHalls;
        }
        // 其余区域在黄色房间 / 潮湿走廊之间由原噪声划分
        return hash01(cx + 7919, cz + 104729, layer) < 0.5 ? yellowRooms : dampCorridors;
    }

    /** 该群系是否为混凝土大厅（供 ChunkGenerator 判定额外特征）。 */
    /** 群系交接处的过渡带宽度（格）。 */
    public static final int BLEND_RADIUS = 4;

    /** 该群系是否为潮湿走廊。 */
    public boolean isDampCorridors(Holder<Biome> biome) {
        return biome == dampCorridors;
    }

    public boolean isConcreteHalls(Holder<Biome> biome) {
        return biome == concreteHalls;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(yellowRooms, concreteHalls, dampCorridors);
    }

    // ==================== 确定性二维值噪声 ====================

    /** 单元格尺寸（格）：群系斑块大致以该尺寸为过渡尺度。 */
    private static final int CELL_SIZE = 32;

    /** 混凝土大厅的单元尺寸（格）：6 区块 = 96 格，保证单区域 >= 5x5 区块。 */
    private static final int HALL_CELL_BLOCKS = 256;

    /** 二维值噪声：对四个角点哈希值做双线性插值 + smoothstep 平滑。 */
    private static double valueNoise(int x, int z, int layer) {
        int gx = Math.floorDiv(x, CELL_SIZE);
        int gz = Math.floorDiv(z, CELL_SIZE);
        double fx = smoothstep((x - gx * CELL_SIZE) / (double) CELL_SIZE);
        double fz = smoothstep((z - gz * CELL_SIZE) / (double) CELL_SIZE);
        double v00 = hash01(gx, gz, layer);
        double v10 = hash01(gx + 1, gz, layer);
        double v01 = hash01(gx, gz + 1, layer);
        double v11 = hash01(gx + 1, gz + 1, layer);
        return lerp(lerp(v00, v10, fx), lerp(v01, v11, fx), fz);
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** 将整数坐标哈希到 [0, 1) 区间（splitmix64 风格的确定性哈希）。 */
    private static double hash01(int x, int z, int layer) {
        long h = (long) x * 0x9E3779B97F4A7C15L ^ (long) z * 0xC2B2AE3D27D4EB4FL ^ (long) layer * 0xD6E8FEB86659FD93L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL;
    }
}