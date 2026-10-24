package com.the_backrooms_complex.worldgen.topology;

/**
 * 把 {@link RoomGraph} 的拓扑转换为「逐方块的可走/墙壁」网格。
 *
 * <h2>映射规则</h2>
 * <ul>
 *   <li><b>走廊</b>：方块到主干（spine）的曼哈顿距离 ≤ {@link #CORRIDOR_HALF} ⇒ 可走；</li>
 *   <li><b>房间</b>：主干节点周围一个 3~24 格的矩形 ⇒ 可走（尺寸由哈希采样）；</li>
 *   <li><b>其余</b>：墙。</li>
 * </ul>
 *
 * <h2>为什么没有死路</h2>
 * <p>走廊是主干的"加宽带" ⇒ 主干网无限连通 ⇒ 走廊网无限连通 ✓
 * 房间都贴着主干 ⇒ 一定与走廊相连 ✓ 不存在封闭房间 ✓</p>
 *
 * <h2>为什么没有周期</h2>
 * <p>主干位置、L 形折线方向、房间尺寸<b>全部由坐标哈希决定</b> ⇒ 无任何
 * {@code mod(x, K) == 0} 规则 ✓</p>
 */
public final class TopologyWalls {

    /** 走廊半宽：方块到主干的曼哈顿距离 ≤ 该值即可走 ⇒ 走廊宽 2×3+1 = 7 格。 */
    public static final int CORRIDOR_HALF = 3;
    /** 房间最小边长。 */
    public static final int ROOM_MIN = 3;
    /** 房间最大边长。 */
    public static final int ROOM_MAX = 24;
    /** 走到主干的搜索步数上限（必须 ≥ CORRIDOR_HALF + 格内抖动）。 */
    private static final int WALK_STEPS = 40;

    private TopologyWalls() {
    }

    /**
     * 计算某个区块某一层楼的墙体网格。
     *
     * @param graph   拓扑图
     * @param chunkX  区块 X
     * @param chunkZ  区块 Z
     * @param layer   层索引（作为拓扑的 floor）
     * @return {@code walls[16][16]}，true = 墙
     */
    public static boolean[][] wallGrid(RoomGraph graph, int chunkX, int chunkZ, int layer) {
        boolean[][] walls = new boolean[16][16];
        long minX = (long) chunkX * 16;
        long minZ = (long) chunkZ * 16;

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                long x = minX + lx;
                long z = minZ + lz;
                walls[lx][lz] = !isOpen(graph, x, z, layer);
            }
        }
        return walls;
    }

    /** 判断一个方块位置是否可走（走廊或房间）。 */
    public static boolean isOpen(RoomGraph graph, long x, long z, int layer) {
        // ① 走廊：距离主干 ≤ CORRIDOR_HALF
        long cx = x;
        long cz = z;
        for (int step = 0; step <= WALK_STEPS; step++) {
            if (graph.isOnSpine(cx, cz, layer)) {
                return true;
            }
            if (step >= CORRIDOR_HALF) {
                break;   // 超出走廊宽度，不再往前走
            }
            int d = graph.parentDir(cx, cz, layer);
            if (d < 0) {
                break;
            }
            switch (d) {
                case RoomGraph.N -> cz -= 1;
                case RoomGraph.S -> cz += 1;
                case RoomGraph.E -> cx += 1;
                case RoomGraph.W -> cx -= 1;
                default -> {
                }
            }
        }

        // ② 房间：主干节点周围的矩形（尺寸哈希采样）
        long gi = Math.floorDiv(x, RoomGraph.CELL);
        long gj = Math.floorDiv(z, RoomGraph.CELL);
        for (long i = gi - 1; i <= gi + 1; i++) {
            for (long j = gj - 1; j <= gj + 1; j++) {
                if (inRoom(graph, x, z, (int) i, (int) j, layer)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 点是否落在格子 (i,j) 的房间矩形内。 */
    private static boolean inRoom(RoomGraph graph, long x, long z, int i, int j, int layer) {
        long cx = graph.spineX(i, j, layer);
        long cz = graph.spineZ(i, j, layer);
        int w = roomSize(graph, i, j, layer, "roomW");
        int d = roomSize(graph, i, j, layer, "roomD");
        return Math.abs(x - cx) <= w / 2 && Math.abs(z - cz) <= d / 2;
    }

    /** 房间边长采样：3~24，r³ 偏斜（峰值 3~10）。 */
    private static int roomSize(RoomGraph graph, int i, int j, int layer, String salt) {
        long h = RoomGraph.hashCoord(graph.seedValue(), i, j, layer, salt);
        double r = (h >>> 11) / (double) (1L << 53);
        int size = ROOM_MIN + (int) ((ROOM_MAX - ROOM_MIN) * r * r * r);
        return Math.max(ROOM_MIN, Math.min(ROOM_MAX, size));
    }
}
