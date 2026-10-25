package com.the_backrooms_complex.worldgen;

import com.the_backrooms_complex.ModBlocks;
import com.the_backrooms_complex.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Level 1「闪烁」现象。
 *
 * <h2>文中设定</h2>
 * <p>"宜居地带的所有自然光源随机切断，导致整个层级陷入完全黑暗"。</p>
 *
 * <h2>流程（逐步熄灭 / 逐步亮起）</h2>
 * <ol>
 *   <li>收集玩家周围<b>整个已加载区域</b>（按服务器视距）内的荧光灯；</li>
 *   <li><b>每 {@value #GROUP_INTERVAL} tick（0.5 秒）熄灭一组</b>，直到全部熄灭；</li>
 *   <li>保持黑暗 {@value #DARK_MIN}~{@value #DARK_MAX} tick（45~60 秒）；</li>
 *   <li>再<b>每 0.5 秒亮起一组</b>，直到全部恢复；</li>
 *   <li>等待下一次触发。</li>
 * </ol>
 *
 * <h2>性能</h2>
 * <ul>
 *   <li>按 {@value #CHUNKS_PER_GROUP} 个区块的灯为一组，组内每 tick 最多写
 *       {@value #BATCH} 个，避免一帧内上千次 {@code setBlock}；</li>
 *   <li>只在状态切换时收集一次，之后只遍历灯表。</li>
 * </ul>
 */
public final class Level1Flicker {

    /** 视距上限（区块）：实际半径取 min(服务器视距, 该值)。 */
    public static final int MAX_RADIUS_CHUNKS = 12;
    /** 每组包含多少个区块的灯。 */
    public static final int CHUNKS_PER_GROUP = 4;
    /** 组与组之间的间隔（tick）：0.5 秒。 */
    public static final int GROUP_INTERVAL = 10;
    /** 每 tick 组内最多写入的方块数。 */
    public static final int BATCH = 48;

    /** 熄灭后保持黑暗的时长（tick）：45~60 秒。 */
    public static final int DARK_MIN = 45 * 20;
    public static final int DARK_MAX = 60 * 20;
    /** 两轮现象之间的间隔（tick）：15~45 秒。 */
    public static final int MIN_INTERVAL = 15 * 20;
    public static final int MAX_INTERVAL = 45 * 20;

    /** Level 1 每层高度（格）。 */
    private static final int LAYER_H = 18;
    /** 子层高度。 */
    private static final int SUB_H = 6;
    /** 子层数量。 */
    private static final int SUB_COUNT = 3;
    /** 灯间距（与生成器的 PASSAGE_LAMP_STEP 一致）。 */
    private static final int LAMP_STEP = 3;
    /** Level 1 世界高度。 */
    private static final int WORLD_H = 288;

    private static final int PHASE_IDLE = 0;
    private static final int PHASE_DARKEN = 1;
    private static final int PHASE_DARK = 2;
    private static final int PHASE_LIGHTEN = 3;

    /** 每个玩家的状态。 */
    private static final class State {
        int phase = PHASE_IDLE;
        int cooldown = 0;
        int groupTimer = 0;
        int darkTicks = 0;
        /** 本次收集到的灯，已按「组」切分。 */
        final List<List<BlockPos>> groups = new ArrayList<>();
        /** 当前处理到第几组。 */
        int groupIndex = 0;
        /** 当前组的待写队列。 */
        final List<BlockPos> pending = new ArrayList<>();
        /** 当前批次的目标状态（null 表示没有待写内容）。 */
        BlockState target = null;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private Level1Flicker() {
    }

    /** 每 tick 调用（只对 Level 1 玩家）。 */
    public static void tick(ServerLevel level, ServerPlayer player) {
        if (!level.dimension().equals(ModDimensions.LEVEL_1_WORLD)) {
            forget(player.getUUID());
            return;
        }
        State st = STATES.computeIfAbsent(player.getUUID(), k -> new State());

        // 有待写的灯就先写完（保证一组一定全部熄灭后才推进下一组）
        if (st.target != null) {
            drain(level, st);
            return;
        }

        switch (st.phase) {
            case PHASE_IDLE -> {
                if (st.cooldown > 0) {
                    st.cooldown--;
                    return;
                }
                collect(level, player, st);
                if (st.groups.isEmpty()) {
                    st.cooldown = MIN_INTERVAL;
                    return;
                }
                st.groupIndex = 0;
                st.groupTimer = 0;
                st.phase = PHASE_DARKEN;
            }
            case PHASE_DARKEN -> {
                if (advanceGroup(level, st, off())) {
                    st.darkTicks = DARK_MIN + level.random.nextInt(DARK_MAX - DARK_MIN + 1);
                    st.phase = PHASE_DARK;
                }
            }
            case PHASE_DARK -> {
                if (--st.darkTicks <= 0) {
                    st.groupIndex = 0;
                    st.groupTimer = 0;
                    st.phase = PHASE_LIGHTEN;
                }
            }
            case PHASE_LIGHTEN -> {
                if (advanceGroup(level, st, on())) {
                    st.groups.clear();
                    st.phase = PHASE_IDLE;
                    st.cooldown = MIN_INTERVAL + level.random.nextInt(MAX_INTERVAL - MIN_INTERVAL + 1);
                }
            }
            default -> st.phase = PHASE_IDLE;
        }
    }

    /**
     * 推进一组：每 {@link #GROUP_INTERVAL} tick 取下一组开始写。
     *
     * @return 是否已全部处理完
     */
    private static boolean advanceGroup(ServerLevel level, State st, BlockState target) {
        if (st.groupIndex >= st.groups.size()) {
            return true;
        }
        if (st.groupTimer > 0) {
            st.groupTimer--;
            return false;
        }
        st.pending.clear();
        st.pending.addAll(st.groups.get(st.groupIndex));
        st.groupIndex++;
        st.groupTimer = GROUP_INTERVAL;
        st.target = target;
        drain(level, st);
        return false;
    }

    /** 写入一批；写完则清空 target。 */
    private static void drain(ServerLevel level, State st) {
        int done = 0;
        Iterator<BlockPos> it = st.pending.iterator();
        while (it.hasNext() && done < BATCH) {
            BlockPos p = it.next();
            it.remove();
            if (!level.isLoaded(p)) {
                continue;
            }
            BlockState cur = level.getBlockState(p);
            if (cur.is(ModBlocks.FLUORESCENT_LAMP.get()) || cur.is(ModBlocks.LAMP_OFF.get())) {
                level.setBlock(p, st.target, 3);
                done++;
            }
        }
        if (!it.hasNext()) {
            st.target = null;
        }
    }

    /**
     * 收集玩家周围<b>整个已加载区域</b>内的荧光灯。
     *
     * <p>半径取服务器视距（上限 {@link #MAX_RADIUS_CHUNKS} 区块）。
     * 灯位与生成器一致：每层 3 个子层，子层天花板在 {@code subOff == 5}，
     * 横向每 {@link #LAMP_STEP} 格一个（{@code x % 3 == 1 && z % 3 == 1}）。</p>
     */
    private static void collect(ServerLevel level, ServerPlayer player, State st) {
        st.groups.clear();
        st.pending.clear();
        st.target = null;
        BlockPos origin = player.blockPosition();
        int radius = Math.min(MAX_RADIUS_CHUNKS,
                level.getServer().getPlayerList().getViewDistance());
        int cx = origin.getX() >> 4;
        int cz = origin.getZ() >> 4;
        int maxY = level.getMaxBuildHeight();

        List<BlockPos> current = new ArrayList<>();
        int chunksInCurrent = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int baseX = (cx + dx) << 4;
                int baseZ = (cz + dz) << 4;
                if (!level.isLoaded(new BlockPos(baseX, origin.getY(), baseZ))) {
                    continue;
                }
                chunksInCurrent++;
                for (int layer = 0; layer < WORLD_H / LAYER_H; layer++) {
                    for (int sub = 0; sub < SUB_COUNT; sub++) {
                        int y = layer * LAYER_H + sub * SUB_H + (SUB_H - 1);
                        if (y >= maxY) {
                            continue;
                        }
                        int x0 = baseX + Math.floorMod(1 - baseX, LAMP_STEP);
                        for (int x = x0; x < baseX + 16; x += LAMP_STEP) {
                            int z0 = baseZ + Math.floorMod(1 - baseZ, LAMP_STEP);
                            for (int z = z0; z < baseZ + 16; z += LAMP_STEP) {
                                BlockPos p = new BlockPos(x, y, z);
                                if (level.getBlockState(p).is(ModBlocks.FLUORESCENT_LAMP.get())) {
                                    current.add(p);
                                }
                            }
                        }
                    }
                }
                if (chunksInCurrent >= CHUNKS_PER_GROUP) {
                    if (!current.isEmpty()) {
                        st.groups.add(new ArrayList<>(current));
                    }
                    current.clear();
                    chunksInCurrent = 0;
                }
            }
        }
        if (!current.isEmpty()) {
            st.groups.add(new ArrayList<>(current));
        }
    }

    private static BlockState on() {
        return ModBlocks.FLUORESCENT_LAMP.get().defaultBlockState();
    }

    private static BlockState off() {
        return ModBlocks.LAMP_OFF.get().defaultBlockState();
    }

    /** 玩家离开 / 换维度时清理。 */
    public static void forget(UUID player) {
        STATES.remove(player);
    }
}
