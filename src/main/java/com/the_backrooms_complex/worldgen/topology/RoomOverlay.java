package com.the_backrooms_complex.worldgen.topology;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 覆盖层（Overlay）与扰动（Perturbation）—— 让拓扑**可运行时改写**。
 *
 * <h2>分层</h2>
 * <pre>
 *   RoomGraph   基础图：确定性、只读、无限、无周期
 *   RoomOverlay 覆盖层：稀疏、可变（只存被改过的边）
 *   isConnected 唯一查询入口：先查覆盖层，未命中回落基础图
 * </pre>
 *
 * <h2>不变量</h2>
 * <ul>
 *   <li><b>成对写</b>：Block/Redirect 必须同时写两端，否则出现单向门；</li>
 *   <li><b>永不死锁</b>：扰动后用 BFS 统计可达房间数，若骤降则回滚；</li>
 *   <li><b>视线外</b>：只改玩家看不到的边（由调用方筛选候选）；</li>
 *   <li><b>不无限增长</b>：合成房间回收时删除相关条目。</li>
 * </ul>
 */
public final class RoomOverlay {

    /** 被改写的边的状态。 */
    public sealed interface Override permits Blocked, Redirect {
    }

    /** 封死。 */
    public record Blocked() implements Override {
    }

    /** 改接到另一扇门。 */
    public record Redirect(long target, int targetDir) implements Override {
    }

    /** 可达性骤降到此比例以下就回滚。 */
    public static final double ROLLBACK_THRESHOLD = 0.6D;
    /** BFS 校验的曼哈顿半径。 */
    public static final int VALIDATE_RADIUS = 14;

    /** 合成房间坐标起点，避免与真实坐标冲突。 */
    public static final long SYNTHETIC_OFFSET = 1L << 40;

    private final RoomGraph graph;
    private final Map<Long, Override> overlay = new HashMap<>();

    /** 合成房间计数器（存档只需存这个数）。 */
    private long syntheticCounter = 0;

    public RoomOverlay(RoomGraph graph) {
        this.graph = graph;
    }

    public RoomGraph graph() {
        return this.graph;
    }

    // ==================== 查询入口（唯一） ====================

    /**
     * 查询一条边是否连通。
     *
     * <p><b>这是唯一允许的查询入口</b>：上层不得绕过它直接查基础图或覆盖层，
     * 否则会出现"两层不一致"的单向门。</p>
     *
     * @return {@code {连通?, 目标坐标, 目标方向}}；不连通时后两项为 {@code null / -1}
     */
    public long[] isConnected(long a, int dir) {
        long key = this.graph.canonical(a, dir);
        Override ov = this.overlay.get(key);
        if (ov instanceof Blocked) {
            return new long[]{0, 0, -1};
        }
        if (ov instanceof Redirect r) {
            return new long[]{1, r.target(), r.targetDir()};
        }
        if (this.graph.baseEdge(a, dir)) {
            return new long[]{1, RoomGraph.neighbor(a, dir), RoomGraph.opposite(dir)};
        }
        return new long[]{0, 0, -1};
    }

    /** 简化版：只关心是否连通。 */
    public boolean connected(long a, int dir) {
        return isConnected(a, dir)[0] != 0;
    }

    // ==================== 覆盖层写操作（必须成对） ====================

    /** 封死一条边（两端同写）。 */
    public void block(long a, int dir) {
        this.overlay.put(this.graph.canonical(a, dir), new Blocked());
    }

    /** 把一条边改接到另一扇门（两端互指）。 */
    public void redirect(long a, int dir, long target, int targetDir) {
        this.block(a, dir);
        this.overlay.put(this.graph.canonical(a, dir), new Redirect(target, targetDir));
        this.overlay.put(this.graph.canonical(target, targetDir), new Redirect(a, dir));
    }

    // ==================== 合成房间 ====================

    /** 分配一个新的合成房间坐标。 */
    public long newSyntheticRoom(int floor) {
        this.syntheticCounter++;
        return RoomGraph.coord(SYNTHETIC_OFFSET + this.syntheticCounter, 0, floor);
    }

