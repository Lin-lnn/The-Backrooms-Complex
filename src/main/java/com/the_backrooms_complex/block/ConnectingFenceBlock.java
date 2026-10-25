package com.the_backrooms_complex.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 我们的栅栏变体。
 *
 * <p>只与栅栏相连，<b>不与墙相连</b> —— 早先为了让栅栏能贴墙而覆写了连接判定，
 * 结果墙和栅栏会互相吸附，两种结构混在一起。现在显式限制为「仅栅栏」。</p>
 */
public class ConnectingFenceBlock extends FenceBlock {

    public ConnectingFenceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean connectsTo(BlockState state, boolean isSideSolid, Direction direction) {
        // 只连栅栏；墙不会与本方块相连
        return state.getBlock() instanceof FenceBlock && super.connectsTo(state, isSideSolid, direction);
    }
}
