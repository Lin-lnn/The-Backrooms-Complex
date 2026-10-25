package com.the_backrooms_complex.block;

import com.the_backrooms_complex.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 「色泽灰暗的神秘的门」。
 *
 * <p>它是<b>真正的两格方块门</b>（继承 {@link DoorBlock}）：放下下半时自动生成上半，
 * 破坏任意一半都会整体消失，与原版门完全一致。</p>
 *
 * <p>但它<b>不会打开</b> —— 右键时直接把玩家传送到 <b>Level 1「宜居地带」</b>的固定落点，
 * 而不是切换 {@code open} 状态。</p>
 */
public class MysteriousDoorBlock extends DoorBlock {

    /**
     * Level 1 的<b>固定安全出生点</b>。
     *
     * <p>选 {@code (2, 1, 2)} 的原因：</p>
     * <ul>
     *   <li>小径的墙体按 {@code x % 5 == 0} / {@code z % 5 == 0} 生成，
     *       {@code 2 % 5 = 2} ⇒ <b>一定不是小径墙</b>；</li>
     *   <li>过道的巨柱在 48 格网格的 {@code (22..25, 22..25)} 处，
     *       {@code (2, 2)} <b>不在柱子上</b>；</li>
     *   <li>第 0 层地板在 {@code Y = 0}，所以站在 {@code Y = 1}。</li>
     * </ul>
     * <p>⇒ 无论该处是过道还是小径，这个点<b>必然是空旷的</b>。</p>
     */
    public static final BlockPos LANDING = new BlockPos(2, 1, 2);

    public MysteriousDoorBlock(Properties properties) {
        super(BlockSetType.DARK_OAK, properties);
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
        // 传送到 Level 1 的固定出生点。
        // 用 target.getSharedSpawnPos() 作为落点：让服务器自己加载该区块，
        // 避免在未加载区块上逐格探测（那会同步生成大量区块并卡死服务器）。
        // 用固定安全点，而不是 getSharedSpawnPos()（那个点可能卡在柱子里）
        serverPlayer.teleportTo(target,
                LANDING.getX() + 0.5D, LANDING.getY(), LANDING.getZ() + 0.5D,
                serverPlayer.getYRot(), serverPlayer.getXRot());
        return InteractionResult.CONSUME;
    }
}
