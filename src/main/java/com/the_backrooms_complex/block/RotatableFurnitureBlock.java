package com.the_backrooms_complex.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 可旋转放置的家具（椅子、凳子）。
 *
 * <p>放置时朝向玩家的反方向，右键不会旋转（避免与"坐下"冲突）。
 * 模型靠 blockstates 里的 {@code y} 旋转，碰撞箱同步按 90° 旋转，保证永远包住模型。</p>
 */
public class RotatableFurnitureBlock extends FurnitureBlock {

    /** 朝向属性。 */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public RotatableFurnitureBlock(Properties properties, String furnitureId) {
        super(properties, furnitureId);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape base = FurnitureShapes.get(this.furnitureIdOf());
        return rotate(base, state.getValue(FACING));
    }

    /** 取本方块在 {@link FurnitureShapes} 里登记的名字。 */
    private String furnitureIdOf() {
        return this.furnitureId;
    }

    /**
     * 把形状按朝向旋转（只处理 90° 的整数倍，直接交换 X/Z 即可）。
     */
    private static VoxelShape rotate(VoxelShape shape, Direction facing) {
        if (facing == Direction.NORTH) {
            return shape;
        }
        // 用逐盒重建的方式旋转，保持形状为轴对齐盒的并集
        final VoxelShape[] acc = {Shapes.empty()};
        shape.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
            double nx0;
            double nz0;
            double nx1;
            double nz1;
            switch (facing) {
                case SOUTH -> {   // 180°
                    nx0 = 1 - x1;
                    nx1 = 1 - x0;
                    nz0 = 1 - z1;
                    nz1 = 1 - z0;
                }
                case EAST -> {    // 顺时针 90°
                    nx0 = 1 - z1;
                    nx1 = 1 - z0;
                    nz0 = x0;
                    nz1 = x1;
                }
                default -> {      // WEST
                    nx0 = z0;
                    nx1 = z1;
                    nz0 = 1 - x1;
                    nz1 = 1 - x0;
                }
            }
            acc[0] = Shapes.joinUnoptimized(acc[0],
                    Block.box(nx0 * 16, y0 * 16, nz0 * 16, nx1 * 16, y1 * 16, nz1 * 16),
                    BooleanOp.OR);
        });
        return acc[0];
    }
}
