package com.the_backrooms_complex.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.the_backrooms_complex.entity.BacteriaEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
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
 * 「细菌」的攻击触手层 —— 一根从体内伸出的灰黑菌丝。
 *
 * <h2>两种形态</h2>
 * <table border="1">
 *   <caption>攻击形态</caption>
 *   <tr><th>类型</th><th>来源</th><th>动画</th></tr>
 *   <tr><td>1 = 突刺</td><td>接触攻击</td><td>沿面朝方向直直伸出后缩回</td></tr>
 *   <tr><td>2 = 横扫</td><td>普通攻击</td><td><b>只扫一次</b>：−60° → +60°，末端背向扫动方向弯曲</td></tr>
 * </table>
 *
 * <h2>几何</h2>
 * <p>菌丝由两段盒子组成：</p>
 * <ul>
 *   <li><b>根段</b>：从身体中心沿 −Z 伸出；</li>
 *   <li><b>末段</b>：接在根段末端，绕 Y 轴额外偏转 —— 造成"鞭子甩出去时末端滞后"的弯曲感。</li>
 * </ul>
 *
 * <p>粗细为 1 模型单位 = 1 像素，起点在史莱姆模型的视觉中心（{@value #BODY_CENTER_Y} 格）。</p>
 */
public class BacteriaTentacleLayer extends RenderLayer<BacteriaEntity,
        net.minecraft.client.model.SlimeModel<BacteriaEntity>> {

    /** 根段长度（模型单位；16 = 1 格 ⇒ 18 ≈ 1.125 格）。 */
    public static final float ROOT_LENGTH = 18.0F;
    /** 末段长度（模型单位）。 */
    public static final float TIP_LENGTH = 12.0F;
    /** 菌丝粗细（模型单位；0.5×2 = 1 像素）。 */
    public static final float THICKNESS = 0.5F;
    /** 起点高度：史莱姆模型的视觉中心（格，从脚底算起）。 */
    public static final float BODY_CENTER_Y = 1.5F;
    /** 两种形态统一的额外抬高（格；3 像素 = 3/16 = 0.1875 格）。 */
    public static final float EXTRA_LIFT = 0.1875F;

    /** 横扫时额外抬高的高度（格；4 像素 = 4/16 = 0.25 格）。 */
    public static final float SWEEP_LIFT = 0.25F;
    /** 横扫角度范围：从 −60° 到 +60°。 */
    public static final float SWEEP_HALF_ANGLE = 60.0F;
    /** 横扫时末段背向扫动方向的弯曲角度（度）。 */
    public static final float SWEEP_BEND = 55.0F;
    /** 横扫动画在前多少比例内可见（之后收回）。 */
    public static final float SWEEP_VISIBLE_PCT = 0.85F;

    /** 根段模型部件。 */
    private final ModelPart root;
    /** 末段模型部件（接在根段末端）。 */
    private final ModelPart tip;

    public BacteriaTentacleLayer(RenderLayerParent<BacteriaEntity,
            net.minecraft.client.model.SlimeModel<BacteriaEntity>> parent) {
        super(parent);

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootPart = mesh.getRoot();

        // 根段：沿 -Z 伸出
        PartDefinition rootDef = rootPart.addOrReplaceChild("root",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-THICKNESS, -THICKNESS, -ROOT_LENGTH,
                                THICKNESS * 2.0F, THICKNESS * 2.0F, ROOT_LENGTH),
                PartPose.ZERO);

        // 末段：接在根段前端，继续沿 -Z 延伸
        rootDef.addOrReplaceChild("tip",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-THICKNESS, -THICKNESS, -TIP_LENGTH,
                                THICKNESS * 2.0F, THICKNESS * 2.0F, TIP_LENGTH),
                PartPose.offset(0.0F, 0.0F, -ROOT_LENGTH));

        ModelPart baked = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        this.root = baked.getChild("root");
        this.tip = this.root.getChild("tip");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       BacteriaEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        int remaining = entity.getAttackTicks();
        if (remaining <= 0) {
            return;                                  // 没有攻击动作 -> 不渲染
        }

        // 单调进度 0 -> 1（用于横扫：保证只扫一次）
        float p = 1.0F - remaining / (float) BacteriaEntity.ATTACK_ANIM_TICKS;
        p = Math.max(0.0F, Math.min(1.0F, p));

        int type = entity.getAttackType();
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucent(BacteriaRenderer.TEXTURE));

        poseStack.pushPose();

        if (type == 2) {
            // ======================= 横扫 =======================
            // 抬高 4 像素
            poseStack.translate(0.0F, BODY_CENTER_Y + SWEEP_LIFT + EXTRA_LIFT, 0.0F);
            // 只扫一次：-60° -> +60°
            float angle = -SWEEP_HALF_ANGLE + SWEEP_HALF_ANGLE * 2.0F * p;
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            // 末段背向扫动方向弯曲（扫得越远弯得越多）
            this.tip.yRot = (float) Math.toRadians(SWEEP_BEND * p);
        } else {
            // ======================= 突刺 =======================
            // 伸出 0~40%，保持 40~60%，缩回 60~100%
            float extend;
            if (p < 0.4F) {
                extend = p / 0.4F;
            } else if (p < 0.6F) {
                extend = 1.0F;
            } else {
                extend = 1.0F - (p - 0.6F) / 0.4F;
            }
            extend = Math.max(0.0F, Math.min(1.0F, extend));
            if (extend <= 0.01F) {
                poseStack.popPose();
                return;
            }
            poseStack.translate(0.0F, BODY_CENTER_Y + EXTRA_LIFT, 0.0F);
            poseStack.scale(1.0F, 1.0F, extend);
            this.tip.yRot = 0.0F;
        }

        this.root.visible = true;
        this.tip.visible = true;
        this.root.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
