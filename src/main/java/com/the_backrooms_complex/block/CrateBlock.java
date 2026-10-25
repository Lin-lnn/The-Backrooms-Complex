package com.the_backrooms_complex.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 板条箱。
 *
 * <p>跃金段里自然生成的箱子带 {@code natural = true}：一旦附近没有玩家，
 * 它就会悄悄消失 —— 对应「把握机会搜集」的设定。玩家自己放的箱子是
 * {@code natural = false}，永远不会消失。</p>
 */
public class CrateBlock extends Block {

    /** 是否自然生成（只有自然生成的箱子会消失）。 */
    public static final BooleanProperty NATURAL = BooleanProperty.create("natural");

    /** 消失判定半径（格）。 */
    public static final double WATCH_RADIUS = 24.0D;

    public CrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(NATURAL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NATURAL);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 玩家放置的箱子：不受影响
        if (!state.getValue(NATURAL)) {
            return;
        }
        // 附近有玩家 => 视为「被观察」，箱子留下
        Player witness = level.getNearestPlayer(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                WATCH_RADIUS, false);
        if (witness == null) {
            level.removeBlock(pos, false);
        }
    }
}
