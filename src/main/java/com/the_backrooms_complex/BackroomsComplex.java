package com.the_backrooms_complex;

import com.mojang.serialization.MapCodec;
import com.the_backrooms_complex.command.BackroomsCommands;
import com.the_backrooms_complex.datagen.DataGenerators;
import com.the_backrooms_complex.worldgen.BackroomsBiomeSource;
import com.the_backrooms_complex.worldgen.BackroomsChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组主类。
 *
 * <p>说明：在 NeoForge 1.21.1 中，区块生成器（ChunkGenerator）与群系源（BiomeSource）的
 * 编解码器（Codec）本身也是注册表对象，分别注册到 {@link Registries#CHUNK_GENERATOR} 和
 * {@link Registries#BIOME_SOURCE}。维度 JSON 通过其注册名引用它们。</p>
 */
@Mod(BackroomsComplex.MODID)
public class BackroomsComplex {

    /** 模组 ID，需与 neoforge.mods.toml 中的 modId 一致。 */
    public static final String MODID = "backrooms_complex";

    /** 自定义区块生成器编解码器注册表（worldgen/chunk_generator）。 */
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MODID);

    /** 自定义群系源编解码器注册表（worldgen/biome_source）。 */
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, MODID);

    public BackroomsComplex(IEventBus modEventBus) {
        // 注册方块、物品与创造模式物品栏
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_MODE_TABS.register(modEventBus);
        ModItems.ARMOR_MATERIALS.register(modEventBus);

        // 注册区块生成器编解码器（维度 JSON 中 "type": "backrooms_complex:backrooms"）
        CHUNK_GENERATORS.register("backrooms", () -> BackroomsChunkGenerator.CODEC);
        // 注册群系源编解码器（维度 JSON 中 "type": "backrooms_complex:backrooms_biome_source"）
        BIOME_SOURCES.register("backrooms_biome_source", () -> BackroomsBiomeSource.CODEC);

        CHUNK_GENERATORS.register(modEventBus);
        BIOME_SOURCES.register(modEventBus);

        // 结构系统注册（StructureType / StructurePiece / StructureProcessor / StructurePlacement）
        ModStructures.STRUCTURE_TYPES.register(modEventBus);
        ModStructures.PIECE_TYPES.register(modEventBus);
        ModStructures.PROCESSOR_TYPES.register(modEventBus);
        ModStructures.PLACEMENT_TYPES.register(modEventBus);

        // 数据生成（仅 runData 时触发，mod bus 事件）
        modEventBus.addListener(DataGenerators::gatherData);

        // 游戏事件（方块破坏/放置、维度切换）：注册到 NeoForge 游戏事件总线
        NeoForge.EVENT_BUS.register(ModEvents.class);
        // 命令注册：RegisterCommandsEvent 同样在游戏事件总线触发
        NeoForge.EVENT_BUS.addListener(BackroomsCommands::register);
    }
}
