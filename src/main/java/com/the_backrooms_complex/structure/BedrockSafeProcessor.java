package com.the_backrooms_complex.structure;

import com.mojang.serialization.MapCodec;
import com.the_backrooms_complex.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;

/**
 * 基岩保护处理器。
 *
 * <p>在放置结构中的每一个方块之前检查目标位置当前方块：</p>
 * <ul>
 *   <li>若目标位置已经是 {@code minecraft:bedrock} → 返回 {@code null}，<b>跳过该方块</b>，
 *       保留世界中原有的基岩；</li>
 *   <li>其余情况 → 返回结构方块信息，<b>强制覆盖</b>（包括后室模组自身的墙壁、踢脚线、
 *       天花板、地板与荧光灯，也包括空气）。</li>
 * </ul>
 *
 * <p>因此：</p>
 * <ul>
 *   <li>NBT 结构自身若包含 {@code minecraft:bedrock}，在非基岩位置可以正常放置；</li>
 *   <li>任何情况下都不会替换或破坏世界中已有的基岩，结构无法"穿过"基岩。</li>
 * </ul>
 */
public class BedrockSafeProcessor extends StructureProcessor {

    /** 无状态处理器，单例复用。 */
    public static final BedrockSafeProcessor INSTANCE = new BedrockSafeProcessor();

    /** 无字段，因此用 unit codec。 */
    public static final MapCodec<BedrockSafeProcessor> CODEC = MapCodec.unit(INSTANCE);

    @Override
    @Nullable
    public StructureTemplate.StructureBlockInfo process(LevelReader level, BlockPos seedPos, BlockPos pos,
                                                        StructureTemplate.StructureBlockInfo rawInfo,
                                                        StructureTemplate.StructureBlockInfo info,
                                                        StructurePlaceSettings settings,
                                                        @Nullable StructureTemplate template) {
        if (info == null) {
            return null;
        }
        // 注意：本方法的第 3 个参数 pos 是"种子位置"（结构原点/参考点），
        // 当前方块的绝对坐标在 info.pos() 中（与 vanilla 的 LavaSubmergedBlockProcessor 一致）。
        if (level.getBlockState(info.pos()).is(Blocks.BEDROCK)) {
            return null; // 跳过该方块，保留世界中原有的基岩
        }
        // 其余方块一律强制覆盖（包括后室模组的墙壁/踢脚线/天花板/地板/荧光灯，以及空气）
        return info;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructures.BEDROCK_SAFE_PROCESSOR.get();
    }
}