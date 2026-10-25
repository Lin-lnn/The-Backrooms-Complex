package com.the_backrooms_complex;

import com.the_backrooms_complex.item.ChiselItem;
import com.the_backrooms_complex.item.ScraperItem;
import com.the_backrooms_complex.item.ThresholdKeyItem;
import com.the_backrooms_complex.item.WallpaperItem;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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
    // 三个创造模式物品栏的定义见 ModCreativeTabs（保持注册表字段不变，外部无需改动）

    // ===== 方块物品 =====
    public static final DeferredItem<BlockItem> WALL_YELLOW =
            ITEMS.register("wall_yellow", () -> new BlockItem(ModBlocks.WALL_YELLOW.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_YELLOW_PLINTH =
            ITEMS.register("wall_yellow_plinth", () -> new BlockItem(ModBlocks.WALL_YELLOW_PLINTH.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_MANILA =
            ITEMS.register("wall_manila", () -> new BlockItem(ModBlocks.WALL_MANILA.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_MANILA_PLINTH =
            ITEMS.register("wall_manila_plinth", () -> new BlockItem(ModBlocks.WALL_MANILA_PLINTH.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FLOOR_MANILA =
            ITEMS.register("floor_manila", () -> new BlockItem(ModBlocks.FLOOR_MANILA.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_DUST_LAYER =
            ITEMS.register("wall_dust_layer", () -> new BlockItem(ModBlocks.WALL_DUST_LAYER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CEILING_MANILA =
            ITEMS.register("ceiling_manila", () -> new BlockItem(ModBlocks.CEILING_MANILA.get(), new Item.Properties()));
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

    /** 「熄灭的荧光灯」的方块物品。 */
    public static final DeferredItem<BlockItem> LAMP_OFF =
            ITEMS.register("lamp_off", () -> new BlockItem(ModBlocks.LAMP_OFF.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> MYCELIUM_CARPET =
            ITEMS.register("mycelium_carpet", () -> new BlockItem(ModBlocks.MYCELIUM_CARPET.get(), new Item.Properties()));

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
                            ArmorItem.Type.CHESTPLATE, 1,
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
                            ArmorItem.Type.HELMET, 1,
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
            ITEMS.register("wall_dust", () -> new com.the_backrooms_complex.item.WallDustItem(new Item.Properties()));

    /** 墙纸帽：头盔形状合成，5 个墙纸，可穿戴、无护甲值、耐久 32。 */
    public static final DeferredItem<ArmorItem> WALLPAPER_HAT =
            ITEMS.register("wallpaper_hat", () -> new ArmorItem(
                    WALLPAPER_HAT_MATERIAL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(32)));

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
    // ===== 家具 BlockItem =====
    public static final DeferredItem<BlockItem> STOOL_JUNGLE =
            ITEMS.register("stool_jungle", () -> new BlockItem(ModBlocks.STOOL_JUNGLE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STOOL_SPRUCE =
            ITEMS.register("stool_spruce", () -> new BlockItem(ModBlocks.STOOL_SPRUCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STOOL_CHERRY =
            ITEMS.register("stool_cherry", () -> new BlockItem(ModBlocks.STOOL_CHERRY.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STOOL_OAK =
            ITEMS.register("stool_oak", () -> new BlockItem(ModBlocks.STOOL_OAK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STOOL_DARK_OAK =
            ITEMS.register("stool_dark_oak", () -> new BlockItem(ModBlocks.STOOL_DARK_OAK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STOOL_BIRCH =
            ITEMS.register("stool_birch", () -> new BlockItem(ModBlocks.STOOL_BIRCH.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STOOL_BAMBOO =
            ITEMS.register("stool_bamboo", () -> new BlockItem(ModBlocks.STOOL_BAMBOO.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STOOL_MANGROVE =
            ITEMS.register("stool_mangrove", () -> new BlockItem(ModBlocks.STOOL_MANGROVE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STOOL_CRIMSON =
            ITEMS.register("stool_crimson", () -> new BlockItem(ModBlocks.STOOL_CRIMSON.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STOOL_WARPED =
            ITEMS.register("stool_warped", () -> new BlockItem(ModBlocks.STOOL_WARPED.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STOOL_ACACIA =
            ITEMS.register("stool_acacia", () -> new BlockItem(ModBlocks.STOOL_ACACIA.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TABLE_JUNGLE =
            ITEMS.register("table_jungle", () -> new BlockItem(ModBlocks.TABLE_JUNGLE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TABLE_SPRUCE =
            ITEMS.register("table_spruce", () -> new BlockItem(ModBlocks.TABLE_SPRUCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TABLE_BIRCH =
            ITEMS.register("table_birch", () -> new BlockItem(ModBlocks.TABLE_BIRCH.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TABLE_CHERRY =
            ITEMS.register("table_cherry", () -> new BlockItem(ModBlocks.TABLE_CHERRY.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TABLE_OAK =
            ITEMS.register("table_oak", () -> new BlockItem(ModBlocks.TABLE_OAK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TABLE_DARK_OAK =
            ITEMS.register("table_dark_oak", () -> new BlockItem(ModBlocks.TABLE_DARK_OAK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TABLE_BAMBOO =
            ITEMS.register("table_bamboo", () -> new BlockItem(ModBlocks.TABLE_BAMBOO.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TABLE_MANGROVE =
            ITEMS.register("table_mangrove", () -> new BlockItem(ModBlocks.TABLE_MANGROVE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TABLE_CRIMSON =
            ITEMS.register("table_crimson", () -> new BlockItem(ModBlocks.TABLE_CRIMSON.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TABLE_WARPED =
            ITEMS.register("table_warped", () -> new BlockItem(ModBlocks.TABLE_WARPED.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TABLE_ACACIA =
            ITEMS.register("table_acacia", () -> new BlockItem(ModBlocks.TABLE_ACACIA.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHAIR_JUNGLE =
            ITEMS.register("chair_jungle", () -> new BlockItem(ModBlocks.CHAIR_JUNGLE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHAIR_SPRUCE =
            ITEMS.register("chair_spruce", () -> new BlockItem(ModBlocks.CHAIR_SPRUCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHAIR_BIRCH =
            ITEMS.register("chair_birch", () -> new BlockItem(ModBlocks.CHAIR_BIRCH.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHAIR_CHERRY =
            ITEMS.register("chair_cherry", () -> new BlockItem(ModBlocks.CHAIR_CHERRY.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHAIR_OAK =
            ITEMS.register("chair_oak", () -> new BlockItem(ModBlocks.CHAIR_OAK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHAIR_DARK_OAK =
            ITEMS.register("chair_dark_oak", () -> new BlockItem(ModBlocks.CHAIR_DARK_OAK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHAIR_BAMBOO =
            ITEMS.register("chair_bamboo", () -> new BlockItem(ModBlocks.CHAIR_BAMBOO.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHAIR_MANGROVE =
            ITEMS.register("chair_mangrove", () -> new BlockItem(ModBlocks.CHAIR_MANGROVE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHAIR_CRIMSON =
            ITEMS.register("chair_crimson", () -> new BlockItem(ModBlocks.CHAIR_CRIMSON.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHAIR_WARPED =
            ITEMS.register("chair_warped", () -> new BlockItem(ModBlocks.CHAIR_WARPED.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHAIR_ACACIA =
            ITEMS.register("chair_acacia", () -> new BlockItem(ModBlocks.CHAIR_ACACIA.get(), new Item.Properties()));


    // ===== 墙/台阶/楼梯/栅栏 变体的 BlockItem =====
    public static final DeferredItem<BlockItem> WALL_YELLOW_WALL =
            ITEMS.register("wall_yellow_wall", () -> new BlockItem(ModBlocks.WALL_YELLOW_WALL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_YELLOW_SLAB =
            ITEMS.register("wall_yellow_slab", () -> new BlockItem(ModBlocks.WALL_YELLOW_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_YELLOW_STAIRS =
            ITEMS.register("wall_yellow_stairs", () -> new BlockItem(ModBlocks.WALL_YELLOW_STAIRS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_YELLOW_FENCE =
            ITEMS.register("wall_yellow_fence", () -> new BlockItem(ModBlocks.WALL_YELLOW_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_DAMP_WALL =
            ITEMS.register("wall_damp_wall", () -> new BlockItem(ModBlocks.WALL_DAMP_WALL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_DAMP_SLAB =
            ITEMS.register("wall_damp_slab", () -> new BlockItem(ModBlocks.WALL_DAMP_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_DAMP_STAIRS =
            ITEMS.register("wall_damp_stairs", () -> new BlockItem(ModBlocks.WALL_DAMP_STAIRS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_DAMP_FENCE =
            ITEMS.register("wall_damp_fence", () -> new BlockItem(ModBlocks.WALL_DAMP_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_CONCRETE_WALL =
            ITEMS.register("wall_concrete_wall", () -> new BlockItem(ModBlocks.WALL_CONCRETE_WALL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_CONCRETE_SLAB =
            ITEMS.register("wall_concrete_slab", () -> new BlockItem(ModBlocks.WALL_CONCRETE_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_CONCRETE_STAIRS =
            ITEMS.register("wall_concrete_stairs", () -> new BlockItem(ModBlocks.WALL_CONCRETE_STAIRS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WALL_CONCRETE_FENCE =
            ITEMS.register("wall_concrete_fence", () -> new BlockItem(ModBlocks.WALL_CONCRETE_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CRACKED_CONCRETE_WALL_WALL =
            ITEMS.register("cracked_concrete_wall_wall", () -> new BlockItem(ModBlocks.CRACKED_CONCRETE_WALL_WALL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CRACKED_CONCRETE_WALL_SLAB =
            ITEMS.register("cracked_concrete_wall_slab", () -> new BlockItem(ModBlocks.CRACKED_CONCRETE_WALL_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CRACKED_CONCRETE_WALL_STAIRS =
            ITEMS.register("cracked_concrete_wall_stairs", () -> new BlockItem(ModBlocks.CRACKED_CONCRETE_WALL_STAIRS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CRACKED_CONCRETE_WALL_FENCE =
            ITEMS.register("cracked_concrete_wall_fence", () -> new BlockItem(ModBlocks.CRACKED_CONCRETE_WALL_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FLOOR_TILE_WALL =
            ITEMS.register("floor_tile_wall", () -> new BlockItem(ModBlocks.FLOOR_TILE_WALL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FLOOR_TILE_SLAB =
            ITEMS.register("floor_tile_slab", () -> new BlockItem(ModBlocks.FLOOR_TILE_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FLOOR_TILE_STAIRS =
            ITEMS.register("floor_tile_stairs", () -> new BlockItem(ModBlocks.FLOOR_TILE_STAIRS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FLOOR_TILE_FENCE =
            ITEMS.register("floor_tile_fence", () -> new BlockItem(ModBlocks.FLOOR_TILE_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DAMP_FLOOR_WALL =
            ITEMS.register("damp_floor_wall", () -> new BlockItem(ModBlocks.DAMP_FLOOR_WALL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DAMP_FLOOR_SLAB =
            ITEMS.register("damp_floor_slab", () -> new BlockItem(ModBlocks.DAMP_FLOOR_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DAMP_FLOOR_STAIRS =
            ITEMS.register("damp_floor_stairs", () -> new BlockItem(ModBlocks.DAMP_FLOOR_STAIRS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DAMP_FLOOR_FENCE =
            ITEMS.register("damp_floor_fence", () -> new BlockItem(ModBlocks.DAMP_FLOOR_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STRIPPED_CARPET_FLOOR_WALL =
            ITEMS.register("stripped_carpet_floor_wall", () -> new BlockItem(ModBlocks.STRIPPED_CARPET_FLOOR_WALL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STRIPPED_CARPET_FLOOR_SLAB =
            ITEMS.register("stripped_carpet_floor_slab", () -> new BlockItem(ModBlocks.STRIPPED_CARPET_FLOOR_SLAB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STRIPPED_CARPET_FLOOR_STAIRS =
            ITEMS.register("stripped_carpet_floor_stairs", () -> new BlockItem(ModBlocks.STRIPPED_CARPET_FLOOR_STAIRS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STRIPPED_CARPET_FLOOR_FENCE =
            ITEMS.register("stripped_carpet_floor_fence", () -> new BlockItem(ModBlocks.STRIPPED_CARPET_FLOOR_FENCE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> METAL_DOOR =
            ITEMS.register("metal_door", () -> new BlockItem(ModBlocks.METAL_DOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> MYSTERIOUS_DOOR =
            ITEMS.register("mysterious_door", () -> new BlockItem(ModBlocks.MYSTERIOUS_DOOR.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> CONCRETE_PILLAR =
            ITEMS.register("concrete_pillar", () -> new BlockItem(ModBlocks.CONCRETE_PILLAR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CONCRETE_WARNING_PILLAR =
            ITEMS.register("concrete_warning_pillar", () -> new BlockItem(ModBlocks.CONCRETE_WARNING_PILLAR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CONCRETE_FLOOR =
            ITEMS.register("concrete_floor", () -> new BlockItem(ModBlocks.CONCRETE_FLOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CONCRETE_CEILING =
            ITEMS.register("concrete_ceiling", () -> new BlockItem(ModBlocks.CONCRETE_CEILING.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WHITE_PLASTERED_WALL =
            ITEMS.register("white_plastered_wall", () -> new BlockItem(ModBlocks.WHITE_PLASTERED_WALL.get(), new Item.Properties()));

    private ModItems() {
    }
}
