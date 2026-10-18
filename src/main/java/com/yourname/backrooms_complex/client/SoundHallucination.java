package com.yourname.backrooms_complex.client;

import com.yourname.backrooms_complex.BackroomsComplex;
import com.yourname.backrooms_complex.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 精神值过低时的「幻听」。
 *
 * <p>精神值 &lt; {@link #THRESHOLD}（40）时，每 tick {@link #CHANCE}（0.005%）概率触发一次：
 * 在玩家周围随机方位播放一段<b>原版恐怖音效</b>（末影龙、监守者、远古守卫者、洞穴环境音等）。</p>
 *
 * <p>音效通过 {@code playLocalSound} 播放 ⇒ <b>只有本玩家能听到</b>，其他玩家与服务器完全不知情。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class SoundHallucination {

    /** 触发/开始的数值阈值 */
    public static final int THRESHOLD = 40;
    /** 每 tick 的触发概率 */
    public static final float CHANCE = 0.00005F;
    /** 音源距离范围。 */
    public static final double MIN_DIST = 6.0D;
    /** 生成/音源的最远距离（格） */
    public static final double MAX_DIST = 28.0D;

    /** 可用的原版恐怖音效。 */
    private static final SoundEvent[] SOUNDS = {
            SoundEvents.ENDER_DRAGON_GROWL,
            SoundEvents.WARDEN_ROAR,
            SoundEvents.WARDEN_HEARTBEAT,
            SoundEvents.ELDER_GUARDIAN_CURSE,
            SoundEvents.AMBIENT_CAVE.value(),
            SoundEvents.WOLF_HOWL,
            SoundEvents.CREEPER_PRIMED,
            SoundEvents.GHAST_AMBIENT,
            SoundEvents.WITHER_AMBIENT,
            SoundEvents.SKELETON_AMBIENT,
            SoundEvents.ENDERMAN_AMBIENT,
            SoundEvents.EVOKER_AMBIENT
    };

    /** 确定性随机源 */
    private static final RandomSource RNG = RandomSource.create();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        if (ModNetwork.clientSanity >= THRESHOLD) {
            return;
        }
        if (RNG.nextFloat() < CHANCE) {
            trigger();
        }
    }

    /** 触发一次幻听。 */
    public static void trigger() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        SoundEvent sound = SOUNDS[RNG.nextInt(SOUNDS.length)];
        double angle = RNG.nextDouble() * Math.PI * 2.0;
        double dist = MIN_DIST + RNG.nextDouble() * (MAX_DIST - MIN_DIST);
        double x = mc.player.getX() + Math.cos(angle) * dist;
        double z = mc.player.getZ() + Math.sin(angle) * dist;
        double y = mc.player.getY() + (RNG.nextDouble() - 0.5) * 6.0;

        // 只有本玩家能听到（playLocalSound 不会上传到服务端）
        mc.level.playLocalSound(x, y, z, sound, SoundSource.AMBIENT,
                1.4F, 0.85F + RNG.nextFloat() * 0.3F, false);
    }

    private SoundHallucination() {
    }
}