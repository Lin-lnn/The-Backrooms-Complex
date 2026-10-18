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
