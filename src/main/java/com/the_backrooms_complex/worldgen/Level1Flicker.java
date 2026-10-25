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
 * <p>"宜居地带的所有自然光源随机切断，导致整个层级陷入完全黑暗" —— 熄灯期间实体大量出现，
 * 玩家只能靠门上的绿色应急灯找到小径躲避。</p>
 *
 * <h2>流程</h2>
 * <ol>
 *   <li>等待 {@value #MIN_INTERVAL}~{@value #MAX_INTERVAL} tick（15~45 秒）；</li>
 *   <li>收集玩家 {@value #RADIUS_CHUNKS} 区块半径内的荧光灯；</li>
 *   <li>闪烁 {@value #FLICKER_TIMES} 次（亮 {@value #FLICKER_ON} / 灭 {@value #FLICKER_OFF} tick）；</li>
 *   <li>全部熄灭，保持 {@value #DARK_MIN}~{@value #DARK_MAX} tick（15~45 秒）；</li>
 *   <li>全部恢复。</li>
 * </ol>
 *
 * <h2>性能设计（不卡顿的关键）</h2>
 * <ul>
 *   <li><b>只扫天花板层</b>：灯按生成规则只出现在 {@code layer*18 + 17}；</li>
 *   <li><b>只查理论灯位</b>：灯只在 {@code x % 5 == 2 && z % 5 == 2}，每区块 16 个候选点；</li>
 *   <li><b>区块级 isLoaded 预筛</b>；</li>
 *   <li><b>分批切换</b>：每 tick 最多改 {@value #BATCH} 个灯 —— 视觉上是"灯一盏接一盏熄灭"，
 *       既不掉帧又更有氛围。</li>
 * </ul>
 * <p>收集量从约 <b>100 万次</b> {@code getBlockState} 降到约 <b>2000 次</b>。</p>
 */
public final class Level1Flicker {

    /** 半径（区块）。 */
    public static final int RADIUS_CHUNKS = 4;
    /** 两次现象之间的间隔（tick）：15~45 秒。 */
    public static final int MIN_INTERVAL = 15 * 20;
    public static final int MAX_INTERVAL = 45 * 20;
    /** 熄灭后保持黑暗的时长（tick）：15~45 秒。 */
    public static final int DARK_MIN = 15 * 20;
    public static final int DARK_MAX = 45 * 20;
    /** 闪烁次数。 */
    public static final int FLICKER_TIMES = 3;
    /** 每次"亮"持续的 tick。 */
    public static final int FLICKER_ON = 4;
    /** 每次"灭"持续的 tick。 */
    public static final int FLICKER_OFF = 6;
    /** 每 tick 最多切换的灯数。 */
    public static final int BATCH = 32;
    /** Level 1 每层高度（格）。 */
    private static final int LAYER_H = 18;
    /** 灯的横向间距（与生成器 LAMP_SPACING 一致）。 */
    private static final int LAMP_STEP = 5;

    private static final int PHASE_IDLE = 0;
    private static final int PHASE_FLICKER = 1;
    private static final int PHASE_DARK = 2;
    private static final int PHASE_SWITCH = 3;

    /** 每个玩家的状态。 */
    private static final class State {
        int phase = PHASE_IDLE;
        int cooldown = 0;
        int phaseTicks = 0;
        int flickerCount = 0;
        final List<BlockPos> lamps = new ArrayList<>();
        final List<BlockPos> pending = new ArrayList<>();
        BlockState pendingState = null;
        int nextPhase = PHASE_IDLE;
        int nextTicks = 0;
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

        if (st.phase == PHASE_SWITCH) {
            if (applyBatch(level, st)) {
                st.phase = st.nextPhase;
                st.phaseTicks = st.nextTicks;
            }
            return;
        }

        switch (st.phase) {
            case PHASE_IDLE -> {
                if (st.cooldown > 0) {
                    st.cooldown--;
                    return;
                }
                collect(level, player, st);
                if (st.lamps.isEmpty()) {
                    st.cooldown = MIN_INTERVAL;
                    return;
                }
                st.flickerCount = 0;
                beginSwitch(level, st, off(), PHASE_FLICKER, FLICKER_OFF);
            }
            case PHASE_FLICKER -> {
                if (--st.phaseTicks > 0) {
                    return;
                }
                st.flickerCount++;
                if (st.flickerCount >= FLICKER_TIMES * 2) {
                    int dark = DARK_MIN + level.random.nextInt(DARK_MAX - DARK_MIN + 1);
                    beginSwitch(level, st, off(), PHASE_DARK, dark);
                    return;
                }
                boolean lit = (st.flickerCount % 2) == 0;
                beginSwitch(level, st, lit ? on() : off(),
                        PHASE_FLICKER, lit ? FLICKER_ON : FLICKER_OFF);
            }
            case PHASE_DARK -> {
                if (--st.phaseTicks > 0) {
                    return;
                }
                int cd = MIN_INTERVAL + level.random.nextInt(MAX_INTERVAL - MIN_INTERVAL + 1);
                beginSwitch(level, st, on(), PHASE_IDLE, cd);
                st.cooldown = cd;
            }
            default -> st.phase = PHASE_IDLE;
        }
    }

    /** 进入下一批切换。 */
    private static void beginSwitch(ServerLevel level, State st, BlockState target, int nextPhase, int nextTicks) {
        st.pending.clear();
        st.pending.addAll(st.lamps);
        st.pendingState = target;
        st.nextPhase = nextPhase;
        st.nextTicks = nextTicks;
        st.phase = PHASE_SWITCH;
        if (applyBatch(level, st)) {
            st.phase = nextPhase;
            st.phaseTicks = nextTicks;
        }
    }

    /** 处理一批；返回是否已全部处理完。 */
    private static boolean applyBatch(ServerLevel level, State st) {
        if (st.pendingState == null) {
            return true;
        }
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
                level.setBlock(p, st.pendingState, 3);
                done++;
            }
        }
        if (!it.hasNext()) {
            st.pendingState = null;
            return true;
        }
        return false;
    }

    /**
     * 收集玩家周围的理论灯位。
     *
     * <p>只检查每层天花板（{@code y = layer*18 + 17}）上 {@code x % 5 == 2 && z % 5 == 2}
     * 的位置 —— 每区块仅 16 个候选点。</p>
     */
    private static void collect(ServerLevel level, ServerPlayer player, State st) {
        st.lamps.clear();
        BlockPos origin = player.blockPosition();
        int cx = origin.getX() >> 4;
        int cz = origin.getZ() >> 4;
        int baseLayer = Math.max(0, origin.getY() / LAYER_H);
        int r = RADIUS_CHUNKS;

        for (int dl = -1; dl <= 1; dl++) {
            int y = (baseLayer + dl) * LAYER_H + (LAYER_H - 1);
            if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                continue;
            }
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int baseX = (cx + dx) << 4;
                    int baseZ = (cz + dz) << 4;
                    if (!level.isLoaded(new BlockPos(baseX, y, baseZ))) {
                        continue;
                    }
                    int x0 = baseX + Math.floorMod(2 - baseX, LAMP_STEP);
                    for (int x = x0; x < baseX + 16; x += LAMP_STEP) {
                        int z0 = baseZ + Math.floorMod(2 - baseZ, LAMP_STEP);
                        for (int z = z0; z < baseZ + 16; z += LAMP_STEP) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (level.getBlockState(p).is(ModBlocks.FLUORESCENT_LAMP.get())) {
                                st.lamps.add(p);
                            }
                        }
                    }
                }
            }
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
