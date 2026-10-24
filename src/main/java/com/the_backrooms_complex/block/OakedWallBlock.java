package com.the_backrooms_complex.block;

import com.mojang.serialization.MapCodec;
import com.the_backrooms_complex.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import com.the_backrooms_complex.ModConfig;

/**
 * 「被刮去墙纸」的 Level 0 墙壁方块。
 *
 * <p>由 {@link com.the_backrooms_complex.item.ScraperItem 刮纸刀} 右键墙壁主体方块时产生，
 * 贴图为"墙纸被刮掉后露出的底层墙面"。经过
 * {@link ModConfig#wallpaperRestoreSeconds 配置的秒数}后，
 * 由 {@link #tick} 自动恢复成原本有墙纸的墙壁。</p>
 *
 * <p>用 {@code variant} 属性记录"它原来是哪一面墙"，从而知道该恢复成
 * {@code wall_yellow} / {@code wall_concrete} / {@code wall_damp} 中的哪一个。</p>
 *
 * <p>若期间该方块被替换或破坏，{@link #tick} 会因为方块已经不是它而什么都不做，
 * 因此不会误恢复。</p>
 */
public class OakedWallBlock extends Block {

    /** 对应三种墙壁主体的变体。 */
    public enum Variant implements StringRepresentable {
        YELLOW("yellow"),
        CONCRETE("concrete"),
        DAMP("damp");

        private final String name;

        Variant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<Variant> VARIANT =
            EnumProperty.create("variant", Variant.class);

    public static final MapCodec<OakedWallBlock> CODEC = simpleCodec(OakedWallBlock::new);

    public OakedWallBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, Variant.YELLOW));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    /**
     * 由 {@code ServerLevel.scheduleTick} 排定的恢复逻辑。
     *
     * <p>只有当前方块仍然是"去墙纸"状态时才会执行，因此方块若已被替换/破坏便不会恢复。</p>
     */
    /** 刷新一个相邻的过渡墙。 */
    private static void refresh(net.minecraft.world.level.Level level, BlockPos p) {
        BlockState st = level.getBlockState(p);
        if (st.getBlock() instanceof TransitionWallBlock) {
            BlockState updated = TransitionWallBlock.updateBoth(st, level, p);
            if (updated != st) {
                level.setBlock(p, updated, Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Block original = ModBlocks.originalWallOf(state.getValue(VARIANT));
        if (original != null) {
            level.setBlock(pos, original.defaultBlockState(), Block.UPDATE_ALL);
            // 恢复后，上下相邻的过渡墙也要刷新回普通贴图
            refresh(level, pos.above());
            refresh(level, pos.below());
        }
    }
}