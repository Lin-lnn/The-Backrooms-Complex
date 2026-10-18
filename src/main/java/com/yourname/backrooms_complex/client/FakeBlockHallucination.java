package com.yourname.backrooms_complex.client;

import com.yourname.backrooms_complex.BackroomsComplex;
import com.yourname.backrooms_complex.ModNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 精神值过低时的「假方块幻视」。
 *
 * <h2>与 ShadowFigure 相同的规则</h2>
 * <ul>
 *   <li>精神值 &lt; {@link #THRESHOLD}（40）时，每 tick {@link #CHANCE}（0.005%）概率触发；</li>
 *   <li>在玩家 {@link #MIN_DIST}~{@link #MAX_DIST} 格内随机一个<b>视线可达</b>的位置；</li>
 *   <li>该位置必须满足<b>3×3×3 空气 + 5×5 脚下实心</b>的大空间检查；</li>
 *   <li>用<b>原版方块</b>的随机一种，画出 2×2×2 的方块堆；</li>
 *   <li>持续 {@link #SHOW_TICKS}（5 秒）后瞬间消失，同时播放蜂鸣声并触发 1 秒强模糊。</li>
 * </ul>
 *
 * <p>方块是真的用 {@code BlockRenderDispatcher.renderSingleBlock} 画的 ⇒ 有正确的模型、贴图、
 * 光照与<b>深度遮挡</b>（会被墙挡住），但世界数据里根本不存在它。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class FakeBlockHallucination {

    /** 触发/开始的数值阈值 */
    public static final int THRESHOLD = 40;
    /** 每 tick 的触发概率 */
    public static final float CHANCE = 0.00005F;
    /** 存活 5 秒。 */
    public static final int SHOW_TICKS = 100;
    /** 消失后的模糊时长（tick） */
    public static final int BLUR_TICKS = 20;
    /** 生成/音源的最近距离（格） */
    public static final double MIN_DIST = 30.0D;
    /** 生成/音源的最远距离（格） */
    public static final double MAX_DIST = 48.0D;

    /** 确定性随机源 */
    private static final RandomSource RNG = RandomSource.create();

    /** 剩余显示 tick；0 表示未激活 */
    private static int showTicks = 0;
    /** 方块堆的左下前角坐标。 */
    private static BlockPos origin = BlockPos.ZERO;
    /** 本次使用的原版方块状态 */
    private static BlockState fakeState = Blocks.COBBLESTONE.defaultBlockState();

    /** 剩余模糊 tick */
    private static int blurTicks = 0;
    /** 供 DehydrationBlur 读取的额外模糊强度 0~1 */
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
                if (mc.level != null) {
                    mc.level.playLocalSound(origin.getX() + 1.0, origin.getY() + 1.0, origin.getZ() + 1.0,
                            net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT.value(),
                            net.minecraft.sounds.SoundSource.AMBIENT, 1.0F, 0.6F, false);
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
            trigger();
        }
    }

    /** 触发一次假方块幻视。 */
    public static void trigger() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        BlockPos spot = pickSpot(mc);
        if (spot == null) {
            return;
        }
        origin = spot;
        fakeState = randomVanillaState();
        showTicks = SHOW_TICKS;
    }

    /** 找一个满足大空间检查的落点（与原 ShadowFigure 一致）。 */
    private static BlockPos pickSpot(Minecraft mc) {
        Level level = mc.level;
        Vec3 eye = mc.player.getEyePosition();
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = RNG.nextDouble() * Math.PI * 2.0;
            double dist = MIN_DIST + RNG.nextDouble() * (MAX_DIST - MIN_DIST);
            int x = (int) Math.floor(eye.x + Math.cos(angle) * dist);
            int z = (int) Math.floor(eye.z + Math.sin(angle) * dist);
            for (int dy = -6; dy <= 6; dy++) {
                BlockPos feet = new BlockPos(x, (int) Math.floor(eye.y) + dy, z);
                if (!hasClearSpace(level, feet)) {
                    continue;
                }
                Vec3 target = new Vec3(x + 0.5D, feet.getY() + 1.0D, z + 0.5D);
                if (hasLineOfSight(level, eye, target)) {
                    return feet;
                }
            }
        }
        return null;
    }

    /** 3×3×3 全空气 + 5×5 脚下实心。 */
    private static boolean hasClearSpace(Level level, BlockPos feet) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy < 3; dy++) {
                    if (!level.getBlockState(feet.offset(dx, dy, dz)).isAir()) {
                        return false;
                    }
                }
            }
        }
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

    /** 逐格视线检测。 */
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

    /** 随机挑一种原版方块的默认状态。 */
    private static BlockState randomVanillaState() {
        for (int attempt = 0; attempt < 40; attempt++) {
            Block b = BuiltInRegistries.BLOCK
                    .byId(RNG.nextInt(BuiltInRegistries.BLOCK.size()));
            if (b == null || b == Blocks.AIR) {
                continue;
            }
            var id = BuiltInRegistries.BLOCK.getKey(b);
            if (!"minecraft".equals(id.getNamespace())) {
                continue;   // 只要原版方块
            }
            try {
                BlockState state = b.defaultBlockState();
                // 只要"完整方块"：
                //   canOcclude() == true  ⇒ 完整不透明立方体（排除楼梯、台阶、栅栏、花草、玻璃等）
                //   hasBlockEntity() == false ⇒ 排除箱子、熔炉等带实体数据的方块
                if (state.isAir() || !state.canOcclude() || state.hasBlockEntity()) {
                    continue;
                }
                return state;
            } catch (Exception ignored) {
                // 某些方块默认状态不可用，跳过
            }
        }
        return Blocks.STONE.defaultBlockState();
    }

    /** 画出 2×2×2 的方块堆。 */
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
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        pose.pushPose();
        pose.translate(origin.getX() - cam.x, origin.getY() - cam.y, origin.getZ() - cam.z);
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dz = 0; dz < 2; dz++) {
                    pose.pushPose();
                    pose.translate(dx, dy, dz);
                    mc.getBlockRenderer().renderSingleBlock(
                            fakeState, pose, buffers,
                            0xF000F0,                       // 全亮
                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
            }
        }
        pose.popPose();
        buffers.endBatch();
    }

    public static boolean isActive() {
        return showTicks > 0;
    }

    private FakeBlockHallucination() {
    }
}