    /** 回收合成房间：删除所有以它为端点的覆盖层条目。 */
    public void reclaimSyntheticRoom(long coord) {
        this.overlay.entrySet().removeIf(e -> {
            long key = e.getKey();
            long lo = key >>> 3;
            Override ov = e.getValue();
            if (lo == coord) {
                return true;
            }
            return ov instanceof Redirect r && r.target() == coord;
        });
    }

    // ==================== 可达性校验 ====================

    /**
     * 统计从 {@code start} 出发、曼哈顿半径 {@code radius} 内可达的房间数。
     *
     * <p>用于"扰动后不得把玩家封死"的校验。</p>
     */
    public int reachableCount(long start, int radius) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        int count = 0;
        int guard = 0;
        while (!queue.isEmpty() && guard++ < 20000) {
            long cur = queue.poll();
            count++;
            if (manhattan(cur, start) > radius) {
                continue;
            }
            for (int d = 0; d < 6; d++) {
                long[] r = isConnected(cur, d);
                if (r[0] == 0) {
                    continue;
                }
                long next = r[1];
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return count;
    }

    private static long manhattan(long a, long b) {
        return Math.abs(RoomGraph.xOf(a) - RoomGraph.xOf(b))
                + Math.abs(RoomGraph.zOf(a) - RoomGraph.zOf(b))
                + Math.abs(RoomGraph.floorOf(a) - RoomGraph.floorOf(b));
    }

    // ==================== 扰动 ====================

    /**
     * 尝试做一次扰动：把某条边改接到附近的另一扇门。
     *
     * <p>流程：截图 → 改写 → 校验可达性 → 失败回滚。</p>
     *
     * @param origin  玩家所在房间
     * @param radius  候选半径
     * @param rnd     随机源（游戏层提供）
     * @return 是否真的改写了
     */
    public boolean swapTarget(long origin, int radius, net.minecraft.util.RandomSource rnd) {
        List<long[]> candidates = collectCandidates(origin, radius);
        if (candidates.isEmpty()) {
            return false;
        }
        long[] pick = candidates.get(rnd.nextInt(candidates.size()));
        long a = pick[0];
        int dir = (int) pick[1];

        int before = reachableCount(origin, VALIDATE_RADIUS);
        Map<Long, Override> snapshot = new HashMap<>(this.overlay);

        // 找另一扇门作为新目标
        List<long[]> others = collectCandidates(origin, radius);
        others.removeIf(c -> c[0] == a && c[1] == dir);
        if (others.isEmpty()) {
            return false;
        }
        long[] other = others.get(rnd.nextInt(others.size()));

        this.redirect(a, dir, other[0], (int) other[1]);
        this.redirect(RoomGraph.neighbor(a, dir), RoomGraph.opposite(dir), other[0], (int) other[1]);

        int after = reachableCount(origin, VALIDATE_RADIUS);
        if (after < before * ROLLBACK_THRESHOLD) {
            this.overlay.clear();
            this.overlay.putAll(snapshot);
            return false;
        }
        return true;
    }

    /** 收集半径内的所有"可改写的门"。 */
    public List<long[]> collectCandidates(long origin, int radius) {
        List<long[]> out = new ArrayList<>();
        long ox = RoomGraph.xOf(origin);
        long oz = RoomGraph.zOf(origin);
        int of = RoomGraph.floorOf(origin);
        for (long dx = -radius; dx <= radius; dx++) {
            for (long dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > radius) {
                    continue;
                }
                long room = RoomGraph.coord(ox + dx, oz + dz, of);
                for (int d = 0; d < 6; d++) {
                    if (connected(room, d)) {
                        out.add(new long[]{room, d});
                    }
                }
            }
        }
        return out;
    }

    // ==================== 调试 ====================

    /** 覆盖层条目数。 */
    public int overlaySize() {
        return this.overlay.size();
    }

    /** 合成房间计数。 */
    public long syntheticCounter() {
        return this.syntheticCounter;
    }

    /** 导出覆盖层（存档用）。 */
    public Map<Long, Override> exportOverlay() {
        return new HashMap<>(this.overlay);
    }

    /** 导入覆盖层（读档用）。 */
    public void importOverlay(Map<Long, Override> data, long counter) {
        this.overlay.clear();
        this.overlay.putAll(data);
        this.syntheticCounter = counter;
    }
}
