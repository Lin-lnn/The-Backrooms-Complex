package com.the_backrooms_complex.worldgen.topology;

import com.the_backrooms_complex.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 拓扑运行时：让「走廊在视线外改变」这件事**真的发生**（方案 B 简化版）。
 *
 * <h2>为什么不改拓扑图，而是改方块</h2>
 * <p>拓扑图（{@link RoomGraph}）是<b>纯逻辑</b>，玩家看不见。要让玩家体验到"路变了"，
 * 最终必须落到<b>方块</b>上。</p>
 *
 * <h2>简化版规则（保守、只增连通）</h2>
 * <ul>
 *   <li>只在玩家<b>视线外</b>（前方 120° 之外）且距离 16~48 格的位置动土；</li>
 *   <li>只做<b>打通</b>（把墙挖开），<b>从不补墙</b> ⇒ 连通性只增不减 ⇒ <b>永不把玩家封死</b>；</li>
 *   <li>每次扰动最多改 {@value #MAX_BLOCKS_PER_EVENT} 个方块；</li>
 *   <li>触发概率随精神值（dread = 1 − sanity/100）升高。</li>
 * </ul>
 *
 * <p>副作用：长期游玩后墙体逐渐变少（因为只打通不补墙）。这是**有意为之的保守取舍** ——
 * 宁可世界越来越通透，也不能把玩家关死。</p>
 */
public final class TopologyRuntime {

    /** 每次扰动最多改动的方块数。 */
    public static final int MAX_BLOCKS_PER_EVENT = 3;
    /** 扰动的最小距离（格）—— 必须大于玩家的视野清晰范围。 */
    public static final int MIN_DISTANCE = 16;
    /** 扰动的最大距离（格）。 */
    public static final int MAX_DISTANCE = 48;
    /** 视线之外的角度阈值（cos 值）：dot &lt; 0 表示在背后 90° 以外。 */
    public static final double BEHIND_COS = 0.0D;
    /** 冷却上限（tick）；dread 越高冷却越短。 */
    public static final int COOLDOWN_MAX = 8 * 20;
    /** 冷却下限（tick）。 */
    public static final int COOLDOWN_MIN = 2 * 20;

    /** 每个玩家的冷却计时。 */
    private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();

    private TopologyRuntime() {
    }

    /**
     * 每 tick 调用：按 dread 概率触发一次「视线外打通」。
     *
     * @param level  服务端世界
     * @param player 玩家
     * @param dread  压力值 0~1（= 1 − sanity/100）
     */
    public static void tick(ServerLevel level, ServerPlayer player, float dread) {
        if (!level.dimension().location().getPath().equals("level_0")) {
            return;
        }
        int cd = COOLDOWN.getOrDefault(player.getUUID(), 0);
        if (cd > 0) {
            COOLDOWN.put(player.getUUID(), cd - 1);
            return;
        }
        // 每 tick 的触发概率：dread 越高越容易
        float chance = Math.max(0.0F, Math.min(1.0F, dread)) * 0.02F;
        if (level.random.nextFloat() >= chance) {
            return;
        }
        // 前兆：先让附近灯闪一下
        flickerLamps(level, player);
        if (openOneWallBehindPlayer(level, player)) {
            int next = (int) (COOLDOWN_MAX + (COOLDOWN_MIN - COOLDOWN_MAX) * dread);
            COOLDOWN.put(player.getUUID(), Math.max(COOLDOWN_MIN, next));
        } else {
            COOLDOWN.put(player.getUUID(), 3 * 20);
        }
    }

    /**
     * 在玩家背后随机找一个墙方块并打通。
     *
     * @return 是否改动了好方块
     */
    private static boolean openOneWallBehindPlayer(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        BlockPos origin = player.blockPosition();

        for (int attempt = 0; attempt < 40; attempt++) {
            int dx = level.random.nextInt(MAX_DISTANCE * 2 + 1) - MAX_DISTANCE;
            int dz = level.random.nextInt(MAX_DISTANCE * 2 + 1) - MAX_DISTANCE;
            int dy = level.random.nextInt(9) - 4;
            double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
            if (dist < MIN_DISTANCE || dist > MAX_DISTANCE) {
                continue;
            }
            BlockPos pos = origin.offset(dx, dy, dz);
            if (!level.isLoaded(pos)) {
                continue;
            }
            // 视线判定：在背后（与视线方向点积 < 0）
            Vec3 toBlock = new Vec3(pos.getX() + 0.5D - eye.x, pos.getY() + 0.5D - eye.y,
                    pos.getZ() + 0.5D - eye.z).normalize();
            if (toBlock.dot(look) > BEHIND_COS) {
                continue;   // 在视野内，跳过
            }
            // 只处理本模组的墙壁方块
            var state = level.getBlockState(pos);
            if (!isWall(state)) {
                continue;
            }
            // 打通（只挖不补）
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.4F, 1.6F);
            return true;
        }
        return false;
    }

    /** 待复原的灯：pos -> 剩余 tick。 */
    private static final Map<BlockPos, Integer> LAMP_RESTORE = new HashMap<>();
    /** 闪烁持续的 tick 数。 */
    public static final int FLICKER_TICKS = 40;
    /** 一次闪烁影响的灯数量上限。 */
    public static final int FLICKER_LAMPS = 6;
    /** 闪烁影响的半径（格）。 */
    public static final int FLICKER_RADIUS = 24;

    /**
     * 前兆特效：把玩家附近的荧光灯换成「熄灭的荧光灯」，{@value #FLICKER_TICKS} tick 后复原。
     *
     * <p>这不是装饰 —— 它在"墙被打开"之前给玩家一次听觉与视觉的预警，
     * 让改变显得"有因果"而不是凭空发生。</p>
     */
    public static void flickerLamps(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        int changed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-FLICKER_RADIUS, -8, -FLICKER_RADIUS),
                origin.offset(FLICKER_RADIUS, 8, FLICKER_RADIUS))) {
            if (changed >= FLICKER_LAMPS) {
                break;
            }
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!level.getBlockState(pos).is(ModBlocks.FLUORESCENT_LAMP.get())) {
                continue;
            }
            if (level.random.nextFloat() > 0.35F) {
                continue;   // 只闪一部分灯
            }
            level.setBlockAndUpdate(pos, ModBlocks.LAMP_OFF.get().defaultBlockState());
            LAMP_RESTORE.put(pos.immutable(), FLICKER_TICKS);
            level.playSound(null, pos, SoundEvents.LANTERN_BREAK, SoundSource.BLOCKS, 0.5F, 0.7F);
            changed++;
        }
    }

    /** 每 tick 推进灯光的复原计时。 */
    public static void tickLampRestore(ServerLevel level) {
        if (LAMP_RESTORE.isEmpty()) {
            return;
        }
        var it = LAMP_RESTORE.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            int left = e.getValue() - 1;
            if (left > 0) {
                e.setValue(left);
                continue;
            }
            BlockPos pos = e.getKey();
            if (level.isLoaded(pos) && level.getBlockState(pos).is(ModBlocks.LAMP_OFF.get())) {
                level.setBlockAndUpdate(pos, ModBlocks.FLUORESCENT_LAMP.get().defaultBlockState());
            }
            it.remove();
        }
    }
    /** 是否为本模组的墙壁方块。 */
    private static boolean isWall(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(ModBlocks.WALL_YELLOW.get())
                || state.is(ModBlocks.WALL_YELLOW_PLINTH.get())
                || state.is(ModBlocks.WALL_CONCRETE.get())
                || state.is(ModBlocks.WALL_CONCRETE_PLINTH.get())
                || state.is(ModBlocks.WALL_DAMP.get())
                || state.is(ModBlocks.WALL_DAMP_PLINTH.get())
                || state.is(ModBlocks.WALL_OAKED.get())
                || state.is(ModBlocks.CRACKED_CONCRETE_WALL.get())
                || state.is(ModBlocks.WARNING_CONCRETE_WALL.get())
                || state.is(ModBlocks.CHISELED_CONCRETE_WALL.get());
    }

    /** 玩家离开时清理计时，避免表无限增长。 */
    public static void forget(UUID player) {
        COOLDOWN.remove(player);
    }
}
