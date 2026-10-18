package com.the_backrooms_complex.command;

import com.mojang.brigadier.CommandDispatcher;
import com.the_backrooms_complex.ModNetwork;
import com.the_backrooms_complex.ModPlayerData;
import com.the_backrooms_complex.worldgen.UnionFind;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /backrooms} 调试命令。
 *
 * <ul>
 *   <li>{@code /backrooms check} —— 扫描玩家附近区域，用并查集输出空气连通分量数量（&gt;1 则警告）。</li>
 * </ul>
 *
 * <p>说明：方块破坏现在只由 {@code strength(-1.0F, 3600000.0F)} 控制（创造模式可破坏），
 * 因此原先的 {@code /backrooms toggleBreak} 已移除。</p>
 */
public final class BackroomsCommands {

    /** 由主类通过 {@code NeoForge.EVENT_BUS.addListener(...)} 注册。 */
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("backrooms")
                        .requires(source -> source.hasPermission(2)) // 仅 OP（权限等级 >= 2）
                        .then(Commands.literal("check")
                                .executes(ctx -> check(ctx.getSource())))
        );

        // ===== 数值修改指令：/backrooms thirst <0-100> ，/backrooms sanity <0-100> =====
        // Brigadier 会把同名根节点合并，因此这里再注册一次 /backrooms 是安全的。
        dispatcher.register(
                Commands.literal("backrooms")
                        .then(Commands.literal("thirst")
                                .then(Commands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, ModPlayerData.MAX))
                                        .executes(ctx -> {
                                            net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int target = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "value");
                                            ModPlayerData.addThirst(player, target - ModPlayerData.getThirst(player));
                                            ModNetwork.sendTo(player);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "饮水值已设为 " + ModPlayerData.getThirst(player)), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("sanity")
                                .then(Commands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, ModPlayerData.MAX))
                                        .executes(ctx -> {
                                            net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int target = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "value");
                                            ModPlayerData.addSanity(player, target - ModPlayerData.getSanity(player));
                                            ModNetwork.sendTo(player);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "精神值已设为 " + ModPlayerData.getSanity(player)), true);
                                            return 1;
                                        })))
        );

    }

    /**
     * 连通性自检：扫描玩家周围 32×32×32 区域内的空气方块，
     * 用并查集统计连通分量数量。若 &gt; 1 则打印警告。
     */


    private static int check(CommandSourceStack source) {
        Level level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        int r = 16;

        int minX = center.getX() - r, maxX = center.getX() + r;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - r);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + r);
        int minZ = center.getZ() - r, maxZ = center.getZ() + r;

        int sx = maxX - minX + 1;
        int sy = maxY - minY + 1;
        int sz = maxZ - minZ + 1;
        int total = sx * sy * sz;

        // 索引 = x*Sy*Sz + y*Sz + z
        UnionFind uf = new UnionFind(total);
        boolean[] isAir = new boolean[total];

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int idx = (x - minX) * sy * sz + (y - minY) * sz + (z - minZ);
                    isAir[idx] = level.getBlockState(mpos.set(x, y, z)).isAir();
                }
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int idx = (x - minX) * sy * sz + (y - minY) * sz + (z - minZ);
                    if (!isAir[idx]) {
                        continue;
                    }
                    if (x + 1 <= maxX && isAir[idx + sy * sz]) uf.union(idx, idx + sy * sz);
                    if (y + 1 <= maxY && isAir[idx + sz]) uf.union(idx, idx + sz);
                    if (z + 1 <= maxZ && isAir[idx + 1]) uf.union(idx, idx + 1);
                }
            }
        }

        boolean[] seenRoot = new boolean[total];
        int components = 0;
        for (int i = 0; i < total; i++) {
            if (!isAir[i]) {
                continue;
            }
            int root = uf.find(i);
            if (!seenRoot[root]) {
                seenRoot[root] = true;
                components++;
            }
        }

        final int result = components;
        source.sendSuccess(() -> Component.translatable("command.backrooms_complex.check", result), true);
        return result;
    }

    private BackroomsCommands() {
    }
}