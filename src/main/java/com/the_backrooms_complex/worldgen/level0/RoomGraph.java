package com.the_backrooms_complex.worldgen.level0;


import com.the_backrooms_complex.worldgen.level0.RoomGraph;
import com.the_backrooms_complex.worldgen.level0.TopologyWalls;
/**
 * 「无限室内迷宫拓扑」的核心逻辑层（BaseGraph）。
 *
 * <h2>设计目标</h2>
 * <ul>
 *   <li><b>无周期</b>：不存在 {@code mod(x, K) == 0} 形式的规则结构，
 *       主干节点位置、L 形折线方向、父方向<b>全部由坐标哈希决定</b>；</li>
 *   <li><b>无限连通</b>：格子网格无限 ⇒ 主干节点无限；相邻格子主干由 L 形折线连通
 *       ⇒ 主干网无限连通；非主干点沿父方向曼哈顿距离严格递减 ⇒ 有限步到主干；</li>
 *   <li><b>确定性</b>：同种子同坐标 ⇒ 结果恒定，与调用顺序无关；</li>
 *   <li><b>O(1) 查询</b>：三次哈希 + LRU 缓存；</li>
 *   <li><b>无单向门</b>：所有边判定都经 {@link #canonical} 归一化，两端必然一致。</li>
 * </ul>
 *
 * <h2>分层</h2>
 * <pre>
 *   BaseGraph   （本类）确定性、只读、无限、无周期
 *   Overlay     稀疏、可变、少量（游戏层实现）
 *   IsConnected 合并两层，唯一查询入口（游戏层调用 {@link #baseEdge}）
 * </pre>
 *
 * <p>本类是<b>纯逻辑</b>：不依赖 Minecraft、不依赖渲染、不依赖随机数发生器。</p>
 */
public final class RoomGraph {

    // ==================== 方向 ====================

    /** 六个方向（序号即数组下标）。 */
    public static final int N = 0, E = 1, S = 2, W = 3, UP = 4, DOWN = 5;

    /** N/E/S/W/Up/Down 的向量（Y 为楼层内的第二水平轴）。 */
    private static final int[][] DIR_VEC = {
            {0, -1, 0}, {1, 0, 0}, {0, 1, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}
    };

    /** 反方向。 */
    public static int opposite(int dir) {
        return switch (dir) {
            case N -> S;
            case S -> N;
            case E -> W;
            case W -> E;
            case UP -> DOWN;
            default -> UP;
        };
    }

    // ==================== 坐标（打包成 long 的字段分离） ====================

    /** 房间坐标：X、Z（水平）、Floor（层）。用 long 打包避免对象分配。 */
    public static long coord(long x, long z, int floor) {
        return ((long) floor << 52) ^ ((x & 0x3FFFFFFL) << 26) ^ (z & 0x3FFFFFFL);
    }

    public static long xOf(long c) {
        long v = (c >>> 26) & 0x3FFFFFFL;
        return (v & 0x2000000L) != 0 ? v - 0x4000000L : v;
    }

    public static long zOf(long c) {
        long v = c & 0x3FFFFFFL;
        return (v & 0x2000000L) != 0 ? v - 0x4000000L : v;
    }

    public static int floorOf(long c) {
        return (int) ((c >>> 52) & 0xFFFL);
    }

    /** 相邻坐标。 */
    public static long neighbor(long c, int dir) {
        int[] v = DIR_VEC[dir];
        return coord(xOf(c) + v[0], zOf(c) + v[1], floorOf(c) + v[2]);
    }

    // ==================== 哈希（FNV-1a 64，跨平台一致） ====================

    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    /** FNV-1a 64 位哈希。 */
    public static long hash64(byte[] bytes) {
        long h = FNV_OFFSET;
        for (byte b : bytes) {
            h ^= (b & 0xFFL);
            h *= FNV_PRIME;
        }
        return h;
    }

