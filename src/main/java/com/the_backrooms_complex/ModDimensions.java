package com.the_backrooms_complex;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.OptionalLong;

/**
 * 维度注册类。
 *
 * <p><b>重要说明：</b>在 Minecraft 1.21.1 / NeoForge 21.1 中，维度类型（DimensionType）、
 * 群系（Biome）、维度（LevelStem）等均为<b>数据包注册表</b>（datapack registry），
 * 它们的运行时来源是 {@code data/&lt;modid&gt;/...} 目录下的 JSON 文件，而非运行时代码注册。
 * 因此本类中的 {@link #bootstrapType(BootstrapContext)} 用于<b>数据生成（datagen）</b>阶段，
 * 通过 {@code RegistrySetBuilder} 生成对应的 JSON（见 {@code datagen.DataGenerators}）。
 * 运行时直接加载 {@code data/backrooms_complex/dimension_type/level_0.json}。</p>
 *
 * <p>本类按规格定义三个 ResourceKey：维度的键（LEVEL_STEM）、世界的键（DIMENSION）、
 * 维度类型的键（DIMENSION_TYPE）。</p>
 */
public final class ModDimensions {

    /** 维度 / 维度类型共用的资源位置。 */
    public static final ResourceLocation LEVEL_0_LOCATION =
            ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "level_0");

    /** 维度的键（Registries.LEVEL_STEM），用于定义 LevelStem（维度类型 + 区块生成器）。 */
    public static final ResourceKey<LevelStem> LEVEL_0_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, LEVEL_0_LOCATION);

    /** 世界的键（Registries.DIMENSION），即 {@code /execute in backrooms_complex:level_0} 中的目标。 */
    public static final ResourceKey<Level> LEVEL_0_WORLD =
            ResourceKey.create(Registries.DIMENSION, LEVEL_0_LOCATION);

    /** 维度类型的键（Registries.DIMENSION_TYPE）。 */
    public static final ResourceKey<DimensionType> LEVEL_0_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, LEVEL_0_LOCATION);

    // ==================== Level 1「宜居地带」 ====================

    /** Level 1 的资源位置。 */
    public static final ResourceLocation LEVEL_1_LOCATION =
            ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "level_1");

    /** Level 1 的维度键。 */
    public static final ResourceKey<LevelStem> LEVEL_1_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, LEVEL_1_LOCATION);

    /** Level 1 的世界键（{@code /execute in backrooms_complex:level_1}）。 */
    public static final ResourceKey<Level> LEVEL_1_WORLD =
            ResourceKey.create(Registries.DIMENSION, LEVEL_1_LOCATION);

    /** Level 1 的维度类型键。 */
    public static final ResourceKey<DimensionType> LEVEL_1_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, LEVEL_1_LOCATION);

    /**
     * 注册维度类型（数据生成阶段调用，见 {@code datagen.DataGenerators}）。
     *
     * @param context 数据生成的 BootstrapContext，用于向 DIMENSION_TYPE 注册表写入条目
     */
    public static void bootstrapType(BootstrapContext<DimensionType> context) {
        context.register(LEVEL_0_TYPE, new DimensionType(
                OptionalLong.of(6000L),      // fixedTime：恒定时间 6000（正午），营造昏暗恒定氛围
                false,                       // hasSkyLight：无天空光，全部照明依赖荧光灯
                true,                        // hasCeiling：有天花板（室内环境）
                false,                       // ultrawarm：非超热
                false,                       // natural：非自然维度
                1.0,                         // coordinateScale：坐标缩放 1:1
                false,                       // bedWorks：床不能工作
                false,                       // respawnAnchorWorks：重生锚不能工作
                0,                           // minY
                256,                         // height
                256,                         // logicalHeight
                BlockTags.INFINIBURN_OVERWORLD,           // 可无限燃烧的方块标签
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,   // 效果位置（ResourceLocation）
                0.1F,                        // ambientLight：极低环境光，保持昏暗但可见
                new DimensionType.MonsterSettings(
                        false,               // piglinSafe
                        false,               // hasRaids
                        ConstantInt.of(0),   // monsterSpawnLightTest：怪物生成光照测试固定为 0
                        0                    // monsterSpawnBlockLightLimit：无怪物生成
                )
        ));
    }

    private ModDimensions() {
    }
}
