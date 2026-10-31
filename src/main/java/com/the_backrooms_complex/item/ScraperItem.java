package com.the_backrooms_complex.item;

import com.the_backrooms_complex.ModBlocks;
import com.the_backrooms_complex.ModConfig;
import com.the_backrooms_complex.ModItems;
import com.the_backrooms_complex.block.OakedWallBlock;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.the_backrooms_complex.DropHelper;

/**
 * 刮纸刀：右键 Level 0 的黄色墙壁<b>主体</b>（{@code wall_yellow}），刮掉墙纸。
 *
 * <p>行为：</p>
 * <ul>
 *   <li>方块变成 {@code backrooms_complex:wall_oaked}（去墙纸贴图），并记住原来的墙种；</li>
 *   <li>掉落 1 个墙纸；</li>
 *   <li>排定 {@link ModConfig#wallpaperRestoreSeconds} 秒后自动恢复；</li>
 *   <li>踢脚线方块（{@code wall_*_plinth}）不在可刮范围内 —— 它们不是墙壁主体，直接 PASS；</li>
 *   <li>已经是"去墙纸"状态的墙壁再次右键无任何效果；</li>
 *   <li>默认不消耗耐久，可用 {@link ModConfig#scraperUsesDurability} 打开。</li>
 * </ul>
 */
public class ScraperItem extends Item {

    public ScraperItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // 只有黄色墙壁主体能被刮；踢脚线、去墙纸状态、其它方块一律 PASS
        if (!state.is(ModBlocks.WALL_YELLOW.get())) {
            return InteractionResult.PASS;
        }

        // 客户端只负责挥手动画，不产生实际改动
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        // 1) 一次性写入去墙纸状态：变种 + 形状（形状由上下邻居决定）
        //    原先分两次 setBlock，会多做一次方块更新与光照刷新。
        serverLevel.setBlock(pos,
                ModBlocks.WALL_OAKED.get().defaultBlockState()
                        .setValue(OakedWallBlock.SHAPE, OakedWallBlock.shapeFor(serverLevel, pos)),
                Block.UPDATE_ALL);

        // 2) 排定自动恢复（方块 tick 会持久化到区块存档里，重登服务器依然有效）
        int delay = Math.max(1, ModConfig.wallpaperRestoreSeconds) * 20;
        // 上下相邻的刮纸墙形状也要跟着变
        refreshTransition(serverLevel, pos.above());
        refreshTransition(serverLevel, pos.below());

        serverLevel.scheduleTick(pos, ModBlocks.WALL_OAKED.get(), delay);

        // 3) 掉落 1 个墙纸
        DropHelper.dropFacing(serverLevel, pos, context.getPlayer(), new ItemStack(ModItems.WALLPAPER.get()));

        // 4) 音效
        serverLevel.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 0.6F, 1.4F);

        // 5) 可选的耐久消耗（默认关闭）
        Player player = context.getPlayer();
        if (ModConfig.scraperUsesDurability && player != null) {
            context.getItemInHand().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }

        return InteractionResult.SUCCESS;
    }
    /** 刷新一个相邻的刮纸墙形状（若不是刮纸墙则什么都不做）。 */
    private static void refreshTransition(ServerLevel level, BlockPos p) {
        BlockState st = level.getBlockState(p);
        if (st.getBlock() instanceof OakedWallBlock) {
            BlockState updated = st.setValue(OakedWallBlock.SHAPE, OakedWallBlock.shapeFor(level, p));
            if (updated != st) {
                level.setBlock(p, updated, Block.UPDATE_ALL);
            }
        }
    }

}