package com.yourname.backrooms_complex;

import com.yourname.backrooms_complex.structure.BedrockSafeProcessor;
import com.yourname.backrooms_complex.structure.Level0Structure;
import com.yourname.backrooms_complex.structure.Level0StructurePiece;
import com.yourname.backrooms_complex.structure.Level0StructurePlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 结构系统注册类（NeoForge 1.21.1 标准 Structure / StructureType / StructurePiece 机制）。
 *
 * <p>注册四类对象：</p>
 * <ul>
 *   <li>{@link Registries#STRUCTURE_TYPE}：结构类型（对应结构 JSON 的 {@code "type"}）；</li>
 *   <li>{@link Registries#STRUCTURE_PIECE}：结构片段类型（用于存档读写片段）；</li>
 *   <li>{@link Registries#STRUCTURE_PROCESSOR}：结构处理器类型（基岩保护）；</li>
 *   <li>{@link Registries#STRUCTURE_PLACEMENT}：放置规则类型（90 格方块网格）。</li>
 * </ul>
 */
public final class ModStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, BackroomsComplex.MODID);

    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, BackroomsComplex.MODID);

    public static final DeferredRegister<StructureProcessorType<?>> PROCESSOR_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, BackroomsComplex.MODID);

    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, BackroomsComplex.MODID);

    /** 结构类型：{@code "type": "backrooms_complex:level0"} */
    public static final DeferredHolder<StructureType<?>, StructureType<Level0Structure>> LEVEL0_STRUCTURE =
            STRUCTURE_TYPES.register("level0", () -> () -> Level0Structure.CODEC);

    /** 结构片段类型。 */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> LEVEL0_PIECE =
            PIECE_TYPES.register("level0",
                    () -> (StructurePieceType.StructureTemplateType) Level0StructurePiece::new);

    /** 基岩保护处理器类型。 */
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<BedrockSafeProcessor>>
            BEDROCK_SAFE_PROCESSOR =
            PROCESSOR_TYPES.register("bedrock_safe", () -> () -> BedrockSafeProcessor.CODEC);

    /** 放置规则类型：90 格方块网格（{@code "type": "backrooms_complex:level0"}）。 */
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<Level0StructurePlacement>>
            LEVEL0_PLACEMENT =
            PLACEMENT_TYPES.register("level0", () -> () -> Level0StructurePlacement.CODEC);

    private ModStructures() {
    }
}
