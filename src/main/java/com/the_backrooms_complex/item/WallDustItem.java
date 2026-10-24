package com.the_backrooms_complex.item;

import com.the_backrooms_complex.ModBlocks;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * 墙灰：可以像红石粉一样<b>铺在地上</b>（铺下后是 {@code wall_dust_layer} 方块）。
 *
 * <p>铺出来的墙灰只是装饰，<b>不传递红石信号</b>。</p>
 */
public class WallDustItem extends Item {

    public WallDustItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, ModBlocks.WALL_DUST_LAYER.get().defaultBlockState());
            level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
            var player = context.getPlayer();
            if (player != null && !player.isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}