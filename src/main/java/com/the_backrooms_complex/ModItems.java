package com.the_backrooms_complex;

import com.the_backrooms_complex.item.ChiselItem;
import com.the_backrooms_complex.item.ScraperItem;
import com.the_backrooms_complex.item.ThresholdKeyItem;
import com.the_backrooms_complex.item.WallpaperItem;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品与创造模式物品栏注册类。
 *
 * <p>每个方块对应一个 BlockItem；此外还有「阈界的层级密钥」这一功能性物品。</p>
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BackroomsComplex.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BackroomsComplex.MODID);

    // ===== 方块物品 =====
    public static final DeferredItem<BlockItem> WALL_YELLOW =
            ITEMS.register("wall_yellow", () -> new BlockItem(ModBlocks.WALL_YELLOW.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_YELLOW_PLINTH =
            ITEMS.register("wall_yellow_plinth", () -> new BlockItem(ModBlocks.WALL_YELLOW_PLINTH.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_CONCRETE =
            ITEMS.register("wall_concrete", () -> new BlockItem(ModBlocks.WALL_CONCRETE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_CONCRETE_PLINTH =
            ITEMS.register("wall_concrete_plinth", () -> new BlockItem(ModBlocks.WALL_CONCRETE_PLINTH.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_DAMP =
            ITEMS.register("wall_damp", () -> new BlockItem(ModBlocks.WALL_DAMP.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_DAMP_PLINTH =
            ITEMS.register("wall_damp_plinth", () -> new BlockItem(ModBlocks.WALL_DAMP_PLINTH.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FLOOR_TILE =
            ITEMS.register("floor_tile", () -> new BlockItem(ModBlocks.FLOOR_TILE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CEILING_TILE =
            ITEMS.register("ceiling_tile", () -> new BlockItem(ModBlocks.CEILING_TILE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FLUORESCENT_LAMP =
            ITEMS.register("fluorescent_lamp", () -> new BlockItem(ModBlocks.FLUORESCENT_LAMP.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> HUB_DOOR =
            ITEMS.register("hub_door", () -> new BlockItem(ModBlocks.HUB_DOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_OAKED =
            ITEMS.register("wall_oaked", () -> new BlockItem(ModBlocks.WALL_OAKED.get(), new Item.Properties()));
    // ===== 潮湿群系方块物品 =====
    public static final DeferredItem<BlockItem> DAMP_FLOOR =
            ITEMS.register("damp_floor", () -> new BlockItem(ModBlocks.DAMP_FLOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DAMP_CEILING =
            ITEMS.register("damp_ceiling", () -> new BlockItem(ModBlocks.DAMP_CEILING.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> STRIPPED_CARPET_FLOOR =
            ITEMS.register("stripped_carpet_floor", () -> new BlockItem(ModBlocks.STRIPPED_CARPET_FLOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RUINED_CEILING =
            ITEMS.register("ruined_ceiling", () -> new BlockItem(ModBlocks.RUINED_CEILING.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RUINED_VENT =
            ITEMS.register("ruined_vent", () -> new BlockItem(ModBlocks.RUINED_VENT.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CRACKED_CONCRETE_WALL =
            ITEMS.register("cracked_concrete_wall", () -> new BlockItem(ModBlocks.CRACKED_CONCRETE_WALL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WARNING_CONCRETE_WALL =
            ITEMS.register("warning_concrete_wall", () -> new BlockItem(ModBlocks.WARNING_CONCRETE_WALL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHISELED_CONCRETE_WALL =
            ITEMS.register("chiseled_concrete_wall", () -> new BlockItem(ModBlocks.CHISELED_CONCRETE_WALL.get(), new Item.Properties()));

    // ===== 功能物品：阈界的层级密钥 =====
    // 手持右键「枢纽之门」可传送至后室 Level 0（普通 Item 子类，仅带 tooltip）。
    public static final DeferredItem<ThresholdKeyItem> THRESHOLD_KEY =
            ITEMS.register("threshold_key", () -> new ThresholdKeyItem(new Item.Properties()));

    // ===== 刮纸刀与墙纸 =====
    /** 刮纸刀：右键墙壁主体方块刮掉墙纸。默认不消耗耐久（见 ModConfig）。 */
    public static final DeferredItem<ScraperItem> SCRAPER =
            ITEMS.register("scraper", () -> new ScraperItem(new Item.Properties().stacksTo(1).durability(64)));

    /** 墙纸：刮墙掉落，可作燃料（0.125 个物品），并加入 c:paper 标签可替代纸。 */
    public static final DeferredItem<WallpaperItem> WALLPAPER =
            ITEMS.register("wallpaper", () -> new WallpaperItem(new Item.Properties()));

    // ===== 墙纸护甲材质（不提供护甲值）=====
    /** 护甲材质注册表：1.21.1 的 ArmorMaterial 是登记在 Registries.ARMOR_MATERIAL 里的。 */
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, BackroomsComplex.MODID);

    /**
     * 墙纸护甲材质：四件防御力全为 0（不提供护甲值），附魔价值 0，
     * 修复材料复用 {@code c:paper} 标签（墙纸本身就在里面），
     * 穿戴贴图取 {@code textures/models/armor/wallpaper_layer_1.png} 与 {@code _layer_2.png}。
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WALLPAPER_MATERIAL =
            ARMOR_MATERIALS.register("wallpaper", () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.HELMET, 0,
                            ArmorItem.Type.CHESTPLATE, 0,
                            ArmorItem.Type.LEGGINGS, 0,
                            ArmorItem.Type.BOOTS, 0),
                    0,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(TagKey.create(Registries.ITEM,
                            ResourceLocation.fromNamespaceAndPath("c", "paper"))),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "wallpaper"))),
                    0.0F,
                    0.0F));

    /** 墙纸大衣：胸甲形状合成，8 个墙纸，可穿戴、无护甲值、耐久 32。 */
    public static final DeferredItem<ArmorItem> WALLPAPER_COAT =
            ITEMS.register("wallpaper_coat", () -> new ArmorItem(
                    WALLPAPER_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(32)));

    /**
     * 墙纸帽<b>专用</b>护甲材质：贴图为 {@code textures/models/armor/wallpaper_hat_model_layer_1.png}
     * （64x64 黄色竖条纹，全幅不透明），保证 3D 礼帽<b>每个面</b>都能采到贴图。
     * 与大衣分开注册，是为了让两者的穿戴贴图互不干扰。
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WALLPAPER_HAT_MATERIAL =
            ARMOR_MATERIALS.register("wallpaper_hat", () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.HELMET, 0,
                            ArmorItem.Type.CHESTPLATE, 0,
                            ArmorItem.Type.LEGGINGS, 0,
                            ArmorItem.Type.BOOTS, 0),
                    0,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(TagKey.create(Registries.ITEM,
                            ResourceLocation.fromNamespaceAndPath("c", "paper"))),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "wallpaper_hat_model"))),
                    0.0F,
                    0.0F));

    /** 凿子：右键混凝土墙壁凿破它，耐久 64。2 铁粒 + 1 木棍合成。 */
    public static final DeferredItem<ChiselItem> CHISEL =
            ITEMS.register("chisel", () -> new ChiselItem(new Item.Properties().stacksTo(1).durability(64)));

    /** 墙灰：凿墙掉落的粉末（贴图为原版火药染淡灰）。 */
    public static final DeferredItem<Item> WALL_DUST =
            ITEMS.register("wall_dust", () -> new Item(new Item.Properties()));

    /** 墙纸帽：头盔形状合成，5 个墙纸，可穿戴、无护甲值、耐久 32。 */
    public static final DeferredItem<ArmorItem> WALLPAPER_HAT =
            ITEMS.register("wallpaper_hat", () -> new ArmorItem(
                    WALLPAPER_HAT_MATERIAL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(32)));

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
                        output.accept(ModBlocks.WALL_CONCRETE.get());
                        output.accept(ModBlocks.WALL_CONCRETE_PLINTH.get());
                        output.accept(ModBlocks.WALL_DAMP.get());
                        output.accept(ModBlocks.WALL_DAMP_PLINTH.get());
                        output.accept(ModBlocks.FLUORESCENT_LAMP.get());
                        output.accept(ModBlocks.HUB_DOOR.get());
                        output.accept(ModBlocks.WALL_OAKED.get());
                        output.accept(ModBlocks.STRIPPED_CARPET_FLOOR.get());
                        output.accept(ModBlocks.RUINED_CEILING.get());
                        output.accept(ModBlocks.RUINED_VENT.get());
                        output.accept(ModBlocks.CRACKED_CONCRETE_WALL.get());
                        output.accept(ModBlocks.WARNING_CONCRETE_WALL.get());
                        output.accept(ModBlocks.DAMP_FLOOR.get());
                        output.accept(ModBlocks.DAMP_CEILING.get());
                        output.accept(ModBlocks.CHISELED_CONCRETE_WALL.get());
                        output.accept(ModItems.CHISEL.get());
                        output.accept(ModItems.WALL_DUST.get());
                        output.accept(ModItems.THRESHOLD_KEY.get());
                        output.accept(ModItems.SCRAPER.get());
                        output.accept(ModItems.WALLPAPER.get());
                        output.accept(ModItems.WALLPAPER_COAT.get());
                        output.accept(ModItems.WALLPAPER_HAT.get());
                        output.accept(ModItems.UNKNOWN_SEWAGE_BOTTLE.get());
                        output.accept(ModItems.CARPET_FIBER.get());
                        output.accept(ModItems.BACTERIA_TISSUE.get());
                        output.accept(ModItems.DAMP_CANNED_ALMOND_WATER.get());
                        output.accept(ModItems.CANNED_ALMOND_WATER.get());
                        output.accept(ModItems.EMPTY_CAN.get());
                    })
                    .build());

    /** 「未知污水瓶」：用原版水瓶右键潮湿墙壁/地板/天花板获得。 */
    public static final DeferredItem<Item> UNKNOWN_SEWAGE_BOTTLE =
            ITEMS.register("unknown_sewage_bottle",
                    () -> new com.the_backrooms_complex.item.SewageBottleItem(new Item.Properties()
                            .stacksTo(16)));

    /** 「地毯纤维」：刮纸刀从地板上刮下来的碎屑，可食用但代价很大。 */
    public static final DeferredItem<Item> CARPET_FIBER =
            ITEMS.register("carpet_fiber",
                    () -> new com.the_backrooms_complex.item.CarpetFiberItem(new Item.Properties().stacksTo(64)));

    /** 「受潮的罐装杏仁水」：2 秒饮用，+20 精神 / +30 饮水 / +2 饥饿，附加 1 秒反胃 I。 */
    public static final DeferredItem<Item> DAMP_CANNED_ALMOND_WATER =
            ITEMS.register("damp_canned_almond_water",
                    () -> new com.the_backrooms_complex.item.CannedAlmondWaterItem(
                            new Item.Properties().stacksTo(16),
                            ModBalance.DAMP_CAN_USE_TICKS, ModBalance.DAMP_CAN_SANITY,
                            ModBalance.DAMP_CAN_THIRST, ModBalance.DAMP_CAN_HUNGER,
                            ModBalance.DAMP_CAN_NAUSEA));

    /** 「保存完好的罐装杏仁水」：1.5 秒饮用，+40 精神 / +30 饮水 / +4 饥饿。 */
    public static final DeferredItem<Item> CANNED_ALMOND_WATER =
            ITEMS.register("canned_almond_water",
                    () -> new com.the_backrooms_complex.item.CannedAlmondWaterItem(
                            new Item.Properties().stacksTo(16),
                            ModBalance.PRISTINE_CAN_USE_TICKS, ModBalance.PRISTINE_CAN_SANITY,
                            ModBalance.PRISTINE_CAN_THIRST, ModBalance.PRISTINE_CAN_HUNGER,
                            ModBalance.PRISTINE_CAN_NAUSEA));

    /** 「易拉罐」：喝完留下的空罐。 */
    public static final DeferredItem<Item> EMPTY_CAN =
            ITEMS.register("empty_can", () -> new Item(new Item.Properties().stacksTo(64)));

    /** 「细菌组织」：击败细菌掉落，可用于合成。 */
    public static final DeferredItem<Item> BACTERIA_TISSUE =
            ITEMS.register("bacteria_tissue", () -> new Item(new Item.Properties()));

    /** 「细菌刷怪蛋」。 */
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> BACTERIA_SPAWN_EGG =
            ITEMS.register("bacteria_spawn_egg", () -> new net.minecraft.world.item.SpawnEggItem(
                    ModEntities.BACTERIA.get(), 0x3A3836, 0x1E1C1A, new Item.Properties().stacksTo(64)));

    /** 实体标签页：后室综合体：实体。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENTITIES_TAB =
            CREATIVE_MODE_TABS.register("backrooms_entities", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.backrooms_complex.entities"))
                    .icon(() -> new ItemStack(BACTERIA_SPAWN_EGG.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(BACTERIA_SPAWN_EGG.get());
                    })
                    .build());

    private ModItems() {
    }
}