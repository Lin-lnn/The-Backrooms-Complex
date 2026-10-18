package com.yourname.backrooms_complex.client;

import com.yourname.backrooms_complex.BackroomsComplex;
import com.yourname.backrooms_complex.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 饮水值过低时的<b>真正高斯模糊</b>。
 *
 * <h2>原理</h2>
 * <p>直接复用原版自带的模糊后处理链
 * {@code minecraft:shaders/post/blur.json}（物品栏背景用的那个，两趟分离高斯卷积），
 * 用它把已经渲染好的世界画面做一次模糊。{@link PostChain#process(float)} 的参数
 * 就是模糊半径，因此可以随饮水值连续变化。</p>
 *
 * <h2>为什么在 AFTER_LEVEL 阶段</h2>
 * <p>必须在<b>世界渲染完成之后、GUI 绘制之前</b>执行，这样只有世界画面被模糊，
 * 血条 / 物品栏 / HUD 依然清晰 —— 这也符合"人虚弱但界面正常"的观感。</p>
 *
 * <h2>阈值</h2>
 * <p>饮水值 &lt; {@link #THRESHOLD}（30）时开始模糊，越低半径越大，
 * 最大 {@link #MAX_RADIUS}（原版上限 10）。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class DehydrationBlur {

    /** 开始模糊的阈值。 */
    public static final int THRESHOLD = 50;
    /** 最大模糊半径（原版上限为 10）。 */
    public static final float MAX_RADIUS = 9.5F;
    /** 轻微模糊：30 <= 饮水值 < 50。 */
    public static final int MILD_THRESHOLD = 50;
    /** 轻微模糊档的半径 */
    public static final float MILD_RADIUS = 2.5F;
    /** 中度模糊：10 <= 饮水值 < 30。 */
    public static final int MEDIUM_THRESHOLD = 30;
    /** 中度模糊档的半径 */
    public static final float MEDIUM_RADIUS = 5.5F;
    /** 严重模糊：饮水值 < 10。 */
    public static final int SEVERE_THRESHOLD = 10;
    /** 严重模糊档的半径 */
    public static final float SEVERE_RADIUS = 9.5F;

    /** 后处理链缓存。 */
    private static PostChain chain;
    /** 上次 resize 时的帧缓冲宽度 */
    private static int lastWidth = -1;
    /** 上次 resize 时的帧缓冲高度 */
    private static int lastHeight = -1;

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        // 与数值条一致：仅生存/冒险显示
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        int thirst = ModNetwork.clientThirst;
        float boost = Math.max(ItemHallucination.blurBoost,
                Math.max(ShadowFigure.blurBoost, FakeBlockHallucination.blurBoost));   // 各事件的额外模糊 0~1
        if (thirst >= THRESHOLD && boost <= 0.0F) {
            return;
        }

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        try {
            if (chain == null) {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(),
                        mc.getMainRenderTarget(),
                        ResourceLocation.withDefaultNamespace("shaders/post/blur.json"));
                lastWidth = width;
                lastHeight = height;
                chain.resize(width, height);
            }
            if (lastWidth != width || lastHeight != height) {
                lastWidth = width;
                lastHeight = height;
                chain.resize(width, height);
            }
            // 三档模糊：<50 轻微 / <30 中度 / <10 严重
            float radius;
            if (thirst < SEVERE_THRESHOLD) {
                radius = SEVERE_RADIUS;
            } else if (thirst < MEDIUM_THRESHOLD) {
                radius = MEDIUM_RADIUS;
            } else {
                radius = MILD_RADIUS;
            }
            // 幻视事件的极强模糊可以与饮水值模糊叠加（取较大值）
            radius = Math.max(radius, boost * MAX_RADIUS);
            chain.process(radius);
            // 关键：PostChain.process 结束后帧缓冲仍停在离屏目标上，
            // 必须重新绑定回主帧缓冲，否则后续绘制的手臂 / 手持物品会被画到
            // 离屏目标里，表现为"手臂和手上的物品消失"。
            mc.getMainRenderTarget().bindWrite(false);
        } catch (Exception e) {
            // 加载/执行失败时停用，避免每帧刷异常
            closeQuietly();
        }
    }

    private static void closeQuietly() {
        if (chain != null) {
            try {
                chain.close();
            } catch (Exception ignored) {
                // ignore
            }
            chain = null;
        }
        lastWidth = -1;
        lastHeight = -1;
    }

    private DehydrationBlur() {
    }
}