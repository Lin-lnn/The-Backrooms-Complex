package com.yourname.backrooms_complex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 掉落物摆放辅助。
 *
 * <p>原版 {@code Block.popResource} 会在方块中心生成掉落物，并附带随机初速，
 * 导致刮纸刀 / 凿子产出的物品"乱飞"。这里改为<b>贴着交互方块的某一面</b>生成、
 * 且初速严格为 0，于是物品会"掉在方块旁边"而不是四处弹开。</p>
 *
 * <p>面的选择：优先取<b>朝向交互者</b>的那一面；交互者未知时退回方块上方。</p>
 */
public final class DropHelper {

    /** 生成偏移：略大于半格，让物品贴在方块外表面。 */
    private static final double OFFSET = 0.55;

    /**
     * 在方块朝向 {@code who} 的那一面外侧生成掉落物，初速为 0。
     *
     * @param level 服务端世界
     * @param pos   交互的方块
     * @param who   交互者（可为 null）
     * @param stack 掉落物
     */
    public static void dropFacing(ServerLevel level, BlockPos pos, Entity who, ItemStack stack) {
        Direction face = faceToward(pos, who);
        Vec3 center = Vec3.atCenterOf(pos);
        double x = center.x + face.getStepX() * OFFSET;
        double y = center.y + face.getStepY() * OFFSET;
        double z = center.z + face.getStepZ() * OFFSET;

        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        // 初速为 0：不飞、不弹，就落在方块旁边
        entity.setDeltaMovement(0.0, 0.0, 0.0);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    /** 计算从方块指向交互者的方向；交互者为 null 时返回 UP。 */
    private static Direction faceToward(BlockPos pos, Entity who) {
        if (who == null) {
            return Direction.UP;
        }
        double dx = who.getX() - (pos.getX() + 0.5);
        double dy = who.getY() + who.getEyeHeight() - (pos.getY() + 0.5);
        double dz = who.getZ() - (pos.getZ() + 0.5);
        // 水平方向优先，避免玩家站着俯视时物品跑到头顶
        if (Math.abs(dx) >= Math.abs(dz) && Math.abs(dx) >= Math.abs(dy)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        if (Math.abs(dz) >= Math.abs(dy)) {
            return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return dy >= 0 ? Direction.UP : Direction.DOWN;
    }

    private DropHelper() {
    }
}