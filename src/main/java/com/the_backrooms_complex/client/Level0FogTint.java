package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModDimensions;
import com.the_backrooms_complex.ModNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Level 0 环境雾的着色。
 *
 * <ul>
 *   <li><b>正常情况</b>：完全交给群系数据包的 {@code effects.fog_color}，
 *       三个群系各自有自己的雾色（黄 / 灰 / 暗绿），与方块着色保持同一套配色标准。</li>
 *   <li><b>精神值 &lt; 5</b>：雾色统一变为与方块相同的<b>暗红</b>
 *       （{@link BiomeTint#DARK_RED}），让"世界变红"从方块延伸到整个环境。</li>
 * </ul>
 *
 * <p>只对生存 / 冒险模式生效，与其它低精神值 / 低饮水值特效保持一致。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class Level0FogTint {

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        // 只处理 Level 0
        if (!mc.level.dimension().equals(ModDimensions.LEVEL_0_WORLD)) {
            return;
        }
        // 仅生存 / 冒险模式
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        // 精神值 >= 5：保持群系自带雾色
        if (ModNetwork.clientSanity >= BiomeTint.DARK_RED_SANITY) {
            return;
        }
        int c = BiomeTint.DARK_RED;
        event.setRed(((c >> 16) & 0xFF) / 255.0F);
        event.setGreen(((c >> 8) & 0xFF) / 255.0F);
        event.setBlue((c & 0xFF) / 255.0F);
    }

    private Level0FogTint() {
    }
}
