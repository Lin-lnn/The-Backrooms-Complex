package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 「细菌」的客户端注册（模型层 + 渲染器）。
 *
 * <p>只在客户端加载 —— 服务端不会触碰任何渲染相关类。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class BacteriaClient {

    /** 模型层：自定义本体（小核心 + 摇曳菌丝）。 */
    public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER =
            new net.minecraft.client.model.geom.ModelLayerLocation(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            BackroomsComplex.MODID, "bacteria_body"), "main");

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, BacteriaModel::createBodyLayer);
    }

    /** 注册渲染器。 */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BACTERIA.get(), BacteriaRenderer::new);
    }

    /**
     * 拦截名牌渲染（第四道保险）。
     *
     * <p>NeoForge 的 {@code RenderNameTagEvent} 使用 {@code TriState} 控制是否渲染，
     * 返回 {@code TriState.FALSE} 即可彻底阻止「细菌」的名牌被绘制 —— 
     * 无论名字是原版逻辑给的还是别的模组设置的都是如此。</p>
     */
    @SubscribeEvent
    public static void onRenderNameTag(net.neoforged.neoforge.client.event.RenderNameTagEvent event) {
        if (event.getEntity() instanceof com.the_backrooms_complex.entity.BacteriaEntity) {
            event.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE);
        }
    }

    private BacteriaClient() {
    }
}