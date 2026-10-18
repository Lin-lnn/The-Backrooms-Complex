package com.the_backrooms_complex.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * 「划痕」粒子：贴在地面上的半透明黑色痕迹。
 *
 * <h2>特性</h2>
 * <ul>
 *   <li>存在 {@value #LIFETIME} tick（3 秒），期间透明度从 0.55 淡到 0；</li>
 *   <li><b>平铺在地面</b>（不是面向摄像机的广告牌）—— 通过覆写 render 画一个水平四边形；</li>
 *   <li>每 tick 尺寸略微扩散，像"黏液渗开"。</li>
 * </ul>
 */
public class ScratchParticle extends TextureSheetParticle {

    /** 存在时长：60 tick = 3 秒。 */
    public static final int LIFETIME = 60;
    /** 初始透明度。 */
    public static final float START_ALPHA = 0.75F;
    /** 贴图路径。 */
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    com.the_backrooms_complex.BackroomsComplex.MODID, "particle/scratch");

    protected ScratchParticle(ClientLevel level, double x, double y, double z,
                              net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        super(level, x, y, z);
        this.setSprite(sprite);   // 必须设置，否则渲染时 sprite 为 null 会崩
        this.lifetime = LIFETIME;
        this.quadSize = 0.55F;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.setAlpha(START_ALPHA);
        // 深灰黑
        this.setColor(0.10F, 0.10F, 0.11F);
        this.roll = (float) (this.random.nextFloat() * Math.PI * 2.0D);
    }

    @Override
    public void tick() {
        super.tick();
        // 淡出 + 轻微扩散
        float t = (float) this.age / this.lifetime;
        this.setAlpha(START_ALPHA * (1.0F - t));
        this.quadSize = 0.55F + t * 0.25F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /**
     * 粒子提供者。
     *
     * <p><b>注意</b>：注册事件发生时贴图图集尚未加载，此时调用
     * {@code spriteSet.get(...)} 会因内部 {@code sprites} 为 null 而崩溃。
     * 所以这里只保存 {@link SpriteSet}，真正取贴图推迟到
     * {@link #createParticle} —— 那时图集已就绪。</p>
     */
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final net.minecraft.client.particle.SpriteSet spriteSet;

        public Provider(net.minecraft.client.particle.SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public net.minecraft.client.particle.Particle createParticle(
                SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ScratchParticle(level, x, y, z, this.spriteSet.get(0, 0));
        }
    }
}
