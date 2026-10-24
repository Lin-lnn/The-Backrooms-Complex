package com.the_backrooms_complex.item;

import com.the_backrooms_complex.ModBlocks;
import com.the_backrooms_complex.ModConfig;
import com.the_backrooms_complex.block.OakedWallBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 墙纸：从 Level 0 墙壁上刮下来的黄色墙纸。
 *
 * <h2>用途</h2>
 * <ul>
 *   <li><b>贴回墙面</b>：对着「剥去墙纸的墙壁」（{@code wall_oaked}）右键，
 *       把它恢复成原来的墙壁主体，并消耗 1 张墙纸；</li>
 *   <li>可作燃料，燃烧时间见 {@link ModConfig#wallpaperBurnTicks}；</li>
 *   <li>可替代纸：本物品被加入 NeoForge 通用标签 {@code c:paper}。</li>
 * </ul>
 */
public class WallpaperItem extends Item {

    public WallpaperItem(Properties properties) {
        super(properties);
    }

    /**
     * 对着「剥去墙纸的墙壁」右键 → 贴回墙纸，方块恢复为原始墙壁。
     *
     * <p>创造模式不消耗墙纸。</p>
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof OakedWallBlock)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            OakedWallBlock.Variant variant = state.getValue(OakedWallBlock.VARIANT);
            var restored = ModBlocks.originalWallOf(variant);
            level.setBlockAndUpdate(pos, restored.defaultBlockState());
            level.playSound(null, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER,
                    SoundSource.BLOCKS, 0.7F, 1.2F);
            var player = context.getPlayer();
            if (player != null && !player.isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return ModConfig.wallpaperBurnTicks;
    }
}
