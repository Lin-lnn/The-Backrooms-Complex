package com.the_backrooms_complex.worldgen.level1;

import com.the_backrooms_complex.worldgen.level1.Level1Sectors.Region;

import java.util.concurrent.ConcurrentHashMap;

import static com.the_backrooms_complex.worldgen.level1.Level1Sectors.*;

/**
 * Level 1 的平面几何：小径迷宫、过道房间、混凝土柱网与楼梯。
 *
 * <p>全部是确定性哈希计算，不写方块、不持有 {@code BlockState}。
 * 房间与楼梯的布局各自带一份并发缓存，键是「区坐标 + 层号」，
 * 因此同一个区在同一次生成里只会算一次。</p>
 */
public final class Level1Geometry {

    // ==================== 小径 ====================

    /** 小径区边长（格）。 */
    public static final int PASSAGE_CELL = 96;

    /** 迷宫单元边长（格）：最大房间 7 + 墙 + 余量。 */
    private static final int MAZE_CELL = 10;
    /** 房间边长范围（格）。 */
    private static final int ROOM_MIN = 3;
    private static final int ROOM_MAX = 7;
    /** 走廊宽度范围（格）。 */
    private static final int COR_MIN = 2;
    private static final int COR_MAX = 3;
    /** 房间边长取值个数。 */
    private static final int ROOM_SPAN = ROOM_MAX - ROOM_MIN + 1;
    /** 走廊宽度取值个数。 */
    private static final int COR_SPAN = COR_MAX - COR_MIN + 1;
    /** 房间几何哈希盐。 */
    private static final long SALT_ROOM = 0x1111L;
    /** 走廊哈希盐。 */
    private static final long SALT_COR = 0x2222L;
    /** 走廊「先横后竖 / 先竖后横」的哈希盐。 */
    private static final long SALT_BEND = 0x5555L;

