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
    public static final float START_ALPHA = 0.55F;
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

    /** 贴地渲染：画一个水平四边形（不用摄像机朝向）。 */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vector3f[] corners = new Vector3f[]{
                new Vector3f(-1.0F, 0.0F, -1.0F),
                new Vector3f(-1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, -1.0F)
        };
        float size = this.getQuadSize(partialTicks);

        // 绕 Y 轴按 roll 旋转，让每片划痕角度不同
        float cos = Mth.cos(this.roll);
        float sin = Mth.sin(this.roll);
        for (Vector3f corner : corners) {
            float x = corner.x() * cos - corner.z() * sin;
            float z = corner.x() * sin + corner.z() * cos;
            corner.set(x * size, 0.0F, z * size);
        }

        float minU = this.getU0();
        float maxU = this.getU1();
        float minV = this.getV0();
        float maxV = this.getV1();

        // 位置相对摄像机的偏移
        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camera.getPosition().x);
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camera.getPosition().y);
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camera.getPosition().z);

        int light = LightTexture.FULL_BRIGHT;
        buffer.addVertex(px + corners[0].x(), py, pz + corners[0].z())
                .setUv(maxU, maxV).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setLight(light);
        buffer.addVertex(px + corners[1].x(), py, pz + corners[1].z())
                .setUv(maxU, minV).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setLight(light);
        buffer.addVertex(px + corners[2].x(), py, pz + corners[2].z())
                .setUv(minU, minV).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setLight(light);
        buffer.addVertex(px + corners[3].x(), py, pz + corners[3].z())
                .setUv(minU, maxV).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** 粒子提供者（持有 SpriteSet 里取出的贴图，传给粒子）。 */
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final net.minecraft.client.renderer.texture.TextureAtlasSprite sprite;

        public Provider(net.minecraft.client.particle.SpriteSet spriteSet) {
            this.sprite = spriteSet.get(0, 0);
        }

        @Override
        public net.minecraft.client.particle.Particle createParticle(
                SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ScratchParticle(level, x, y, z, this.sprite);
        }
    }
}