    /**
     * 坐标哈希：把 (seed, x, z, floor, salt) 按小端序打包后做 FNV-1a。
     *
     * @param salt 盐字符串（不同用途必须用不同盐，避免相关性）
     */
    public static long hashCoord(long seed, long x, long z, int floor, String salt) {
        byte[] saltBytes = salt.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] buf = new byte[8 + 8 + 8 + 4 + saltBytes.length];
        int p = 0;
        for (int i = 0; i < 8; i++) {
            buf[p++] = (byte) (seed >>> (i * 8));
        }
        for (int i = 0; i < 8; i++) {
            buf[p++] = (byte) (x >>> (i * 8));
        }
        for (int i = 0; i < 8; i++) {
            buf[p++] = (byte) (z >>> (i * 8));
        }
        for (int i = 0; i < 4; i++) {
            buf[p++] = (byte) (floor >>> (i * 8));
        }
        System.arraycopy(saltBytes, 0, buf, p, saltBytes.length);
        return hash64(buf);
    }

    // ==================== 基础图参数 ====================

    /** 格子尺度：一个格子多少格方块。 */
    public static final int CELL = 32;
    /** 随机额外边的百分比（0~30）。 */
    public static final int EXTRA_EDGE_PERCENT = 15;

    private final long seed;

    public RoomGraph(long seed) {
        this.seed = seed;
    }

    /** 暴露种子供其他拓扑类使用。 */
    public long seedValue() {
        return this.seed;
    }

    // ==================== 主干节点（哈希抖动） ====================

    /** 格子 (i,j,floor) 的主干节点 X。 */
    public long spineX(int i, int j, int floor) {
        return (long) i * CELL + Math.floorMod(hashCoord(seed, i, j, floor, "gx"), CELL);
    }

    /** 格子 (i,j,floor) 的主干节点 Z（与 gx 用不同盐，避免相关）。 */
    public long spineZ(int i, int j, int floor) {
        return (long) j * CELL + Math.floorMod(hashCoord(seed, i, j, floor, "gz"), CELL);
    }

    // ==================== 缓存（性能） ====================

    /**
     * spineX / spineZ 的缓存。
     *
     * <p>{@code spineX/spineZ} 每次都要做一次 FNV-1a 哈希（跨 21 字节）✓ 而
     * {@code TopologyWalls} 每个方块要查 9~25 次同样的格点 ✓ 一个区块层就是数十万次
     * 重复哈希 ✗ 这是进入维度卡顿的主要原因 ✓</p>
     *
     * <p>这里用一个简单的数组缓存（键 = (i,j,floor) 折叠成 long），命中即 O(1) ✓
     * 容量固定 {@value #CACHE_SIZE} ✓ 满了直接清空（生成是分块的，清空代价很小）✓</p>
     */
    private static final int CACHE_SIZE = 4096;   // 2 的幂，便于位掩码

    /** 键表（0 表示空槽）。 */
    private final long[] cacheKey = new long[CACHE_SIZE];
    /** 值表：高 32 位 = x，低 32 位 = z。 */
    private final long[] cacheVal = new long[CACHE_SIZE];

    /**
     * 取（或计算）格子 (i,j,floor) 的主干节点坐标，返回 {@code [x, z]}。
     *
     * <p><b>直接映射缓存</b>：用键的哈希低位选槽位，查不到就直接覆盖 —— 查找是
     * 严格 O(1)，不会被大量条目拖慢（之前的线性扫描版本反而更慢）。</p>
     */
    private long[] spineCached(int i, int j, int floor) {
        long key = ((long) (i & 0xFFFFF) << 42) | ((long) (j & 0xFFFFF) << 22) | (floor & 0x3FFFFF);
        int slot = (int) ((key * 0x9E3779B97F4A7C15L) >>> 52) & (CACHE_SIZE - 1);
        long stored = this.cacheKey[slot];
        if (stored == key && key != 0L) {
            long v = this.cacheVal[slot];
            return new long[]{v >> 32, v & 0xFFFFFFFFL};
        }
        long x = spineX(i, j, floor);
        long z = spineZ(i, j, floor);
        this.cacheKey[slot] = key;
        this.cacheVal[slot] = ((x & 0xFFFFFFFFL) << 32) | (z & 0xFFFFFFFFL);
        return new long[]{x, z};
    }
    // ==================== 主干判定 ====================

    /**
     * 点 (x,z) 是否位于第 floor 层的主干上。
     *
     * <p>扫描周围 3×3 个格子，检查点是否落在任一 L 形路径上。</p>
     */
    public boolean isOnSpine(long x, long z, int floor) {
        long i0 = Math.floorDiv(x, CELL);
        long j0 = Math.floorDiv(z, CELL);
        for (long i = i0 - 1; i <= i0 + 1; i++) {
            for (long j = j0 - 1; j <= j0 + 1; j++) {
                if (onSpineSegment(x, z, (int) i, (int) j, floor)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 检查点是否落在格子 (i,j) 的四条 L 形主干段上。 */
    private boolean onSpineSegment(long x, long z, int i, int j, int floor) {
        // 东向段：(i,j) -> (i+1,j)
        if (onLPath(x, z, i, j, i + 1, j, floor, "ldir")) {
            return true;
        }
        // 西向段：(i-1,j) -> (i,j)
        if (onLPath(x, z, i - 1, j, i, j, floor, "ldir")) {
            return true;
        }
        // 南向段：(i,j) -> (i,j+1)
        if (onLPath(x, z, i, j, i, j + 1, floor, "ldirNS")) {
            return true;
        }
        // 北向段：(i,j-1) -> (i,j)
        return onLPath(x, z, i, j - 1, i, j, floor, "ldirNS");
    }

    /**
     * 点是否落在连接两个主干节点的 L 形折线上。
     *
     * <p>相邻格子的主干节点必然满足 A.x &lt; B.x（或 A.z &lt; B.z），
     * 因此折线方向由哈希决定，且<b>不含任何周期性判定</b>。</p>
     */
    private boolean onLPath(long x, long z, int ai, int aj, int bi, int bj,
                            int floor, String salt) {
        long[] a = spineCached(ai, aj, floor);
        long[] b = spineCached(bi, bj, floor);
        long ax = a[0];
        long az = a[1];
        long bx = b[0];
        long bz = b[1];
        int ldir = (int) Math.floorMod(hashCoord(seed, ai, aj, floor, salt), 2);

        if (ax > bx || (ax == bx && az > bz)) {
            // 归一化，保证 A 在左/上
            long t = ax; ax = bx; bx = t;
            t = az; az = bz; bz = t;
        }
        if (ldir == 0) {
            // 先水平（z = az）后垂直（x = bx）
            if (z == az && x >= ax && x <= bx) {
                return true;
            }
            return x == bx && z >= Math.min(az, bz) && z <= Math.max(az, bz);
        }
        // 先垂直（x = ax）后水平（z = bz）
        if (x == ax && z >= Math.min(az, bz) && z <= Math.max(az, bz)) {
            return true;
        }
        return z == bz && x >= ax && x <= bx;
    }

    /**
     * 到最近主干<b>线段</b>的垂直距离（格）。
     *
     * <p>关键：这里量的是到<b>线段</b>的距离，而不是到<b>节点</b>的距离。
     * 围绕节点画带会在转角处产生多级台阶；围绕线段画带则得到
     * <b>一条条笔直的走廊</b>，转角是<b>一次完整的 90°</b>。</p>
     *
     * @return 距离；若 3×3 范围内没有主干线段则返回 {@link Integer#MAX_VALUE}
     */
    public int spineDistance(long x, long z, int floor) {
        long i0 = Math.floorDiv(x, CELL);
        long j0 = Math.floorDiv(z, CELL);

        // 先用缓存的节点坐标找出最近的格点（9 次查表，几乎全命中）
        long bestDist = Long.MAX_VALUE;
        int bi = (int) i0;
        int bj = (int) j0;
        for (long i = i0 - 1; i <= i0 + 1; i++) {
            for (long j = j0 - 1; j <= j0 + 1; j++) {
                long[] n = spineCached((int) i, (int) j, floor);
                long d = Math.abs(n[0] - x) + Math.abs(n[1] - z);
                if (d < bestDist) {
                    bestDist = d;
                    bi = (int) i;
                    bj = (int) j;
                }
            }
        }

        // 只算"最近格子"及其 4 邻的线段（5 个格子 × 4 段 = 20 段，比原来少一半）
        int best = distToCellSegments(x, z, bi, bj, floor);
        best = Math.min(best, distToCellSegments(x, z, bi + 1, bj, floor));
        best = Math.min(best, distToCellSegments(x, z, bi - 1, bj, floor));
        best = Math.min(best, distToCellSegments(x, z, bi, bj + 1, floor));
        best = Math.min(best, distToCellSegments(x, z, bi, bj - 1, floor));
        return best;
    }

    /** 到格子 (i,j) 四条 L 形线段的最近距离。 */
    private int distToCellSegments(long x, long z, int i, int j, int floor) {
        int best = Integer.MAX_VALUE;
        best = Math.min(best, segDist(x, z, i, j, i + 1, j, floor, "ldir"));
        best = Math.min(best, segDist(x, z, i - 1, j, i, j, floor, "ldir"));
        best = Math.min(best, segDist(x, z, i, j, i, j + 1, floor, "ldirNS"));
        best = Math.min(best, segDist(x, z, i, j - 1, i, j, floor, "ldirNS"));
        return best;
    }

    /** 到一条 L 形折线（两段轴对齐线段取最近）的距离。 */
    private int segDist(long x, long z, int ai, int aj, int bi, int bj, int floor, String salt) {
        long[] a = spineCached(ai, aj, floor);
        long[] b = spineCached(bi, bj, floor);
        long ax = a[0];
        long az = a[1];
        long bx = b[0];
        long bz = b[1];
        int ldir = (int) Math.floorMod(hashCoord(seed, ai, aj, floor, salt), 2);
        if (ax > bx || (ax == bx && az > bz)) {
            long t = ax; ax = bx; bx = t;
            t = az; az = bz; bz = t;
        }
        int d1;
        int d2;
        if (ldir == 0) {
            // 水平段 z=az, x∈[ax,bx]；垂直段 x=bx, z∈[az,bz]
            d1 = (x >= ax && x <= bx) ? (int) Math.abs(z - az) : Integer.MAX_VALUE;
            d2 = (z >= Math.min(az, bz) && z <= Math.max(az, bz)) ? (int) Math.abs(x - bx) : Integer.MAX_VALUE;
        } else {
            // 垂直段 x=ax, z∈[az,bz]；水平段 z=bz, x∈[ax,bx]
            d1 = (z >= Math.min(az, bz) && z <= Math.max(az, bz)) ? (int) Math.abs(x - ax) : Integer.MAX_VALUE;
            d2 = (x >= ax && x <= bx) ? (int) Math.abs(z - bz) : Integer.MAX_VALUE;
        }
        return Math.min(d1, d2);
    }
    // ==================== 父方向 ====================

    /**
     * 朝最近主干节点的方向。
     *
     * <p>扫描周围 5×5 个格子取曼哈顿距离最近的主干节点。
     * 每沿该方向走一步，距离严格减 1 ⇒ 有限步到主干。</p>
     *
     * @return 方向常量；若已位于某主干节点上则返回 -1
     */
    public int parentDir(long x, long z, int floor) {
        long i0 = Math.floorDiv(x, CELL);
        long j0 = Math.floorDiv(z, CELL);
        long bestX = 0, bestZ = 0;
        long bestDist = Long.MAX_VALUE;
        for (long i = i0 - 1; i <= i0 + 1; i++) {
            for (long j = j0 - 1; j <= j0 + 1; j++) {
                long tx = spineX((int) i, (int) j, floor);
                long tz = spineZ((int) i, (int) j, floor);
                long d = Math.abs(tx - x) + Math.abs(tz - z);
                if (d < bestDist) {
                    bestDist = d;
                    bestX = tx;
                    bestZ = tz;
                }
            }
        }
        if (bestDist == Long.MAX_VALUE) {
            return -1;
        }
        if (bestX > x) {
            return E;
        }
        if (bestX < x) {
            return W;
        }
        if (bestZ > z) {
            return S;
        }
        if (bestZ < z) {
            return N;
        }
        return -1;   // 已在主干节点
    }

    // ==================== 基础边判定 ====================

    /** 规范键：把一条边归一化为「较小的端 + 从该端出发的方向」。 */
    public long canonical(long a, int dir) {
        long b = neighbor(a, dir);
        if (compare(b, a) < 0) {
            return packKey(b, opposite(dir));
        }
        return packKey(a, dir);
    }

    private static int compare(long p, long q) {
        int fp = floorOf(p), fq = floorOf(q);
        if (fp != fq) {
            return Integer.compare(fp, fq);
        }
        long xp = xOf(p), xq = xOf(q);
        if (xp != xq) {
            return Long.compare(xp, xq);
        }
        return Long.compare(zOf(p), zOf(q));
    }

    /** 把 (坐标, 方向) 打包成边键。 */
    public static long packKey(long coord, int dir) {
        return (coord << 3) | (dir & 7L);
    }

    /**
     * 基础图的边判定（只读、确定性、无周期）。
     *
     * <p>判定顺序：</p>
     * <ol>
     *   <li>两端都在主干上 ⇒ 连通；</li>
     *   <li>从 a 出发朝主干的方向就是 d ⇒ 连通；</li>
     *   <li>从 b 出发朝主干的方向是 d 的反向 ⇒ 连通；</li>
     *   <li>否则按规范键做哈希判定，以 {@link #EXTRA_EDGE_PERCENT} 概率连通。</li>
     * </ol>
     */
    public boolean baseEdge(long a, int dir) {
        long b = neighbor(a, dir);
        boolean spineA = isOnSpine(xOf(a), zOf(a), floorOf(a));
        boolean spineB = isOnSpine(xOf(b), zOf(b), floorOf(b));
        if (spineA && spineB) {
            return true;
        }
        if (parentDir(xOf(a), zOf(a), floorOf(a)) == dir) {
            return true;
        }
        if (parentDir(xOf(b), zOf(b), floorOf(b)) == opposite(dir)) {
            return true;
        }
        return isRandomEdge(a, dir);
    }

    /** 按规范键做哈希的随机额外边。 */
    public boolean isRandomEdge(long a, int dir) {
        long key = canonical(a, dir);
        long lo = key >>> 3;
        int loDir = (int) (key & 7L);
        long h = hashCoord(seed, xOf(lo), zOf(lo), floorOf(lo), "rand:" + loDir);
        return Math.floorMod(h, 100L) < EXTRA_EDGE_PERCENT;
    }
}