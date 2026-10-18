package com.the_backrooms_complex.client;

import com.the_backrooms_complex.entity.BacteriaEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 「细菌」的本体模型：一个很小的核心 + 一圈摇曳的短菌丝。
 *
 * <h2>结构</h2>
 * <ul>
 *   <li><b>核心</b>：6×6×6 模型单位（≈0.375 格）的小立方体，位于 y=12（≈0.75 格）；</li>
 *   <li><b>菌丝</b>：{@value #FILAMENT_COUNT} 根，围绕核心均匀分布，
 *       每根粗细 1 个单位（1 像素），长度在 {@value #FILAMENT_MIN} ~ {@value #FILAMENT_MAX} 之间随机；</li>
 *   <li>每根菌丝带一个独立的相位，用正弦函数让它们<b>各自摇摆</b>，不会整齐划一地动。</li>
 * </ul>
 *
 * <p>所有菌丝都是<b>短</b>的（< 1 格），与攻击触手（约 1.9 格）形成对比。</p>
 */
public class BacteriaModel extends EntityModel<BacteriaEntity> {

    /** 菌丝数量。 */
    public static final int FILAMENT_COUNT = 12;
    /** 菌丝最短长度（模型单位）。 */
    public static final float FILAMENT_MIN = 3.0F;
    /** 菌丝最长长度（模型单位）。 */
    public static final float FILAMENT_MAX = 7.0F;
    /** 核心高度（模型单位）；宽度用 CORE_SIZE，高度用本值 —— 更扁。 */
    public static final float CORE_HEIGHT = 2.5F;

    /** 核心边长（模型单位）。 */
    public static final float CORE_SIZE = 5.0F;
    /** 核心中心高度（模型单位；16 = 1 格）。 */
    public static final float CORE_Y = 21.0F;
    /** 摇摆幅度（弧度）。 */
    public static final float SWAY_AMPLITUDE = 0.45F;
    /** 摇摆速度。 */
    public static final float SWAY_SPEED = 0.18F;

    /** 核心部件。 */
    private final ModelPart core;
    /** 菌丝部件数组。 */
    private final ModelPart[] filaments;

    public BacteriaModel(ModelPart root) {
        this.core = root.getChild("core");
        this.filaments = new ModelPart[FILAMENT_COUNT];
        for (int i = 0; i < FILAMENT_COUNT; i++) {
            this.filaments[i] = this.core.getChild("f" + i);
        }
    }

    /**
     * 构建模型层定义。
     *
     * <p>菌丝用固定字符串 "f0".."f11" 命名，构造时按同样顺序取回。</p>
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 核心：小立方体，中心在 y = CORE_Y
        PartDefinition coreDef = root.addOrReplaceChild("core",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-CORE_SIZE / 2.0F, -CORE_HEIGHT / 2.0F, -CORE_SIZE / 2.0F,
                                CORE_SIZE, CORE_HEIGHT, CORE_SIZE),
                PartPose.offset(0.0F, CORE_Y, 0.0F));

        // 一圈菌丝：均匀分布在水平面，稍微向上/下倾斜
        for (int i = 0; i < FILAMENT_COUNT; i++) {
            double angle = (Math.PI * 2.0D / FILAMENT_COUNT) * i;
            // 每 3 根交替长短，避免看起来像一个圆环
            float length = (i % 3 == 0) ? FILAMENT_MAX : (i % 3 == 1 ? (FILAMENT_MAX + FILAMENT_MIN) / 2 : FILAMENT_MIN);
            float ox = (float) Math.cos(angle) * (CORE_SIZE / 2.0F);
            float oz = (float) Math.sin(angle) * (CORE_SIZE / 2.0F);
            float tilt = (float) Math.sin(angle) * 0.35F;   // 初始倾斜

            coreDef.addOrReplaceChild("f" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 0)
                            .addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, length),
                    PartPose.offsetAndRotation(ox, 0.0F, oz,
                            0.0F, (float) -angle, 0.0F));
        }

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(BacteriaEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 每根菌丝独立摇摆：相位错开，形成"随风飘动"的观感
        for (int i = 0; i < this.filaments.length; i++) {
            float phase = i * 0.9F;
            float sway = Mth.sin(ageInTicks * SWAY_SPEED + phase) * SWAY_AMPLITUDE;
            float sway2 = Mth.cos(ageInTicks * SWAY_SPEED * 0.7F + phase) * SWAY_AMPLITUDE * 0.6F;
            this.filaments[i].xRot = sway;
            this.filaments[i].zRot = sway2;
        }
        // 核心轻微呼吸
        float pulse = 1.0F + Mth.sin(ageInTicks * 0.12F) * 0.05F;
        this.core.xScale = pulse;
        this.core.yScale = pulse;
        this.core.zScale = pulse;
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack,
                               com.mojang.blaze3d.vertex.VertexConsumer consumer,
                               int packedLight, int packedOverlay, int color) {
        this.core.render(poseStack, consumer, packedLight, packedOverlay);
    }
}
