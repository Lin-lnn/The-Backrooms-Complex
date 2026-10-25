package com.the_backrooms_complex.block;

import net.minecraft.world.level.block.PipeBlock;

/**
 * 黄铜管道。
 *
 * <p>沿用原版 {@link PipeBlock} 的六向连接逻辑（与紫颂植物同一套）：
 * 每个方向一个布尔属性，相邻有管道就自动连上。只负责外观，没有输送功能。</p>
 *
 * <p>连接判定直接用基类默认实现（同类方块才连），因此不需要重写
 * {@code canConnectTo}，只需补上粒子半径参数与 {@code codec()}。</p>
 */
public class BrassPipeBlock extends PipeBlock {

    public BrassPipeBlock(Properties properties) {
        super(0.25F, properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends PipeBlock> codec() {
        return simpleCodec(BrassPipeBlock::new);
    }
}