package com.the_backrooms_complex.worldgen;

/**
 * 后室地图的世界种子来源。
 *
 * <p>生成器本身是纯函数：同一坐标恒定产出同一结果。要让「同一个种子生成同一张地图」，
 * 就把该世界的种子混进每个哈希里 —— 也就是这里的 {@link #salt()}。
 * 服务器启动时由 {@code ModEvents} 写入；未写入时取 0，行为与固定种子一致。</p>
 *
 * <p>种子只影响<b>布局</b>（拓扑、房间、街道、家具），不影响方块种类与层高。</p>
 */
public final class BackroomsSeed {

    /** 未设置时的默认世界种子。 */
    private static final long DEFAULT_SEED = 0L;

    /** 拓扑专用基础盐（与原固定值一致，便于对照）。 */
    private static final long TOPOLOGY_SALT = 0x5EEDBACC0001L;

    private static volatile long worldSeed = DEFAULT_SEED;
    private static volatile long mixedSalt = mix(DEFAULT_SEED);
    private static volatile long graphSeed = DEFAULT_SEED ^ TOPOLOGY_SALT;

    private BackroomsSeed() {
    }

    /** 服务器启动时调用一次。 */
    public static void set(long seed) {
        worldSeed = seed;
        mixedSalt = mix(seed);
        graphSeed = seed ^ TOPOLOGY_SALT;
    }

    /** 混入各哈希的盐值。 */
    public static long salt() {
        return mixedSalt;
    }

    /** 拓扑图使用的种子。 */
    public static long graphSeed() {
        return graphSeed;
    }

    /** 当前世界种子（调试/指令用）。 */
    public static long worldSeed() {
        return worldSeed;
    }

    /** splitmix64 混合，保证相邻种子也产生差异极大的盐。 */
    private static long mix(long value) {
        long h = value + 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }
}
