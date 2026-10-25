package com.the_backrooms_complex.worldgen;

import com.the_backrooms_complex.ModBlocks;
import com.the_backrooms_complex.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

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
 * <p>"宜居地带的所有自然光源随机切断，导致整个层级陷入完全黑暗" —— 这是 Level 1 最致命的
 * 现象之一：熄灯期间实体大量出现，玩家只能靠门上的绿色应急灯找到小径躲避。</p>
 *
 * <h2>本实现</h2>
 * <ol>
 *   <li>定时触发（每 {@value #MIN_INTERVAL}~{@value #MAX_INTERVAL} tick = 15~45 秒）；</li>
 *   <li>收集玩家 <b>{@value #RADIUS_CHUNKS} 区块</b>半径内的所有「荧光灯」；</li>
 *   <li><b>闪烁数次</b>（{@value #FLICKER_TIMES} 次，每次 {@value #FLICKER_ON}/
 *       {@value #FLICKER_OFF} tick）后全部<b>熄灭</b>（换成「熄灭的荧光灯」）；</li>
 *   <li>保持黑暗 {@value #MIN_INTERVAL}~{@value #MAX_INTERVAL} tick（15~45 秒）后全部<b>恢复</b>。</li>
 * </ol>
 */
public final class Level1Flicker {

    /** 半径（区块）。 */
    public static final int RADIUS_CHUNKS = 4;
    /** 半径（格）= 4 × 16。 */
    public static final int RADIUS_BLOCKS = RADIUS_CHUNKS * 16;

    /** 垂直搜索范围（格）。Level 1 一层 18 格，取 ±24 覆盖上下层。 */
    public static final int Y_RANGE = 24;

    /** 两次闪烁现象之间的间隔（tick）：15~45 秒。 */
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

    /** 每个玩家的状态。 */
    private static final class State {
        /** 距离下一次触发还有多少 tick。 */
        int cooldown = 0;
        /** 当前相位：0 = 空闲，1 = 闪烁中，2 = 全暗。 */
        int phase = 0;
        /** 相位内剩余 tick。 */
        int phaseTicks = 0;
        /** 已闪烁次数。 */
        int flickerCount = 0;
        /** 本次被影响的灯。 */
        final List<BlockPos> lamps = new ArrayList<>();
        /** 保暗剩余 tick。 */
        int darkTicks = 0;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private Level1Flicker() {
    }

    /** 每 tick 调用（只在 Level 1 生效）。 */
    public static void tick(ServerLevel level, ServerPlayer player) {
        if (!level.dimension().equals(ModDimensions.LEVEL_1_WORLD)) {
            forget(player.getUUID());
            return;
        }
        State st = STATES.computeIfAbsent(player.getUUID(), k -> new State());

        if (st.phase == 0) {
            if (st.cooldown > 0) {
                st.cooldown--;
                return;
            }
            start(level, player, st);
            return;
        }
        if (st.phase == 1) {
            st.phaseTicks--;
            if (st.phaseTicks > 0) {
                return;
            }
            // 一次"亮/灭"结束
            st.flickerCount++;
            if (st.flickerCount >= FLICKER_TIMES * 2) {
                // 闪够了 => 全灭
                setAll(level, st, ModBlocks.LAMP_OFF.get().defaultBlockState());
                st.phase = 2;
                st.darkTicks = DARK_MIN + level.random.nextInt(DARK_MAX - DARK_MIN + 1);
                st.phaseTicks = st.darkTicks;
                return;
            }
            boolean lit = (st.flickerCount % 2) == 0;
            setAll(level, st, lit
                    ? ModBlocks.FLUORESCENT_LAMP.get().defaultBlockState()
                    : ModBlocks.LAMP_OFF.get().defaultBlockState());
            st.phaseTicks = lit ? FLICKER_ON : FLICKER_OFF;
            return;
        }
        if (st.phase == 2) {
            st.phaseTicks--;
            if (st.phaseTicks > 0) {
                return;
            }
            // 恢复
            setAll(level, st, ModBlocks.FLUORESCENT_LAMP.get().defaultBlockState());
            st.lamps.clear();
            st.phase = 0;
            st.cooldown = MIN_INTERVAL + level.random.nextInt(MAX_INTERVAL - MIN_INTERVAL + 1);
        }
    }

    /** 收集玩家周围的荧光灯并开始第一次闪烁。 */
    private static void start(ServerLevel level, ServerPlayer player, State st) {
        st.lamps.clear();
        BlockPos origin = player.blockPosition();
        int cx = origin.getX() >> 4;
        int cz = origin.getZ() >> 4;
        int r = RADIUS_CHUNKS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int baseX = (cx + dx) << 4;
                int baseZ = (cz + dz) << 4;
                if (!level.isLoaded(new BlockPos(baseX, origin.getY(), baseZ))) {
                    continue;
                }
                for (int x = baseX; x < baseX + 16; x++) {
                    for (int z = baseZ; z < baseZ + 16; z++) {
                        for (int y = origin.getY() - Y_RANGE; y <= origin.getY() + Y_RANGE; y++) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (level.getBlockState(p).is(ModBlocks.FLUORESCENT_LAMP.get())) {
                                st.lamps.add(p);
                            }
                        }
                    }
                }
            }
        }
        if (st.lamps.isEmpty()) {
            st.cooldown = MIN_INTERVAL;
            return;
        }
        // 第一次闪烁：先灭
        setAll(level, st, ModBlocks.LAMP_OFF.get().defaultBlockState());
        st.phase = 1;
        st.flickerCount = 0;
        st.phaseTicks = FLICKER_OFF;
    }

    /** 把本次涉及的灯全部设为指定状态。 */
    private static void setAll(ServerLevel level, State st, net.minecraft.world.level.block.state.BlockState state) {
        for (BlockPos p : st.lamps) {
            if (!level.isLoaded(p)) {
                continue;
            }
            var cur = level.getBlockState(p);
            if (cur.is(ModBlocks.FLUORESCENT_LAMP.get()) || cur.is(ModBlocks.LAMP_OFF.get())) {
                level.setBlock(p, state, 3);
            }
        }
    }

    /** 玩家离开 / 换维度时清理。 */
    public static void forget(UUID player) {
        STATES.remove(player);
    }
}
