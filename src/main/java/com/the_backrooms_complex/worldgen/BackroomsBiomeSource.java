package com.the_backrooms_complex.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.the_backrooms_complex.worldgen.level1.Level1Sectors;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * 后室三维群系源。
 *
 * <p>一个必填的 {@code primary_biome} 加最多六个可选的区段群系。
 * Level 0 只声明 {@code yellow_rooms}，整个维度都是同一种黄色房间；
 * Level 1 声明 {@code warehouse_aisles} 作为主群系，再给出天鹰与五个区段群系，
 * 由 {@link Level1Sectors#regionAt(int, int, int)} 按层选择。</p>
 *
 * <p>层高由维度 JSON 的 {@code layer_height} 指定；Level 0 省略时使用 8，
 * Level 1 显式写 12。该值必须与生成器的层高常量一致，否则雾色切换会与楼层错开。</p>
 */
public class BackroomsBiomeSource extends BiomeSource {

    /** 未指定 {@code layer_height} 时的默认单层高度（Level 0 的 8 格）。 */
    public static final int DEFAULT_LAYER_HEIGHT = 8;

    /** 群系源编解码器。 */
    public static final MapCodec<BackroomsBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Biome.CODEC.fieldOf("primary_biome").forGetter(s -> s.primaryBiome),
                    Codec.INT.optionalFieldOf("layer_height", DEFAULT_LAYER_HEIGHT)
                            .forGetter(s -> s.layerHeight),
                    Biome.CODEC.optionalFieldOf("aquila_sector").forGetter(s -> s.aquilaSector),
                    Biome.CODEC.optionalFieldOf("yuejin_sector").forGetter(s -> s.yuejinSector),
                    Biome.CODEC.optionalFieldOf("gothic_sector").forGetter(s -> s.gothicSector),
                    Biome.CODEC.optionalFieldOf("ouroboros_sector").forGetter(s -> s.ouroborosSector),
                    Biome.CODEC.optionalFieldOf("garden_sector").forGetter(s -> s.gardenSector),
                    Biome.CODEC.optionalFieldOf("legend_sector").forGetter(s -> s.legendSector)
            ).apply(instance, BackroomsBiomeSource::new));

    private final Holder<Biome> primaryBiome;
    private final int layerHeight;
    private final Optional<Holder<Biome>> aquilaSector;
    private final Optional<Holder<Biome>> yuejinSector;
    private final Optional<Holder<Biome>> gothicSector;
    private final Optional<Holder<Biome>> ouroborosSector;
    private final Optional<Holder<Biome>> gardenSector;
    private final Optional<Holder<Biome>> legendSector;

    public BackroomsBiomeSource(Holder<Biome> primaryBiome, int layerHeight,
                                Optional<Holder<Biome>> aquilaSector,
                                Optional<Holder<Biome>> yuejinSector,
                                Optional<Holder<Biome>> gothicSector,
                                Optional<Holder<Biome>> ouroborosSector,
                                Optional<Holder<Biome>> gardenSector,
                                Optional<Holder<Biome>> legendSector) {
        this.primaryBiome = primaryBiome;
        this.layerHeight = layerHeight <= 0 ? DEFAULT_LAYER_HEIGHT : layerHeight;
        this.aquilaSector = aquilaSector;
        this.yuejinSector = yuejinSector;
        this.gothicSector = gothicSector;
        this.ouroborosSector = ouroborosSector;
        this.gardenSector = gardenSector;
        this.legendSector = legendSector;
    }

    /** 本维度的单层高度。 */
    public int layerHeight() {
        return this.layerHeight;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        // getNoiseBiome 的三个坐标都是 quart（1/4 方块）分辨率，乘 4 还原成方块坐标。
        return biomeAt(x * 4, z * 4, Math.floorDiv(y * 4, this.layerHeight));
    }

    // ==================== 缓存（每实例独立） ====================

    /** 缓存槽位（2 的幂）。 */
    private static final int BIOME_CACHE_SLOTS = 4096;

    /** 每个维度各有一份缓存，避免 Level 0 / Level 1 互相串色。 */
    private final long[] cacheKeys = new long[BIOME_CACHE_SLOTS];
    @SuppressWarnings("unchecked")
    private final Holder<Biome>[] cacheVals = (Holder<Biome>[]) new Holder<?>[BIOME_CACHE_SLOTS];

    /** 带缓存的群系查询。 */
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
        Holder<Biome> sector = sectorBiomeAt(x, z, layer);
        return sector != null ? sector : this.primaryBiome;
    }

    /**
     * 按 Level 1 的区段取群系；没有配置任何区段群系时返回 {@code null}，
     * 调用方退回 {@code primary_biome}。缺省某个区段时也退回主群系。
     */
    private Holder<Biome> sectorBiomeAt(int x, int z, int layer) {
        if (this.aquilaSector.isEmpty() && this.yuejinSector.isEmpty()
                && this.gothicSector.isEmpty() && this.ouroborosSector.isEmpty()
                && this.gardenSector.isEmpty() && this.legendSector.isEmpty()) {
            return null;
        }
        var region = Level1Sectors.regionAt(x, z, layer);
        return switch (region) {
            case AQUILA -> this.aquilaSector.orElse(this.primaryBiome);
            case YUEJIN -> this.yuejinSector.orElse(this.primaryBiome);
            case GOTHIC -> this.gothicSector.orElse(this.primaryBiome);
            case OUROBOROS -> this.ouroborosSector.orElse(this.primaryBiome);
            case GARDEN -> this.gardenSector.orElse(this.primaryBiome);
            case LEGEND -> this.legendSector.orElse(this.primaryBiome);
            default -> this.primaryBiome;
        };
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(
                Stream.of(this.primaryBiome),
                Stream.of(this.aquilaSector, this.yuejinSector, this.gothicSector,
                                this.ouroborosSector, this.gardenSector, this.legendSector)
                        .filter(Optional::isPresent)
                        .map(Optional::get));
    }
}
