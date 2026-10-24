package com.the_backrooms_complex.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;


/**
 * 可坐的家具（椅子、凳子）。
 *
 * <p>右键时把玩家<b>传送到家具表面</b>并让他保持坐在原地的状态（不骑乘实体，
 * 因此不需要额外的 EntityType，也不会有实体残留问题）。玩家潜行或移动时会自动站起。</p>
 */
public class SeatBlock extends RotatableFurnitureBlock {

    /** 座位高度（方块坐标），按模型顶面估算。 */
    private final double seatHeight;

    public SeatBlock(Properties properties, String furnitureId, double seatHeight) {
        super(properties, furnitureId);
        this.seatHeight = seatHeight;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        // 站到座位上：位置对齐方块中心，Y = 方块顶面 + 座位高度
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + this.seatHeight;
        double z = pos.getZ() + 0.5D;
        player.teleportTo(x, y, z);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.hasImpulse = true;
        return InteractionResult.CONSUME;
    }

}
