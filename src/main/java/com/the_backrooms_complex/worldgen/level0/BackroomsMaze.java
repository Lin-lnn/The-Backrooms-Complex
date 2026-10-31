package com.the_backrooms_complex.worldgen.level0;

import com.the_backrooms_complex.worldgen.level0.RoomGraph;
import com.the_backrooms_complex.worldgen.level0.TopologyWalls;

/**
 * 后室 Level 0 的走廊布局入口，以及若干供其他系统复用的确定性哈希工具。
 *
 * <h2>布局如何生成</h2>
 * <p>布局由 {@link RoomGraph}（无周期拓扑）+ {@link TopologyWalls}（拓扑转方块）生成，
 * 本类只负责转发「区块坐标 + 层数」。</p>
 *
 * <h2>为什么不再用完美迷宫</h2>
 * <p>旧实现是「瓦片完美迷宫 + 变宽走廊」，有两个无法根治的问题：</p>
 * <ul>
 *   <li><b>死路极多</b>：完美迷宫没有回路，走几步就必然碰壁；</li>
 *   <li><b>周期性</b>：瓦片按固定尺寸对齐，走多远都是同一套图案。</li>
 * </ul>
 * <p>拓扑方案用「哈希抖动的主干节点 + L 形折线」取代瓦片迷宫，
 * 因此<b>无周期</b>，且主干网天然连通、没有死路。</p>
 *
 * <h2>保留的公共成员</h2>
 * <p>{@link #CELL_PITCH} 与 {@link #hash}/{@link #hash3} 仍被结构系统与调试指令复用，
 * 因此保留在本类中（它们与迷宫算法无关，只是确定性哈希工具）。</p>
 */
public final class BackroomsMaze {

    /** 逻辑单元格边长（格）；结构锚点吸附到该网格，保证结构与墙壁对齐。 */
    public static final int CELL_PITCH = 32;

    /** 拓扑生成用的固定种子（确定性；同一世界永远一样）。 */
    private static final long TOPOLOGY_SEED = 0x5EEDBACC0001L;

    private BackroomsMaze() {
    }

    // ==================== 确定性哈希工具 ====================

    /** 二维坐标 + 盐值 → 64 位哈希。 */
    public static long hash(int a, int b, long salt) {
        return hash3(a, b, 0, salt);
    }

    /** 三维坐标 + 盐值 → 64 位哈希（splitmix 风格混合，雪崩性好）。 */
    public static long hash3(int a, int b, int c, long salt) {
        long h = salt;
        h ^= (long) a * 0x9E3779B97F4A7C15L;
        h = Long.rotateLeft(h, 27);
        h ^= (long) b * 0xC2B2AE3D27D4EB4FL;
        h = Long.rotateLeft(h, 31);
        h ^= (long) c * 0x165667B19E3779F9L;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return h;
    }

    // ==================== 布局入口 ====================

    /**
     * 全局唯一的拓扑实例。
     *
     * <p>拓扑是纯函数（同种子同坐标恒定），因此可以安全共享；共享后
     * {@link RoomGraph} 内部的 LRU 查询缓存能<b>跨区块复用</b>，
     * 而原来每区块 new 一个会让缓存每块都被丢弃。</p>
     */
    private static final RoomGraph GRAPH = new RoomGraph(TOPOLOGY_SEED);

    /** 共享拓扑实例（供需要跨区块查询墙体的调用方使用）。 */
    static RoomGraph graph() {
        return GRAPH;
    }

    /**
     * 生成某个区块各层楼的墙体网格。
     *
     * @param chunkX     区块 X
     * @param chunkZ     区块 Z
     * @param layerCount 层数
     * @return {@code walls[层][局部X][局部Z]}，{@code true} = 墙
     */

    public static boolean[][][] generateWallGrids(int chunkX, int chunkZ, int layerCount) {
        boolean[][][] walls = new boolean[layerCount][16][16];
        for (int layer = 0; layer < layerCount; layer++) {
            walls[layer] = TopologyWalls.wallGrid(GRAPH, chunkX, chunkZ, layer);
        }
        return walls;
    }
}