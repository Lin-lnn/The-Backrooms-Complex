package com.the_backrooms_complex.worldgen;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 后室 Level 0 的走廊布局（纯函数、完全确定性）。
 *
 * <h2>逐层独立种子</h2>
 * <p><b>每一层的迷宫布局完全不同</b>：所有随机性（瓦片生成树、瓦片间门洞、走廊宽度）
 * 都把<b>层索引</b>作为哈希输入之一，因此第 0 层与第 1 层是两套毫无关联的布局。
 * 每一层仍然各自满足连通性与宽度分布约束。</p>
 *
 * <h2>可变宽度走廊（1~7 格）</h2>
 * <p>水平面被划分为间距 {@link #CELL_PITCH}（8 格）的逻辑单元格网格，每个单元格中心是节点，
 * 节点周围开出 3×3 房间。相连的两个单元格之间铺设一条宽度 1~7 格的走廊段。
 * 8 格间距刚好容纳最宽的 7 格走廊 + 1 格墙。</p>
 *
 * <h2>迷宫拓扑（分块完美迷宫）</h2>
 * <ol>
 *   <li>把平面划分为 {@link #TILE_CELLS}×{@link #TILE_CELLS} 个单元格的"瓦片"（64×64 格）；</li>
 *   <li>每个瓦片内部用<b>递归回溯算法</b>生成一棵完美迷宫生成树（种子 = 瓦片坐标 + 层索引）；</li>
 *   <li>相邻瓦片之间按确定性哈希在共享边界上开<b>一个门洞</b>（同样绑定层索引），
 *       保证该层整个平面连通。</li>
 * </ol>
 *
 * <h2>宽度分配</h2>
 * <p>每条走廊段的宽度按"起点单元格 + 方向 + 层索引"哈希分配，概率分布：
 * 1→1%、2→1.5%、3→10%、4→75%、5→10%、6→1.5%、7→1%。</p>
 *
 * <h2>连通性验证</h2>
 * <p>{@link #generateWallGrids} 在逻辑单元格层面用并查集（{@link UnionFind}）逐层复核连通性，
 * 若（理论不会发生）出现多个分量则确定性重新打通该单元格的走廊作为修复。</p>
 *
 * <p>本类<b>不</b>生成任何竖井：层与层之间不通过竖井连接，跨层连通由「特定结构」承担。</p>
 */
public final class BackroomsMaze {

    /** 逻辑单元格间距（格）：可容纳最宽 7 格走廊 + 1 格墙。 */
    public static final int CELL_PITCH = 8;

    /** 每个瓦片每轴的单元格数（瓦片 = 64×64 格）。 */
    public static final int TILE_CELLS = 8;

    /** 节点房间半径：房间尺寸 = (2*ROOM_HALF+1) = 5×5（原 3×3，更开阔）。 */
    private static final int ROOM_HALF = 2;

    /** 瓦片生成树缓存的容量上限（超过则整体清空，避免无限增长）。 */
    private static final int TILE_CACHE_LIMIT = 8192;

    // 盐值（区分用途，避免相关性）
    private static final long SALT_TILE = 0x243F6A8885A308D3L;
    /** 哈希盐：决定东西向走廊的宽度 */
    private static final long SALT_WIDTH_X = 0x13198A2E03707344L;
    /** 哈希盐：决定南北向走廊的宽度 */
    private static final long SALT_WIDTH_Z = 0xA4093822299F31D0L;
    /** 哈希盐：决定东西向走廊上门的分布 */
    private static final long SALT_DOOR_X = 0x082EFA98EC4E6C89L;
    /** 哈希盐：决定南北向走廊上门的分布 */
    private static final long SALT_DOOR_Z = 0x452821E638D01377L;

    /**
     * 瓦片生成树缓存：key = (tileX, tileZ, layer)。
     * 一个瓦片覆盖 4×4=16 个区块，缓存可把逐层布局的额外开销摊薄十几倍。
     */
    private static final ConcurrentHashMap<Long, boolean[][][]> TILE_CACHE = new ConcurrentHashMap<>();

    private BackroomsMaze() {
    }

    // ==================== 确定性哈希 ====================

    /** 二维坐标哈希（splitmix64 风格）。 */
    public static long hash(int a, int b, long salt) {
        return hash3(a, b, 0, salt);
    }

    /** 三维坐标哈希（第三维用于层索引）。 */
    public static long hash3(int a, int b, int c, long salt) {
        long h = (long) a * 0x9E3779B97F4A7C15L
                ^ (long) b * 0xC2B2AE3D27D4EB4FL
                ^ (long) c * 0x165667B19E3779F9L
                ^ salt;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    /** 映射到 [0,1) 的确定性哈希。 */
    public static double hash01(int a, int b, int layer, long salt) {
        return (hash3(a, b, layer, salt) & 0x7FFFFFFFFFFFFFFFL) / (double) 0x7FFFFFFFFFFFFFFFL;
    }

    // ==================== 逻辑单元格与节点坐标 ====================

    /** 单元格 ci 的节点世界 X 坐标（单元格中心）。 */
    public static int nodeX(int ci) {
        return ci * CELL_PITCH + CELL_PITCH / 2;
    }

    /** 单元格 cj 的节点世界 Z 坐标（单元格中心）。 */
    public static int nodeZ(int cj) {
        return cj * CELL_PITCH + CELL_PITCH / 2;
    }

    // ==================== 瓦片内的完美迷宫（逐层） ====================

    /**
     * 生成瓦片 (tx,tz) 在指定层内部的完美迷宫（零分配实现 + 全局缓存）。
     *
     * @return {@code conn[li][lj][0]} 表示与东侧 (li+1,lj) 连通；
     *         {@code conn[li][lj][1]} 表示与南侧 (li,lj+1) 连通
     */
    private static boolean[][][] tileMaze(int tx, int tz, int layer) {
        long key = ((long) tx & 0x1FFFFFL) << 42 | ((long) tz & 0x1FFFFFL) << 21 | (layer & 0x1FFFFFL);
        boolean[][][] cached = TILE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        boolean[][][] conn = computeTileMaze(tx, tz, layer);
        if (TILE_CACHE.size() > TILE_CACHE_LIMIT) {
            TILE_CACHE.clear();
        }
        TILE_CACHE.put(key, conn);
        return conn;
    }

    private static boolean[][][] computeTileMaze(int tx, int tz, int layer) {
        boolean[][][] conn = new boolean[TILE_CELLS][TILE_CELLS][2];
        boolean[][] visited = new boolean[TILE_CELLS][TILE_CELLS];
        java.util.Random random = new java.util.Random(hash3(tx, tz, layer, SALT_TILE));

        // 手工栈 + 定长候选数组，全程零分配
        int[] stack = new int[TILE_CELLS * TILE_CELLS];
        int top = 0;
        int[] candCell = new int[4];
        int[] candDir = new int[4];

        int startI = random.nextInt(TILE_CELLS);
        int startJ = random.nextInt(TILE_CELLS);
        visited[startI][startJ] = true;
        stack[top++] = startI * TILE_CELLS + startJ;

        while (top > 0) {
            int cur = stack[top - 1];
            int i = cur / TILE_CELLS;
            int j = cur % TILE_CELLS;

            int n = 0;
            if (i > 0 && !visited[i - 1][j]) {
                candCell[n] = (i - 1) * TILE_CELLS + j;
                candDir[n] = 0; // 西邻：由 (i-1,j) 的东连接表示
                n++;
            }
            if (i < TILE_CELLS - 1 && !visited[i + 1][j]) {
                candCell[n] = (i + 1) * TILE_CELLS + j;
                candDir[n] = 1;
                n++;
            }
            if (j > 0 && !visited[i][j - 1]) {
                candCell[n] = i * TILE_CELLS + (j - 1);
                candDir[n] = 2; // 北邻：由 (i,j-1) 的南连接表示
                n++;
            }
            if (j < TILE_CELLS - 1 && !visited[i][j + 1]) {
                candCell[n] = i * TILE_CELLS + (j + 1);
                candDir[n] = 3;
                n++;
            }

            if (n == 0) {
                top--;
                continue;
            }
            int pick = random.nextInt(n);
            int next = candCell[pick];
            int dir = candDir[pick];
            int ni = next / TILE_CELLS;
            int nj = next % TILE_CELLS;

            if (dir == 0) {
                conn[ni][nj][0] = true; // (i-1,j) -- east --> (i,j)
            } else if (dir == 1) {
                conn[i][j][0] = true;   // (i,j)   -- east --> (i+1,j)
            } else if (dir == 2) {
                conn[ni][nj][1] = true; // (i,j-1) -- south --> (i,j)
            } else {
                conn[i][j][1] = true;   // (i,j)   -- south --> (i,j+1)
            }
            visited[ni][nj] = true;
            stack[top++] = next;
        }
        return conn;
    }

    // ==================== 瓦片之间的确定性门洞（逐层） ====================

    /** 瓦片 (tx,tz) 与其东邻瓦片之间门洞所在的局部行号（绑定层索引）。 */
    private static int doorRowEast(int tx, int tz, int layer) {
        return (int) Math.floorMod(hash3(tx, tz, layer, SALT_DOOR_X) >>> 16, (long) TILE_CELLS);
    }

    /** 瓦片 (tx,tz) 与其南邻瓦片之间门洞所在的局部列号（绑定层索引）。 */
    private static int doorColSouth(int tx, int tz, int layer) {
        return (int) Math.floorMod(hash3(tx, tz, layer, SALT_DOOR_Z) >>> 16, (long) TILE_CELLS);
    }

    /** 收集单元格 (ci,cj) 在 +X / +Z 两个方向上连通的邻居（含瓦片内生成树边与瓦片间门洞）。 */
    private static void collectForwardConnections(boolean[][][] conn, int ci, int cj, int layer,
                                                  java.util.List<int[]> out) {
        int tx = Math.floorDiv(ci, TILE_CELLS);
        int tz = Math.floorDiv(cj, TILE_CELLS);
        int li = Math.floorMod(ci, TILE_CELLS);
        int lj = Math.floorMod(cj, TILE_CELLS);

        if (conn[li][lj][0]) {
            out.add(new int[]{ci + 1, cj});
        }
        if (conn[li][lj][1]) {
            out.add(new int[]{ci, cj + 1});
        }
        // 瓦片边界门洞（只由西侧/北侧瓦片负责发出，避免重复）
        if (li == TILE_CELLS - 1 && doorRowEast(tx, tz, layer) == lj) {
            out.add(new int[]{ci + 1, cj});
        }
        if (lj == TILE_CELLS - 1 && doorColSouth(tx, tz, layer) == li) {
            out.add(new int[]{ci, cj + 1});
        }
    }

    /** 分档层数：第 1~12 层为一档，第 13~24 层为二档，第 25 层及往上为三档。 */
    public static final int WIDTH_BAND_LOW = 12;   // 层索引 0..11  -> 第 1~12 层
    /** 宽度分档边界：层索引 < 该值时属于"中层档"（第 13~24 层），否则为高层档 */
    public static final int WIDTH_BAND_MID = 24;   // 层索引 12..23 -> 第 13~24 层（其余为第 25 层以上）

    /** 走廊宽度上限。 */
    public static final int MAX_CORRIDOR_WIDTH = 10;

    /**
     * 走廊宽度<b>统一加成</b>（格），用于把 Level 0 的空气占比整体抬高约 20%。
     *
     * <p>因为上两层的基础宽度本来就大、加成后容易顶到 {@link #MAX_CORRIDOR_WIDTH} 而"吃不满"，
     * 所以三档分别给不同加成，让<b>各档的空气占比增幅都落在 +20% 附近</b>（实测见 README）。</p>
     *
     * <p>全部设为 0 即完全回到原始宽度分布。加宽只会多挖掉墙壁方块，<b>不会</b>破坏逐层连通性。</p>
     */
    public static final int WIDTH_BONUS_LOW = 2;   // 第 1~12 层
    /** 中层档的额外宽度加成 */
    public static final int WIDTH_BONUS_MID = 3;   // 第 13~24 层
    /** 高层档的额外宽度加成 */
    public static final int WIDTH_BONUS_HIGH = 3;  // 第 25 层及以上

    /** 取该层的宽度加成。 */
    public static int widthBonus(int layer) {
        if (layer < WIDTH_BAND_LOW) {
            return WIDTH_BONUS_LOW;
        }
        if (layer < WIDTH_BAND_MID) {
            return WIDTH_BONUS_MID;
        }
        return WIDTH_BONUS_HIGH;
    }

    /**
     * 走廊段宽度 = {@link #baseCorridorWidth 基础宽度} + 该层加成（上限 {@value #MAX_CORRIDOR_WIDTH}）。
     *
     * <p><b>总体趋势：底部偏狭窄、顶部偏宽敞。</b>基础分布（层索引从 0 开始，即"第 N 层" = 层索引 N-1）：</p>
     *
     * <pre>
     * 第 1~12 层（层索引 0..11）  范围 1~7 格
     *     1→1%   2→1.5%   3→10%   4→75%   5→10%   6→1.5%   7→1%
     *
     * 第 13~24 层（层索引 12..23） 范围 1~7 格，明显更宽
     *     1→1%   2→1.5%   3→20%   4→30%   5→16%   6→1.5%   7→30%
     *
     * 第 25 层及以上（层索引 24..35）范围 2~7 格（<b>不再出现 1 格极窄通道</b>）
     *     2→1.5% 3→10%    4→11%   5→46%   6→1.5%   7→30%
     * </pre>
     *
     * <p>随机数只由坐标与层索引哈希得到，与世界种子、区块加载顺序均无关，
     * 因此同一位置任何时刻生成结果一致。</p>
     */
    public static int corridorWidth(int ci, int cj, boolean east, int layer) {
        // 走廊宽度只在"拐弯"时才变化：
        //   东西向走廊的宽度由所在"行"(cj) 决定 —— 沿 X 前进时 cj 不变，宽度恒定；
        //   南北向走廊的宽度由所在"列"(ci) 决定 —— 沿 Z 前进时 ci 不变，宽度恒定。
        // 因此直线段永远是直墙，只有转弯换到另一行/列时才会换宽。
        int bandIndex = east ? cj : ci;
        return Math.min(MAX_CORRIDOR_WIDTH,
                baseCorridorWidth(bandIndex, bandIndex, east, layer) + widthBonus(layer));
    }

    /** 未加成的基础宽度（三档概率分布见 {@link #corridorWidth} 的说明）。 */
    private static int baseCorridorWidth(int ci, int cj, boolean east, int layer) {
        double r = hash01(ci, cj, layer, east ? SALT_WIDTH_X : SALT_WIDTH_Z);

        if (layer < WIDTH_BAND_LOW) {
            // 第 1~12 层：主流 4 格
            if (r < 0.010) {
                return 1;
            }
            if (r < 0.025) {
                return 2;
            }
            if (r < 0.125) {
                return 3;
            }
            if (r < 0.875) {
                return 4;
            }
            if (r < 0.975) {
                return 5;
            }
            if (r < 0.990) {
                return 6;
            }
            return 7;
        }

        if (layer < WIDTH_BAND_MID) {
            // 第 13~24 层：7 格极宽通道大幅增加
            if (r < 0.010) {
                return 1;
            }
            if (r < 0.025) {
                return 2;
            }
            if (r < 0.225) {   // +20%
                return 3;
            }
            if (r < 0.525) {   // +30%
                return 4;
            }
            if (r < 0.685) {   // +16%
                return 5;
            }
            if (r < 0.700) {   // +1.5%
                return 6;
            }
            return 7;          // +30%
        }

        // 第 25 层及以上：范围收窄到 2~7 格，5 格为主流
        if (r < 0.015) {
            return 2;
        }
        if (r < 0.115) {       // +10%
            return 3;
        }
        if (r < 0.225) {       // +11%
            return 4;
        }
        if (r < 0.685) {       // +46%
            return 5;
        }
        if (r < 0.700) {       // +1.5%
            return 6;
        }
        return 7;              // +30%
    }

    // ==================== 区块墙壁网格（逐层） ====================

    /**
     * 生成区块在<b>所有层</b>上的布局网格。
     *
     * @param layerCount 层数（第 0 .. layerCount-1 层）
     * @return {@code wall[layer][lx][lz]}，{@code true} 表示墙壁，{@code false} 表示通道（空气）
     */
    public static boolean[][][] generateWallGrids(int chunkX, int chunkZ, int layerCount) {
        final int minX = chunkX * 16;
        final int minZ = chunkZ * 16;
        final int maxX = minX + 15;
        final int maxZ = minZ + 15;

        // 走廊可能跨越区块边界，因此留出余量（>= CELL_PITCH + ROOM_HALF）
        final int margin = CELL_PITCH * 2;
        final int ci0 = Math.floorDiv(minX - margin, CELL_PITCH);
        final int ci1 = Math.floorDiv(maxX + margin, CELL_PITCH);
        final int cj0 = Math.floorDiv(minZ - margin, CELL_PITCH);
        final int cj1 = Math.floorDiv(maxZ + margin, CELL_PITCH);

        boolean[][][] walls = new boolean[layerCount][16][16];
        java.util.List<int[]> neighbors = new java.util.ArrayList<>(4);

        for (int layer = 0; layer < layerCount; layer++) {
            boolean[][] open = new boolean[16][16];

            for (int ci = ci0; ci <= ci1; ci++) {
                for (int cj = cj0; cj <= cj1; cj++) {
                    int nx = nodeX(ci);
                    int nz = nodeZ(cj);

                    // 1) 先收集本节点的所有连接，求出最宽走廊
                    boolean[][][] conn = tileMaze(Math.floorDiv(ci, TILE_CELLS),
                            Math.floorDiv(cj, TILE_CELLS), layer);
                    neighbors.clear();
                    collectForwardConnections(conn, ci, cj, layer, neighbors);
                    int maxW = ROOM_HALF * 2 + 1;
                    for (int[] nb : neighbors) {
                        boolean east = nb[1] == cj;
                        maxW = Math.max(maxW, corridorWidth(ci, cj, east, layer));
                    }

                    // 2) 节点房间尺寸随最宽走廊扩展：
                    //    宽走廊接入节点时不再错出台阶 => 消除转角锯齿
                    int half = Math.max(ROOM_HALF, (maxW - 1) / 2);
                    carveRect(open, nx - half, nz - half,
                            nx + half, nz + half, minX, minZ);

                    // 3) 铺设走廊段（按本层分配的宽度）
                    for (int[] nb : neighbors) {
                        boolean east = nb[1] == cj;
                        int width = corridorWidth(ci, cj, east, layer);
                        carveCorridor(open, nx, nz, nodeX(nb[0]), nodeZ(nb[1]), width, minX, minZ);
                    }
                }
            }

            // 3) 并查集在本层逻辑单元格层面复核连通性（安全网）
            ensureCellConnectivity(open, ci0, ci1, cj0, cj1, layer, minX, minZ);

            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    walls[layer][lx][lz] = !open[lx][lz];
                }
            }
        }
        return walls;
    }

    /** 把世界坐标矩形 [x0,x1]×[z0,z1] 裁剪到本区块并标记为通道。 */
    private static void carveRect(boolean[][] open, int x0, int z0, int x1, int z1, int minX, int minZ) {
        int sx = Math.max(x0, minX);
        int ex = Math.min(x1, minX + 15);
        int sz = Math.max(z0, minZ);
        int ez = Math.min(z1, minZ + 15);
        if (sx > ex || sz > ez) {
            return;
        }
        for (int x = sx; x <= ex; x++) {
            int lx = x - minX;
            for (int z = sz; z <= ez; z++) {
                open[lx][z - minZ] = true;
            }
        }
    }

    /** 在节点 A 与节点 B 之间铺设一条宽度为 {@code width} 的走廊段。 */
    private static void carveCorridor(boolean[][] open, int ax, int az, int bx, int bz,
                                      int width, int minX, int minZ) {
        int lo = -(width - 1) / 2;
        int hi = lo + width - 1;
        if (az == bz) {
            carveRect(open, Math.min(ax, bx), az + lo, Math.max(ax, bx), az + hi, minX, minZ);
        } else {
            carveRect(open, ax + lo, Math.min(az, bz), ax + hi, Math.max(az, bz), minX, minZ);
        }
    }

    // ==================== 并查集连通性验证（逻辑单元格层面，逐层） ====================

    /** 判断两个相邻单元格之间是否存在走廊（含瓦片内生成树边与瓦片间门洞，绑定层索引）。 */
    private static boolean edgeExists(int ai, int aj, int bi, int bj, int layer) {
        // 归一化，保证 (ai,aj) 是靠西/靠北的一端
        if (bi < ai || (bi == ai && bj < aj)) {
            int ti = ai;
            ai = bi;
            bi = ti;
            int tj = aj;
            aj = bj;
            bj = tj;
        }
        int tx = Math.floorDiv(ai, TILE_CELLS);
        int tz = Math.floorDiv(aj, TILE_CELLS);
        int li = Math.floorMod(ai, TILE_CELLS);
        int lj = Math.floorMod(aj, TILE_CELLS);
        boolean[][][] conn = tileMaze(tx, tz, layer);

        if (bi == ai + 1 && bj == aj) {
            return li < TILE_CELLS - 1 ? conn[li][lj][0] : doorRowEast(tx, tz, layer) == lj;
        }
        if (bi == ai && bj == aj + 1) {
            return lj < TILE_CELLS - 1 ? conn[li][lj][1] : doorColSouth(tx, tz, layer) == li;
        }
        return false;
    }

    /**
     * 在逻辑单元格层面用并查集复核本层连通性：把区域内每个单元格与它 +X/+Z 方向上的连通邻居合并，
     * 然后检查是否只有一个分量；若出现异常分量则按 1 格宽重新打通作为兜底修复。
     */
    private static void ensureCellConnectivity(boolean[][] open, int ci0, int ci1, int cj0, int cj1,
                                               int layer, int minX, int minZ) {
        int spanJ = cj1 - cj0 + 1;
        int cellCount = (ci1 - ci0 + 1) * spanJ;
        UnionFind uf = new UnionFind(cellCount);

        for (int ci = ci0; ci <= ci1; ci++) {
            for (int cj = cj0; cj <= cj1; cj++) {
                int idx = (ci - ci0) * spanJ + (cj - cj0);
                if (ci + 1 <= ci1 && edgeExists(ci, cj, ci + 1, cj, layer)) {
                    uf.union(idx, (ci + 1 - ci0) * spanJ + (cj - cj0));
                }
                if (cj + 1 <= cj1 && edgeExists(ci, cj, ci, cj + 1, layer)) {
                    uf.union(idx, (ci - ci0) * spanJ + (cj + 1 - cj0));
                }
            }
        }

        int refRoot = -1;
        int refCi = 0;
        int refCj = 0;
        for (int ci = ci0; ci <= ci1; ci++) {
            for (int cj = cj0; cj <= cj1; cj++) {
                int idx = (ci - ci0) * spanJ + (cj - cj0);
                int root = uf.find(idx);
                if (refRoot < 0) {
                    refRoot = root;
                    refCi = ci;
                    refCj = cj;
                    continue;
                }
                if (root != refRoot) {
                    // 理论上不会发生（生成树 + 门洞已保证连通）：按 1 格宽打通修复走廊
                    carveCorridor(open, nodeX(ci), nodeZ(cj), nodeX(refCi), nodeZ(refCj),
                            1, minX, minZ);
                    uf.union(idx, refRoot);
                }
            }
        }
    }

    // ==================== 竖井：已移除 ====================
    //
    // 层与层之间不再通过竖井连接。跨层连通改由「特定结构」
    // （backrooms_complex:level0，模板 data/backrooms_complex/structure/level0_X.nbt）承担：
    // 结构允许跨层，可覆盖中间的天花板与地板（但不覆盖 minecraft:bedrock）。
}