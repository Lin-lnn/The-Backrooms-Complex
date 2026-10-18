package com.the_backrooms_complex.client;

import com.the_backrooms_complex.entity.BacteriaEntity;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * 「细菌」的渲染器。
 *
 * <p>复用原版的 {@link SlimeModel}（史莱姆模型）—— 它本身就是"几个大小不等的
 * 半透明方块堆叠"，正好对应细菌的<b>黏液团</b>形态。</p>
 *
 * <h2>半透明</h2>
 * <p>通过覆写 {@code getRenderType} 返回 {@link RenderType#entityTranslucent}，
 * 让整个模型以约 70% 不透明度渲染 —— 看起来像半透明的灰黑色黏液。</p>
 */
public class BacteriaRenderer extends LivingEntityRenderer<BacteriaEntity, SlimeModel<BacteriaEntity>> {

    /** 贴图路径。 */
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    com.the_backrooms_complex.BackroomsComplex.MODID, "textures/entity/bacteria.png");

    public BacteriaRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.SLIME)), 0.5F);
        this.addLayer(new SlimeOuterLayer<>(this, context.getModelSet()));
        // 攻击触手（灰黑色，从体内伸出）
        this.addLayer(new BacteriaTentacleLayer(this));
    }

    /**
     * 视觉上的「爬墙 / 附顶」效果（方案 B）。
     *
     * <p>实体本身仍在地面行走（不引入寻路 bug），但渲染时根据
     * <b>四邻与正上方是否有实心方块</b> 对模型做旋转与平移：</p>
     * <ul>
     *   <li>侧面贴墙 ⇒ 模型旋转 90° 贴到墙上；</li>
     *   <li>头顶有方块 ⇒ 模型旋转 180° 倒挂在天花板上；</li>
     *   <li>四面无墙 ⇒ 正常贴地。</li>
     * </ul>
     */
    @Override
    protected void setupRotations(com.the_backrooms_complex.entity.BacteriaEntity entity,
                                  com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks, float scale) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks, scale);

        var level = entity.level();
        var box = entity.getBoundingBox();
        double cx = (box.minX + box.maxX) * 0.5D;
        double cz = (box.minZ + box.maxZ) * 0.5D;

        // 正上方有实心方块 -> 倒挂在天花板
        net.minecraft.core.BlockPos above = net.minecraft.core.BlockPos.containing(cx, box.maxY + 0.3D, cz);
        if (level.getBlockState(above).isSolidRender(level, above)) {
            poseStack.translate(0.0F, entity.getBbHeight(), 0.0F);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
            return;
        }

        // 侧面贴墙 -> 朝那面墙倾倒 90°
        net.minecraft.core.Direction[] sides = {
                net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH,
                net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST
        };
        for (net.minecraft.core.Direction dir : sides) {
            double px = cx + dir.getStepX() * 0.55D;
            double pz = cz + dir.getStepZ() * 0.55D;
            net.minecraft.core.BlockPos probe = net.minecraft.core.BlockPos.containing(px, box.minY + 0.4D, pz);
            if (!level.getBlockState(probe).isSolidRender(level, probe)) {
                continue;
            }
            // 沿墙方向旋转 90°，并把模型挪到墙面
            float angle = switch (dir) {
                case NORTH -> 90.0F;
                case SOUTH -> -90.0F;
                case WEST -> -90.0F;
                default -> 90.0F;
            };
            poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);
            if (dir.getAxis() == net.minecraft.core.Direction.Axis.Z) {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(angle));
            } else {
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
            }
            poseStack.translate(0.0F, -entity.getBbHeight() * 0.5F, 0.0F);
            return;
        }
    }
    @Override
    public ResourceLocation getTextureLocation(BacteriaEntity entity) {
        return TEXTURE;
    }

    /** 半透明渲染：让黏液呈现"能看见里面的轮廓"的观感。 */
    @Override
    protected RenderType getRenderType(BacteriaEntity entity, boolean visible,
                                       boolean translucent, boolean outline) {
        ResourceLocation texture = this.getTextureLocation(entity);
        if (outline) {
            return RenderType.outline(texture);
        }
        return RenderType.entityTranslucent(texture);
    }
}
