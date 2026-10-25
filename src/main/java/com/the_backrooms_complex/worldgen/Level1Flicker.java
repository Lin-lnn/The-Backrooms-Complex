package com.the_backrooms_complex.worldgen;

import com.the_backrooms_complex.ModBlocks;
import com.the_backrooms_complex.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Level 1「闪烁」现象。
 *
 * <h2>文中设定</h2>
 * <p>"宜居地带的所有自然光源随机切断，导致整个层级陷入完全黑暗"。</p>
 *
 * <h2>作用范围：<b>只看有玩家所在的层</b></h2>
 * <p>Level 1 每 {@value #LAYER_H} 格为一层，实际只生成 {@value #LAYERS} 层。
 * 只有<b>当前有玩家站着的那几层</b>才会被扫描、才会熄灯；</p>
 * <ul>
 *   <li>某层的已加载区域内<b>没有玩家</b> ⇒ <b>整层跳过，不扫描</b>（性能 + 逻辑都正确）；</li>
 *   <li>有玩家 ⇒ 只扫描该玩家视距范围内、该层内的灯位。</li>
 * </ul>
 *
 * <h2>流程</h2>
 * <ol>
 *   <li>收集上述范围内的荧光灯，按 {@value #CHUNKS_PER_GROUP} 个区块切成若干组；</li>
 *   <li><b>每 {@value #GROUP_INTERVAL} tick（0.5 秒）熄灭一组</b>，直到全部熄灭；</li>
 *   <li>保持黑暗 {@value #DARK_MIN}~{@value #DARK_MAX} tick（45~60 秒）；</li>
 *   <li>再<b>每 0.5 秒亮起一组</b>，直到全部恢复；</li>
 *   <li>等待下一轮。</li>
 * </ol>
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

    /** 每层高度（格）。 */
    public static final int LAYER_H = 18;
    /** 总层数。 */
    public static final int LAYERS = Level1ChunkGenerator.LAYERS;
    /** 子层高度。 */
    private static final int SUB_H = 6;
    /** 子层数量。 */
    private static final int SUB_COUNT = 3;
    /** 灯间距（与生成器的 PASSAGE_LAMP_STEP 一致）。 */
    private static final int LAMP_STEP = 3;

    private static final int PHASE_IDLE = 0;
    private static final int PHASE_DARKEN = 1;
    private static final int PHASE_DARK = 2;
    private static final int PHASE_LIGHTEN = 3;

    /** 全局状态（只服务于 Level 1，一次现象全维度共享）。 */
    private static final class State {
        int phase = PHASE_IDLE;
        int cooldown = 0;
        int groupTimer = 0;
        int darkTicks = 0;
        final List<List<BlockPos>> groups = new ArrayList<>();
        int groupIndex = 0;
        final List<BlockPos> pending = new ArrayList<>();
        BlockState target = null;
    }

    private static final State STATE = new State();

    private Level1Flicker() {
    }

    /** 每 tick 由 {@code ModEvents} 调用一次（只处理 Level 1）。 */
    public static void tick(ServerLevel level) {
        if (!level.dimension().equals(ModDimensions.LEVEL_1_WORLD)) {
            return;
        }
        // 玩家当前所在的层集合；空集合说明没有任何人在 Level 1
        Set<Integer> activeLayers = activeLayers(level);
        if (activeLayers.isEmpty()) {
            reset();
            return;
        }

        // 有待写的灯就先写完，保证一组完整熄灭后才推进
        if (STATE.target != null) {
            drain(level);
            return;
        }

        switch (STATE.phase) {
            case PHASE_IDLE -> {
                if (STATE.cooldown > 0) {
                    STATE.cooldown--;
                    return;
                }
                collect(level, activeLayers);
                if (STATE.groups.isEmpty()) {
                    STATE.cooldown = MIN_INTERVAL;
                    return;
                }
                STATE.groupIndex = 0;
                STATE.groupTimer = 0;
                STATE.phase = PHASE_DARKEN;
            }
            case PHASE_DARKEN -> {
                if (advanceGroup(off())) {
                    STATE.darkTicks = DARK_MIN + level.random.nextInt(DARK_MAX - DARK_MIN + 1);
                    STATE.phase = PHASE_DARK;
                }
            }
            case PHASE_DARK -> {
                if (--STATE.darkTicks <= 0) {
                    STATE.groupIndex = 0;
                    STATE.groupTimer = 0;
                    STATE.phase = PHASE_LIGHTEN;
                }
            }
            case PHASE_LIGHTEN -> {
                if (advanceGroup(on())) {
                    STATE.groups.clear();
                    STATE.phase = PHASE_IDLE;
                    STATE.cooldown = MIN_INTERVAL
                            + level.random.nextInt(MAX_INTERVAL - MIN_INTERVAL + 1);
                }
            }
            default -> STATE.phase = PHASE_IDLE;
        }
    }

    /** 收集所有在 Level 1 的玩家所处的层号。 */
    private static Set<Integer> activeLayers(ServerLevel level) {
        Set<Integer> layers = new HashSet<>();
        for (ServerPlayer p : level.players()) {
            int layer = Math.floorDiv(p.blockPosition().getY(), LAYER_H);
            if (layer >= 0 && layer < LAYERS) {
                layers.add(layer);
            }
        }
        return layers;
    }

    /**
     * 推进一组：每 {@link #GROUP_INTERVAL} tick 取下一组开始写。
     *
     * @return 是否已全部处理完
     */
    private static boolean advanceGroup(BlockState target) {
        if (STATE.groupIndex >= STATE.groups.size()) {
            return true;
        }
        if (STATE.groupTimer > 0) {
            STATE.groupTimer--;
            return false;
        }
        STATE.pending.clear();
        STATE.pending.addAll(STATE.groups.get(STATE.groupIndex));
        STATE.groupIndex++;
        STATE.groupTimer = GROUP_INTERVAL;
        STATE.target = target;
        return false;
    }

    /** 写入一批；写完则清空 target。 */
    private static void drain(ServerLevel level) {
        int done = 0;
        Iterator<BlockPos> it = STATE.pending.iterator();
        while (it.hasNext() && done < BATCH) {
            BlockPos p = it.next();
            it.remove();
            if (!level.isLoaded(p)) {
                continue;
            }
            BlockState cur = level.getBlockState(p);
            if (cur.is(ModBlocks.FLUORESCENT_LAMP.get()) || cur.is(ModBlocks.LAMP_OFF.get())) {
                level.setBlock(p, STATE.target, 3);
                done++;
            }
        }
        if (!it.hasNext()) {
            STATE.target = null;
        }
    }

    /**
     * 只扫描<b>有玩家的层</b>里、每个该层玩家视距范围内的灯位。
     *
     * <p>灯位与生成器一致：层内 3 个子层，子层天花板在 {@code subOff == 5}，
     * 横向每 {@link #LAMP_STEP} 格一个（{@code x % 3 == 1 && z % 3 == 1}）。</p>
     */
    private static void collect(ServerLevel level, Set<Integer> activeLayers) {
        STATE.groups.clear();
        STATE.pending.clear();
        STATE.target = null;

        int radius = Math.min(MAX_RADIUS_CHUNKS,
                level.getServer().getPlayerList().getViewDistance());
        int maxY = level.getMaxBuildHeight();

        // 用 Set 去重（多个玩家 / 多个层可能覆盖同一盏灯）
        Set<BlockPos> lamps = new LinkedHashSet<>();
        for (ServerPlayer p : level.players()) {
            int layer = Math.floorDiv(p.blockPosition().getY(), LAYER_H);
            if (!activeLayers.contains(layer) || layer < 0 || layer >= LAYERS) {
                continue;
            }
            int cx = p.blockPosition().getX() >> 4;
            int cz = p.blockPosition().getZ() >> 4;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int baseX = (cx + dx) << 4;
                    int baseZ = (cz + dz) << 4;
                    if (!level.isLoaded(new BlockPos(baseX, p.blockPosition().getY(), baseZ))) {
                        continue;
                    }
                    for (int sub = 0; sub < SUB_COUNT; sub++) {
                        int y = layer * LAYER_H + sub * SUB_H + (SUB_H - 1);
                        if (y >= maxY) {
                            continue;
                        }
                        int x0 = baseX + Math.floorMod(1 - baseX, LAMP_STEP);
                        for (int x = x0; x < baseX + 16; x += LAMP_STEP) {
                            int z0 = baseZ + Math.floorMod(1 - baseZ, LAMP_STEP);
                            for (int z = z0; z < baseZ + 16; z += LAMP_STEP) {
                                BlockPos pos = new BlockPos(x, y, z);
                                if (level.getBlockState(pos).is(ModBlocks.FLUORESCENT_LAMP.get())) {
                                    lamps.add(pos);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 按「每 CHUNKS_PER_GROUP 个区块的量」切组：直接按数量切，简单且稳定
        List<BlockPos> all = new ArrayList<>(lamps);
        int perGroup = Math.max(1, CHUNKS_PER_GROUP * 25);
        for (int i = 0; i < all.size(); i += perGroup) {
            STATE.groups.add(new ArrayList<>(all.subList(i, Math.min(all.size(), i + perGroup))));
        }
    }

    private static BlockState on() {
        return ModBlocks.FLUORESCENT_LAMP.get().defaultBlockState();
    }

    private static BlockState off() {
        return ModBlocks.LAMP_OFF.get().defaultBlockState();
    }

    /** 清空状态（无人时调用）。 */
    private static void reset() {
        STATE.groups.clear();
        STATE.pending.clear();
        STATE.target = null;
        STATE.phase = PHASE_IDLE;
        STATE.cooldown = 0;
    }
}
