package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModBlocks;
import com.the_backrooms_complex.ModNetwork;
import com.the_backrooms_complex.worldgen.BackroomsChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * 墙壁 / 踢脚线 / 地板 / 天花板的群系颜色过渡（仿原版草方块）。
 *
 * 层高已从 7 改为 8，正好等于 2 个 4 格 section，因此 getBiome(pos) 返回的群系
 * 与生成器逐层选择的群系完全一致，不会跨层串色。
 *
 * 贴图为灰度（保留原纹理图案与明暗），最终颜色由染色器提供：
 * 对方块周围 (2r+1)^2 的每个位置查询群系、取代表色，做 RGB 算术平均。
 */
public final class BiomeTint {

    /** 黄色房间群系的代表色 */
    public static final int HUE_YELLOW = 0x9B9F57;
    /** 混凝土大厅群系的代表色（深灰） */
    public static final int HUE_CONCRETE = 0x333336;
    /** 潮湿走廊群系的代表色 */
    public static final int HUE_DAMP = 0x6F714A;

    /** 群系过渡采样半径（格），(2r+1)^2 个采样点求平均 */
    public static final int BLEND_RADIUS = 4;
    /** 保留原贴图颜色所用的 tintIndex（裙边等部位） */
    public static final int TINT_KEEP = 1;

    /** 精神值低于此值时，所有后室方块统一着暗红色（仅该玩家客户端可见）。 */
    public static final int DARK_RED_SANITY = 5;
    /** 暗红色。 */
    public static final int DARK_RED = 0x741515;

    public static int getBlockColor(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        // 裙边（tintIndex 1）现在也参与 Level0 着色：
        // 它自带较暗的灰度贴图，因此乘同一颜色后自然比墙面深，收边效果保留。
        // 精神值 < 5：整个后室统一变成暗红（各客户端各自判断，因此只对该玩家可见）
        // 仅生存 / 冒险模式生效；创造与旁观模式保持正常配色。
        net.minecraft.client.player.LocalPlayer localPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (localPlayer != null
                && !localPlayer.isCreative() && !localPlayer.isSpectator()
                && ModNetwork.clientSanity < DARK_RED_SANITY) {
            return DARK_RED;
        }
        if (level == null || pos == null) {
            return HUE_YELLOW;
        }
        int layerBaseY = Math.floorDiv(pos.getY(), BackroomsChunkGenerator.LAYER_HEIGHT)
                * BackroomsChunkGenerator.LAYER_HEIGHT;
        int r = 0;
        int g = 0;
        int b = 0;
        int n = 0;
        for (int dx = -BLEND_RADIUS; dx <= BLEND_RADIUS; dx++) {
            for (int dz = -BLEND_RADIUS; dz <= BLEND_RADIUS; dz++) {
                // Y 方向过渡距离 = 0（完全直接过渡）：采样 Y 一律吸附到本层地板高度，
                // 因此颜色在 Y 方向完全不渐变，层与层之间直接切换。
                Holder<Biome> biome = biomeAt(level, pos.offset(dx, 0, dz).atY(layerBaseY));
                if (biome == null) {
                    continue;
                }
                int c = hueOf(biome);
                r += (c >> 16) & 0xFF;
                g += (c >> 8) & 0xFF;
                b += c & 0xFF;
                n++;
            }
        }
        if (n == 0) {
            return HUE_YELLOW;
        }
        return ((r / n) << 16) | ((g / n) << 8) | (b / n);
    }

    public static int getItemColor(net.minecraft.world.item.ItemStack stack, int tintIndex) {
        return tintIndex == TINT_KEEP ? 0xFFFFFF : HUE_YELLOW;
    }

    /** 取该位置的群系：优先 LevelReader，区块编译期回退到客户端世界。 */
    private static Holder<Biome> biomeAt(BlockAndTintGetter level, BlockPos pos) {
        if (level instanceof net.minecraft.world.level.LevelReader reader) {
            return reader.getBiome(pos);
        }
        net.minecraft.client.multiplayer.ClientLevel cl = net.minecraft.client.Minecraft.getInstance().level;
        return cl == null ? null : cl.getBiome(pos);
    }
    private static int hueOf(Holder<Biome> biome) {
        String path = biome.unwrapKey().map(k -> k.location().getPath()).orElse("");
        switch (path) {
            case "yellow_rooms":
                return HUE_YELLOW;
            case "concrete_halls":
                return HUE_CONCRETE;
            case "damp_corridors":
                return HUE_DAMP;
            default:
                // 非后室 Level0 三群系（如主世界、下界）：着为正常黄色调，
                // 避免灰度贴图在其他维度里显示成灰白。
                return HUE_YELLOW;
        }
    }

    private static Block[] tintedBlocks() {
        DeferredBlock<?>[] holders = new DeferredBlock<?>[] {
                ModBlocks.WALL_YELLOW, ModBlocks.WALL_CONCRETE, ModBlocks.WALL_DAMP,
                ModBlocks.WALL_YELLOW_PLINTH, ModBlocks.WALL_CONCRETE_PLINTH, ModBlocks.WALL_DAMP_PLINTH,
                ModBlocks.FLOOR_TILE, ModBlocks.STRIPPED_CARPET_FLOOR, ModBlocks.DAMP_FLOOR,
                ModBlocks.CEILING_TILE, ModBlocks.RUINED_CEILING, ModBlocks.DAMP_CEILING,
                ModBlocks.WALL_OAKED, ModBlocks.CHISELED_CONCRETE_WALL,
                ModBlocks.CRACKED_CONCRETE_WALL, ModBlocks.WARNING_CONCRETE_WALL,
                ModBlocks.RUINED_VENT
        };
        Block[] out = new Block[holders.length];
        for (int i = 0; i < holders.length; i++) {
            out[i] = holders[i].get();
        }
        return out;
    }

    private BiomeTint() {
    }

    @EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
    static final class Events {

        @SubscribeEvent
        public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
            event.register(BiomeTint::getBlockColor, tintedBlocks());
        }

        @SubscribeEvent
        public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
            for (Block b : tintedBlocks()) {
                if (b.asItem() != null) {
                    event.register(BiomeTint::getItemColor, b);
                }
            }
        }

        private Events() {
        }
    }
}