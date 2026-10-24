package com.the_backrooms_complex.structure;

import com.the_backrooms_complex.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * 后室特定结构的一个片段（基于用户提供的 .nbt 模板）。
 *
 * <h2>两阶段放置（关键）</h2>
 * <ol>
 *   <li><b>第一次生成</b>：{@code ChunkGenerator.fillFromNoise} 铺出迷宫的墙壁 / 地板 / 天花板；</li>
 *   <li><b>第二次生成</b>：本片段把结构包围盒清成空气（<b>绝不触碰基岩</b>），
 *       随后由 {@code StructureTemplate.placeInWorld} 放上模板自身的方块。</li>
 * </ol>
 *
 * <p>为什么要先清空：用户提供的模板中，有些（如 {@code level0_1}）<b>没有显式 air 条目</b> ——
 * 未被写入的位置在 NBT 里就是"缺失"，若直接放置，迷宫的墙壁会残留在房间内部，
 * 结构看起来就像一片插进墙里的残缺家具。先清空再放置，等价于
 * "让结构中的空气占位掉第一次生成的墙壁"，而结构自身的墙壁 / 地板 / 天花板
 * 会在第二步重新放上去，<b>一个都不会被删掉</b>。</p>
 *
 * <p>整个过程使用 {@link BedrockSafeProcessor}：世界中的 {@code minecraft:bedrock} 既不清理也不覆盖。</p>
 */
public class Level0StructurePiece extends TemplateStructurePiece {

    /** 用于新建片段。 */
    public Level0StructurePiece(StructureTemplateManager templateManager, ResourceLocation templateId,
                                StructurePlaceSettings settings, BlockPos origin) {
        super(ModStructures.LEVEL0_PIECE.get(), 0, templateManager, templateId,
                templateId.toString(), settings, origin);
    }

    /** 用于从存档 NBT 反序列化。 */
    public Level0StructurePiece(StructureTemplateManager templateManager, CompoundTag tag) {
        super(ModStructures.LEVEL0_PIECE.get(), tag, templateManager,
                id -> defaultSettings());
    }

    /** 统一的放置设置：忽略实体 + 基岩保护处理器。 */
    static StructurePlaceSettings defaultSettings() {
        return new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .addProcessor(BedrockSafeProcessor.INSTANCE);
    }

    /**
     * 两阶段放置：先清空包围盒（保留基岩），再放上模板自身的方块。
     */
    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // ---- 第一步：清除。只处理"结构包围盒 ∩ 本区块可写区域" ----
        BoundingBox pieceBox = this.getBoundingBox();
        int minX = Math.max(pieceBox.minX(), box.minX());
        int minY = Math.max(pieceBox.minY(), box.minY());
        int minZ = Math.max(pieceBox.minZ(), box.minZ());
        int maxX = Math.min(pieceBox.maxX(), box.maxX());
        int maxY = Math.min(pieceBox.maxY(), box.maxY());
        int maxZ = Math.min(pieceBox.maxZ(), box.maxZ());
        if (minX <= maxX && minY <= maxY && minZ <= maxZ) {
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        mutable.set(x, y, z);
                        // 世界中的基岩既不清理也不覆盖
                        if (!level.getBlockState(mutable).is(Blocks.BEDROCK)) {
                            level.setBlock(mutable, air, 2);
                        }
                    }
                }
            }
        }

        // ---- 第二步：放置模板自身的方块（墙壁/地板/天花板/家具，含显式 air）----
        // BedrockSafeProcessor 会在这一层再次保护基岩。
        super.postProcess(level, structureManager, generator, random, box, chunkPos, pos);
    }

    @Override
    protected void handleDataMarker(String metadata, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox box) {
        // 用户提供的结构不包含数据标记（DATA 模式的结构方块）
    }
}