package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 模组物品的说明文字（tooltip）。
 *
 * <h2>格式</h2>
 * <p>每件物品固定三行：</p>
 * <ol>
 *   <li><b>外观</b>（灰色）—— 这东西看起来是什么样；</li>
 *   <li><b>功能</b>（浅蓝）—— 它能做什么；</li>
 *   <li><b>用途</b>（淡黄）—— 玩家拿它该干什么。</li>
 * </ol>
 *
 * <h2>实现方式</h2>
 * <p>不在每个 Item 类里覆写 {@code appendHoverText}，而是统一在
 * {@link ItemTooltipEvent} 里按物品 id 拼装语言键
 * {@code item.backrooms_complex.<id>.line1/2/3}，缺失的行自动跳过。
 * 这样文案全部集中在语言文件里，改文字不用碰 Java。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class ItemTooltips {

    /** 说明行前缀。 */
    private static final String KEY_PREFIX = "item.backrooms_complex.";

    /** 三行说明的格式：外观 / 功能 / 用途。 */
    private static final ChatFormatting[] STYLES = {
            ChatFormatting.GRAY,        // 外观
            ChatFormatting.AQUA,        // 功能
            ChatFormatting.YELLOW       // 用途
    };

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !BackroomsComplex.MODID.equals(id.getNamespace())) {
            return;   // 只管本模组的物品
        }
        String path = id.getPath();
        for (int i = 1; i <= STYLES.length; i++) {
            String key = KEY_PREFIX + path + ".line" + i;
            if (net.minecraft.client.resources.language.I18n.exists(key)) {
                event.getToolTip().add(Component.translatable(key).withStyle(STYLES[i - 1]));
            }
        }
    }

    private ItemTooltips() {
    }
}
