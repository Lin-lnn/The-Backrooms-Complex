package com.yourname.backrooms_complex;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * 群系注册类。
 *
 * <p>与维度类型一样，1.21.1 中群系也是数据包注册表，运行时来自
 * {@code data/backrooms_complex/worldgen/biome/*.json}。本类的 {@link #bootstrap(BootstrapContext)}
 * 用于数据生成阶段生成这些 JSON。</p>
 *
 * <p>本模组定义了 3 种群系，区别在于墙壁花纹（由 {@code BackroomsChunkGenerator} 根据群系选择）
 * 与战利品表（由 {@code BackroomsChunkGenerator} 根据群系选择战利品箱）。</p>
 */
public final class ModBiomes {

    /** 黄色房间：经典后室黄色墙纸风格。 */
    public static final ResourceKey<Biome> YELLOW_ROOMS = key("yellow_rooms");
    /** 混凝土大厅：灰色工业风格，物资偏工具类。 */
    public static final ResourceKey<Biome> CONCRETE_HALLS = key("concrete_halls");
    /** 潮湿走廊：暗绿色发霉风格，物资偏食物类。 */
    public static final ResourceKey<Biome> DAMP_CORRIDORS = key("damp_corridors");

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, name));
    }

    /**
     * 注册群系（数据生成阶段调用，见 {@code datagen.DataGenerators}）。
     *
     * @param context 数据生成的 BootstrapContext，用于向 BIOME 注册表写入条目
     */
    public static void bootstrap(BootstrapContext<Biome> context) {
        // 1.21.1 中 BiomeGenerationSettings.Builder 需要特性 / 雕刻器的 HolderGetter
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        // 各群系的雾色、天色营造不同的氛围（无天空光，天色仅作辅助）
        context.register(YELLOW_ROOMS, buildBiome(placedFeatures, carvers,
                0xC9BB63, // fog_color：暗黄色雾
                0x14140D  // sky_color：近黑
        ));
        context.register(CONCRETE_HALLS, buildBiome(placedFeatures, carvers,
                0x8E8E8E, // fog_color：灰色雾
                0x0B0B0B  // sky_color：近黑
        ));
        context.register(DAMP_CORRIDORS, buildBiome(placedFeatures, carvers,
                0x55603C, // fog_color：暗绿色雾
                0x0B0D09  // sky_color：近黑
        ));
    }

    /**
     * 构造一个"空"群系（无特性、无自然生成生物）。
     * 方块与战利品箱均由自定义 ChunkGenerator 直接放置，因此群系本身不需要特性与刷怪设置。
     */
    private static Biome buildBiome(HolderGetter<PlacedFeature> placedFeatures,
                                    HolderGetter<ConfiguredWorldCarver<?>> carvers,
                                    int fogColor, int skyColor) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .fogColor(fogColor)
                        .skyColor(skyColor)
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(new BiomeGenerationSettings.Builder(placedFeatures, carvers).build())
                .build();
    }

    private ModBiomes() {
    }
}
