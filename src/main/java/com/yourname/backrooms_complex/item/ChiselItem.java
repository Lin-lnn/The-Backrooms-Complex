package com.yourname.backrooms_complex.item;

import com.yourname.backrooms_complex.ModBlocks;
import com.yourname.backrooms_complex.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import com.yourname.backrooms_complex.ModConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.yourname.backrooms_complex.DropHelper;

/**
 * 凿子：右键「混凝土墙壁」把它凿破成「被凿破的混凝土墙壁」。
 *
 * <p>行为：</p>
 * <ul>
 *   <li>只对 {@code backrooms_complex:wall_concrete} 生效，其它方块返回 {@code PASS}；</li>
 *   <li>方块替换为 {@code chiseled_concrete_wall}；</li>
 *   <li>播放原版<b>破坏石头</b>音效（{@link SoundEvents#STONE_BREAK}）；</li>
 *   <li>掉落 1~3 个「墙灰」；</li>
 *   <li>每次消耗 1 点耐久（上限 64）。</li>
 * </ul>
 */
public class ChiselItem extends Item {

    public ChiselItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // 只对混凝土墙壁生效
        if (!state.is(ModBlocks.WALL_CONCRETE.get())) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        // 1) 方块变成被凿破的状态
        serverLevel.setBlock(pos, ModBlocks.CHISELED_CONCRETE_WALL.get().defaultBlockState(), Block.UPDATE_ALL);

        // 2) 排定 60 秒后自动复原（方块 tick 会写入区块存档）
        int restoreDelay = Math.max(1, ModConfig.wallpaperRestoreSeconds) * 20;
        serverLevel.scheduleTick(pos, ModBlocks.CHISELED_CONCRETE_WALL.get(), restoreDelay);

        // 3) 原版"破坏石头"音效
        serverLevel.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 0.9F);

        // 3) 掉落 1~3 个墙灰
        int count = 1 + serverLevel.getRandom().nextInt(3);
        DropHelper.dropFacing(serverLevel, pos, context.getPlayer(), new ItemStack(ModItems.WALL_DUST.get(), count));

        // 4) 消耗 1 点耐久
        Player player = context.getPlayer();
        if (player != null) {
            context.getItemInHand().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }

        return InteractionResult.SUCCESS;
    }
}
