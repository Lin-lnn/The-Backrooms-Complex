package com.the_backrooms_complex.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 铺在地上的墙灰。
 *
 * <p>外观是<b>没有被连接的红石粉</b>（灰白色贴图），但<b>不传递任何红石信号</b> ——
 * 它只是装饰，没有红石元件的任何行为。玩家可以像铺红石粉那样把它放在地上。</p>
 */
public class WallDustLayerBlock extends Block {

    /** 贴地薄层碰撞箱（高 1 像素）。 */
    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.0625D, 1.0D);

    public WallDustLayerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** 不完整方块：不阻挡光照传播。 */
    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** 不是红石导体，也不输出信号。 */
    @Override
    protected boolean isSignalSource(BlockState state) {
        return false;
    }
}
