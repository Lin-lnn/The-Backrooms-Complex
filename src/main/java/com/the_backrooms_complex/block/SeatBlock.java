package com.the_backrooms_complex.block;

import com.the_backrooms_complex.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 可坐的家具（椅子、凳子）。
 *
 * <p>右键时在家具上方生成一个不可见的 {@link com.the_backrooms_complex.entity.SeatEntity}
 * 并让玩家骑乘上去 —— 与坐矿车 / 船是同一套机制：位置固定、不会下坠、潜行即可起身。</p>
 */
public class SeatBlock extends RotatableFurnitureBlock {

    /** 座位高度（方块坐标）。 */
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
        // 先看这个位置是否已经有座位实体（避免叠加）
        var seats = level.getEntitiesOfClass(com.the_backrooms_complex.entity.SeatEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(0.6D));
        com.the_backrooms_complex.entity.SeatEntity seat;
        if (!seats.isEmpty()) {
            seat = seats.get(0);
        } else {
            seat = ModEntities.SEAT.get().create(level);
            if (seat == null) {
                return InteractionResult.PASS;
            }
            seat.place(pos, this.seatHeight);
            level.addFreshEntity(seat);
        }
        // 真正的坐下：让玩家骑上去
        player.startRiding(seat, true);
        return InteractionResult.CONSUME;
    }
}