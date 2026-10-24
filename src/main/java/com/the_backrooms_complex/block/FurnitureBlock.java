package com.the_backrooms_complex.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 家具方块基类。
 *
 * <p>碰撞箱由 {@link FurnitureShapes} 按模型逐元素生成，是一个<b>复合几何体</b>，
 * 能比较精确地包住模型外形（而不是一整块 16³ 的实心盒）。</p>
 *
 * <p>{@code climbable = true} 时（梯子、篱笆）玩家贴着它按前进即可上下攀爬。</p>
 */
public class FurnitureBlock extends Block {

    /** 供 {@link FurnitureShapes} 查询的 id（= 方块注册名）。 */
    protected final String furnitureId;

    /** 是否可攀爬。 */
    private final boolean climbable;

    public FurnitureBlock(Properties properties, String furnitureId) {
        this(properties, furnitureId, false);
    }

    public FurnitureBlock(Properties properties, String furnitureId, boolean climbable) {
        super(properties);
        this.furnitureId = furnitureId;
        this.climbable = climbable;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return FurnitureShapes.get(this.furnitureId);
    }

    /** 光照遮挡用同一个复合形状，避免整块挡光。 */
    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    /** 梯子/篱笆可攀爬。 */
    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return this.climbable;
    }
}