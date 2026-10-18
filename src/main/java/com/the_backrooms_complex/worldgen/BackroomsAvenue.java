package com.the_backrooms_complex.worldgen;

/**
 * Level 0 的「宽阔走廊地带」（主干道 + 侧向支廊）。纯数学，无 Minecraft 依赖。
 *
 * <h2>规格</h2>
 * <ul>
 *   <li>主干道<b>宽度 &gt; 16 格</b>（17~24），<b>长度 ≥ 50 格</b>（56~96）；</li>
 *   <li>沿主干道两侧<b>每 8 格</b>衍生一条<b>3 格宽</b>的侧向小走廊；</li>
 *   <li>小走廊<b>长度 &gt; 20 格</b>（24 格）；</li>
 *   <li>按 160×160 格单元稀疏放置，<b>每层独立</b>（哈希含层索引）。</li>
 * </ul>
 *
 * <p>主干道沿 X 或 Z 方向，居中于单元；侧廊从主干道两侧向外延伸 24 格，
 * 因 24 &lt; 单元半边长（80），最多只需检查相邻 3×3 个单元。</p>
 */
public final class BackroomsAvenue {

    /** 单元边长（格）。 */
    public static final int CELL = 160;
    /** 主干道宽度范围（必须 > 16）。 */
    public static final int MIN_WIDTH = 17;
    /** 主厅走廊的最大宽度（格） */
    public static final int MAX_WIDTH = 24;
    /** 主干道长度范围（必须 ≥ 50）。 */
    public static final int MIN_LENGTH = 56;
    /** 主厅走廊的最大长度（格） */
    public static final int MAX_LENGTH = 96;
    /** 侧廊间隔（格）。 */
    public static final int BRANCH_EVERY = 8;
    /** 侧廊宽度（格）。 */
    public static final int BRANCH_WIDTH = 3;
    /** 侧廊长度（必须 > 20）。 */
    public static final int BRANCH_LENGTH = 24;
    /** 单元命中率（百分比）。 */
    public static final int RATE = 40;

    /** 哈希盐：用于把坐标打散成确定性随机数 */
    private static final long SALT = 0xA24BAED4963EE407L;

    /** (x,z) 是否属于宽阔走廊地带（主干道或侧廊）。 */
    public static boolean isWideArea(int x, int z, int layer) {
        // 主干道（长 <= 96、宽 <= 24）与侧廊（外延 24）都完整落在本单元内
        // （单元半边长 80 > 48 + 24），因此只需检查当前单元，无需扫描 3x3 邻域。
        int cx = Math.floorDiv(x, CELL);
        int cz = Math.floorDiv(z, CELL);
        return inCell(cx, cz, x, z, layer);
    }

    /** 判断 (x,z) 是否落在由单元 (cx,cz) 产生的主干道或侧廊内。 */
    private static boolean inCell(int cx, int cz, int x, int z, int layer) {
        long h = hash(cx, cz, layer);
        if (Math.floorMod(h >>> 4, 100L) >= RATE) {
            return false;
        }

        boolean alongX = (h & 1L) == 0L;
        int w = MIN_WIDTH + (int) Math.floorMod(h >>> 8, (long) (MAX_WIDTH - MIN_WIDTH + 1));
        int len = MIN_LENGTH + (int) Math.floorMod(h >>> 16, (long) (MAX_LENGTH - MIN_LENGTH + 1));

        int baseX = cx * CELL;
        int baseZ = cz * CELL;
        // 主干道中心
        int cenX = baseX + CELL / 2;
        int cenZ = baseZ + CELL / 2;

        int alongStart;
        int alongEnd;
        int crossMin;
        int crossMax;
        if (alongX) {
            alongStart = cenX - len / 2;
            alongEnd = alongStart + len - 1;
            crossMin = cenZ - w / 2;
            crossMax = crossMin + w - 1;
        } else {
            alongStart = cenZ - len / 2;
            alongEnd = alongStart + len - 1;
            crossMin = cenX - w / 2;
            crossMax = crossMin + w - 1;
        }

        int along = alongX ? x : z;
        int cross = alongX ? z : x;

        // 主干道本体
        if (along >= alongStart && along <= alongEnd && cross >= crossMin && cross <= crossMax) {
            return true;
        }

        // 侧廊：沿主干道每 8 格，两侧各一条 3 格宽、24 格长
        if (along < alongStart || along > alongEnd) {
            return false;
        }
        int phase = Math.floorMod(along - alongStart, BRANCH_EVERY);
        if (phase > BRANCH_WIDTH - 1) {
            return false;
        }
        // 负侧
        if (cross < crossMin && cross >= crossMin - BRANCH_LENGTH) {
            return true;
        }
        // 正侧
        return cross > crossMax && cross <= crossMax + BRANCH_LENGTH;
    }

    /** splitmix64 风格哈希（含层索引）。 */
    private static long hash(int cx, int cz, int layer) {
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

    private BackroomsAvenue() {
    }
}