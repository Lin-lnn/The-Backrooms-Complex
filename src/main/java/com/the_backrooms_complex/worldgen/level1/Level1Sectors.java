package com.the_backrooms_complex.worldgen.level1;

import net.minecraft.core.Direction;

/**
 * Level 1 的区段与分层纯计算。
 *
 * <p>本类不持有任何 {@code BlockState}、不访问注册表、不做区块写入，
 * 只回答「这一列属于哪个区段」「这里是不是区段边界 / 通道」以及
 * 「这一层有多高」。生成器、群系源与闪烁现象都从这里取层高常量，
 * 避免再出现 12 / 18 两套数字。</p>
 */
public final class Level1Sectors {

    // ==================== 分层常量 ====================

    /** 单层高度（格）。 */
    public static final int LAYER_H = 12;
    /** 子层高度（小径用）。 */
    public static final int SUB_H = 6;
    /** 子层数量。 */
    public static final int SUB_COUNT = 2;

    /**
     * 实际生成的层数。
     *
     * <p>世界高度是 288（{@code 288 / 12 = 24} 层），但只生成 {@value #LAYERS} 层，其余留空 ——
     * 逐方块循环的上界因此从 288 格降到 {@code 3 × 12 = 36} 格，区块生成快约 8 倍。</p>
     */
    public static final int LAYERS = 3;

    // ==================== 区段 ====================

    /** 区段单元边长（格）。 */
    public static final int SECTOR_CELL = 96;

    /** 区段边界通道宽度（每 16 格留 2 格可通行）。 */
    public static final int SECTOR_GATE_STEP = 16;

    /** Level 1 的区段。 */
    public enum Region {
        AQUILA, YUEJIN, GOTHIC, OUROBOROS, GARDEN, LEGEND, CORRIDOR
    }

    /** 每层的区段计划：主区段在前，占比最大。 */
    private static final Region[][] LAYER_PLAN = {
        { Region.AQUILA, Region.GOTHIC, Region.LEGEND },        // 第 0 层：经典停车场
        { Region.YUEJIN, Region.GARDEN, Region.OUROBOROS },     // 第 1 层：泛黄仓库层
        { Region.OUROBOROS, Region.GOTHIC, Region.LEGEND },     // 第 2 层：毛坯层
    };

    /** 与 {@link #LAYER_PLAN} 对应的百分比权重。 */
    private static final int[][] LAYER_WEIGHTS = {
        { 70, 15, 15 },
        { 70, 15, 15 },
        { 50, 25, 25 },
    };

    /**
     * 该列属于哪个区段（按第 0 层判定，供群系 / 染色等无层信息的地方使用）。
     */
    public static Region regionAt(int x, int z) {
        return regionAt(x, z, 0);
    }

    /**
     * 该列在指定层上属于哪个区段。
     *
     * <p>{@code layer} 参与哈希，因此<b>每一层的区段分布都不一样</b> ——
     * 这正是「每层结构不同」的来源。</p>
     */
    public static Region regionAt(int x, int z, int layer) {
        return regionAt0(x, z, layer);
    }

    /**
     * 按层决定区段。
     *
     * <p>每层有自己的主区段与权重表，因此三层观感明显不同，同时层内仍保留
     * 多区段的多样性。层号同时进入哈希，所以同一列在三层的区段不会重合。</p>
     */
    private static Region regionAt0(int x, int z, int layer) {
        int plan = Math.floorMod(layer, LAYER_PLAN.length);
        Region[] regions = LAYER_PLAN[plan];
        int[] weights = LAYER_WEIGHTS[plan];
        int cx = Math.floorDiv(x, SECTOR_CELL);
        int cz = Math.floorDiv(z, SECTOR_CELL);
        long h = mix(cx, cz, 0x736563746F72L ^ (layer * 0x9E3779B97F4A7C15L));
        double roll = ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) * 100.0D;
        double accumulated = 0.0D;
        for (int i = 0; i < regions.length; i++) {
            accumulated += weights[i];
            if (roll < accumulated) {
                return regions[i];
            }
        }
        return regions[0];
    }

    // ==================== 边界与通道 ====================

    /** 该列是否位于区段边界上（按第 0 层）。 */
    public static boolean isSectorEdge(int x, int z) {
        return isSectorEdge(x, z, 0);
    }

    /** 指定层上的区段边界判定（四个水平方向都检查，避免只补一侧）。 */
    public static boolean isSectorEdge(int x, int z, int layer) {
        Region here = regionAt(x, z, layer);
        return regionAt(x + 1, z, layer) != here || regionAt(x - 1, z, layer) != here
                || regionAt(x, z + 1, layer) != here || regionAt(x, z - 1, layer) != here;
    }

    /** 该区段是否是「两层」结构（只有两层的区段才需要楼梯）。 */
    public static boolean isStackedRegion(Region r) {
        return switch (r) {
            case YUEJIN, GOTHIC, AQUILA -> false;
            default -> true;
        };
    }

    /** 该区段的「单层高度」：两层区段取 SUB_H，通高区段取 LAYER_H。 */
    public static int storeyHeight(Region region) {
        return isStackedRegion(region) ? SUB_H : LAYER_H;
    }

    /** 边界另一侧的区段（找不到不同区段时返回自身）。 */
    public static Region otherRegionAcross(int x, int z, int layer, Region here) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int nx = x + direction.getStepX();
            int nz = z + direction.getStepZ();
            Region there = regionAt(nx, nz, layer);
            if (there != here) {
                return there;
            }
        }
        return here;
    }

    /**
     * 该列是否位于「通道开口」上。
     *
     * <p>沿边界的走向每 32 格开一条 4 格宽的通道：边界法线为 X 时沿 Z 取模，
     * 反之沿 X 取模。</p>
     */
    public static boolean isTunnelOpening(int x, int z, int layer) {
        Region here = regionAt(x, z, layer);
        // 先看 X 方向是否跨界（此时通道沿 Z 展开），再退回 Z 方向
        boolean crossX = regionAt(x + 1, z, layer) != here || regionAt(x - 1, z, layer) != here;
        boolean crossZ = regionAt(x, z + 1, layer) != here || regionAt(x, z - 1, layer) != here;
        if (!crossX && !crossZ) {
            return false;
        }
        int along = crossX ? z : x;
        return Math.floorMod(along, 32) < 4;
    }

    /**
     * 该高度开门是否安全。
     *
     * <p>只在每层最底部的两格开门：那里两侧都一定有地板（地板在层内第 0 格）。
     * 早先允许堆叠区段在第二层开门，但隔壁若是通高区段就没有地板，门孔会悬在空中。</p>
     */
    public static boolean isSafeGateHeight(int offset, boolean thisStacked) {
        int inSub = offset % LAYER_H;
        return inSub == 1 || inSub == 2;
    }

    /** 边界上的通道位置（每 16 格 2 格宽）。 */
    public static boolean isSectorGate(int x, int z) {
        return Math.floorMod(x + z, SECTOR_GATE_STEP) < 2;
    }

    // ==================== 哈希 ====================

    /** 64 位混合哈希（区段 / 地物 / 几何共用）。 */
    public static long mix(long a, long b, long c) {
        long h = a * 0x9E3779B97F4A7C15L;
        h ^= b * 0xC2B2AE3D27D4EB4FL;
        h ^= c * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    private Level1Sectors() {
    }
}
