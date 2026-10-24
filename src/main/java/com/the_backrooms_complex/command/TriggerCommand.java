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
                                                  .suggests(EVENTS)
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

    /** 可用事件名（用于自动补全）。 */
    private static final String[] EVENT_NAMES = {
            "hallucination", "shadow", "fake_block", "sound", "control_loss", "topology", "shake"
    };

    /** 事件说明（补全提示里显示）。 */
    private static final java.util.Map<String, String> EVENT_DESCRIPTIONS = java.util.Map.of(
            "hallucination", "物品幻视：手持物名字随机变化 + 强模糊",
            "shadow", "阴影幻视：视野边缘出现黑影",
            "fake_block", "假方块：眼前出现不存在的方块",
            "sound", "幻听：随机播放脚步声",
            "control_loss", "失去控制：短时间移动方向错乱",
            "topology", "拓扑扰动：视线外开门 / 封路",
            "shake", "屏幕抖动 10 秒");
    /**
     * 事件名自动补全。
     *
     * <p>输入 {@code /backrooms trigger <玩家> } 后按 Tab 就会列出全部可用事件；
     * 每项还带一段说明，方便知道这个事件会做什么。</p>
     */
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> EVENTS =
            (ctx, builder) -> {
                for (String name : EVENT_NAMES) {
                    builder.suggest(name, Component.literal(EVENT_DESCRIPTIONS.getOrDefault(name, "")));
                }
                return builder.buildFuture();
            };


    private static boolean isKnown(String name) {
        return "hallucination".equals(name) || "shadow".equals(name)
                || "fake_block".equals(name) || "sound".equals(name) || "control_loss".equals(name) || "topology".equals(name)
                || "shake".equals(name);
    }

    private TriggerCommand() {
    }
}