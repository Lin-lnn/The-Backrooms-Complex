package com.yourname.backrooms_complex.worldgen;

/**
 * Level 0 的「房间」布局（纯数学，无 Minecraft 依赖）。
 *
 * <h2>规则</h2>
 * <ul>
 *   <li>房间是<b>矩形空间</b>，长宽都 <b>&gt; 10 且 &lt; 50</b> 格（11~49）；</li>
 *   <li>房间按 80×80 格的单元放置，居中且四周留白，
 *       因此<b>相邻房间的最小间距 ≥ 31 格</b>（远大于要求的 10 格）；</li>
 *   <li>约 25% 的单元放房间，其余保持纯迷宫；</li>
 *   <li>房间内部整块清空成走廊式空地，<b>由既有的迷宫走廊相连</b>；
 *       另外在四条边的中点各开一个 3 格宽的门洞，确保出入口可见；</li>
 *   <li>长或宽<b>大于 20 格</b>的房间内部，均匀布置 <b>1×1 的柱子</b>，
 *       间距 6 格（≥ 要求的最小 3 格），且<b>不贴墙壁</b>（离四边至少 3 格）。</li>
 * </ul>
 *
 * <p>所有随机性只由<b>单元坐标 + 层索引</b>哈希决定：同一层同一位置结果恒定，
 * 且<b>每一层的房间位置完全不同</b>（不会形成竖直叠放的房间柱）。</p>
 */
public final class BackroomsRooms {

    /** 房间单元边长（格）。 */
    public static final int CELL = 51;
    /** 房间最小边长：> 10。 */
    public static final int MIN_SIZE = 11;
    /** 房间最大边长：< 50。 */
    public static final int MAX_SIZE = 49;
    /** 柱子间距（格），必须 ≥ 3。 */
    public static final int PILLAR_SPACING = 6;
    /** 柱子离房间边缘的最小距离（格），保证不贴墙壁。 */
    public static final int PILLAR_MARGIN = 3;
    /** 超过该尺寸的房间才布置柱子。 */
    public static final int PILLAR_THRESHOLD = 20;
    /** 门洞半宽（3 格宽 => 半宽 1）。 */
    public static final int DOOR_HALF = 1;

    /** 哈希盐：用于把坐标打散成确定性随机数 */
    private static final long SALT = 0x9E3779B97F4A7C15L;

    /** 一个房间矩形（世界坐标，含边界）。 */
    public static final class Room {
        public final int minX;
        public final int minZ;
        public final int maxX;
        public final int maxZ;

        Room(int minX, int minZ, int maxX, int maxZ) {
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
        }

        public int width() {
            return this.maxX - this.minX + 1;
        }

        public int length() {
            return this.maxZ - this.minZ + 1;
        }
    }

    /**
     * 若 (x,z) 落在某个房间内则返回它，否则返回 {@code null}。
     *
     * <p>房间始终完整地落在自己的单元内（居中 + 留白），所以只需查询当前单元。</p>
     */
    public static Room roomAt(int x, int z, int layer, boolean hall) {
        int cx = Math.floorDiv(x, CELL);
        int cz = Math.floorDiv(z, CELL);
        long h = hash(cx, cz, layer);

        // 最小间距放宽到 1 格后单元可以很小，用百分比阈值精确控制面积占比：
        //   混凝土大厅 72% 的单元放房间（目标约 10 倍）
        //   其它群系   36% 的单元放房间（目标约 5 倍，正好一半）
        int roll = (int) Math.floorMod(h >>> 4, 100L);
        int threshold = hall ? 85 : 42;
        if (roll >= threshold) {
            return null;
        }

        // 尺寸偏向较大值（取两个随机数的较大者），以提高空间占比
        // 约 1/4 的房间是"小房间"（11~20 格）：天然不会有柱子
        boolean small = ((h >>> 6) & 3L) == 0L;
        int hi = small ? PILLAR_THRESHOLD : MAX_SIZE;
        int span = hi - MIN_SIZE;
        int tw = (int) (Math.max((h >>> 8) & 0xFFFFL, (h >>> 40) & 0xFFFFL) * span / 65535L);
        int tl = (int) (Math.max((h >>> 24) & 0xFFFFL, (h >>> 52) & 0xFFFFL) * span / 65535L);
        int bump = small ? 0 : 5;
        int w = MIN_SIZE + Math.min(span, tw + bump);
        int l = MIN_SIZE + Math.min(span, tl + bump);
        // 房间必须完整落在同一个群系单元内（群系单元 256 格，且整单元同群系）
        // 否则会跨群系 => 违反"房间所属区域必须属于同一群系"
        int biomeCell = 256;
        if (Math.floorDiv(minXof(cx, w), biomeCell) != Math.floorDiv(minXof(cx, w) + w - 1, biomeCell)
                || Math.floorDiv(minZof(cz, l), biomeCell) != Math.floorDiv(minZof(cz, l) + l - 1, biomeCell)) {
            return null;
        }
        int ox = (CELL - w) / 2;
        int oz = (CELL - l) / 2;
        int minX = cx * CELL + ox;
        int minZ = cz * CELL + oz;
        int maxX = minX + w - 1;
        int maxZ = minZ + l - 1;

        if (x < minX || x > maxX || z < minZ || z > maxZ) {
            return null;
        }
        return new Room(minX, minZ, maxX, maxZ);
    }

