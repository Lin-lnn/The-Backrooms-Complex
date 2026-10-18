package com.the_backrooms_complex.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 「菌毯」—— 铺在潮湿地板上的一层灰黑色菌丝地毯。
 *
 * <h2>特性</h2>
 * <ul>
 *   <li>形状与地毯完全相同（1~128 层，每层 1 像素）；</li>
 *   <li>硬度 {@value #HARDNESS}，与原版幽匿块一致；</li>
 *   <li>用锄头挖掘更快（由方块标签 {@code mineable/hoe} 提供）；</li>
 *   <li>必须放置在实心方块上。</li>
 * </ul>
 */
public class MyceliumCarpetBlock extends Block {

    /** 硬度：与原版幽匿块一致。 */
    public static final float HARDNESS = 0.2F;
    /** 地毯层数属性（1~128）。 */
    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
    /** 各层数的碰撞/渲染形状。 */
    private static final VoxelShape[] SHAPES = new VoxelShape[]{
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 5.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D)
    };

    public MyceliumCarpetBlock() {
        super(Properties.of()
                .strength(HARDNESS)
                .sound(SoundType.MOSS_CARPET)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false));
        this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS) - 1];
    }

    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}