package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModDimensions;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Level 0 环境雾的距离限制。
 *
 * <p>雾色完全交给群系数据包的 {@code effects.fog_color}；本类只把 Level 0 的
 * 可见距离压缩到 {@value #VIEW_DISTANCE_CHUNKS} 区块，营造「看不到远处」的观感。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class Level0Fog {

    // ==================== 视距限制（雾） ====================

    /**
     * Level 0 的有效视距（区块）。
     *
     * <p><b>说明</b>：Minecraft 的渲染距离是<b>客户端全局设置</b>，原生没有"按维度设置"的机制
     * （要做到那一点需要 Mixin 改 {@code LevelRenderer}，本模组没有 Mixin）。
     * 因此这里用<b>雾</b>做视觉上的视距限制：超过 {@value #VIEW_DISTANCE_CHUNKS} 区块的世界
     * 被雾完全吞没，玩家看不到远处，观感等同视距 8。</p>
     */
    public static final int VIEW_DISTANCE_CHUNKS = 8;

    /** 雾的终点距离（方块）：8 区块 × 16 = 128 格。 */
    public static final float FOG_END = VIEW_DISTANCE_CHUNKS * 16.0F;

    /** 雾的起点：终点前 2 区块开始起雾。 */
    public static final float FOG_START = FOG_END - 32.0F;

    /** 压缩雾距，让 Level 0 的可见距离约为 {@value #VIEW_DISTANCE_CHUNKS} 区块。 */
    @SubscribeEvent
    public static void onRenderFog(net.neoforged.neoforge.client.event.ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!mc.level.dimension().equals(ModDimensions.LEVEL_0_WORLD)) {
            return;
        }
        event.setNearPlaneDistance(FOG_START);
        event.setFarPlaneDistance(FOG_END);
        event.setCanceled(true);
    }

    private Level0Fog() {
    }
}