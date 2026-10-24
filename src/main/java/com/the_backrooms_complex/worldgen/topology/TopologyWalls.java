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

    // ==================== 网格缓存（性能） ====================

    /** 缓存槽位（2 的幂）。 */
    private static final int GRID_CACHE_SLOTS = 256;

    /** 键：chunkX | chunkZ<<20 | layer<<40。 */
    private static final long[] GRID_KEYS = new long[GRID_CACHE_SLOTS];

    /** 值：16×16 墙网格。 */
    private static final boolean[][][] GRID_VALS = new boolean[GRID_CACHE_SLOTS][][];

    private TopologyWalls() {
    }

    /**
     * 带缓存的墙体网格获取。
     *
     * <p>同一个区块的同一层只计算一次 —— 生成器可能因为区块重载 / 结构放置等原因
     * 重复请求同一块数据，这里直接命中缓存。</p>
     */
    public static boolean[][] wallGridCached(RoomGraph graph, int chunkX, int chunkZ, int layer) {
        long key = ((long) chunkX & 0xFFFFFL) | (((long) chunkZ & 0xFFFFFL) << 20)
                | (((long) layer & 0xFFFFFL) << 40);
        int slot = (int) ((key * 0x9E3779B97F4A7C15L) >>> 56) & (GRID_CACHE_SLOTS - 1);
        if (GRID_KEYS[slot] == key && key != 0L && GRID_VALS[slot] != null) {
            return GRID_VALS[slot];
        }
        boolean[][] grid = wallGrid(graph, chunkX, chunkZ, layer);
        GRID_KEYS[slot] = key;
        GRID_VALS[slot] = grid;
        return grid;
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

        // ===== SCHEMATIC_SMOOTH：补平台阶 =====
        // 规则：若一个"通道格"的两条【相邻边】都是墙（= 凹角/台阶内角），
        // 就把它填成墙。这样楼梯状的锯齿会被填平，得到平直的长墙。
        // 这里【忽略墙厚限制】—— 允许墙因此变厚，换取墙线平滑。
        boolean[][] snap = new boolean[16][16];
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                snap[lx][lz] = walls[lx][lz];
            }
        }
        for (int pass = 0; pass < 2; pass++) {
            boolean[][] next = new boolean[16][16];
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    next[lx][lz] = snap[lx][lz];
                }
            }
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    if (snap[lx][lz]) {
                        continue;   // 已经是墙
                    }
                    boolean n = lz > 0 && snap[lx][lz - 1];
                    boolean s = lz < 15 && snap[lx][lz + 1];
                    boolean w = lx > 0 && snap[lx - 1][lz];
                    boolean e = lx < 15 && snap[lx + 1][lz];
                    int wallsAround = (n ? 1 : 0) + (s ? 1 : 0) + (w ? 1 : 0) + (e ? 1 : 0);
                    // 3 面以上是墙 -> 明显是凹角死角，填掉
                    if (wallsAround >= 3) {
                        next[lx][lz] = true;
                    }
                }
            }
            snap = next;
        }
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                walls[lx][lz] = snap[lx][lz];
            }
        }
        return walls;
    }

    /** 判断一个方块位置是否可走（走廊或房间）。 */
    public static boolean isOpen(RoomGraph graph, long x, long z, int layer) {
        // ① 房间：对称方形房间（中心对齐主干节点，边长 = 走廊宽 7 + 2k）
        //    关键：房间以主干节点为中心、且边长恒为奇数 => 它与走廊带的交界是
        //    【左右对称】的，看起来是"门洞扩宽"，而不是错位的楼梯。
        long gi = Math.floorDiv(x, RoomGraph.CELL);
        long gj = Math.floorDiv(z, RoomGraph.CELL);
        for (long i = gi - 1; i <= gi + 1; i++) {
            for (long j = gj - 1; j <= gj + 1; j++) {
                if (inRoom(graph, x, z, (int) i, (int) j, layer)) {
                    return true;
                }
            }
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


    /** (x,z) 的四邻中是否有属于房间的格子（用于保护房间边缘不被掏空）。 */
    /** 该墙格沿四个方向 maxThickness 步内是否能碰到通道。 */
    /** 点是否落在格子 (i,j) 的房间矩形内。 */
    private static boolean inRoom(RoomGraph graph, long x, long z, int i, int j, int layer) {
        long cx = graph.spineX(i, j, layer);
        long cz = graph.spineZ(i, j, layer);
        int w = roomSizeCached(graph, i, j, layer, "roomW");
        int d = roomSizeCached(graph, i, j, layer, "roomD");
        return Math.abs(x - cx) <= w / 2 && Math.abs(z - cz) <= d / 2;
    }

    /** 房间边长缓存（直接映射，避免同一格重复哈希）。 */
    private static final int ROOM_CACHE_SLOTS = 256;
    private static final long[] ROOM_CACHE_KEYS = new long[ROOM_CACHE_SLOTS];
    private static final int[] ROOM_CACHE_VALS = new int[ROOM_CACHE_SLOTS];

    /** 房间边长采样：7 + 2k（k=0..8），并与走廊宽度对称。 */
    private static int roomSizeCached(RoomGraph graph, int i, int j, int layer, String salt) {
        long key = ((long) (i & 0xFFFFF) << 40) | ((long) (j & 0xFFFFF) << 18) | (layer & 0x3FFFF);
        if (salt.equals("roomD")) { key ^= 0x5DEECE66DL; }
        int slot = (int) ((key * 0x9E3779B97F4A7C15L) >>> 56) & (ROOM_CACHE_SLOTS - 1);
        if (ROOM_CACHE_KEYS[slot] == key && key != 0L) {
            return ROOM_CACHE_VALS[slot];
        }
        int v = roomSize(graph, i, j, layer, salt);
        ROOM_CACHE_KEYS[slot] = key;
        ROOM_CACHE_VALS[slot] = v;
        return v;
    }

    /** 房间边长采样：3~24，r³ 偏斜（峰值 3~10）。 */
    private static int roomSize(RoomGraph graph, int i, int j, int layer, String salt) {
        long h = RoomGraph.hashCoord(graph.seedValue(), i, j, layer, salt);
        double r = (h >>> 11) / (double) (1L << 53);
        // 房间边长 = 走廊宽(7) + 2k，k=0..8 => 7,9,11,...,23（全为奇数）
        int k = (int) Math.floor((ROOM_MAX - ROOM_MIN) * r * r * r / 2.0D);
        k = Math.max(0, Math.min(8, k));
        return CORRIDOR_MIN + k * 2;
    }
}