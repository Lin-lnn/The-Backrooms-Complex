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

    /** 向下/向上各找 8 格，返回一个脚下实心、身体为空的位置。 */
    private static BlockPos findLanding(ServerLevel level, BlockPos base) {
        for (int dy = 0; dy <= 8; dy++) {
            for (int sign = 0; sign <= 1; sign++) {
                int y = base.getY() + (sign == 0 ? dy : -dy);
                BlockPos feet = new BlockPos(base.getX(), y, base.getZ());
                if (y < level.getMinBuildHeight() + 1 || y > level.getMaxBuildHeight() - 3) {
                    continue;
                }
                if (level.getBlockState(feet.below()).isSolidRender(level, feet.below())
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) {
                    return feet;
                }
            }
        }
        return null;
    }
}
