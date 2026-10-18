package com.the_backrooms_complex.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 「潮湿的天花板」。
 *
 * <p>通过<b>方块随机刻</b>在天花板正下方生成原版「滴水」粒子
 * （{@link ParticleTypes#DRIPPING_WATER}），使水珠<b>紧贴天花板/墙面</b>出现并向下落，
 * 而不是像群系环境粒子那样随机悬浮在半空中。</p>
 *
 * <p>相比群系粒子的优势：</p>
 * <ul>
 *   <li>位置精确 —— 永远从天花板底面往下滴；</li>
 *   <li>渲染正常 —— 带位置上下文，不会退化成缺失贴图的方块；</li>
 *   <li>只在本方块附近出现，天然限定在潮湿走廊。</li>
 * </ul>
 */
public class DampCeilingBlock extends Block {

    public DampCeilingBlock(Properties properties) {
        super(properties);
    }

    /** 需要随机刻才能产生滴水。 */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 约 2/3 的随机刻都会滴水（比原来 1/6 密集 4 倍），营造潮湿走廊的水汽感
        if (random.nextInt(3) == 0) {
            return;
        }
        // 在天花板底面中心稍下方生成，横向轻微散布，让水滴看起来是从缝隙渗出
        level.sendParticles(ParticleTypes.DRIPPING_WATER,
                pos.getX() + 0.5, pos.getY() - 0.05, pos.getZ() + 0.5,
                2, 0.45, 0.0, 0.45, 0.0);
    }
}
