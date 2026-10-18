package com.the_backrooms_complex.item;

import com.the_backrooms_complex.ModConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

/**
 * 墙纸：从 Level 0 墙壁上刮下来的黄色墙纸。
 *
 * <p>可作燃料，燃烧时间见 {@link ModConfig#wallpaperBurnTicks} —— 默认 25 tick，
 * 相当于 {@code 0.125} 个物品（1 个物品 = 200 tick，例如煤炭 1600 tick 烧 8 个）。</p>
 *
 * <p>可替代纸：本物品被加入 NeoForge 通用标签 {@code c:paper}
 * （{@code data/c/tags/item/paper.json}），因此所有以 {@code c:paper} 为材料的合成配方都能用它。
 * 原版配方直接以 {@code minecraft:paper} 这个物品为材料（而非标签），
 * 若要让原版配方也接受墙纸，需要按 ID 覆盖对应的原版配方 JSON。</p>
 */
public class WallpaperItem extends Item {

    public WallpaperItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return ModConfig.wallpaperBurnTicks;
    }
}
