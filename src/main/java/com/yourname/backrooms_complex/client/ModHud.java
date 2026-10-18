package com.yourname.backrooms_complex.client;

import com.yourname.backrooms_complex.BackroomsComplex;
import com.yourname.backrooms_complex.ModPlayerData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import com.yourname.backrooms_complex.ModNetwork;

/**
 * 「饮水值条」与「精神值条」的 HUD 渲染。
 *
 * <h2>位置</h2>
 * <ul>
 *   <li><b>饮水值条</b>：饥饿条<b>正上方</b>（右侧，右对齐到与饥饿条相同的位置）。</li>
 *   <li><b>精神值条</b>：血条<b>正上方</b>；若显示了盔甲条，则位于盔甲条上方。</li>
 * </ul>
 *
 * <h2>排版</h2>
 * <p>与原版血条/饥饿条一致：共 10 格，每格 8 像素间距，图标 9×9，
 * 每格代表 10 点（满值 100）。数值 ≥ 半格时画半格图标。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class ModHud {

    /** 饮水值的空底贴图 */
    private static final ResourceLocation THIRST_EMPTY = tex("thirst_empty");
    /** 饮水值的半格贴图 */
    private static final ResourceLocation THIRST_HALF = tex("thirst_half");
    /** 饮水值的满格贴图 */
    private static final ResourceLocation THIRST_FULL = tex("thirst_full");
    /** 精神值的空底贴图 */
    private static final ResourceLocation SANITY_EMPTY = tex("sanity_empty");
    /** 精神值的半格贴图 */
    private static final ResourceLocation SANITY_HALF = tex("sanity_half");
    /** 精神值的满格贴图 */
    private static final ResourceLocation SANITY_FULL = tex("sanity_full");

    /** 每格像素间距、图标尺寸、格数。 */
    private static final int SPACING = 8;
    /** 状态条图标的边长（像素） */
    private static final int ICON = 9;
    /** 状态条的总格数 */
    private static final int SEGMENTS = 10;

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "textures/gui/" + name + ".png");
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // 饮水值：饥饿条正上方
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
                ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "thirst"),
                ModHud::renderThirst);
        // 精神值：血条/盔甲条正上方
        event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL,
                ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "sanity"),
                ModHud::renderSanity);
    }

    private static void renderThirst(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        // 只有生存模式和冒险模式才显示这两条；创造/旁观模式隐藏
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        // 从同步缓存读取（服务端每秒推送一次）
        int value = ModNetwork.clientThirst;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        // 右对齐到饥饿条（右侧起点 = 中心 + 91），再向上移 10 像素
        int x0 = w / 2 + 91 - SEGMENTS * SPACING;
        int y = h - 39 - 10;
        drawBar(graphics, value, x0, y, THIRST_EMPTY, THIRST_HALF, THIRST_FULL);
    }

    private static void renderSanity(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        // 只有生存模式和冒险模式才显示这两条；创造/旁观模式隐藏
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        int value = ModNetwork.clientSanity;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        // 左对齐到血条（左侧起点 = 中心 - 91）
        int x0 = w / 2 - 91;
        // 血条在 h-39，其上方 10 像素是盔甲条位置；有盔甲再上移 10 像素
        int y = h - 39 - 10 - (mc.player.getArmorValue() > 0 ? 10 : 0);
        drawBar(graphics, value, x0, y, SANITY_EMPTY, SANITY_HALF, SANITY_FULL);
    }

    /** 画 10 格条：先画底图，再按数值叠加满格/半格。 */
    private static void drawBar(GuiGraphics graphics, int value, int x0, int y,
                                ResourceLocation empty, ResourceLocation half, ResourceLocation full) {
        int perSegment = ModPlayerData.MAX / SEGMENTS;   // 10
        for (int i = 0; i < SEGMENTS; i++) {
            int x = x0 + i * SPACING;
            graphics.blit(empty, x, y, 0, 0, ICON, ICON, ICON, ICON);
            int segStart = i * perSegment;
            int filled = value - segStart;
            if (filled >= perSegment) {
                graphics.blit(full, x, y, 0, 0, ICON, ICON, ICON, ICON);
            } else if (filled >= perSegment / 2) {
                graphics.blit(half, x, y, 0, 0, ICON, ICON, ICON, ICON);
            }
        }
    }

    private ModHud() {
    }
}