package com.the_backrooms_complex.worldgen.topology;

/**
 * 把 {@link RoomGraph} 的拓扑翻译成「逐方块的可走/墙壁」网格。
 *
 * <h2>为什么用「按矩形挖」而不是「距离场」</h2>
 * <p>距离场（{@code 到最近线段的距离 <= 半宽}）在两条线段的接缝处会切换距离来源，
 * 边界因此跳变 —— 那就是楼梯状墙壁的来源。</p>
 *
 * <p>这里改为<b>显式按矩形挖</b>：每条 L 形线段都是一条轴对齐的长方形通道，
 * 宽度恒定 {@value #CORRIDOR_WIDTH} 格。长方形与长方形在转角处正好对齐，
 * 因此<b>墙线完全笔直，转角就是一次 90°</b>。</p>
 *
 * <h2>房间</h2>
 * <p>房间是<b>主干节点周围的方形</b>，边长 = 走廊宽 + 2k（奇数），
 * 中心与主干节点重合 ⇒ 与走廊左右对称衔接，看起来是「门洞扩宽」而不是错位台阶。</p>
 */
public final class TopologyWalls {

    /** 走廊宽度（格）：恒定值，保证墙线笔直。 */
    public static final int CORRIDOR_WIDTH = 7;

    /** 房间最小/最大边长（格），均为奇数。 */
    public static final int ROOM_MIN = 7;
    public static final int ROOM_MAX = 23;

    /** 计算区块墙网格时向外扩展的格数（保证跨区块边界的线段被完整绘制）。 */
    private static final int CELL_MARGIN = 2;

    private TopologyWalls() {
    }

    /**
     * 计算某个区块某一层的墙体网格。
     *
     * @return {@code walls[16][16]}，{@code true} = 墙
     */
    public static boolean[][] wallGrid(RoomGraph graph, int chunkX, int chunkZ, int layer) {
        int size = 16 + CELL_MARGIN * 4;
        int baseX = chunkX * 16 - CELL_MARGIN * 2;
        int baseZ = chunkZ * 16 - CELL_MARGIN * 2;
        boolean[][] open = new boolean[size][size];

        long ci0 = Math.floorDiv(baseX, RoomGraph.CELL);
        long ci1 = Math.floorDiv(baseX + size - 1, RoomGraph.CELL);
        long cj0 = Math.floorDiv(baseZ, RoomGraph.CELL);
        long cj1 = Math.floorDiv(baseZ + size - 1, RoomGraph.CELL);

        // 1) 挖走廊：每个格子的四条 L 形线段（每条都是一条长方形）
        for (long ci = ci0 - 1; ci <= ci1 + 1; ci++) {
            for (long cj = cj0 - 1; cj <= cj1 + 1; cj++) {
                carveCellEdges(graph, (int) ci, (int) cj, layer, open, baseX, baseZ, size);
            }
        }

        // 2) 挖房间：主干节点周围的方形
        for (long ci = ci0 - 1; ci <= ci1 + 1; ci++) {
            for (long cj = cj0 - 1; cj <= cj1 + 1; cj++) {
                carveRoom(graph, (int) ci, (int) cj, layer, open, baseX, baseZ, size);
            }
        }

        // 3) 导出本区块的 16×16
        boolean[][] walls = new boolean[16][16];
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                walls[lx][lz] = !open[chunkX * 16 + lx - baseX][chunkZ * 16 + lz - baseZ];
            }
        }
        return walls;
    }

    // ==================== 走廊 ====================

    /** 挖格子 (ci,cj) 的四条 L 形线段（东/西/南/北各一条）。 */
    private static void carveCellEdges(RoomGraph graph, int ci, int cj, int layer,
                                       boolean[][] open, int baseX, int baseZ, int size) {
        carveL(graph, ci, cj, ci + 1, cj, layer, "ldir", open, baseX, baseZ, size);
        carveL(graph, ci - 1, cj, ci, cj, layer, "ldir", open, baseX, baseZ, size);
        carveL(graph, ci, cj, ci, cj + 1, layer, "ldirNS", open, baseX, baseZ, size);
        carveL(graph, ci, cj - 1, ci, cj, layer, "ldirNS", open, baseX, baseZ, size);
    }

    /** 挖连接两个主干节点的 L 形通道（两段轴对齐长方形）。 */
    private static void carveL(RoomGraph graph, int ai, int aj, int bi, int bj, int layer, String salt,
                               boolean[][] open, int baseX, int baseZ, int size) {
        long ax = graph.spineX(ai, aj, layer);
        long az = graph.spineZ(ai, aj, layer);
        long bx = graph.spineX(bi, bj, layer);
        long bz = graph.spineZ(bi, bj, layer);
        int ldir = (int) Math.floorMod(RoomGraph.hashCoord(graph.seedValue(), ai, aj, layer, salt), 2);
        if (ax > bx || (ax == bx && az > bz)) {
            long t = ax; ax = bx; bx = t;
            t = az; az = bz; bz = t;
        }

        int half = CORRIDOR_WIDTH / 2;

        if (ldir == 0) {
            fillRect(open, ax - half, az - half, bx + half, az + half, baseX, baseZ, size);
            fillRect(open, bx - half, Math.min(az, bz) - half,
                    bx + half, Math.max(az, bz) + half, baseX, baseZ, size);
        } else {
            fillRect(open, ax - half, Math.min(az, bz) - half,
                    ax + half, Math.max(az, bz) + half, baseX, baseZ, size);
            fillRect(open, ax - half, bz - half, bx + half, bz + half, baseX, baseZ, size);
        }
    }

    // ==================== 房间 ====================

    /** 挖格子 (ci,cj) 主干节点周围的方形房间。 */
    private static void carveRoom(RoomGraph graph, int ci, int cj, int layer,
                                  boolean[][] open, int baseX, int baseZ, int size) {
        long cx = graph.spineX(ci, cj, layer);
        long cz = graph.spineZ(ci, cj, layer);
        int half = roomSize(graph, ci, cj, layer) / 2;
        fillRect(open, cx - half, cz - half, cx + half, cz + half, baseX, baseZ, size);
    }

    /** 房间边长：{@link #ROOM_MIN} ~ {@link #ROOM_MAX}，恒定奇数（中心对齐主干节点）。 */
    private static int roomSize(RoomGraph graph, int ci, int cj, int layer) {
        long h = RoomGraph.hashCoord(graph.seedValue(), ci, cj, layer, "roomEdge");
        int steps = (ROOM_MAX - ROOM_MIN) / 2 + 1;
        return ROOM_MIN + (int) Math.floorMod(h, steps) * 2;
    }

    // ==================== 工具 ====================

    /** 把一个世界坐标矩形填进局部可走图（自动裁剪）。 */
    private static void fillRect(boolean[][] open, long x0, long z0, long x1, long z1,
                                 int baseX, int baseZ, int size) {
        int sx = (int) Math.max(x0 - baseX, 0);
        int sz = (int) Math.max(z0 - baseZ, 0);
        int ex = (int) Math.min(x1 - baseX, size - 1);
        int ez = (int) Math.min(z1 - baseZ, size - 1);
        if (sx > ex || sz > ez) {
            return;
        }
        for (int x = sx; x <= ex; x++) {
            for (int z = sz; z <= ez; z++) {
                open[x][z] = true;
            }
        }
    }
}
