package com.the_backrooms_complex;

import static com.the_backrooms_complex.ModItems.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 两个创造模式物品栏页签：后室综合体 / 家具。
 *
 * <p>从 {@link ModItems} 拆出，让 493 行的注册类只保留字段声明。
 * 注册表本身仍是 {@code ModItems.CREATIVE_MODE_TABS}，
 * 静态导入让它在这里可以直接引用。</p>
 */
public final class ModCreativeTabs {


    // ===== 创造模式物品栏 =====
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BACKROOMS_TAB =
            CREATIVE_MODE_TABS.register("backrooms_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.backrooms_complex"))
                    .icon(() -> new ItemStack(ModBlocks.FLOOR_TILE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.FLOOR_TILE.get());
                        output.accept(ModBlocks.CEILING_TILE.get());
                        output.accept(ModBlocks.WALL_YELLOW.get());
                        output.accept(ModBlocks.WALL_YELLOW_PLINTH.get());
                        output.accept(ModBlocks.WALL_MANILA.get());
                        output.accept(ModBlocks.WALL_MANILA_PLINTH.get());
                        output.accept(ModBlocks.FLOOR_MANILA.get());
                        output.accept(ModBlocks.CEILING_MANILA.get());
                        output.accept(ModItems.MYSTERIOUS_DOOR.get());
                        output.accept(ModItems.METAL_DOOR.get());
                        output.accept(ModItems.CONCRETE_PILLAR.get());
                        output.accept(ModItems.CONCRETE_WARNING_PILLAR.get());
                        output.accept(ModItems.CONCRETE_FLOOR.get());
                        output.accept(ModItems.WHITE_PLASTERED_WALL.get());
                        output.accept(ModItems.CONCRETE_CEILING.get());
                        output.accept(ModItems.WALL_YELLOW_WALL.get());
                        output.accept(ModItems.WALL_YELLOW_SLAB.get());
                        output.accept(ModItems.WALL_YELLOW_STAIRS.get());
                        output.accept(ModItems.WALL_YELLOW_FENCE.get());
                        output.accept(ModItems.FLOOR_TILE_WALL.get());
                        output.accept(ModItems.FLOOR_TILE_SLAB.get());
                        output.accept(ModItems.FLOOR_TILE_STAIRS.get());
                        output.accept(ModItems.FLOOR_TILE_FENCE.get());
                        output.accept(ModBlocks.FLUORESCENT_LAMP.get());
                        output.accept(ModBlocks.HUB_DOOR.get());
                        output.accept(ModBlocks.WALL_OAKED.get());
                        output.accept(ModItems.THRESHOLD_KEY.get());
                        output.accept(ModItems.SCRAPER.get());
                        output.accept(ModItems.WALLPAPER.get());
                        output.accept(ModItems.WALLPAPER_COAT.get());
                        output.accept(ModItems.WALLPAPER_HAT.get());
                        output.accept(ModItems.UNKNOWN_SEWAGE_BOTTLE.get());
                        output.accept(ModItems.CARPET_FIBER.get());
                        output.accept(ModItems.DAMP_CANNED_ALMOND_WATER.get());
                        output.accept(ModItems.CANNED_ALMOND_WATER.get());
                        output.accept(ModItems.EMPTY_CAN.get());
                output.accept(ModBlocks.BRASS_PIPE.get());
                output.accept(ModBlocks.RED_PIPE.get());
                output.accept(ModBlocks.CONCRETE_CEILING_FENCE.get());
                output.accept(ModBlocks.CONCRETE_CEILING_SLAB.get());
                output.accept(ModBlocks.CONCRETE_CEILING_STAIRS.get());
                output.accept(ModBlocks.CONCRETE_CEILING_WALL.get());
                output.accept(ModBlocks.CONCRETE_FLOOR_FENCE.get());
                output.accept(ModBlocks.CONCRETE_FLOOR_SLAB.get());
                output.accept(ModBlocks.CONCRETE_FLOOR_STAIRS.get());
                output.accept(ModBlocks.CONCRETE_FLOOR_WALL.get());
                output.accept(ModBlocks.CONCRETE_PILLAR_FENCE.get());
                output.accept(ModBlocks.CONCRETE_PILLAR_SLAB.get());
                output.accept(ModBlocks.CONCRETE_PILLAR_STAIRS.get());
                output.accept(ModBlocks.CONCRETE_PILLAR_WALL.get());
                output.accept(ModBlocks.CONCRETE_WARNING_PILLAR_FENCE.get());
                output.accept(ModBlocks.CONCRETE_WARNING_PILLAR_SLAB.get());
                output.accept(ModBlocks.CONCRETE_WARNING_PILLAR_STAIRS.get());
                output.accept(ModBlocks.CONCRETE_WARNING_PILLAR_WALL.get());
                output.accept(ModBlocks.CRATE.get());
                output.accept(ModBlocks.GARDEN_WALL_SKIRTING.get());
                output.accept(ModBlocks.LEGEND_WALL_SKIRTING.get());
                output.accept(ModBlocks.WHITE_PLASTERED_WALL_FENCE.get());
                output.accept(ModBlocks.WHITE_PLASTERED_WALL_SKIRTING.get());
                output.accept(ModBlocks.WHITE_PLASTERED_WALL_SLAB.get());
                output.accept(ModBlocks.WHITE_PLASTERED_WALL_STAIRS.get());
                output.accept(ModBlocks.WHITE_PLASTERED_WALL_WALL.get());
                output.accept(ModBlocks.YELLOWED_CONCRETE_FLOOR.get());
                output.accept(ModBlocks.YELLOWED_CONCRETE_FLOOR_FENCE.get());
                output.accept(ModBlocks.YELLOWED_CONCRETE_FLOOR_SLAB.get());
                output.accept(ModBlocks.YELLOWED_CONCRETE_FLOOR_STAIRS.get());
                output.accept(ModBlocks.YELLOWED_CONCRETE_FLOOR_WALL.get());
                output.accept(ModBlocks.YELLOWED_CONCRETE_WALL.get());
                output.accept(ModBlocks.YELLOWED_CONCRETE_WALL_FENCE.get());
                output.accept(ModBlocks.YELLOWED_CONCRETE_WALL_SKIRTING.get());
                output.accept(ModBlocks.YELLOWED_CONCRETE_WALL_SLAB.get());
                output.accept(ModBlocks.YELLOWED_CONCRETE_WALL_STAIRS.get());
                output.accept(ModBlocks.YELLOWED_CONCRETE_WALL_WALL.get());
                output.accept(ModBlocks.YELLOWED_CORRUGATED_METAL.get());
                output.accept(ModBlocks.YELLOWED_CORRUGATED_METAL_FENCE.get());
                output.accept(ModBlocks.YELLOWED_CORRUGATED_METAL_SLAB.get());
                output.accept(ModBlocks.YELLOWED_CORRUGATED_METAL_STAIRS.get());
                output.accept(ModBlocks.YELLOWED_CORRUGATED_METAL_WALL.get());
                    })
                    .build());


    /** 家具标签页。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FURNITURE_TAB =
            CREATIVE_MODE_TABS.register("backrooms_furniture", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.backrooms_complex.furniture"))
                    .icon(() -> new ItemStack(ModItems.CHAIR_OAK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.STOOL_JUNGLE.get());
                        output.accept(ModItems.STOOL_SPRUCE.get());
                        output.accept(ModItems.STOOL_CHERRY.get());
                        output.accept(ModItems.STOOL_OAK.get());
                        output.accept(ModItems.STOOL_DARK_OAK.get());
                        output.accept(ModItems.STOOL_BIRCH.get());
                        output.accept(ModItems.STOOL_BAMBOO.get());
                        output.accept(ModItems.STOOL_MANGROVE.get());
                        output.accept(ModItems.STOOL_CRIMSON.get());
                        output.accept(ModItems.STOOL_WARPED.get());
                        output.accept(ModItems.STOOL_ACACIA.get());
                        output.accept(ModItems.TABLE_JUNGLE.get());
                        output.accept(ModItems.TABLE_SPRUCE.get());
                        output.accept(ModItems.TABLE_BIRCH.get());
                        output.accept(ModItems.TABLE_CHERRY.get());
                        output.accept(ModItems.TABLE_OAK.get());
                        output.accept(ModItems.TABLE_DARK_OAK.get());
                        output.accept(ModItems.TABLE_BAMBOO.get());
                        output.accept(ModItems.TABLE_MANGROVE.get());
                        output.accept(ModItems.TABLE_CRIMSON.get());
                        output.accept(ModItems.TABLE_WARPED.get());
                        output.accept(ModItems.TABLE_ACACIA.get());
                        output.accept(ModItems.CHAIR_JUNGLE.get());
                        output.accept(ModItems.CHAIR_SPRUCE.get());
                        output.accept(ModItems.CHAIR_BIRCH.get());
                        output.accept(ModItems.CHAIR_CHERRY.get());
                        output.accept(ModItems.CHAIR_OAK.get());
                        output.accept(ModItems.CHAIR_DARK_OAK.get());
                        output.accept(ModItems.CHAIR_BAMBOO.get());
                        output.accept(ModItems.CHAIR_MANGROVE.get());
                        output.accept(ModItems.CHAIR_CRIMSON.get());
                        output.accept(ModItems.CHAIR_WARPED.get());
                        output.accept(ModItems.CHAIR_ACACIA.get());
                    })
                    .build());


    /**
     * 触发本类的静态初始化。
     *
     * <p>两个页签的注册写在静态字段的初始化表达式里，而 JVM 只有在<b>首次访问</b>
     * 本类时才会执行它 —— 若没有这行调用，页签就永远不会注册（创造栏里看不到本模组内容）。</p>
     */
    public static void init() {
        // 空方法体：调用即是目的
    }

    private ModCreativeTabs() {
    }
}
