package com.the_backrooms_complex.block;

import com.mojang.serialization.MapCodec;
import com.the_backrooms_complex.ModBlocks;
import com.the_backrooms_complex.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * 「被刮去墙纸」的 Level 0 墙壁方块。
 *
 * <p>由刮纸刀右键墙壁主体时产生。贴图为"墙纸被刮掉后露出的底层墙面"，
 * 经过 {@link ModConfig#wallpaperRestoreSeconds} 秒后自动恢复。</p>
 *
 * <h2>过渡贴图（本方块自己改变，而不是改变相邻的墙）</h2>
 * <p>根据<b>上下相邻是否也是「被刮去墙纸的墙壁」</b>切换贴图：</p>
 * <ul>
 *   <li>{@link Shape#FULL} —— 上下都不是刮纸墙（孤立一格）⇒ 整面都露出底层；</li>
 *   <li>{@link Shape#UPPER} —— 下方也是刮纸墙 ⇒ 只剩<b>上半部分</b>是刮过的；</li>
 *   <li>{@link Shape#LOWER} —— 上方也是刮纸墙 ⇒ 只剩<b>下半部分</b>是刮过的；</li>
 *   <li>{@link Shape#MID} —— 上下都是刮纸墙 ⇒ 只有<b>中间一条</b>是刮过的。</li>
 * </ul>
 * <p>贴图使用预先着好黄色调的版本，不再依赖运行时方块染色。</p>
 */
public class OakedWallBlock extends Block {

    /** 刮纸形状（决定用哪张过渡贴图）。 */
    public enum Shape implements StringRepresentable {
        /** 上下都不是刮纸墙 —— 整面露出底层。 */
        FULL("full"),
        /** 下方是刮纸墙 —— 只剩上半部分是刮过的。 */
        UPPER("upper"),
        /** 上方是刮纸墙 —— 只剩下半部分是刮过的。 */
        LOWER("lower"),
        /** 上下都是刮纸墙 —— 只有中间一条是刮过的。 */
        MID("mid");

        private final String name;

        Shape(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

    public static final MapCodec<OakedWallBlock> CODEC = simpleCodec(OakedWallBlock::new);

    public OakedWallBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, Shape.FULL));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE);
    }

    /** 判断某方块是不是同类（刮纸墙）。 */
    private static boolean isOaked(BlockState s) {
        return s.getBlock() instanceof OakedWallBlock;
    }

    /**
     * 按上下邻居决定形状。
     *
     * <ul>
     *   <li>上下都是刮纸墙 ⇒ {@link Shape#FULL}（整段连续，整面露出底层）；</li>
     *   <li>只有上方是刮纸墙 ⇒ {@link Shape#UPPER}（刮痕延伸向上，本格上半仍刮）；</li>
     *   <li>只有下方是刮纸墙 ⇒ {@link Shape#LOWER}（刮痕延伸向下，本格下半仍刮）；</li>
     *   <li>上下都不是 ⇒ {@link Shape#MID}（孤立一格，只有中间一条是刮过的）。</li>
     * </ul>
     */
    public static Shape shapeFor(LevelAccessor level, BlockPos pos) {
        boolean up = isOaked(level.getBlockState(pos.above()));
        boolean down = isOaked(level.getBlockState(pos.below()));
        if (up && down) {
            return Shape.FULL;
        }
        if (up) {
            return Shape.UPPER;
        }
        if (down) {
            return Shape.LOWER;
        }
        return Shape.MID;
    }

    /** 放下时立即按邻居算出正确形状，不等到邻居更新。 */
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(SHAPE, shapeFor(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public void onPlace(BlockState state, net.minecraft.world.level.Level level,
                        BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            BlockState fixed = state.setValue(SHAPE, shapeFor(level, pos));
            if (fixed != state) {
                level.setBlock(pos, fixed, Block.UPDATE_ALL);
            }
        }
    }

    /** 邻居变化时刷新形状。 */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP || direction == Direction.DOWN) {
            return state.setValue(SHAPE, shapeFor(level, pos));
        }
        return state;
    }

    /** 自动恢复成原本的墙壁。 */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, ModBlocks.WALL_YELLOW.get().defaultBlockState(), Block.UPDATE_ALL);
        refresh(level, pos.above());
        refresh(level, pos.below());
    }

    /** 刷新一个相邻的刮纸墙形状。 */
    private static void refresh(ServerLevel level, BlockPos p) {
        BlockState st = level.getBlockState(p);
        if (st.getBlock() instanceof OakedWallBlock) {
            BlockState updated = st.setValue(SHAPE, shapeFor(level, p));
            if (updated != st) {
                level.setBlock(p, updated, Block.UPDATE_ALL);
            }
        }
    }
}
