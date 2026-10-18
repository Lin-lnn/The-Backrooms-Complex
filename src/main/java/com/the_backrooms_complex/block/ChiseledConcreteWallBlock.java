package com.the_backrooms_complex.block;

import com.mojang.serialization.MapCodec;
import com.the_backrooms_complex.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 「被凿破的混凝土墙壁」。
 *
 * <p>由 {@link com.the_backrooms_complex.item.ChiselItem 凿子} 右键混凝土墙壁时产生。
 * 经过 {@code ModConfig.wallpaperRestoreSeconds}（默认 60）秒后，由 {@link #tick}
 * 自动恢复成普通的 {@code wall_concrete}。</p>
 *
 * <p>与刮纸刀的墙纸复原使用同一套机制：{@code ServerLevel.scheduleTick} 排定的方块 tick
 * <b>会写进区块存档</b>，因此重登服务器后计时依然有效。若期间该方块被替换或破坏，
 * 引擎会因为方块不匹配而跳过这次 tick，也就不会误恢复。</p>
 */
public class ChiseledConcreteWallBlock extends Block {

    public static final MapCodec<ChiseledConcreteWallBlock> CODEC =
            simpleCodec(ChiseledConcreteWallBlock::new);

    public ChiseledConcreteWallBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /** 到期自动恢复成混凝土墙壁。 */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, ModBlocks.WALL_CONCRETE.get().defaultBlockState(), Block.UPDATE_ALL);
    }
}