    /** 是否在房间内部（不在四条边构成的"墙圈"上）。 */
    public static boolean isInterior(Room r, int x, int z) {
        return x > r.minX && x < r.maxX && z > r.minZ && z < r.maxZ;
    }

    /**
     * 是否落在房间的四条边界上（含门洞位置）。
     *
     * <p>生成器据此把房间边界<b>强制设为墙</b>，使房间内部成为规整的矩形空间，
     * 而不是沿用地形原本参差的墙线。</p>
     */
    public static boolean isPerimeter(Room r, int x, int z) {
        boolean insideX = x >= r.minX && x <= r.maxX;
        boolean insideZ = z >= r.minZ && z <= r.maxZ;
        if (!insideX || !insideZ) {
            return false;
        }
        return x == r.minX || x == r.maxX || z == r.minZ || z == r.maxZ;
    }
    /** 是否为四条边的中点门洞（3 格宽）。 */
    public static boolean isDoorway(Room r, int x, int z) {
        int midX = (r.minX + r.maxX) / 2;
        int midZ = (r.minZ + r.maxZ) / 2;
        if (z == r.minZ || z == r.maxZ) {
            return Math.abs(x - midX) <= DOOR_HALF;
        }
        if (x == r.minX || x == r.maxX) {
            return Math.abs(z - midZ) <= DOOR_HALF;
        }
        return false;
    }

    /** 该列是否为房间内的柱子（1×1，间距 6，离墙 ≥ 3）。 */
    public static boolean isPillar(Room r, int x, int z) {
        if (r.width() <= PILLAR_THRESHOLD && r.length() <= PILLAR_THRESHOLD) {
            return false;
        }
        if (x < r.minX + PILLAR_MARGIN || x > r.maxX - PILLAR_MARGIN) {
            return false;
        }
        if (z < r.minZ + PILLAR_MARGIN || z > r.maxZ - PILLAR_MARGIN) {
            return false;
        }
        return Math.floorMod(x - r.minX, PILLAR_SPACING) == PILLAR_MARGIN
                && Math.floorMod(z - r.minZ, PILLAR_SPACING) == PILLAR_MARGIN;
    }

    private static int minXof(int cx, int w) {
        return cx * CELL + (CELL - w) / 2;
    }

    private static int minZof(int cz, int l) {
        return cz * CELL + (CELL - l) / 2;
    }

    /** splitmix64 风格哈希。 */
    private static long hash(int cx, int cz, int layer) {
        // 层索引也参与哈希 => 每一层的房间位置、数量、大小都各不相同
        long h = (long) cx * 0x9E3779B97F4A7C15L
                ^ (long) cz * 0xC2B2AE3D27D4EB4FL
                ^ (long) layer * 0xD6E8FEB86659FD93L
                ^ SALT;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    private BackroomsRooms() {
    }
}