package com.the_backrooms_complex;

import com.the_backrooms_complex.block.ChiseledConcreteWallBlock;
import com.the_backrooms_complex.block.DampCeilingBlock;
import com.the_backrooms_complex.block.DampFloorBlock;
import com.the_backrooms_complex.block.TableBlock;
import com.the_backrooms_complex.block.WallDustLayerBlock;
import com.the_backrooms_complex.block.SeatBlock;
import com.the_backrooms_complex.block.FluorescentLampBlock;
import com.the_backrooms_complex.block.HubDoorBlock;
import com.the_backrooms_complex.block.MyceliumCarpetBlock;
import com.the_backrooms_complex.block.OakedWallBlock;
import com.the_backrooms_complex.block.StrippedCarpetFloorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块注册类。
 *
 * <p>所有方块使用统一的 {@code strength(-1.0F, 3600000.0F)}：hardness = -1 使方块在
 * <b>生存模式</b>下无法被挖掘（与基岩、屏障一致）；<b>创造模式可正常破坏</b>，便于调试与地图编辑。</p>
 *
 * <p>本模组<strong>不</strong>监听 {@code BlockEvent.BreakEvent}，不做任何额外破坏拦截，
 * 破坏规则完全由上述属性决定。</p>
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BackroomsComplex.MODID);

    // ===== 墙壁主体 =====
    public static final DeferredBlock<Block> WALL_YELLOW = BLOCKS.register("wall_yellow",
            () -> new Block(unbreakable()));
    public static final DeferredBlock<Block> WALL_CONCRETE = BLOCKS.register("wall_concrete",
            () -> new Block(unbreakable()));
    public static final DeferredBlock<Block> WALL_DAMP = BLOCKS.register("wall_damp",
            () -> new Block(unbreakable()));
    /** 马尼拉墙壁：米白配色的黄色墙壁。 */
    public static final DeferredBlock<Block> WALL_MANILA = BLOCKS.register("wall_manila",
            () -> new Block(unbreakable()));

    // ===== 墙壁踢脚线（模型四边凸出裙边，不增加碰撞）=====
    public static final DeferredBlock<Block> WALL_YELLOW_PLINTH =
            BLOCKS.register("wall_yellow_plinth", () -> new Block(unbreakable()));
    public static final DeferredBlock<Block> WALL_CONCRETE_PLINTH =
            BLOCKS.register("wall_concrete_plinth", () -> new Block(unbreakable()));
    public static final DeferredBlock<Block> WALL_DAMP_PLINTH =
            BLOCKS.register("wall_damp_plinth", () -> new Block(unbreakable()));
    /** 马尼拉墙壁踢脚线。 */
    public static final DeferredBlock<Block> WALL_MANILA_PLINTH =
            BLOCKS.register("wall_manila_plinth", () -> new Block(unbreakable()));

    // ===== 地板 / 天花板 =====
    public static final DeferredBlock<Block> FLOOR_TILE = BLOCKS.register("floor_tile",
            () -> new Block(unbreakable()));
    public static final DeferredBlock<Block> CEILING_TILE = BLOCKS.register("ceiling_tile",
            () -> new Block(unbreakable()));
    /** 马尼拉地板：米白配色的地砖。 */
    public static final DeferredBlock<Block> FLOOR_MANILA = BLOCKS.register("floor_manila",
            () -> new Block(unbreakable()));
    /** 铺在地上的墙灰：外观同未连接的红石粉，但不传递红石信号。 */
    public static final DeferredBlock<WallDustLayerBlock> WALL_DUST_LAYER =
            BLOCKS.register("wall_dust_layer", () -> new WallDustLayerBlock(
                    BlockBehaviour.Properties.of()
                            .strength(0.2F)
                            .sound(net.minecraft.world.level.block.SoundType.SAND)
                            .noCollission()
                            .noOcclusion()
                            .instabreak()));

    /** 马尼拉天花板：米白配色的天花板。 */
    public static final DeferredBlock<Block> CEILING_MANILA = BLOCKS.register("ceiling_manila",
            () -> new Block(unbreakable()));

    // ===== 荧光灯（光照等级 15）=====
    public static final DeferredBlock<FluorescentLampBlock> FLUORESCENT_LAMP =
            BLOCKS.register("fluorescent_lamp", () -> new FluorescentLampBlock(
                    unbreakable().lightLevel(state -> 15)));

    // ===== 枢纽之门（两格高，黄色调的云杉木门样式）=====
    public static final DeferredBlock<HubDoorBlock> HUB_DOOR =
            BLOCKS.register("hub_door", () -> new HubDoorBlock(BlockSetType.SPRUCE,
                    BlockBehaviour.Properties.of()
                            .strength(-1.0F, 3600000.0F) // 生存不可挖掘，创造可破坏
                            .noLootTable()
                            .noOcclusion()               // 门是非满方块，需禁用遮挡剔除
                            .sound(SoundType.WOOD)));

    // ===== 潮湿走廊群系的专属方块 =====
    /** 潮湿的地板：潮湿走廊群系的地板。 */
    public static final DeferredBlock<DampFloorBlock> DAMP_FLOOR =
            BLOCKS.register("damp_floor", () -> new DampFloorBlock(unbreakable()));
    /** 潮湿的天花板：潮湿走廊群系的天花板。 */
    public static final DeferredBlock<DampCeilingBlock> DAMP_CEILING =
            BLOCKS.register("damp_ceiling", () -> new DampCeilingBlock(unbreakable()));

    // ===== 混凝土大厅群系专用方块 =====
    /** 剥离地毯的地板：该群系的地板。 */
    public static final DeferredBlock<StrippedCarpetFloorBlock> STRIPPED_CARPET_FLOOR =
            BLOCKS.register("stripped_carpet_floor", () -> new StrippedCarpetFloorBlock(unbreakable()));
    /** 破败的天花板：该群系的天花板。 */
    public static final DeferredBlock<Block> RUINED_CEILING =
            BLOCKS.register("ruined_ceiling", () -> new Block(unbreakable()));
    /** 破败的通风口：替换该群系的荧光灯，不发光。 */
    public static final DeferredBlock<Block> RUINED_VENT =
            BLOCKS.register("ruined_vent", () -> new Block(unbreakable()));
    /** 破碎的混凝土墙壁：混凝土墙壁的差分变种，约占 10%。 */
    public static final DeferredBlock<Block> CRACKED_CONCRETE_WALL =
            BLOCKS.register("cracked_concrete_wall", () -> new Block(unbreakable()));
    /** 警示的混凝土墙壁：仅在每层 Y 中点出现，约占 0.01%。 */
    public static final DeferredBlock<Block> WARNING_CONCRETE_WALL =
            BLOCKS.register("warning_concrete_wall", () -> new Block(unbreakable()));
    /** 被凿破的混凝土墙壁：用凿子右键混凝土墙壁后产生的状态，60 秒后自动复原。 */
    public static final DeferredBlock<ChiseledConcreteWallBlock> CHISELED_CONCRETE_WALL =
            BLOCKS.register("chiseled_concrete_wall", () -> new ChiseledConcreteWallBlock(unbreakable()));

    // ===== 被刮去墙纸的墙面 =====
    public static final DeferredBlock<OakedWallBlock> WALL_OAKED =
            BLOCKS.register("wall_oaked", () -> new OakedWallBlock(unbreakable()));

    /**
     * 若给定方块是 Level 0 的<b>黄色墙壁主体</b>（{@code wall_yellow}），返回它对应的去墙纸变体；否则返回 {@code null}。
     *
     * <p>按设计<b>只有黄色墙壁可以刮纸</b>；混凝土墙壁、潮湿墙壁以及所有踢脚线都不在可刮范围内。</p>
     */
    public static OakedWallBlock.Variant wallVariantOf(Block block) {
        if (block == WALL_YELLOW.get()) {
            return OakedWallBlock.Variant.YELLOW;
        }
        return null;
    }

    /** 去墙纸变体对应的原始墙壁主体方块（恢复时使用）。 */
    public static Block originalWallOf(OakedWallBlock.Variant variant) {
        return switch (variant) {
            case YELLOW -> WALL_YELLOW.get();
            case CONCRETE -> WALL_CONCRETE.get();
            case DAMP -> WALL_DAMP.get();
        };
    }

    /**
     * 构造统一的"不可破坏"方块属性：{@code strength(-1.0F, 3600000.0F)} 使生存模式无法挖掘，
     * 创造模式可正常破坏；同时不产生掉落表，音效为石头。
     */
    // ===== 家具（木凳/桌/椅/梯/篱笆 及各 11 种木材 + 办公椅 + 工地照明灯）=====
    public static final DeferredBlock<Block> STOOL_JUNGLE = BLOCKS.register("stool_jungle", () -> new SeatBlock(unbreakable().noOcclusion(), "stool_jungle", 0.3125D));
    public static final DeferredBlock<Block> STOOL_SPRUCE = BLOCKS.register("stool_spruce", () -> new SeatBlock(unbreakable().noOcclusion(), "stool_spruce", 0.3125D));
    public static final DeferredBlock<Block> STOOL_CHERRY = BLOCKS.register("stool_cherry", () -> new SeatBlock(unbreakable().noOcclusion(), "stool_cherry", 0.3125D));
    public static final DeferredBlock<Block> STOOL_OAK = BLOCKS.register("stool_oak", () -> new SeatBlock(unbreakable().noOcclusion(), "stool_oak", 0.3125D));
    public static final DeferredBlock<Block> STOOL_DARK_OAK = BLOCKS.register("stool_dark_oak", () -> new SeatBlock(unbreakable().noOcclusion(), "stool_dark_oak", 0.3125D));
    public static final DeferredBlock<Block> STOOL_BIRCH = BLOCKS.register("stool_birch", () -> new SeatBlock(unbreakable().noOcclusion(), "stool_birch", 0.3125D));
    public static final DeferredBlock<Block> STOOL_BAMBOO = BLOCKS.register("stool_bamboo", () -> new SeatBlock(unbreakable().noOcclusion(), "stool_bamboo", 0.3125D));
    public static final DeferredBlock<Block> STOOL_MANGROVE = BLOCKS.register("stool_mangrove", () -> new SeatBlock(unbreakable().noOcclusion(), "stool_mangrove", 0.3125D));
    public static final DeferredBlock<Block> STOOL_CRIMSON = BLOCKS.register("stool_crimson", () -> new SeatBlock(unbreakable().noOcclusion(), "stool_crimson", 0.3125D));
    public static final DeferredBlock<Block> STOOL_WARPED = BLOCKS.register("stool_warped", () -> new SeatBlock(unbreakable().noOcclusion(), "stool_warped", 0.3125D));
    public static final DeferredBlock<Block> STOOL_ACACIA = BLOCKS.register("stool_acacia", () -> new SeatBlock(unbreakable().noOcclusion(), "stool_acacia", 0.3125D));
    public static final DeferredBlock<Block> TABLE_JUNGLE = BLOCKS.register("table_jungle", () -> new TableBlock(unbreakable().noOcclusion(), "table_jungle"));
    public static final DeferredBlock<Block> TABLE_SPRUCE = BLOCKS.register("table_spruce", () -> new TableBlock(unbreakable().noOcclusion(), "table_spruce"));
    public static final DeferredBlock<Block> TABLE_BIRCH = BLOCKS.register("table_birch", () -> new TableBlock(unbreakable().noOcclusion(), "table_birch"));
    public static final DeferredBlock<Block> TABLE_CHERRY = BLOCKS.register("table_cherry", () -> new TableBlock(unbreakable().noOcclusion(), "table_cherry"));
    public static final DeferredBlock<Block> TABLE_OAK = BLOCKS.register("table_oak", () -> new TableBlock(unbreakable().noOcclusion(), "table_oak"));
    public static final DeferredBlock<Block> TABLE_DARK_OAK = BLOCKS.register("table_dark_oak", () -> new TableBlock(unbreakable().noOcclusion(), "table_dark_oak"));
    public static final DeferredBlock<Block> TABLE_BAMBOO = BLOCKS.register("table_bamboo", () -> new TableBlock(unbreakable().noOcclusion(), "table_bamboo"));
    public static final DeferredBlock<Block> TABLE_MANGROVE = BLOCKS.register("table_mangrove", () -> new TableBlock(unbreakable().noOcclusion(), "table_mangrove"));
    public static final DeferredBlock<Block> TABLE_CRIMSON = BLOCKS.register("table_crimson", () -> new TableBlock(unbreakable().noOcclusion(), "table_crimson"));
    public static final DeferredBlock<Block> TABLE_WARPED = BLOCKS.register("table_warped", () -> new TableBlock(unbreakable().noOcclusion(), "table_warped"));
    public static final DeferredBlock<Block> TABLE_ACACIA = BLOCKS.register("table_acacia", () -> new TableBlock(unbreakable().noOcclusion(), "table_acacia"));
    public static final DeferredBlock<Block> CHAIR_JUNGLE = BLOCKS.register("chair_jungle", () -> new SeatBlock(unbreakable().noOcclusion(), "chair_jungle", 0.5D));
    public static final DeferredBlock<Block> CHAIR_SPRUCE = BLOCKS.register("chair_spruce", () -> new SeatBlock(unbreakable().noOcclusion(), "chair_spruce", 0.5D));
    public static final DeferredBlock<Block> CHAIR_BIRCH = BLOCKS.register("chair_birch", () -> new SeatBlock(unbreakable().noOcclusion(), "chair_birch", 0.5D));
    public static final DeferredBlock<Block> CHAIR_CHERRY = BLOCKS.register("chair_cherry", () -> new SeatBlock(unbreakable().noOcclusion(), "chair_cherry", 0.5D));
    public static final DeferredBlock<Block> CHAIR_OAK = BLOCKS.register("chair_oak", () -> new SeatBlock(unbreakable().noOcclusion(), "chair_oak", 0.5D));
    public static final DeferredBlock<Block> CHAIR_DARK_OAK = BLOCKS.register("chair_dark_oak", () -> new SeatBlock(unbreakable().noOcclusion(), "chair_dark_oak", 0.5D));
    public static final DeferredBlock<Block> CHAIR_BAMBOO = BLOCKS.register("chair_bamboo", () -> new SeatBlock(unbreakable().noOcclusion(), "chair_bamboo", 0.5D));
    public static final DeferredBlock<Block> CHAIR_MANGROVE = BLOCKS.register("chair_mangrove", () -> new SeatBlock(unbreakable().noOcclusion(), "chair_mangrove", 0.5D));
    public static final DeferredBlock<Block> CHAIR_CRIMSON = BLOCKS.register("chair_crimson", () -> new SeatBlock(unbreakable().noOcclusion(), "chair_crimson", 0.5D));
    public static final DeferredBlock<Block> CHAIR_WARPED = BLOCKS.register("chair_warped", () -> new SeatBlock(unbreakable().noOcclusion(), "chair_warped", 0.5D));
    public static final DeferredBlock<Block> CHAIR_ACACIA = BLOCKS.register("chair_acacia", () -> new SeatBlock(unbreakable().noOcclusion(), "chair_acacia", 0.5D));

    /**
     * 是否是「马尼拉」系列方块（墙壁 / 踢脚线 / 地板 / 天花板）。
     *
     * <p>这些方块使用固定的米黄色贴图，<b>不参与群系着色</b>，
     * 因此客户端着色时直接返回纯白，保留贴图本身的颜色。</p>
     */
    public static boolean isManila(Block block) {
        return block == WALL_MANILA.get()
                || block == WALL_MANILA_PLINTH.get()
                || block == FLOOR_MANILA.get()
                || block == CEILING_MANILA.get();
    }


    // ===== 墙 / 台阶 / 楼梯 / 栅栏 变体（7 种基础材质 × 4 种形状）=====
    public static final DeferredBlock<WallBlock> WALL_YELLOW_WALL =
            BLOCKS.register("wall_yellow_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> WALL_DAMP_WALL =
            BLOCKS.register("wall_damp_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> WALL_CONCRETE_WALL =
            BLOCKS.register("wall_concrete_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> CRACKED_CONCRETE_WALL_WALL =
            BLOCKS.register("cracked_concrete_wall_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> FLOOR_TILE_WALL =
            BLOCKS.register("floor_tile_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> DAMP_FLOOR_WALL =
            BLOCKS.register("damp_floor_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> STRIPPED_CARPET_FLOOR_WALL =
            BLOCKS.register("stripped_carpet_floor_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<SlabBlock> WALL_YELLOW_SLAB =
            BLOCKS.register("wall_yellow_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<SlabBlock> WALL_DAMP_SLAB =
            BLOCKS.register("wall_damp_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<SlabBlock> WALL_CONCRETE_SLAB =
            BLOCKS.register("wall_concrete_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<SlabBlock> CRACKED_CONCRETE_WALL_SLAB =
            BLOCKS.register("cracked_concrete_wall_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<SlabBlock> FLOOR_TILE_SLAB =
            BLOCKS.register("floor_tile_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<SlabBlock> DAMP_FLOOR_SLAB =
            BLOCKS.register("damp_floor_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<SlabBlock> STRIPPED_CARPET_FLOOR_SLAB =
            BLOCKS.register("stripped_carpet_floor_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<StairBlock> WALL_YELLOW_STAIRS =
            BLOCKS.register("wall_yellow_stairs", () -> new StairBlock(WALL_YELLOW.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<StairBlock> WALL_DAMP_STAIRS =
            BLOCKS.register("wall_damp_stairs", () -> new StairBlock(WALL_DAMP.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<StairBlock> WALL_CONCRETE_STAIRS =
            BLOCKS.register("wall_concrete_stairs", () -> new StairBlock(WALL_CONCRETE.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<StairBlock> CRACKED_CONCRETE_WALL_STAIRS =
            BLOCKS.register("cracked_concrete_wall_stairs", () -> new StairBlock(CRACKED_CONCRETE_WALL.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<StairBlock> FLOOR_TILE_STAIRS =
            BLOCKS.register("floor_tile_stairs", () -> new StairBlock(FLOOR_TILE.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<StairBlock> DAMP_FLOOR_STAIRS =
            BLOCKS.register("damp_floor_stairs", () -> new StairBlock(DAMP_FLOOR.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<StairBlock> STRIPPED_CARPET_FLOOR_STAIRS =
            BLOCKS.register("stripped_carpet_floor_stairs", () -> new StairBlock(STRIPPED_CARPET_FLOOR.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<FenceBlock> WALL_YELLOW_FENCE =
            BLOCKS.register("wall_yellow_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> WALL_DAMP_FENCE =
            BLOCKS.register("wall_damp_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> WALL_CONCRETE_FENCE =
            BLOCKS.register("wall_concrete_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> CRACKED_CONCRETE_WALL_FENCE =
            BLOCKS.register("cracked_concrete_wall_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> FLOOR_TILE_FENCE =
            BLOCKS.register("floor_tile_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> DAMP_FLOOR_FENCE =
            BLOCKS.register("damp_floor_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> STRIPPED_CARPET_FLOOR_FENCE =
            BLOCKS.register("stripped_carpet_floor_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    /** 金属门：深灰黑色的白桦木门，可开关，用于小径入口。 */
    public static final DeferredBlock<com.the_backrooms_complex.block.MetalDoorBlock> METAL_DOOR =
            BLOCKS.register("metal_door", () -> new com.the_backrooms_complex.block.MetalDoorBlock(unbreakable().noOcclusion()));

    /** 色泽灰暗的神秘的门：右键传送到 Level 1 的固定安全出生点，不能打开。 */
    public static final DeferredBlock<com.the_backrooms_complex.block.MysteriousDoorBlock> MYSTERIOUS_DOOR =
            BLOCKS.register("mysterious_door", () -> new com.the_backrooms_complex.block.MysteriousDoorBlock(unbreakable().noOcclusion()));

    // ===== Level 1 的混凝土构件 =====
    /** 混凝土柱（贴图使用原版 light_gray_concrete）。 */
    public static final DeferredBlock<Block> CONCRETE_PILLAR =
            BLOCKS.register("concrete_pillar", () -> new Block(unbreakable()));
    /** 混凝土警示柱（贴图使用原版 reinforced_deepslate）。 */
    public static final DeferredBlock<Block> CONCRETE_WARNING_PILLAR =
            BLOCKS.register("concrete_warning_pillar", () -> new Block(unbreakable()));
    /** 混凝土地板（贴图使用原版 stone）。 */
    public static final DeferredBlock<Block> CONCRETE_FLOOR =
            BLOCKS.register("concrete_floor", () -> new Block(unbreakable()));
    /** 混凝土天花板（贴图使用原版 smooth_stone）。 */
    public static final DeferredBlock<Block> CONCRETE_CEILING =
            BLOCKS.register("concrete_ceiling", () -> new Block(unbreakable()));

    /** 粉刷为白色的混凝土墙壁（贴图使用原版 stripped_pale_oak_log）。 */
    public static final DeferredBlock<Block> WHITE_PLASTERED_WALL =
            BLOCKS.register("white_plastered_wall", () -> new Block(unbreakable()));

    // ===== Level 1 方块的 楼梯 / 台阶 / 墙 / 栅栏 变体 =====
    public static final DeferredBlock<StairBlock> CONCRETE_PILLAR_STAIRS =
            BLOCKS.register("concrete_pillar_stairs", () -> new StairBlock(
                    CONCRETE_PILLAR.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<SlabBlock> CONCRETE_PILLAR_SLAB =
            BLOCKS.register("concrete_pillar_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> CONCRETE_PILLAR_WALL =
            BLOCKS.register("concrete_pillar_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> CONCRETE_PILLAR_FENCE =
            BLOCKS.register("concrete_pillar_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    public static final DeferredBlock<StairBlock> CONCRETE_WARNING_PILLAR_STAIRS =
            BLOCKS.register("concrete_warning_pillar_stairs", () -> new StairBlock(
                    CONCRETE_WARNING_PILLAR.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<SlabBlock> CONCRETE_WARNING_PILLAR_SLAB =
            BLOCKS.register("concrete_warning_pillar_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> CONCRETE_WARNING_PILLAR_WALL =
            BLOCKS.register("concrete_warning_pillar_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> CONCRETE_WARNING_PILLAR_FENCE =
            BLOCKS.register("concrete_warning_pillar_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    public static final DeferredBlock<StairBlock> CONCRETE_FLOOR_STAIRS =
            BLOCKS.register("concrete_floor_stairs", () -> new StairBlock(
                    CONCRETE_FLOOR.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<SlabBlock> CONCRETE_FLOOR_SLAB =
            BLOCKS.register("concrete_floor_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> CONCRETE_FLOOR_WALL =
            BLOCKS.register("concrete_floor_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> CONCRETE_FLOOR_FENCE =
            BLOCKS.register("concrete_floor_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    public static final DeferredBlock<StairBlock> CONCRETE_CEILING_STAIRS =
            BLOCKS.register("concrete_ceiling_stairs", () -> new StairBlock(
                    CONCRETE_CEILING.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<SlabBlock> CONCRETE_CEILING_SLAB =
            BLOCKS.register("concrete_ceiling_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> CONCRETE_CEILING_WALL =
            BLOCKS.register("concrete_ceiling_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> CONCRETE_CEILING_FENCE =
            BLOCKS.register("concrete_ceiling_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    public static final DeferredBlock<StairBlock> WHITE_PLASTERED_WALL_STAIRS =
            BLOCKS.register("white_plastered_wall_stairs", () -> new StairBlock(
                    WHITE_PLASTERED_WALL.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<SlabBlock> WHITE_PLASTERED_WALL_SLAB =
            BLOCKS.register("white_plastered_wall_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> WHITE_PLASTERED_WALL_WALL =
            BLOCKS.register("white_plastered_wall_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> WHITE_PLASTERED_WALL_FENCE =
            BLOCKS.register("white_plastered_wall_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));

    // ===== 跃金段（Level 1 第三区段）=====
    public static final DeferredBlock<Block> YELLOWED_CONCRETE_FLOOR =
            BLOCKS.register("yellowed_concrete_floor", () -> new Block(unbreakable()));
    public static final DeferredBlock<Block> YELLOWED_CONCRETE_WALL =
            BLOCKS.register("yellowed_concrete_wall", () -> new Block(unbreakable()));
    public static final DeferredBlock<Block> YELLOWED_CORRUGATED_METAL =
            BLOCKS.register("yellowed_corrugated_metal", () -> new Block(unbreakable()));
    public static final DeferredBlock<StairBlock> YELLOWED_CONCRETE_FLOOR_STAIRS =
            BLOCKS.register("yellowed_concrete_floor_stairs", () -> new StairBlock(YELLOWED_CONCRETE_FLOOR.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<SlabBlock> YELLOWED_CONCRETE_FLOOR_SLAB =
            BLOCKS.register("yellowed_concrete_floor_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> YELLOWED_CONCRETE_FLOOR_WALL =
            BLOCKS.register("yellowed_concrete_floor_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> YELLOWED_CONCRETE_FLOOR_FENCE =
            BLOCKS.register("yellowed_concrete_floor_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    public static final DeferredBlock<StairBlock> YELLOWED_CONCRETE_WALL_STAIRS =
            BLOCKS.register("yellowed_concrete_wall_stairs", () -> new StairBlock(YELLOWED_CONCRETE_WALL.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<SlabBlock> YELLOWED_CONCRETE_WALL_SLAB =
            BLOCKS.register("yellowed_concrete_wall_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> YELLOWED_CONCRETE_WALL_WALL =
            BLOCKS.register("yellowed_concrete_wall_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> YELLOWED_CONCRETE_WALL_FENCE =
            BLOCKS.register("yellowed_concrete_wall_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));
    public static final DeferredBlock<StairBlock> YELLOWED_CORRUGATED_METAL_STAIRS =
            BLOCKS.register("yellowed_corrugated_metal_stairs", () -> new StairBlock(YELLOWED_CORRUGATED_METAL.get().defaultBlockState(), unbreakable()));
    public static final DeferredBlock<SlabBlock> YELLOWED_CORRUGATED_METAL_SLAB =
            BLOCKS.register("yellowed_corrugated_metal_slab", () -> new SlabBlock(unbreakable()));
    public static final DeferredBlock<WallBlock> YELLOWED_CORRUGATED_METAL_WALL =
            BLOCKS.register("yellowed_corrugated_metal_wall", () -> new com.the_backrooms_complex.block.ConnectingWallBlock(unbreakable()));
    public static final DeferredBlock<FenceBlock> YELLOWED_CORRUGATED_METAL_FENCE =
            BLOCKS.register("yellowed_corrugated_metal_fence", () -> new com.the_backrooms_complex.block.ConnectingFenceBlock(unbreakable()));

    /** 板条箱：跃金段的可搜刮木箱（战利品见后续实现）。 */
    public static final DeferredBlock<Block> CRATE =
            BLOCKS.register("crate", () -> new com.the_backrooms_complex.block.CrateBlock(
                    unbreakable().noOcclusion().randomTicks()));

    private static BlockBehaviour.Properties unbreakable() {
        return BlockBehaviour.Properties.of()
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .sound(SoundType.STONE);
    }

    /** 「菌毯」：潮湿房间地板上的灰黑菌丝地毯。 */
    public static final DeferredBlock<MyceliumCarpetBlock> MYCELIUM_CARPET =
            BLOCKS.register("mycelium_carpet", MyceliumCarpetBlock::new);

    /** 「熄灭的荧光灯」：灯光异常闪烁时的替身方块。 */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> LAMP_OFF =
            BLOCKS.register("lamp_off", () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .strength(-1.0F, 3600000.0F)
                            .lightLevel(state -> 0)));

    private ModBlocks() {
    }
}