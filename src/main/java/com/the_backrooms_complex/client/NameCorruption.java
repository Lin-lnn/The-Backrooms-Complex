package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 精神值过低时的「名称乱码」。
 *
 * <p>精神值 &lt; {@link #THRESHOLD}（20）时，把<b>所有物品的名称</b>显示成随机跳动的乱码，
 * 仅对本地客户端生效（服务端与其他玩家看到的都是正常名称）。</p>
 *
 * <p>实现方式：给名称套上原版的 {@link ChatFormatting#OBFUSCATED}（`§k`）格式 ——
 * 游戏会用随机字形持续替换这段文字，正好就是「乱码」的观感，且无需自绘字体。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class NameCorruption {

    /** 触发阈值。 */
    public static final int THRESHOLD = 20;

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // 仅生存 / 冒险模式
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        if (ModNetwork.clientSanity >= THRESHOLD) {
            return;
        }
        if (event.getToolTip().isEmpty()) {
            return;
        }
        // 第 0 行是物品名称，其余是附加信息（附魔、耐久等）—— 只改名称
        Component name = event.getToolTip().get(0);
        event.getToolTip().set(0, name.copy().withStyle(ChatFormatting.OBFUSCATED));
    }

    private NameCorruption() {
    }
}