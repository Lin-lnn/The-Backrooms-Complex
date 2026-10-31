package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModNetwork;
import com.the_backrooms_complex.ModPlayerData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 饮水值条 HUD。
 *
 * <p>位于饥饿条正上方，右对齐；共 10 格，每格 8 像素间距、图标 9×9，
 * 每格代表 10 点（满值 100）。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class ModHud {

    private static final ResourceLocation THIRST_EMPTY = tex("thirst_empty");
    private static final ResourceLocation THIRST_HALF = tex("thirst_half");
    private static final ResourceLocation THIRST_FULL = tex("thirst_full");

    /** 每格像素间距、图标尺寸、格数。 */
    private static final int SPACING = 8;
    private static final int ICON = 9;
    private static final int SEGMENTS = 10;

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID,
                "textures/gui/" + name + ".png");
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
                ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "thirst"),
                ModHud::renderThirst);
    }

    private static void renderThirst(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        int value = ModNetwork.clientThirst;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        // 右对齐到饥饿条（右侧起点 = 中心 + 91），再向上移 10 像素
        int x0 = w / 2 + 91 - SEGMENTS * SPACING;
        int y = h - 39 - 10;
        drawBar(graphics, value, x0, y);
    }

    /** 画 10 格条：先画底图，再按数值叠加满格/半格。 */
    private static void drawBar(GuiGraphics graphics, int value, int x0, int y) {
        int perSegment = ModPlayerData.MAX / SEGMENTS;   // 10
        for (int i = 0; i < SEGMENTS; i++) {
            int x = x0 + i * SPACING;
            graphics.blit(THIRST_EMPTY, x, y, 0, 0, ICON, ICON, ICON, ICON);
            int segStart = i * perSegment;
            int filled = value - segStart;
            if (filled >= perSegment) {
                graphics.blit(THIRST_FULL, x, y, 0, 0, ICON, ICON, ICON, ICON);
            } else if (filled >= perSegment / 2) {
                graphics.blit(THIRST_HALF, x, y, 0, 0, ICON, ICON, ICON, ICON);
            }
        }
    }

    private ModHud() {
    }
}
