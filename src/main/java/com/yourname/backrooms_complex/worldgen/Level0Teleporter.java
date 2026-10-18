package com.yourname.backrooms_complex.worldgen;

import com.yourname.backrooms_complex.BackroomsComplex;
import com.yourname.backrooms_complex.ModBlocks;
import com.yourname.backrooms_complex.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * 后室 Level 0 传送工具。
 *
 * <p>传送目标必须是一个"真正的走廊空位"，判定条件：</p>
 * <ul>
 *   <li>下方为 {@code backrooms_complex:floor_tile}（可站立）；</li>
 *   <li>脚部（y）与头部（y+1）为空气；</li>
 *   <li><b>不在特定结构内部</b>（避开 {@code backrooms_complex:level0} 结构的片段范围，
 *       避免直接落进结构墙体或封闭空间）。</li>
 * </ul>
 *
 * <p>搜索方式：以固定起点 {@code (0, 8, 0)} 为原点向外螺旋扩展，半径上限
 * {@link #SEARCH_RADIUS} 格，找到第一个有效位置立即返回，避免无限循环。</p>
 */
public final class Level0Teleporter {

    /** 螺旋搜索半径上限（格）。 */
    public static final int SEARCH_RADIUS = 64;
    /** 固定搜索起点（Y=8 位于第 1 层的走廊空间内）。 */
    public static final int START_X = 0;
    /** 搜索起始 Y：第 1 层的"脚部"高度 = 层高 + 1（层高 8 时为 9）。 */
    public static final int START_Y = BackroomsChunkGenerator.LAYER_HEIGHT + 1;
    public static final int START_Z = 0;

    private Level0Teleporter() {
    }

    /**
     * 把玩家传送到后室 Level 0 的一个有效走廊空位。
     *
     * @return 是否成功传送
     */
    public static boolean teleportToLevel0(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        ServerLevel target = serverPlayer.getServer().getLevel(ModDimensions.LEVEL_0_WORLD);
        if (target == null) {
            return false;
        }
        // 每次进入随机选择一个楼层：层高 8，共 32 层；脚部 Y = 层号 * 8 + 1
        int randomLayer = java.util.concurrent.ThreadLocalRandom.current()
                .nextInt(0, 256 / BackroomsChunkGenerator.LAYER_HEIGHT);
        int randomStartY = randomLayer * BackroomsChunkGenerator.LAYER_HEIGHT + 1;
        BlockPos spot = findSafeStand(target, START_X, randomStartY, START_Z);
        if (spot == null) {
            return false;
        }
        // 传送到方块中心，避免卡在边界
        serverPlayer.teleportTo(target,
                spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D,
                serverPlayer.getYRot(), serverPlayer.getXRot());
        return true;
    }

    /**
     * 以 (startX, startZ) 为起点向外螺旋搜索第一个有效站立位置。
     *
     * @return 有效位置；若半径内全部无效则返回 {@code null}
     */
    public static BlockPos findSafeStand(ServerLevel level, int startX, int startY, int startZ) {
        if (isValidStand(level, startX, startY, startZ)) {
            return new BlockPos(startX, startY, startZ);
        }
        for (int r = 1; r <= SEARCH_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // 只检查当前这一圈，避免重复检查内部
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int x = startX + dx;
                    int z = startZ + dz;
                    if (isValidStand(level, x, startY, z)) {
                        return new BlockPos(x, startY, z);
                    }
                }
            }
        }
        return null;
    }

    /** 有效站立位置判定（详见类注释）。 */
    public static boolean isValidStand(ServerLevel level, int x, int y, int z) {
        if (y <= level.getMinBuildHeight() || y + 1 >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockState below = level.getBlockState(new BlockPos(x, y - 1, z));
        if (!below.is(ModBlocks.FLOOR_TILE.get())) {
            return false; // 下方必须是地板方块
        }
        if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
            return false; // 脚部必须是空气（排除墙壁/踢脚线）
        }
        if (!level.getBlockState(new BlockPos(x, y + 1, z)).isAir()) {
            return false; // 头部必须是空气
        }
        // 放在最后：此时区块已因上面的读方块而被加载，结构信息可用
        return !isInsideStructure(level, new BlockPos(x, y, z));
    }

    /**
     * 目标位置是否落在某个 {@code backrooms_complex:level0} 特定结构的片段范围内。
     * 若是，则避开该位置（结构与走廊共用同一套方块，因此仅靠方块判定无法区分）。
     */
    private static boolean isInsideStructure(ServerLevel level, BlockPos pos) {
        Structure structure = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "level0"));
        if (structure == null) {
            return false;
        }
        return level.structureManager().getStructureWithPieceAt(pos, structure).isValid();
    }
}
