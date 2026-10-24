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

    /** 走廊宽度（格）：恒定 7，保证墙线完全笔直、无任何宽度接缝。 */
    public static final int CORRIDOR_MIN = 7;
    public static final int CORRIDOR_MAX = 7;

    /** 兼容旧代码用的走廊半宽。 */
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

    /**
     * 判断 (x,z) 的 r 邻域内是否存在主干。
     *
     * <p>只做一次 parentDir 判断方向，再检查该方向的点与四邻，
     * 避免逐格朝主干行走导致每格重复哈希。</p>
     */
    private static boolean nearSpine(RoomGraph graph, long x, long z, int layer, int r) {
        if (graph.isOnSpine(x, z, layer)) {
            return true;
        }
        int d = graph.parentDir(x, z, layer);
        if (d < 0) {
            return false;
        }
        long cx = x;
        long cz = z;
        switch (d) {
            case RoomGraph.N -> cz -= r;
            case RoomGraph.S -> cz += r;
            case RoomGraph.E -> cx += r;
            case RoomGraph.W -> cx -= r;
            default -> { return false; }
        }
        return graph.isOnSpine(cx, cz, layer)
                || graph.isOnSpine(x + 1, z, layer)
                || graph.isOnSpine(x - 1, z, layer)
                || graph.isOnSpine(x, z + 1, layer)
                || graph.isOnSpine(x, z - 1, layer);
    }

    /** 判断一个方块位置是否可走（走廊或房间）。 */
    public static boolean isOpen(RoomGraph graph, long x, long z, int layer) {
        // ① 房间：只取"最近主干节点"所在格子的矩形（只扫 3x3 找最近，不叠加多个房间）
        long gi = Math.floorDiv(x, RoomGraph.CELL);
        long gj = Math.floorDiv(z, RoomGraph.CELL);
        int bestRoom = Integer.MAX_VALUE;
        boolean inAnyRoom = false;
        for (long i = gi - 1; i <= gi + 1; i++) {
            for (long j = gj - 1; j <= gj + 1; j++) {
                if (inRoom(graph, x, z, (int) i, (int) j, layer)) {
                    inAnyRoom = true;
                }
            }
        }
        if (inAnyRoom) {
            return true;
        }

        // ② 走廊：到最近主干【线段】的距离 <= 半宽
        //    围绕线段画带 -> 走廊笔直，转角是一次完整的 90°（不再有台阶）
        // 宽度按"最近主干节点所在格子"取值 -> 一条走廊段内宽度恒定
        // => 台阶只出现在节点处，而节点处正是房间位置，台阶被房间遮住
        long gi2 = Math.floorDiv(x, RoomGraph.CELL);
        long gj2 = Math.floorDiv(z, RoomGraph.CELL);
        long bestD2 = Long.MAX_VALUE;
        int bestI = (int) gi2;
        int bestJ = (int) gj2;
        for (long i = gi2 - 1; i <= gi2 + 1; i++) {
            for (long j = gj2 - 1; j <= gj2 + 1; j++) {
                long tx = graph.spineX((int) i, (int) j, layer);
                long tz = graph.spineZ((int) i, (int) j, layer);
                long dd = Math.abs(tx - x) + Math.abs(tz - z);
                if (dd < bestD2) {
                    bestD2 = dd;
                    bestI = (int) i;
                    bestJ = (int) j;
                }
            }
        }
        int width = corridorWidth(graph, bestI, bestJ, layer);
        int dist = graph.spineDistance(x, z, layer);
        return dist <= width / 2;
    }

    /** 走廊宽度（3~14 格），由格子坐标哈希决定 —— 每格独立，故交界处陡变。 */
    private static int corridorWidth(RoomGraph graph, int i, int j, int layer) {
        long h = RoomGraph.hashCoord(graph.seedValue(), i, j, layer, "corrW");
        return CORRIDOR_MIN + (int) Math.floorMod(h, (long) (CORRIDOR_MAX - CORRIDOR_MIN + 1));
    }

    /** 墙壁最大厚度（格）；超厚的部分会被打通。 */
    public static final int MAX_WALL_THICKNESS = 2;

    /** (x,z) 的四邻中是否有属于房间的格子（用于保护房间边缘不被掏空）。 */
    private static boolean adjacentToRoom(RoomGraph graph, long x, long z, int layer) {
        long[][] dirs = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (long[] d : dirs) {
            long px = x + d[0];
            long pz = z + d[1];
            long gi = Math.floorDiv(px, RoomGraph.CELL);
            long gj = Math.floorDiv(pz, RoomGraph.CELL);
            for (long i = gi - 1; i <= gi + 1; i++) {
                for (long j = gj - 1; j <= gj + 1; j++) {
                    if (inRoom(graph, px, pz, (int) i, (int) j, layer)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 该墙格沿四个方向 maxThickness 步内是否能碰到通道。 */
    private static boolean reachesOpen(boolean[][] walls, int lx, int lz, int maxThickness) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            for (int step = 1; step <= maxThickness; step++) {
                int nx = lx + d[0] * step;
                int nz = lz + d[1] * step;
                if (nx < 0 || nx > 15 || nz < 0 || nz > 15) {
                    continue;
                }
                if (!walls[nx][nz]) {
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
        size = Math.max(ROOM_MIN, Math.min(ROOM_MAX, size));
        if ((size & 1) == 0) {
            size--;   // 强制奇数：房间中心与主干节点对齐，避免半格错位
        }
        return Math.max(ROOM_MIN, size);
    }
}