package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 精神值过低时的「抖动」事件。
 *
 * <h2>规则</h2>
 * <ul>
 *   <li>精神值 &lt; {@link #THRESHOLD}（50）时，<b>每 1 tick</b>有
 *       {@link #CHANCE}（0.05%）的概率触发；</li>
 *   <li>触发后屏幕<b>持续抖动 10 秒</b>（{@value #DURATION} tick）；</li>
 *   <li>抖动幅度<b>从最大线性衰减到 0</b>，归零时事件结束；</li>
 *   <li>事件进行中<b>不会再次触发</b>。</li>
 * </ul>
 *
 * <h2>实现</h2>
 * <p>通过 {@link ViewportEvent.ComputeCameraAngles} 在每帧的相机角度上叠加随机偏移
 * （yaw / pitch / roll 都抖，其中 roll 的抖动幅度更大，看起来像"视线不稳"）。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class SanityShake {

    /** 触发所需的精神值上限。 */
    public static final int THRESHOLD = 50;
    /** 每 tick 触发概率：0.05%。 */
    public static final float CHANCE = 0.0005F;
    /** 抖动持续 tick 数：10 秒（20 tick/秒 x 10）。 */
    public static final int DURATION = 200;
    /** 起始最大幅度（角度）。 */
    public static final float MAX_AMPLITUDE = 0.6F;

    /** 剩余抖动 tick 数；0 表示未在抖动中。 */
    private static int shakeTicks = 0;

    /** 确定性随机源 */
    private static final RandomSource RNG = RandomSource.create();

    /** 每 tick 判定是否触发。 */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            shakeTicks = 0;
            return;
        }
        // 事件进行中：递减计时，期间不再触发
        if (shakeTicks > 0) {
            shakeTicks--;
            return;
        }
        // 与数值条一致：仅生存/冒险模式
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        // 精神值 < 2：持续抖动（不消耗事件计时器，始终保持满幅度）
        if (ModNetwork.clientSanity < 2) {
            shakeTicks = DURATION;
            return;
        }
        if (ModNetwork.clientSanity >= THRESHOLD) {
            return;
        }
        if (RNG.nextFloat() < CHANCE) {
            shakeTicks = DURATION;
        }
    }

    /** 每帧修改相机角度，做出抖动。 */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTicks <= 0) {
            return;
        }
        // 幅度从 MAX_AMPLITUDE 线性衰减到 0
        float amplitude = MAX_AMPLITUDE * (shakeTicks / (float) DURATION);
        event.setYaw(event.getYaw() + (RNG.nextFloat() - 0.5F) * amplitude);
        event.setPitch(event.getPitch() + (RNG.nextFloat() - 0.5F) * amplitude);
        // roll 幅度更大：视觉上像"视线在打转"
        event.setRoll(event.getRoll() + (RNG.nextFloat() - 0.5F) * amplitude * 1.8F);
    }

    /** 供指令直接触发一次抖动。 */
    public static boolean isShaking() {
        return shakeTicks > 0;
    }

    /** 剩余 tick 数（调试用）。 */
    public static int remainingTicks() {
        return shakeTicks;
    }

    /** 供指令直接触发。 */
    public static void trigger() {
        shakeTicks = DURATION;
    }

    private SanityShake() {
    }
}