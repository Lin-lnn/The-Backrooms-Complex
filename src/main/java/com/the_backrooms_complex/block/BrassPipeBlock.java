package com.the_backrooms_complex.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 六向自动连接的管道。
 *
 * <p>{@link PipeBlock} 只提供属性表与碰撞形状，<b>三个关键动作都要子类自己写</b>，
 * 否则管道放下后六个方向全是 false，看起来像一根孤零零的短棒：</p>
 * <ol>
 *   <li>{@link #createBlockStateDefinition} —— 注册六个方向属性；</li>
 *   <li>{@link #getStateForPlacement} —— <b>放置瞬间</b>就按邻居决定连接方向；</li>
 *   <li>{@link #updateShape} —— 邻居变化时（放下/拆掉另一根管）刷新自己的连接。</li>
 * </ol>
 *
 * <p>与「机械动力」的黄铜管道行为一致：同类互连、转角自动成形，但没有输送功能。</p>
 */
public class BrassPipeBlock extends PipeBlock {

    public BrassPipeBlock(Properties properties) {
        super(0.25F, properties);
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, Boolean.FALSE);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
            builder.add(property);
        }
    }

    /**
     * 放置瞬间按邻居决定六个方向：邻居也是管道就连，否则断开。
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            BlockState neighbour = context.getLevel()
                    .getBlockState(context.getClickedPos().relative(direction));
            if (canConnectTo(neighbour)) {
                state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), Boolean.TRUE);
            }
        }
        return state;
    }

    /**
     * 邻居变化时刷新：某方向通向的方块失去连接，就把该方向断开；反之接上。
     */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction,
                                     BlockState neighbourState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        boolean connected = canConnectTo(neighbourState);
        BooleanProperty property = PROPERTY_BY_DIRECTION.get(direction);
        if (state.getValue(property) != connected) {
            return state.setValue(property, connected);
        }
        return state;
    }

    /** 什么方块算「可连接」：本类管道。 */
    private static boolean canConnectTo(BlockState state) {
        return state.getBlock() instanceof BrassPipeBlock;
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends PipeBlock> codec() {
        return simpleCodec(BrassPipeBlock::new);
    }
}
