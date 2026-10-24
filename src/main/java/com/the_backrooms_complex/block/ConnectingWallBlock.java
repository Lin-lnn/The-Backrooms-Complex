package com.the_backrooms_complex.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;

/**
 * 可以与「栅栏」互相连接的墙。
 *
 * <p>原版 {@code WallBlock.connectsTo} 是 <b>private</b> 的，无法通过继承改写，
 * 所以墙永远不会主动连栅栏。这里改用另一种办法：在<b>邻居变化 / 放置后</b>
 * 手动检查四邻，如果邻居是栅栏就把对应方向的墙侧属性设为 {@link WallSide#LOW}，
 * 从而让墙向栅栏凸出 —— 做到<b>双向连接</b>。</p>
 */
public class ConnectingWallBlock extends WallBlock {

    public ConnectingWallBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            fixFenceSides(level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   net.minecraft.world.level.block.Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide()) {
            fixFenceSides(level, pos);
        }
    }

    /**
     * 检查四邻：若邻居是栅栏，就把本方块对应方向的墙侧设为 LOW。
     *
     * <p>只做"补上"，不清除其他方向（其他方向仍由原版逻辑维护）。</p>
     */
    private void fixFenceSides(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ConnectingWallBlock)) {
            return;
        }
        BlockState updated = state;
        for (Direction direction : new Direction[]{
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.getBlock() instanceof FenceBlock) {
                var prop = switch (direction) {
                    case NORTH -> NORTH_WALL;
                    case SOUTH -> SOUTH_WALL;
                    case EAST -> EAST_WALL;
                    default -> WEST_WALL;
                };
                if (updated.getValue(prop) == WallSide.NONE) {
                    updated = updated.setValue(prop, WallSide.LOW);
                }
            }
        }
        if (updated != state) {
            level.setBlockAndUpdate(pos, updated);
        }
    }
}
