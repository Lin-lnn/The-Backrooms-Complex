package com.the_backrooms_complex.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 调试指令：{@code /backrooms trigger <玩家> <事件>}。
 *
 * <p>Brigadier 会把同名根节点合并，所以这里单独注册一次 {@code /backrooms} 是安全的，
 * 与 {@link BackroomsCommands} 中的 check / thirst / sanity 并存。</p>
 *
 * <p>可用事件：</p>
 * <ul>
 *   <li>{@code hallucination} —— 物品幻视（手持物名字随机变成原版物品名 + 强模糊）</li>
 *   <li>{@code fake_player} —— 假玩家幻视（随机默认皮肤，入屏 1 秒后消失 + 强模糊）</li>
 *   <li>{@code shake} —— 屏幕抖动 10 秒</li>
 * </ul>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID)
public final class TriggerCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("backrooms")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("trigger")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("event", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
                                                    String name = StringArgumentType.getString(ctx, "event");
                                                    if (!isKnown(name)) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("未知事件: " + name
                                                                        + "（可用: hallucination / shadow / fake_block / sound / control_loss / shake）"));
                                                        return 0;
                                                    }
                                                    ModNetwork.sendTrigger(player, name);
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "已向 " + player.getGameProfile().getName() + " 触发事件: " + name), true);
                                                    return 1;
                                                }))))
        );
    }

    private static boolean isKnown(String name) {
        return "hallucination".equals(name) || "shadow".equals(name)
                || "fake_block".equals(name) || "sound".equals(name) || "control_loss".equals(name)
                || "shake".equals(name);
    }

    private TriggerCommand() {
    }
}
