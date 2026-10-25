package com.the_backrooms_complex.worldgen.level1;

import com.the_backrooms_complex.ModBlocks;
import com.the_backrooms_complex.worldgen.level1.Level1Sectors.Region;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import static com.the_backrooms_complex.worldgen.level1.Level1Sectors.*;

/**
 * Level 1 的区段地物：灯位、板条箱、草簇、藤蔓、碎屑与管道。
 *
 * <p>这里是「某格要不要放地物、放哪一种」的判定与状态构造；
 * 真正把方块写进区块仍由生成器负责。地物只依赖坐标哈希和本层区段，
 * 因此不缓存、不持有世界状态。</p>
 */
public final class Level1Decor {

    // ==================== 灯位 ====================

    /** 天鹰 / 走廊 / 传说段的灯距（格）。 */
    public static final int LAMP_STEP = 5;
    /** 小径荧光灯间距（格）。 */
    public static final int PASSAGE_LAMP_STEP = 3;

    /** 天鹰 / 走廊段的灯位判定。 */
    public static boolean isLamp(int x, int z) {
        return Math.floorMod(x, LAMP_STEP) == 2 && Math.floorMod(z, LAMP_STEP) == 2;
    }

    /** 小径天花板灯位：每 3 格一个，居中偏移 1 格。 */
    public static boolean isPassageLamp(int x, int z) {
        return Math.floorMod(x, PASSAGE_LAMP_STEP) == 1
                && Math.floorMod(z, PASSAGE_LAMP_STEP) == 1;
    }

    /** 跃金段灯位：每 2 格一盏（比过道的每 5 格更亮）。 */
    public static boolean isYuejinLamp(int x, int z) {
        return Math.floorMod(x, 2) == 0 && Math.floorMod(z, 2) == 0;
    }

    /** 哥特段灯位：每 7 格一盏（比其他区段更暗，仅拱顶下有光）。 */
    public static boolean isGothicLamp(int x, int z) {
        return Math.floorMod(x, 7) == 3 && Math.floorMod(z, 7) == 3;
    }

    /** 衔尾段灯位：每 6 格一盏，且只挂在天花板下（模拟裸露的灯管）。 */
    public static boolean isOuroborosLamp(int x, int z) {
        return Math.floorMod(x, 6) == 2 && Math.floorMod(z, 6) == 2;
    }

    /** 花园段灯位：每 8 格一盏，且偏暖（模拟阳光透过藤蔓）。 */
    public static boolean isGardenLamp(int x, int z) {
        return Math.floorMod(x, 8) == 4 && Math.floorMod(z, 8) == 4;
    }

    /** 传说段灯位：每 5 格一盏长条日光灯。 */
    public static boolean isLegendLamp(int x, int z) {
        return Math.floorMod(x, 5) == 2 && Math.floorMod(z, 5) == 2;
    }

    // ==================== 区段范围 ====================

    // ==================== 板条箱 ====================

    /** 跃金段板条箱密度（地面格比例）。 */
    public static final double CRATE_CHANCE_YUEJIN = 0.0012D;

    /** 其他区段的板条箱密度：跃金段的十分之一。 */
    public static final double CRATE_CHANCE_OTHER = CRATE_CHANCE_YUEJIN / 10.0D;

