package com.yourname.backrooms_complex.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 荧光灯方块。
 *
 * <p>发出最高等级（15）的光照。在后室维度中，由于没有天空光（hasSkyLight=false），
 * 所有照明都依赖这些荧光灯。</p>
 *
 * <p>说明：在 1.21.1 中设置光照有两种等价方式——</p>
 * <ol>
 *   <li>方块属性：{@code BlockBehaviour.Properties.lightLevel(state -> 15)}（推荐，已在注册时使用）；</li>
 *   <li>重写本类的 {@link #getLightEmission(BlockState, BlockGetter, BlockPos)} 返回 15。</li>
 * </ol>
 * <p>本模组两者都实现了（返回一致，无冲突），确保光照引擎稳定生效。</p>
 */
public class FluorescentLampBlock extends Block {

    public FluorescentLampBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 15;
    }
}
