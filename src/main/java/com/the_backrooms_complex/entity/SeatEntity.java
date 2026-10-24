package com.the_backrooms_complex.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 「座位」实体：让玩家真正<b>坐</b>在家具上（与原版矿车 / 船同级的乘坐体验）。
 *
 * <h2>为什么需要它</h2>
 * <p>仅靠"把玩家传送到座位上"不是真正的坐下 ✗ —— 重力会把玩家拉下来，视角也不会被锁定。</p>
 *
 * <p>这里用一个<b>不可见、无碰撞、无重力</b>的实体作为载具，玩家 {@code startRiding} 到它身上，
 * 于是：</p>
 * <ul>
 *   <li>玩家坐在固定高度，不会下坠；</li>
 *   <li>按潜行即可下车，和矿车一致；</li>
 *   <li>实体被下车后自动移除，不会残留。</li>
 * </ul>
 */
public class SeatEntity extends Entity {

    /** 座位相对方块底面的高度（格）。 */
    private double seatHeight = 0.5D;

    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;      // 不受重力
        this.setNoGravity(true);
    }

    /** 放置到指定方块、指定座位高度。 */
    public void place(BlockPos pos, double height) {
        this.seatHeight = height;
        this.setPos(pos.getX() + 0.5D, pos.getY() + height, pos.getZ() + 0.5D);
    }

    @Override
    public void tick() {
        super.tick();
        // 坐姿锁定：速度归零，位置保持在座位上
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.level().isClientSide() && this.getPassengers().isEmpty()) {
            this.discard();   // 没人坐就消失
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // 不需要同步任何数据
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.seatHeight = tag.getDouble("SeatHeight");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("SeatHeight", this.seatHeight);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false;   // 永远不渲染
    }
}
