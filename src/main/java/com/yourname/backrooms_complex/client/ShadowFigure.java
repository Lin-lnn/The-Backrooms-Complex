package com.yourname.backrooms_complex.client;

import com.yourname.backrooms_complex.BackroomsComplex;
import com.yourname.backrooms_complex.ModNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 「人影幻视」——3D 公告板版（方案 D）。
 *
 * <h2>为什么用公告板而不是 GUI 叠加</h2>
 * <p>GUI 叠加永远是同一大小、永远在最上层；而在世界里画一个<b>始终朝向相机</b>的四边形，
 * 就能获得真实的空间感：远处的影子会变小、被墙壁遮挡、随镜头转动而改变方位。</p>
 *
 * <h2>规则</h2>
 * <ul>
 *   <li>精神值 &lt; {@link #THRESHOLD}（40）时，每 tick 有 {@link #CHANCE}（0.005%）概率触发；</li>
 *   <li>在玩家周边 {@link #MIN_DIST}~{@link #MAX_DIST} 格内随机一个<b>视线可达</b>的位置；</li>
 *   <li>影子<b>始终朝向玩家</b>，以人的比例（宽:高 = 1:2）显示；</li>
 *   <li>持续 {@link #SHOW_TICKS}（100 tick = 5 秒）后瞬间消失；</li>
 *   <li>消失同时触发 {@link #BLUR_TICKS}（1 秒）强模糊（0.5 秒淡入 + 0.5 秒淡出）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class ShadowFigure {

    /** 触发/开始的数值阈值 */
    public static final int THRESHOLD = 40;
    /** 每 tick 的触发概率 */
    public static final float CHANCE = 0.00005F;
    /** 影子的存活时间：5 秒。 */
    public static final int SHOW_TICKS = 100;
    /** 消失后的模糊时长（tick） */
    public static final int BLUR_TICKS = 20;

    /** 影子与玩家的距离范围。 */
    public static final double MIN_DIST = 30.0D;
    /** 生成/音源的最远距离（格） */
    public static final double MAX_DIST = 48.0D;
    /** 影子高度（格）。 */
    public static final float HEIGHT = 3.0F;

    /** 影子贴图 */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "textures/gui/shadow_figure.png");
    /** 影子使用的渲染类型（写深度，故会被墙遮挡） */
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);

    /** 确定性随机源 */
    private static final RandomSource RNG = RandomSource.create();

    /** 剩余显示 tick。 */
    private static int showTicks = 0;
    /** 影子底部中心的世界坐标。 */
    private static Vec3 shadowPos = Vec3.ZERO;

    /** 剩余模糊 tick */
    private static int blurTicks = 0;
    /** 供 DehydrationBlur 读取的额外模糊强度 0~1。 */
    public static float blurBoost = 0.0F;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            showTicks = 0;
            blurTicks = 0;
            blurBoost = 0.0F;
            return;
        }

        if (blurTicks > 0) {
            blurTicks--;
            int half = BLUR_TICKS / 2;
            blurBoost = blurTicks > half
                    ? (BLUR_TICKS - blurTicks) / (float) half
                    : blurTicks / (float) half;
            if (blurTicks == 0) {
                blurBoost = 0.0F;
            }
        }

        if (showTicks > 0) {
            showTicks--;
            if (showTicks == 0) {
                blurTicks = BLUR_TICKS;
                // 消失瞬间的蜂鸣声：仅本玩家能听到
                if (mc.level != null && mc.player != null) {
                    mc.level.playLocalSound(shadowPos.x, shadowPos.y + HEIGHT * 0.5, shadowPos.z,
                            net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT.value(),
                            net.minecraft.sounds.SoundSource.AMBIENT,
                            1.0F, 0.6F, false);
                }
            }
            return;
        }

        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        if (ModNetwork.clientSanity >= THRESHOLD) {
            return;
        }
        if (RNG.nextFloat() < CHANCE) {
            if (pickSpot(mc)) {
                showTicks = SHOW_TICKS;
            }
        }
    }

    /** 找一个视线可达、且脚下有实心方块的随机位置。 */
    private static boolean pickSpot(Minecraft mc) {
        Level level = mc.level;
        Vec3 eye = mc.player.getEyePosition();
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = RNG.nextDouble() * Math.PI * 2.0;
            double dist = MIN_DIST + RNG.nextDouble() * (MAX_DIST - MIN_DIST);
            int x = (int) Math.floor(eye.x + Math.cos(angle) * dist);
            int z = (int) Math.floor(eye.z + Math.sin(angle) * dist);
            // 从玩家眼睛高度向下找一段可站立的位置
            for (int dy = -6; dy <= 6; dy++) {
                BlockPos feet = new BlockPos(x, (int) Math.floor(eye.y) + dy, z);
                BlockPos below = feet.below();
                if (level.getBlockState(below).isSolidRender(level, below)
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()
                        && hasClearSpace(level, feet)) {
                    Vec3 target = new Vec3(x + 0.5D, feet.getY(), z + 0.5D);
                    // 视线可达性：连线上不能有实心方块
                    if (hasLineOfSight(level, eye, target.add(0, HEIGHT * 0.5, 0))) {
                        shadowPos = target;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 「周围 3x3 必须空旷」检查：
     * 影子宽 1.5 格、高 3 格，因此要求脚印 5x5 范围内的地面连续、
     * 且中间 3x3 的 3 层高度全为空气，避免贴图穿插进墙壁。
     */
    private static boolean hasClearSpace(Level level, BlockPos feet) {
        // 中间 3x3 的 3 层高度必须全是空气（影子本体占据的空间）
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy < 3; dy++) {
                    BlockPos p = feet.offset(dx, dy, dz);
                    if (!level.getBlockState(p).isAir()) {
                        return false;
                    }
                }
            }
        }
        // 外围一圈（5x5）脚下必须是实心，保证不是悬在走廊边缘
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    continue;
                }
                BlockPos below = feet.offset(dx, -1, dz);
                if (!level.getBlockState(below).isSolidRender(level, below)) {
                    return false;
                }
            }
        }
        return true;
    }
    /** 粗略的视线检测（逐格采样）。 */
    private static boolean hasLineOfSight(Level level, Vec3 from, Vec3 to) {
        int steps = (int) Math.ceil(from.distanceTo(to) * 2.0D);
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            BlockPos p = BlockPos.containing(
                    from.x + (to.x - from.x) * t,
                    from.y + (to.y - from.y) * t,
                    from.z + (to.z - from.z) * t);
            if (level.getBlockState(p).isSolidRender(level, p)) {
                return false;
            }
        }
        return true;
    }

    /** 在世界里画一个始终朝向相机的四边形。 */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (showTicks <= 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();

        float half = HEIGHT * 0.25F;   // 宽 = 高 / 2
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(shadowPos.x - cam.x, shadowPos.y - cam.y, shadowPos.z - cam.z);
        // 让四边形始终正对相机
        pose.mulPose(camera.rotation());

        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RENDER_TYPE);
        int light = 0xF000F0;   // 全亮，让影子在任何环境下都清晰
        int alpha = 235;
        // 四边形：左下、右下、右上、左上（UV 已翻转，贴图顶部朝上）
        vc.addVertex(m, -half, 0.0F, 0.0F).setColor(255, 255, 255, alpha).setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vc.addVertex(m, half, 0.0F, 0.0F).setColor(255, 255, 255, alpha).setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vc.addVertex(m, half, HEIGHT, 0.0F).setColor(255, 255, 255, alpha).setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        vc.addVertex(m, -half, HEIGHT, 0.0F).setColor(255, 255, 255, alpha).setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        pose.popPose();
        buffers.endBatch(RENDER_TYPE);
    }

    /** 供指令直接触发。 */
    public static void trigger() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && pickSpot(mc)) {
            showTicks = SHOW_TICKS;
        }
    }

    /** 当前是否有影子（调试用）。 */
    public static boolean isActive() {
        return showTicks > 0;
    }

    private ShadowFigure() {
    }
}