    /**
     * 板条箱落点。
     *
     * <p>跃金段最密（约 0.12%），其他区段只有它的十分之一（约 0.012%）。</p>
     */
    public static boolean isCrateSpot(int x, int z, Region region) {
        long h = mix(x, z, 0x637261746573L);   // 'crates'
        double roll = (h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL;
        return roll < (region == Region.YUEJIN ? CRATE_CHANCE_YUEJIN : CRATE_CHANCE_OTHER);
    }

    // ==================== 碎屑 ====================

    /** 碎屑方块：砂砾为主，混入淡灰与白色混凝土粉末。 */
    public static BlockState debrisState(int x, int y, int z) {
        long h = mix(x, y * 31 + z, 0x6465627269734CL);   // 'debris'
        int pick = (int) Math.floorMod(h >>> 7, 100L);
        if (pick < 62) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (pick < 82) {
            return Blocks.LIGHT_GRAY_CONCRETE_POWDER.defaultBlockState();
        }
        return Blocks.WHITE_CONCRETE_POWDER.defaultBlockState();
    }

    /** 衔尾段的地面碎屑：约 3%。 */
    public static boolean isOuroborosDebris(int x, int z) {
        long h = mix(x, z, 0x646562726973L);   // 'debris'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.03;
    }

    /** 衔尾段墙面剥落：约 12%。 */
    public static boolean isOuroborosPeeling(int x, int z, int offset) {
        long h = mix(x * 31 + z, offset, 0x7065656CL);   // 'peel'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.12;
    }

    // ==================== 植物 ====================

    /**
     * 该列是否有一根从天花板垂到地面的藤条。
     *
     * <p>用较稀疏的哈希撒点，密度约 6%，形成「垂落的藤帘」。</p>
     */
    public static boolean isGardenVine(int x, int z) {
        long h = mix(x, z, 0x76696E6573L);   // 'vines'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.06;
    }

    /** 花园段草簇落点：约 30%。 */
    public static boolean isGardenTuft(int x, int z) {
        long h = mix(x, z, 0x74756666L);   // 'tuff'
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.30;
    }

    /** 藤蔓在当前格应贴的方向；没有可贴的墙时返回 {@code null}。 */
    public static Direction vineFace(int x, int z, int layer) {
        if (isSectorEdge(x - 1, z, layer)) {
            return Direction.WEST;
        }
        if (isSectorEdge(x + 1, z, layer)) {
            return Direction.EAST;
        }
        if (isSectorEdge(x, z - 1, layer)) {
            return Direction.NORTH;
        }
        if (isSectorEdge(x, z + 1, layer)) {
            return Direction.SOUTH;
        }
        return null;
    }

    /** 方向 -> 藤蔓方块的方向属性。 */
    public static BooleanProperty vineProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> VineBlock.NORTH;
            case SOUTH -> VineBlock.SOUTH;
            case EAST -> VineBlock.EAST;
            default -> VineBlock.WEST;
        };
    }

    /** 传说段墙面上部的磨砂玻璃格：每 3 格一块，嵌在木护墙板上。 */
    public static boolean isLegendFrostedPanel(int x, int z, int offset) {
        if (offset < 8 || offset > 10) {
            return false;
        }
        return Math.floorMod(x, 3) != 0 && Math.floorMod(z, 3) != 0;
    }

    // ==================== 管道 ====================

    /**
     * 衔尾段 / 传说段天花板管道：横向与纵向按各自步长铺设，交叉处四向连通。
     *
     * @return 该列应放的管道状态；不是管线时返回 {@code null}
     */
    public static BlockState pipeAt(Region region, int x, int z) {
        // 衔尾段用红漆管道（每 8 格），传说段用灰色管道（每 12 格）
        int step;
        net.neoforged.neoforge.registries.DeferredBlock<?> block;
        if (region == Region.OUROBOROS) {
            step = 8;
            block = ModBlocks.RED_PIPE;
        } else if (region == Region.LEGEND) {
            step = 12;
            block = ModBlocks.BRASS_PIPE;
        } else {
            return null;
        }
        int offset = step / 2 + 1;
        boolean eastWest = Math.floorMod(z, step) == offset;
        boolean northSouth = Math.floorMod(x, step) == offset;
        if (!eastWest && !northSouth) {
            return null;
        }
        BlockState state = block.get().defaultBlockState();
        if (eastWest) {
            state = state.setValue(PipeBlock.EAST, Boolean.TRUE)
                    .setValue(PipeBlock.WEST, Boolean.TRUE);
        }
        if (northSouth) {
            state = state.setValue(PipeBlock.NORTH, Boolean.TRUE)
                    .setValue(PipeBlock.SOUTH, Boolean.TRUE);
        }
        // 交叉处向上接天花板，形成吊架（每 step 格一个）
        if (eastWest && northSouth) {
            state = state.setValue(PipeBlock.UP, Boolean.TRUE);
        }
        return state;
    }

    private Level1Decor() {
    }
}