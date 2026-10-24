package com.the_backrooms_complex.block;

import com.the_backrooms_complex.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * 「剥离地毯的地板」——被刮纸刀刮掉地毯后的地板。
 *
 * <h2>复原机制</h2>
 * <p>被刮开时会用 {@code scheduleTick} 预约 {@link #RESTORE_DELAY}（60 秒）后的复原。
 * 复原目标由 {@link Origin} 属性决定：原本是黄色房间的地板还是潮湿地板。</p>
 *
 * <h2>混凝土大厅不复原</h2>
 * <p>若当前所在群系是 {@code concrete_halls}，则<b>跳过复原</b> —— 混凝土大厅的地毯一旦被
 * 剥离就会永久保持剥离状态。</p>
 */
public class StrippedCarpetFloorBlock extends Block {

    /** 复原延迟：60 秒 = 1200 tick。 */
    public static final int RESTORE_DELAY = 1200;

    /** 这个剥离态原本来自哪种地板。 */
    public enum Origin implements net.minecraft.util.StringRepresentable {
        FLOOR_TILE("floor_tile"),
        DAMP_FLOOR("damp_floor");

        private final String name;

        Origin(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    /** 来源地板类型。 */
    public static final EnumProperty<Origin> ORIGIN =
            EnumProperty.create("origin", Origin.class);

    public StrippedCarpetFloorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[] { ORIGIN });
    }

    /** 预约复原。 */
    public static void scheduleRestore(ServerLevel level, BlockPos pos) {
        level.scheduleTick(pos, ModBlocks.STRIPPED_CARPET_FLOOR.get(), RESTORE_DELAY);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 混凝土大厅不复原
        String path = level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getResourceKey(level.getBiome(pos).value())
                .map(k -> k.location().getPath())
                .orElse("");
        if ("concrete_halls".equals(path)) {
            return;
        }
        Block target = state.getValue(ORIGIN) == Origin.DAMP_FLOOR
                ? ModBlocks.DAMP_FLOOR.get()
                : ModBlocks.FLOOR_TILE.get();
        level.setBlockAndUpdate(pos, target.defaultBlockState());
    }
}