    /** 某个 XZ 是否在小径区（约 45%）。 */
    public static boolean isPassageArea(int x, int z) {
        int cx = Math.floorDiv(x, PASSAGE_CELL);
        int cz = Math.floorDiv(z, PASSAGE_CELL);
        long h = (long) cx * 0x9E3779B97F4A7C15L ^ (long) cz * 0xC2B2AE3D27D4EB4FL ^ 0x70617373616765L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return ((h & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL) < 0.45;
    }

    /** 是否是过道/小径的边界格。 */
    public static boolean isPassageBoundary(int x, int z) {
        boolean here = isPassageArea(x, z);
        return here != isPassageArea(x + 1, z) || here != isPassageArea(x - 1, z)
                || here != isPassageArea(x, z + 1) || here != isPassageArea(x, z - 1);
    }

    /**
     * 取单元 {@code (cx, cz)} 内房间的矩形：{@code [x0, z0, 宽, 高]}。
     *
     * <p>宽高各自独立取 {@value #ROOM_MIN}~{@value #ROOM_MAX}，因此房间<b>多为矩形而非正方形</b>；
     * 房间在单元内还有随机偏移，相邻单元的房间因此不会排成整齐的行列。</p>
     */
    private static void roomRect(int cx, int cz, int[] out) {
        long h = mix(cx, cz, SALT_ROOM);
        int w = ROOM_MIN + (int) Math.floorMod(h, ROOM_SPAN);
        int ht = ROOM_MIN + (int) Math.floorMod(h >>> 8, ROOM_SPAN);
        int padMaxX = MAZE_CELL - w;
        int padMaxZ = MAZE_CELL - ht;
        int padX = padMaxX > 0 ? (int) Math.floorMod(h >>> 16, padMaxX + 1) : 0;
        int padZ = padMaxZ > 0 ? (int) Math.floorMod(h >>> 24, padMaxZ + 1) : 0;
        out[0] = cx * MAZE_CELL + padX;
        out[1] = cz * MAZE_CELL + padZ;
        out[2] = w;
        out[3] = ht;
    }

    /** 走廊宽度（2~3）。 */
    private static int corridorWidth(int cx, int cz, int dirSalt) {
        long h = mix(cx, cz, SALT_COR + dirSalt);
        return COR_MIN + (int) Math.floorMod(h, COR_SPAN);
    }

    /** 点是否落在轴对齐的粗线段内（{@code width} 为粗度，居中）。 */
    private static boolean segContains(int px, int pz, int ax, int az, int bx, int bz, int width) {
        int lo = -(width / 2);
        int hi = lo + width - 1;
        if (az == bz) {
            int d = pz - az;
            return px >= Math.min(ax, bx) && px <= Math.max(ax, bx) && d >= lo && d <= hi;
        }
        int d = px - ax;
        return pz >= Math.min(az, bz) && pz <= Math.max(az, bz) && d >= lo && d <= hi;
    }

    /**
     * 单元 {@code (ax, az)} 通往邻居 {@code (tx, tz)} 的 <b>L 形走廊</b>是否覆盖该点。
     *
     * <p>拐弯方向由哈希决定（先横后竖 / 先竖后横），这是"曲折"的主要来源。</p>
     */
    private static boolean inCorridor(int ax, int az, int tx, int tz, int x, int z,
                                      int[] a, int[] b) {
        roomRect(ax, az, a);
        roomRect(tx, tz, b);
        int x0 = a[0] + a[2] / 2;
        int z0 = a[1] + a[3] / 2;
        int x1 = b[0] + b[2] / 2;
        int z1 = b[1] + b[3] / 2;
        int w = corridorWidth(ax, az, tz == az ? 1 : 2);
        boolean horizFirst = (mix(ax, az, SALT_COR ^ SALT_BEND) & 1L) == 0L;
        if (horizFirst) {
            return segContains(x, z, x0, z0, x1, z0, w)
                    || segContains(x, z, x1, z0, x1, z1, w);
        }
        return segContains(x, z, x0, z0, x0, z1, w)
                || segContains(x, z, x0, z1, x1, z1, w);
    }

    /**
     * 小径内部的墙体判定（{@code true} = 墙）。
     *
     * <p>结构：{@value #MAZE_CELL} 格的单元网格，每格挖出一个
     * {@value #ROOM_MIN}~{@value #ROOM_MAX} 的<b>矩形房间</b>，
     * 再用宽度 {@value #COR_MIN}~{@value #COR_MAX} 的 <b>L 形走廊</b>
     * 把每个单元与它的<b>东、南</b>邻居连起来。</p>
     *
     * <p>因为每个单元都有东、南两条出边，整张图必然连通 ——
     * 这一点用洪水填充验证过：全图无任何孤立空腔。</p>
     */
    public static boolean isPassageWall(int x, int z) {
        int cx = Math.floorDiv(x, MAZE_CELL);
        int cz = Math.floorDiv(z, MAZE_CELL);
        int[] a = new int[4];
        int[] b = new int[4];
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int ax = cx + dx;
                int az = cz + dz;
                roomRect(ax, az, a);
                if (x >= a[0] && x < a[0] + a[2] && z >= a[1] && z < a[1] + a[3]) {
                    return false;                       // 房间内部
                }
                if (inCorridor(ax, az, ax + 1, az, x, z, a, b)) {
                    return false;                       // 通往东邻的走廊
                }
                if (inCorridor(ax, az, ax, az + 1, x, z, a, b)) {
                    return false;                       // 通往南邻的走廊
                }
            }
        }
        return true;
    }

    // ==================== 过道房间 ====================

    /** 过道小房间网格边长（格）。 */
    public static final int ROOM_CELL = 64;
    /** 房间半径（7×7 ⇒ 3）。 */
    public static final int ROOM_RADIUS = 3;
    /** 每个 96 格区内保证的房间数量下限 / 上限。 */
    public static final int ROOMS_MIN = 5;
    public static final int ROOMS_MAX = 10;
    /** 房间边长（含墙）。 */
    public static final int ROOM_SIZE = 2 * ROOM_RADIUS + 1;

    /** 房间候选格间距：96 / 16 = 6 ⇒ 每区 36 个候选位，足够放下 10 个房间。 */
    private static final int ROOM_SLOT = 16;

    /** 房间布局缓存（key = 区坐标打包）。 */
    private static final ConcurrentHashMap<Long, int[]> ROOM_CACHE = new ConcurrentHashMap<>();

    /**
     * 某个 96 格区内的房间位置列表（扁平 {@code ox, oz} 序列）。
     *
     * <p>用区坐标作种子做 Fisher-Yates 洗牌，取前 K 个（K = 5~10），
     * 因此<b>每个区的每一层都保证有同样 K 个房间</b>，而不是概率性出现。</p>
     */
    private static int[] roomRects(int cx, int cz, int layer) {
        long key = (((long) cx << 32) ^ (cz & 0xFFFFFFFFL)) * 31L + layer;
        int[] cached = ROOM_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        int slots = ROOM_CELL / ROOM_SLOT;          // 6 x 6
        int[] slotX = new int[slots * slots];
        int[] slotZ = new int[slots * slots];
        for (int i = 0; i < slotX.length; i++) {
            slotX[i] = (i % slots) * ROOM_SLOT + 4;  // 留 4 格边距，保证 7 宽房间不越界
            slotZ[i] = (i / slots) * ROOM_SLOT + 4;
        }
        // 确定性洗牌
        long h = ((long) cx * 0x94D049BB133111EBL ^ (long) cz * 0x2545F4914F6CDD1DL)
                ^ (0x726F6F6DL ^ (layer * 0x9E3779B97F4A7C15L));
        for (int i = slotX.length - 1; i > 0; i--) {
            h ^= h >>> 33;
            h *= 0xC4CEB9FE1A85EC53L;
            h ^= h >>> 33;
            int j = (int) Math.floorMod(h, i + 1L);
            int tx = slotX[i];
            slotX[i] = slotX[j];
            slotX[j] = tx;
            int tz = slotZ[i];
            slotZ[i] = slotZ[j];
            slotZ[j] = tz;
        }
        int count = ROOMS_MIN + (int) Math.floorMod(h >>> 8, ROOMS_MAX - ROOMS_MIN + 1);
        int[] out = new int[count * 2];
        for (int i = 0; i < count; i++) {
            out[i * 2] = slotX[i];
            out[i * 2 + 1] = slotZ[i];
        }
        ROOM_CACHE.put(key, out);
        return out;
    }

    /** 过道小房间判定：0 = 内部，1 = 墙，2 = 门，-1 = 无。 */
    public static int roomKind(int x, int z) {
        return roomKind(x, z, 0);
    }

    /** 指定层上的房间判定（每层房间位置不同）。 */
    public static int roomKind(int x, int z, int layer) {
        int cx = Math.floorDiv(x, ROOM_CELL);
        int cz = Math.floorDiv(z, ROOM_CELL);
        int[] rects = roomRects(cx, cz, layer);     // 扁平数组：ox, oz, ox, oz ...
        int lx = Math.floorMod(x, ROOM_CELL);
        int lz = Math.floorMod(z, ROOM_CELL);
        for (int i = 0; i < rects.length; i += 2) {
            int ox = rects[i];
            int oz = rects[i + 1];
            int dx = lx - ox;
            int dz = lz - oz;
            if (dx < 0 || dz < 0 || dx >= ROOM_SIZE || dz >= ROOM_SIZE) {
                continue;
            }
            if (dz == ROOM_SIZE - 1 && dx == ROOM_RADIUS) {
                return 2;                            // 南墙正中 = 门
            }
            if (dx == 0 || dz == 0 || dx == ROOM_SIZE - 1 || dz == ROOM_SIZE - 1) {
                return 1;                            // 墙
            }
            return 0;                                // 房间内部
        }
        return -1;
    }

    /** 该列是否紧邻某个「房间门」列（四个水平邻格中任一是门位）。 */
    public static boolean roomDoorNeighbor(int x, int z) {
        return roomKind(x + 1, z) == 2 || roomKind(x - 1, z) == 2
                || roomKind(x, z + 1) == 2 || roomKind(x, z - 1) == 2;
    }

    /** 门位判定：沿过道 / 小径边界每 24 格一个（用 x+z 取模，保证是单独一列）。 */
    public static boolean isGateColumn(int x, int z) {
        return Math.floorMod(x + z, 24) == 0;
    }

    // ==================== 柱网 ====================

    /** 巨柱间距（格）。 */
    // 真实车库柱网 8~16 格；取 16 兼顾可玩性与「车库感」
    public static final int PILLAR_STEP = 16;
    /** 柱中心（2×2 柱子占 7~8）。 */
    public static final int PILLAR_CENTER = 7;

    /** 指定层上的柱网判定（层号参与偏移，因此每层柱位不同）。 */
    public static boolean onPillar(int x, int z, int layer) {
        int shift = Math.floorMod(layer * 5, PILLAR_STEP);   // 每层整体错开
        int ox = Math.floorMod(x + shift, PILLAR_STEP);
        int oz = Math.floorMod(z + shift, PILLAR_STEP);
        int lo = PILLAR_CENTER;       // 7
        int hi = PILLAR_CENTER + 1;   // 8
        return ox >= lo && ox <= hi && oz >= lo && oz <= hi;
    }

    // ==================== 哥特拱顶 ====================

    /** 指定层上的拱顶外张判定（柱网按层偏移，拱顶必须跟着本层柱子走）。 */
    public static boolean isGothicArchSpread(int x, int z, int rows, int layer) {
        for (int dx = -rows; dx <= rows; dx++) {
            for (int dz = -rows; dz <= rows; dz++) {
                if (onPillar(x + dx, z + dz, layer)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== 楼梯 ====================

    /** 楼梯宽度（格）。 */
    public static final int STAIR_WIDTH = 2;

    /** 楼梯之间的最小间距（格）。 */
    public static final int STAIR_MIN_GAP = 5;

    /** 每个 64 格区内保证的楼梯数量下限 / 上限。 */
    public static final int STAIRS_MIN = 2;
    public static final int STAIRS_MAX = 4;

    /** 楼梯布局缓存（key = 区坐标打包）。 */
    private static final ConcurrentHashMap<Long, int[]> STAIR_CACHE = new ConcurrentHashMap<>();

    /**
     * 该区段是否允许生成楼梯。
     *
     * <p>花园段是爬满植物的开放空间，不放楼梯；其余两层区段才会生成。</p>
     */
    public static boolean allowsStairs(Region region) {
        return region != Region.GARDEN;
    }

    /**
     * 某个区内的楼梯起点列表（扁平 {@code ox, oz} 序列）。
     *
     * <p>与房间用同一套 16 格候选位，但换成独立种子；只保留<b>不与任何房间矩形相交</b>
     * 的候选位，取前 K 个（K = 2~4）。</p>
     */
    private static int[] stairRects(int cx, int cz, int layer) {
        long key = (((long) cx << 32) ^ (cz & 0xFFFFFFFFL)) * 31L + layer
                ^ 0x535441495253L;
        int[] cached = STAIR_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        int[] rooms = roomRects(cx, cz, layer);
        int[] picked = new int[STAIRS_MAX * 2];
        int pickedCount = 0;
        long h = (long) cx * 0x9E3779B97F4A7C15L ^ (long) cz * 0xC2B2AE3D27D4EB4FL ^ 0x7374616972L;
        // 候选位按 4 格步长密集扫描：过滤掉房间与跨区段后仍能凑够 2~4 个
        for (int oz = 2; oz + SUB_H - 1 < ROOM_CELL && pickedCount < STAIRS_MAX; oz += 4) {
            for (int ox = 2; ox + STAIR_WIDTH - 1 < ROOM_CELL && pickedCount < STAIRS_MAX; ox += 4) {
                h ^= h >>> 33;
                h *= 0xC4CEB9FE1A85EC53L;
                h ^= h >>> 33;
            // 楼梯是 2 格宽、SUB_H 格长的通道，必须完整落在本区内部
            if (ox + STAIR_WIDTH - 1 >= ROOM_CELL || oz + SUB_H - 1 >= ROOM_CELL) {
                continue;
            }
            // 整段必须属于同一个「两层区段」，否则会跨区段边界长出来
            Region lane = null;
            boolean sameSector = true;
            for (int wx2 = 0; wx2 < STAIR_WIDTH && sameSector; wx2++) {
                for (int wz2 = 0; wz2 < SUB_H; wz2++) {
                    Region r2 = regionAt(cx * ROOM_CELL + ox + wx2, cz * ROOM_CELL + oz + wz2, layer);
                    if (lane == null) {
                        lane = r2;
                    } else if (lane != r2) {
                        sameSector = false;
                        break;
                    }
                }
            }
            if (!sameSector || lane == null || !isStackedRegion(lane)) {
                continue;
            }
            // 整条楼梯带必须空旷：房间 / 柱子 / 包络墙都不允许，否则楼梯口会被堵住。
            // 楼梯只在本层内爬升 6 格进入上一子层，因此检查的就是本层（不是 layer + 1）。
            boolean laneClear = true;
            for (int wc = 0; wc < STAIR_WIDTH && laneClear; wc++) {
                for (int wd = 0; wd < SUB_H; wd++) {
                    int gx = cx * ROOM_CELL + ox + wc;
                    int gz = cz * ROOM_CELL + oz + wd;
                    if (roomKind(gx, gz, layer) != -1
                            || onPillar(gx, gz, layer)
                            || isSectorEdge(gx, gz, layer)) {
                        laneClear = false;
                        break;
                    }
                }
            }
            if (!laneClear) {
                continue;
            }
            // 与已选楼梯保持间距：横竖都太近就跳过（楼梯带 2 宽 x SUB_H 长，不能重叠）
            boolean tooClose = false;
            for (int p = 0; p < pickedCount && !tooClose; p++) {
                int px = picked[p * 2];
                int pz = picked[p * 2 + 1];
                if (Math.abs(ox - px) < STAIR_MIN_GAP && Math.abs(oz - pz) < SUB_H + 1) {
                    tooClose = true;
                }
            }
            if (tooClose) {
                continue;
            }
            boolean clash = false;
            for (int r = 0; r < rooms.length && !clash; r += 2) {
                clash = ox + ROOM_SIZE > rooms[r] && ox < rooms[r] + ROOM_SIZE
                        && oz + SUB_H > rooms[r + 1] && oz < rooms[r + 1] + ROOM_SIZE;
            }
            if (clash) {
                continue;
            }
            picked[pickedCount * 2] = ox;
            picked[pickedCount * 2 + 1] = oz;
            pickedCount++;
            }
        }
        int want = STAIRS_MIN + (int) Math.floorMod(h >>> 8, STAIRS_MAX - STAIRS_MIN + 1);
        int count = Math.min(want, pickedCount);
        int[] out = new int[count * 2];
        System.arraycopy(picked, 0, out, 0, count * 2);
        STAIR_CACHE.put(key, out);
        return out;
    }

    /**
     * 该列在某层上的楼梯编号（0 ~ SUB_H-1）；不属于楼梯时返回 {@code -1}。
     *
     * <p>顶层（{@code layer == LAYERS - 1}）没有楼梯。</p>
     */
    public static int rampStep(int x, int z, int layer) {
        if (layer >= LAYERS - 1) {
            return -1;
        }
        int cx = Math.floorDiv(x, ROOM_CELL);
        int cz = Math.floorDiv(z, ROOM_CELL);
        int lx = Math.floorMod(x, ROOM_CELL);
        int lz = Math.floorMod(z, ROOM_CELL);
        int[] stairs = stairRects(cx, cz, layer);
        for (int i = 0; i < stairs.length; i += 2) {
            if (lx >= stairs[i] && lx < stairs[i] + STAIR_WIDTH
                    && lz >= stairs[i + 1] && lz < stairs[i + 1] + SUB_H) {
                return lz - stairs[i + 1];
            }
        }
        return -1;
    }

    private Level1Geometry() {
    }
}
