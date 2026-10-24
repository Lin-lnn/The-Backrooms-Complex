package com.the_backrooms_complex.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 可与「墙」相连的栅栏。
 *
 * <p>原版栅栏只会连接栅栏 / 栅栏门，不会连墙 —— 因此在后室里把栅栏接在墙上时会出现断口。
 * 这里覆写 {@link #connectsTo}（1.21 里连接判定的真正入口），
 * 让栅栏也接受 {@link WallBlock}，从而能和墙连成一体。</p>
 */
public class ConnectingFenceBlock extends FenceBlock {

    public ConnectingFenceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean connectsTo(BlockState state, boolean isSideSolid, Direction direction) {
        // 墙方块也视为可连接
        if (state.getBlock() instanceof WallBlock) {
            return true;
        }
        return super.connectsTo(state, isSideSolid, direction);
    }
}
