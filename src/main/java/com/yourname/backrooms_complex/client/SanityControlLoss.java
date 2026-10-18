package com.yourname.backrooms_complex.client;

import com.yourname.backrooms_complex.BackroomsComplex;
import com.yourname.backrooms_complex.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 精神值过低时的「失控」事件。
 *
 * <h2>规则</h2>
 * <ul>
 *   <li>精神值 &lt; {@link #THRESHOLD}（40）时，每 tick 有 {@link #CHANCE}（0.005%）概率触发；</li>
 *   <li>持续 {@link #DURATION}（40 tick = 2 秒）；</li>
 *   <li>期间角色<b>持续向前移动</b>（模拟"腿自己在走"），
 *       但玩家的左右转向、后退、跳跃、潜行输入全部被忽略；</li>
 *   <li>事件进行中不会再次触发。</li>
 * </ul>
 *
 * <p>实现方式：每 tick 强制把"前进"键设为按下、其余移动键设为松开。
 * 由于 {@code KeyMapping#isDown()} 是输入轮询的实际来源，
 * 强制其状态即可达到"无法操控"的效果。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class SanityControlLoss {

    /** 触发阈值。 */
    public static final int THRESHOLD = 40;
    /** 每 tick 触发概率：0.005%。 */
    public static final float CHANCE = 0.00005F;
    /** 持续 tick：2 秒。 */
    public static final int DURATION = 40;

    private static final RandomSource RNG = RandomSource.create();

    /** 剩余失控 tick；0 表示未在失控中。 */
    private static int remaining = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            remaining = 0;
            return;
        }

        // 失控中：强制前进，忽略其他移动输入
        if (remaining > 0) {
            remaining--;
            mc.options.keyUp.setDown(true);
            mc.options.keyDown.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keyShift.setDown(false);
            return;
        }

        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        if (ModNetwork.clientSanity >= THRESHOLD) {
            return;
        }
        if (RNG.nextFloat() < CHANCE) {
            remaining = DURATION;
        }
    }

    /** 供指令直接触发。 */
    public static void trigger() {
        remaining = DURATION;
    }

    /** 当前是否在失控中（调试用）。 */
    public static boolean isActive() {
        return remaining > 0;
    }

    private SanityControlLoss() {
    }
}
