package com.the_backrooms_complex.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 黄铜管道。
 *
 * <p>沿用原版 {@link PipeBlock} 的六向连接逻辑（与紫颂植物同一套）：
 * 每个方向一个布尔属性，相邻有管道就自动连上。只负责外观，没有输送功能。</p>
 *
 * <p>注意：{@code PipeBlock} 只是提供了属性表与形状表，<b>六个属性必须由子类
 * 自己注册</b>（原版 {@code ChorusPlantBlock} 也是这么做的），否则
 * {@code PipeBlock#getShape} 取属性时会抛 IllegalArgumentException。</p>
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

    @Override
    protected com.mojang.serialization.MapCodec<? extends PipeBlock> codec() {
        return simpleCodec(BrassPipeBlock::new);
    }
}