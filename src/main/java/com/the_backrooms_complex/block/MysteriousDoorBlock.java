package com.the_backrooms_complex.block;

import com.the_backrooms_complex.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 「色泽灰暗的神秘的门」。
 *
 * <p>外观是<b>染成暗灰色的深色橡木门</b>，但它<b>不能打开</b> ——
 * 右键时会直接把玩家传送到 <b>Level 1「宜居地带」</b>。</p>
 *
 * <p>传送落点会在目标层级找一个安全位置（脚下是实心、身体位置是空气），
 * 避免卡进墙里。</p>
 */
public class MysteriousDoorBlock extends Block {

    /** 传送目标层级的基准高度（层 0 的地板）。 */
    private static final int TARGET_Y = 1;

    public MysteriousDoorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ServerLevel target = serverPlayer.server.getLevel(ModDimensions.LEVEL_1_WORLD);
        if (target == null) {
            return InteractionResult.PASS;
        }
        // 在目标层级的同一 XZ 附近找一个能站的位置
        BlockPos base = new BlockPos(pos.getX(), TARGET_Y, pos.getZ());
        BlockPos landing = findLanding(target, base);
        if (landing == null) {
            return InteractionResult.PASS;
        }
        serverPlayer.teleportTo(target,
                landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D,
                serverPlayer.getYRot(), serverPlayer.getXRot());
        return InteractionResult.CONSUME;
    }

    /** 水平搜索半径（格）。 */
    private static final int SEARCH_RADIUS = 32;
    /** 垂直搜索范围（格，向上/向下各这么多）。 */
    private static final int SEARCH_HEIGHT = 48;

    /**
     * 以 base 为中心，在 <b>±{@value #SEARCH_RADIUS} 格的水平范围</b>、
     * <b>±{@value #SEARCH_HEIGHT} 格的垂直范围</b>内，螺旋式寻找
     * 「脚下实心 + 身体与头部为空气」的安全落点。
     *
     * <p>先搜中心附近（距离短的优先），保证落点尽量靠近门在 Level 0 的位置。</p>
     */
    private static BlockPos findLanding(ServerLevel level, BlockPos base) {
        // 按"距离平方"从小到大枚举偏移，找到即返回
        for (int r = 0; r <= SEARCH_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // 只处理"恰好在这一圈"的位置，避免重复扫描
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int dy = 0; dy <= SEARCH_HEIGHT; dy++) {
                        for (int sign = 0; sign <= 1; sign++) {
                            int y = base.getY() + (sign == 0 ? dy : -dy);
                            if (y < level.getMinBuildHeight() + 1 || y > level.getMaxBuildHeight() - 3) {
                                continue;
                            }
                            BlockPos feet = new BlockPos(base.getX() + dx, y, base.getZ() + dz);
                            if (level.getBlockState(feet.below()).isSolidRender(level, feet.below())
                                    && level.getBlockState(feet).isAir()
                                    && level.getBlockState(feet.above()).isAir()) {
                                return feet;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
