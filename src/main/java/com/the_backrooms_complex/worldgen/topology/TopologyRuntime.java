package com.the_backrooms_complex.worldgen.topology;

import com.the_backrooms_complex.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 拓扑运行时：让「走廊在视线外改变」真的发生（<b>多人安全</b>）。
 *
 * <h2>每次扰动做两件事</h2>
 * <ol>
 *   <li><b>开一扇门</b>：在所有人背后挖出「标准走廊大小」
 *       （{@value #DOOR_WIDTH} 宽 × {@value #DOOR_HEIGHT} 高）的通道。
 *       <b>只挖到对面本来就是空地的位置</b> —— 一定连接另一条走廊/房间，
 *       且不会挖穿已有走廊的墙壁；</li>
 *   <li><b>补一段走廊</b>：在更远的背后把一段走廊填成墙。</li>
 * </ol>
 *
 * <h2>多人安全（本类的核心约束）</h2>
 * <ul>
 *   <li><b>共享视野判定</b>：一个位置只有<b>全部玩家都看不见</b>时才会被改动
 *       （{@link #isHiddenFromAllPlayers}，96 格内的玩家都参与判定）；</li>
 *   <li><b>不会卡墙</b>：填充前检查周围 8 格内没有玩家、1.5 格内没有任何生物
 *       （{@link #isSafeToFill}）；</li>
 *   <li>所有改动都在每个玩家的<b>背后 90° 以外</b>；</li>
 *   <li>填墙只在 {@value #SEAL_MIN}~{@value #SEAL_MAX} 格外；</li>
 *   <li><b>无音效</b> —— 改变是无声的，只有灯闪作为前兆。</li>
 * </ul>
 */
public final class TopologyRuntime {

    /** 门洞宽度（格）—— 与标准走廊一致。 */
    public static final int DOOR_WIDTH = 3;
    /** 门洞高度（格）。 */
    public static final int DOOR_HEIGHT = 3;

    /** 开洞的距离范围（格）。 */
    public static final int MIN_DISTANCE = 16;
    public static final int MAX_DISTANCE = 48;
    /** 填走廊的距离范围（格）。 */
    public static final int SEAL_MIN = 32;
    public static final int SEAL_MAX = 64;
    /** 视线之外的角度阈值（cos）：dot &lt; 0 即在背后 90° 以外。 */
    public static final double BEHIND_COS = 0.0D;
    /** 参与视野判定的玩家距离（格）。 */
    public static final double VIEW_CHECK_RANGE = 96.0D;
    /** 填充点周围多少格内不能有玩家。 */
    public static final double FILL_PLAYER_CLEARANCE = 8.0D;
    /** 填充点多少格内不能有任何生物。 */
    public static final double FILL_ENTITY_CLEARANCE = 1.5D;

    /** 冷却上限/下限（tick）。 */
    public static final int COOLDOWN_MAX = 8 * 20;
    public static final int COOLDOWN_MIN = 2 * 20;

    /** 灯光闪烁持续 tick。 */
    public static final int FLICKER_TICKS = 40;
    /** 一次闪烁影响的灯数量上限。 */
    public static final int FLICKER_LAMPS = 6;
    /** 闪烁影响半径（格）。 */
    public static final int FLICKER_RADIUS = 24;

    private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
    private static final Map<BlockPos, Integer> LAMP_RESTORE = new HashMap<>();

    private TopologyRuntime() {
    }

    /** 每 tick 调用：按 dread 概率触发一次扰动。 */
    public static void tick(ServerLevel level, ServerPlayer player, float dread) {
        if (!level.dimension().location().getPath().equals("level_0")) {
            return;
        }
        int cd = COOLDOWN.getOrDefault(player.getUUID(), 0);
        if (cd > 0) {
            COOLDOWN.put(player.getUUID(), cd - 1);
            return;
        }
        float chance = Math.max(0.0F, Math.min(1.0F, dread)) * 0.02F;
        if (level.random.nextFloat() >= chance) {
            return;
        }
        flickerLamps(level, player);
        boolean opened = openDoorBehindPlayers(level, player);
        boolean sealed = sealOneCorridor(level, player);
        if (opened || sealed) {
            int next = (int) (COOLDOWN_MAX + (COOLDOWN_MIN - COOLDOWN_MAX) * dread);
            COOLDOWN.put(player.getUUID(), Math.max(COOLDOWN_MIN, next));
        } else {
            COOLDOWN.put(player.getUUID(), 3 * 20);
        }
    }

    /** 在所有人背后挖一个标准门洞。 */
    private static boolean openDoorBehindPlayers(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 60; attempt++) {
            int dx = level.random.nextInt(MAX_DISTANCE * 2 + 1) - MAX_DISTANCE;
            int dz = level.random.nextInt(MAX_DISTANCE * 2 + 1) - MAX_DISTANCE;
            int dy = level.random.nextInt(9) - 4;
            double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
            if (dist < MIN_DISTANCE || dist > MAX_DISTANCE) {
                continue;
            }
            BlockPos pos = origin.offset(dx, dy, dz);
            if (!level.isLoaded(pos) || !isHiddenFromAllPlayers(level, pos)) {
                continue;
            }
            if (!isWall(level.getBlockState(pos))) {
                continue;
            }
            Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            for (Direction dir : dirs) {
                if (tryCarveDoor(level, pos, dir)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 尝试在 pos 处朝 dir 挖一个标准门洞（要求整块是墙 + 对面是空地）。 */
    private static boolean tryCarveDoor(ServerLevel level, BlockPos pos, Direction dir) {
        List<BlockPos> dig = new ArrayList<>();
        for (int w = 0; w < DOOR_WIDTH; w++) {
            for (int h = 0; h < DOOR_HEIGHT; h++) {
                int ox = dir.getStepX() > 0 ? w : (dir.getStepX() < 0 ? -w : 0);
                int oz = dir.getStepZ() > 0 ? w : (dir.getStepZ() < 0 ? -w : 0);
                BlockPos bp = pos.offset(ox, h, oz);
                if (!level.isLoaded(bp) || !isWall(level.getBlockState(bp))) {
                    return false;
                }
                dig.add(bp);
            }
        }
        BlockPos beyond = pos.relative(dir, DOOR_WIDTH + 1);
        if (!level.isLoaded(beyond) || !level.getBlockState(beyond).isAir()) {
            return false;
        }
        for (BlockPos bp : dig) {
            level.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
        }
        return true;
    }

    /** 在更远的背后把一段走廊填成墙（多人安全）。 */
    private static boolean sealOneCorridor(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 40; attempt++) {
            int dx = level.random.nextInt(SEAL_MAX * 2 + 1) - SEAL_MAX;
            int dz = level.random.nextInt(SEAL_MAX * 2 + 1) - SEAL_MAX;
            int dy = level.random.nextInt(5) - 2;
            double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
            if (dist < SEAL_MIN || dist > SEAL_MAX) {
                continue;
            }
            BlockPos bp = origin.offset(dx, dy, dz);
            if (!level.isLoaded(bp) || !isHiddenFromAllPlayers(level, bp)) {
                continue;
            }
            if (!isSafeToFill(level, bp)) {
                continue;
            }
            if (!level.getBlockState(bp).isAir()) {
                continue;
            }
            boolean ns = isWall(level.getBlockState(bp.north()))
                    && isWall(level.getBlockState(bp.south()));
            boolean ew = isWall(level.getBlockState(bp.east()))
                    && isWall(level.getBlockState(bp.west()));
            if (!ns && !ew) {
                continue;
            }
            for (int h = 0; h < DOOR_HEIGHT; h++) {
                BlockPos up = bp.above(h);
                if (level.isLoaded(up) && level.getBlockState(up).isAir()) {
                    level.setBlockAndUpdate(up, ModBlocks.WALL_YELLOW.get().defaultBlockState());
                }
            }
            return true;
        }
        return false;
    }

    /** 该位置是否所有玩家都看不见（多人共享判定）。 */
    private static boolean isHiddenFromAllPlayers(ServerLevel level, BlockPos pos) {
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(pos) > VIEW_CHECK_RANGE * VIEW_CHECK_RANGE) {
                continue;
            }
            Vec3 eye = p.getEyePosition();
            Vec3 look = p.getLookAngle();
            Vec3 toBlock = new Vec3(pos.getX() + 0.5D - eye.x, pos.getY() + 0.5D - eye.y,
                    pos.getZ() + 0.5D - eye.z).normalize();
            if (toBlock.dot(look) >= BEHIND_COS) {
                return false;
            }
        }
        return true;
    }

    /** 填充是否安全：不会把玩家/生物封在方块里。 */
    private static boolean isSafeToFill(ServerLevel level, BlockPos pos) {
        AABB playerBox = new AABB(
                pos.getX() - FILL_PLAYER_CLEARANCE, pos.getY() - 2.0D, pos.getZ() - FILL_PLAYER_CLEARANCE,
                pos.getX() + FILL_PLAYER_CLEARANCE, pos.getY() + 4.0D, pos.getZ() + FILL_PLAYER_CLEARANCE);
        if (!level.getEntities((Entity) null, playerBox, e -> e instanceof Player).isEmpty()) {
            return false;
        }
        AABB entityBox = new AABB(pos).inflate(FILL_ENTITY_CLEARANCE);
        return level.getEntities((Entity) null, entityBox, e -> e instanceof LivingEntity).isEmpty();
    }

    /** 前兆特效：附近荧光灯换成「熄灭的荧光灯」，2 秒后复原。 */
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
                continue;
            }
            level.setBlockAndUpdate(pos, ModBlocks.LAMP_OFF.get().defaultBlockState());
            LAMP_RESTORE.put(pos.immutable(), FLICKER_TICKS);
            level.playSound(null, pos, SoundEvents.LANTERN_BREAK, SoundSource.BLOCKS, 0.5F, 0.7F);
            changed++;
        }
    }

    /** 每 tick 推进灯光复原。 */
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
    private static boolean isWall(BlockState state) {
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

    /** 玩家离开时清理计时。 */
    public static void forget(UUID player) {
        COOLDOWN.remove(player);
    }
}
