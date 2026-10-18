package com.the_backrooms_complex.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.the_backrooms_complex.entity.BacteriaEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 「细菌」的攻击触手层。
 *
 * <h2>动画</h2>
 * <p>利用 {@link net.minecraft.world.entity.LivingEntity#getAttackAnim(float)}（0~1 的挥击进度）驱动：</p>
 * <ul>
 *   <li>进度 0~0.25：触手从<b>体内</b>快速伸出（长度 0 → 最大）；</li>
 *   <li>进度 0.25~0.5：保持完全伸出；</li>
 *   <li>进度 0.5~1：快速缩回体内。</li>
 * </ul>
 *
 * <p>触手沿实体<b>面朝方向</b>（也就是被攻击玩家所在方向）伸出，
 * 起点相对身体中心有约 0.35 格的偏移，避免从正中央穿出来。</p>
 */
public class BacteriaTentacleLayer extends RenderLayer<BacteriaEntity,
        net.minecraft.client.model.SlimeModel<BacteriaEntity>> {

    /** 触手最大伸出长度（格）。 */
    public static final float MAX_LENGTH = 1.6F;
    /** 触手粗细（格）。 */
    public static final float THICKNESS = 0.18F;
    /** 起点相对身体中心的横向偏移（格）。 */
    public static final float SIDE_OFFSET = 0.35F;

    /** 触手用的模型部件（整块灰色，取贴图中的纯灰区域）。 */
    private final ModelPart tentacle;

    public BacteriaTentacleLayer(RenderLayerParent<BacteriaEntity,
            net.minecraft.client.model.SlimeModel<BacteriaEntity>> parent) {
        super(parent);

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 一根沿 +Z 方向伸出的长方盒；UV 取贴图的 (0,0) 区域（纯灰黑）
        root.addOrReplaceChild("tentacle",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-THICKNESS, -THICKNESS, 0.0F, THICKNESS * 2, THICKNESS * 2, MAX_LENGTH),
                PartPose.ZERO);
        this.tentacle = LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       BacteriaEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        float anim = entity.getAttackAnim(partialTicks);
        if (anim <= 0.0F) {
            return;   // 没有攻击动作 -> 触手完全缩在体内，不渲染
        }

        // 伸出曲线：0~0.25 伸出，0.25~0.5 保持，0.5~1 缩回
        float extend;
        if (anim < 0.25F) {
            extend = anim / 0.25F;
        } else if (anim < 0.5F) {
            extend = 1.0F;
        } else {
            extend = 1.0F - (anim - 0.5F) / 0.5F;
        }
        extend = Math.max(0.0F, Math.min(1.0F, extend));
        if (extend <= 0.01F) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucent(BacteriaRenderer.TEXTURE));
        poseStack.pushPose();
        // 起点偏移 + 沿面朝方向伸出
        poseStack.translate(SIDE_OFFSET, entity.getBbHeight() * 0.5F, 0.0F);
        poseStack.scale(1.0F, 1.0F, extend);
        // 缩放到实际尺寸（ModelPart 单位是 1/16 格）
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        this.tentacle.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
