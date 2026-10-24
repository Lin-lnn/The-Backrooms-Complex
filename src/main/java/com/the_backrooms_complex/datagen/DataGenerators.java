package com.the_backrooms_complex.datagen;

import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModBiomes;
import com.the_backrooms_complex.ModDimensions;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 数据生成（datagen）入口。
 *
 * <p>运行 {@code gradlew runData} 时，本类通过 {@link RegistrySetBuilder} 调用
 * {@link ModDimensions#bootstrapType} 与 {@link ModBiomes#bootstrap}，把维度类型与群系
 * 的 {@code BootstrapContext} 注册结果生成到 {@code src/generated/resources/data/...} 下的
 * JSON 文件。</p>
 *
 * <p>注意：本模组已在 {@code src/main/resources/data/backrooms_complex/} 下直接提供了等价的
 * dimension_type / worldgen/biome JSON（运行时实际加载这些文件）。datagen 用于按代码重新生成，
 * 二者不应同时出现在资源路径上，否则可能重复。二选一即可。</p>
 */
public final class DataGenerators {

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        RegistrySetBuilder builder = new RegistrySetBuilder()
                .add(Registries.DIMENSION_TYPE, ModDimensions::bootstrapType)
                .add(Registries.BIOME, ModBiomes::bootstrap);

        generator.addProvider(event.includeServer(),
                new DatapackBuiltinEntriesProvider(output, lookupProvider, builder,
                        Set.of(BackroomsComplex.MODID)));
    }

    private DataGenerators() {
    }
}