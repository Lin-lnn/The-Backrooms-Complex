package com.the_backrooms_complex.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 会随上下邻方块变化而换贴图的墙壁（用于「刮去墙纸的墙壁」的过渡）。
 *
 * <p>两个布尔属性：</p>
 * <ul>
 *   <li>{@link #SCRAPED_ABOVE} —— 上方是「刮去墙纸的墙壁」⇒ 用"上半部分刮去墙纸"的贴图；</li>
 *   <li>{@link #SCRAPED_BELOW} —— 下方是「刮去墙纸的墙壁」⇒ 用"下半部分刮去墙纸"的贴图。</li>
 * </ul>
 *
 * <p>两个都为真时优先使用"上半"贴图（视觉上更自然）。</p>
 */
public class TransitionWallBlock extends Block {

    /** 上方是刮去墙纸的墙壁。 */
    public static final BooleanProperty SCRAPED_ABOVE = BooleanProperty.create("scraped_above");
    /** 下方是刮去墙纸的墙壁。 */
    public static final BooleanProperty SCRAPED_BELOW = BooleanProperty.create("scraped_below");

    public static final MapCodec<TransitionWallBlock> CODEC = simpleCodec(TransitionWallBlock::new);

    public TransitionWallBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(SCRAPED_ABOVE, false)
                .setValue(SCRAPED_BELOW, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SCRAPED_ABOVE, SCRAPED_BELOW);
    }

    /**
     * 判断某个方块是不是「刮去墙纸的墙壁」。
     *
     * <p>用类名判断而不是 {@code instanceof}，避免与 <b>com.the_backrooms_complex.block</b>
     * 的其它内部类产生循环依赖。</p>
     */
    private static boolean isScraped(BlockState state) {
        return state.getBlock() instanceof OakedWallBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP) {
            return state.setValue(SCRAPED_ABOVE, isScraped(neighborState));
        }
        if (direction == Direction.DOWN) {
            return state.setValue(SCRAPED_BELOW, isScraped(neighborState));
        }
        return state;
    }

    /** 供放置时初始化（放置后 updateShape 也会补正）。 */
    public static BlockState updateBoth(BlockState state, BlockGetter level, BlockPos pos) {
        return state
                .setValue(SCRAPED_ABOVE, isScraped(level.getBlockState(pos.above())))
                .setValue(SCRAPED_BELOW, isScraped(level.getBlockState(pos.below())));
    }
}
