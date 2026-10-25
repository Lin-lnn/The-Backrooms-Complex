package com.the_backrooms_complex.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.the_backrooms_complex.ModBiomes;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * 后室三维群系源。
 *
 * <p>三个字段（黄房间 / 混凝土大厅 / 潮湿走廊）由<b>每个维度的 JSON 独立指定</b>：
 * Level 0 用 {@code yellow_rooms / concrete_halls / damp_corridors}，
 * Level 1 用 {@code warehouse_aisles / aquila_sector / puddle_zone}。</p>
 *
 * <h2>维度独立性</h2>
 * <ul>
 *   <li>缓存改为<b>实例级</b>（见 {@link #cacheKeys}）—— 原来是 {@code static}，
 *       导致 Level 0 与 Level 1 共用同一份缓存，同一 (x,z,layer) 会返回
 *       先算出来的那个维度的群系（串维度 bug）；</li>
 *   <li>层高由 {@link #layerHeight} 字段决定，不再硬编码 Level 0 的 8 格，
 *       因此 Level 1 可以在自己的维度 JSON 里写 {@code "layer_height": 18}。</li>
 * </ul>
 */
public class BackroomsBiomeSource extends BiomeSource {

    /** 未指定 {@code layer_height} 时的默认单层高度（Level 0 的 8 格）。 */
    public static final int DEFAULT_LAYER_HEIGHT = 8;

    /** 群系源编解码器。 */
    public static final MapCodec<BackroomsBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Biome.CODEC.fieldOf("yellow_rooms").forGetter(s -> s.yellowRooms),
                    Biome.CODEC.fieldOf("concrete_halls").forGetter(s -> s.concreteHalls),
                    Biome.CODEC.fieldOf("damp_corridors").forGetter(s -> s.dampCorridors),
                    // 可选：单层高度。省略时默认 Level 0 的 8 格。
                    Codec.INT.optionalFieldOf("layer_height", DEFAULT_LAYER_HEIGHT)
                            .forGetter(s -> s.layerHeight)
            ).apply(instance, BackroomsBiomeSource::new));

    private final Holder<Biome> yellowRooms;
    private final Holder<Biome> concreteHalls;
    private final Holder<Biome> dampCorridors;
    /** 本维度的单层高度（格）。 */
    private final int layerHeight;

    public BackroomsBiomeSource(Holder<Biome> yellowRooms, Holder<Biome> concreteHalls,
                                Holder<Biome> dampCorridors, int layerHeight) {
        this.yellowRooms = yellowRooms;
        this.concreteHalls = concreteHalls;
        this.dampCorridors = dampCorridors;
        this.layerHeight = layerHeight <= 0 ? DEFAULT_LAYER_HEIGHT : layerHeight;
    }

    /** 兼容旧的 3 参数构造（层高取 Level 0 的默认值）。 */
    public BackroomsBiomeSource(Holder<Biome> yellowRooms, Holder<Biome> concreteHalls,
                                Holder<Biome> dampCorridors) {
        this(yellowRooms, concreteHalls, dampCorridors, DEFAULT_LAYER_HEIGHT);
    }

    /** 本维度的单层高度。 */
    public int layerHeight() {
        return this.layerHeight;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        // 本群系源只按 (x, z, layer) 决定，忽略 sampler。
        // getNoiseBiome 的三个坐标都是 "quart"（1/4 方块）分辨率，必须乘 4 还原成方块坐标。
        // 层高用本实例自己的 layerHeight，不再借用 Level 0 的常量。
        return biomeAt(x * 4, z * 4, Math.floorDiv(y * 4, this.layerHeight));
    }

    // ==================== 缓存（每实例独立） ====================

    /** 缓存槽位（2 的幂）。 */
    private static final int BIOME_CACHE_SLOTS = 4096;

    /**
     * 每实例独立的缓存。
     *
     * <p>原先是 {@code static} —— Level 0 与 Level 1 会命中同一份缓存，
     * 同一 (x,z,layer) 直接返回另一个维度的群系，属于跨维度串色的严重 bug。
     * 改为实例字段后，每个维度各有一份缓存，互不影响。</p>
     */
    private final long[] cacheKeys = new long[BIOME_CACHE_SLOTS];
    @SuppressWarnings("unchecked")
    private final Holder<Biome>[] cacheVals = (Holder<Biome>[]) new Holder<?>[BIOME_CACHE_SLOTS];

    /**
     * 带缓存的群系查询。
     *
     * <p>{@code computeBiomeAt} 要做哈希 + 单元吸附，是逐方块生成里较贵的操作；
     * 同一次区块生成会对同一个 (x, z, layer) 反复查询多次，这里 O(1) 命中。</p>
     */
    public Holder<Biome> biomeAt(int x, int z, int layer) {
        long key = ((long) (x & 0xFFFFFF) << 34) | ((long) (z & 0xFFFFFF) << 10) | (layer & 0x3FF);
        int slot = (int) ((key * 0x9E3779B97F4A7C15L) >>> 52) & (BIOME_CACHE_SLOTS - 1);
        if (this.cacheKeys[slot] == key && key != 0L) {
            return this.cacheVals[slot];
        }
        Holder<Biome> v = computeBiomeAt(x, z, layer);
        this.cacheKeys[slot] = key;
        this.cacheVals[slot] = v;
        return v;
    }

    /** 真正的群系计算（无缓存）。 */
    private Holder<Biome> computeBiomeAt(int x, int z, int layer) {
        // 三个群系都以 256x256 格为单元整体分配，每个区块必然完整属于同一个群系，
        // 不会出现"区块内一半黄一半混凝土"的错位。
        int cx = Math.floorDiv(Math.floorDiv(x, 16), HALL_CELL_BLOCKS / 16);
        int cz = Math.floorDiv(Math.floorDiv(z, 16), HALL_CELL_BLOCKS / 16);
        // 群系比例：主群系 4 : 次群系 1 : 第三群系 1
        double roll = hash01(cx, cz, layer);
        if (roll < 1.0 / 6.0) {
            return concreteHalls;
        }
        if (roll < 2.0 / 6.0) {
            return dampCorridors;
        }
        return yellowRooms;
    }


    /** 是否是本维度的「第三个」群系（Level 0 为潮湿走廊，Level 1 为积水区）。 */
    public boolean isDampCorridors(Holder<Biome> biome) {
        return biome.is(ModBiomes.DAMP_CORRIDORS);
    }

    /** 是否是本维度的「第二个」群系（Level 0 为混凝土大厅，Level 1 为天鹰段）。 */
    public boolean isConcreteHalls(Holder<Biome> biome) {
        return biome.is(ModBiomes.CONCRETE_HALLS);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(this.yellowRooms, this.concreteHalls, this.dampCorridors);
    }

    // ==================== 噪声 / 哈希 ====================

    /** 群系单元边长（格）= 256，即 16×16 区块。 */
    private static final int HALL_CELL_BLOCKS = 256;

    /** 三维确定性哈希 → [0,1)。 */
    private static double hash01(int x, int z, int layer) {
        long h = (long) x * 0x9E3779B97F4A7C15L
                ^ (long) z * 0xC2B2AE3D27D4EB4FL
                ^ (long) layer * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL;
    }
}
