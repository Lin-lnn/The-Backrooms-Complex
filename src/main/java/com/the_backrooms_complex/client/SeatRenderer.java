package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModEntities;
import com.the_backrooms_complex.entity.SeatEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 「座位」实体的渲染器。
 *
 * <p>座位是<b>不可见</b>的载具，但 Minecraft 仍然要求每种实体都注册一个渲染器 ——
 * 否则 {@code EntityRenderDispatcher.shouldRender} 会对 {@code null} 解引用并<b>崩溃</b>。
 * 所以这里注册一个"永远不渲染"的空渲染器：{@link #shouldRender} 恒为 {@code false}。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class SeatRenderer extends EntityRenderer<SeatEntity> {

    /** 虚拟贴图（不会真的用到）。 */
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "textures/misc/empty.png");

    public SeatRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public boolean shouldRender(SeatEntity entity, Frustum frustum, double x, double y, double z) {
        return false;   // 永不渲染
    }

    @Override
    public ResourceLocation getTextureLocation(SeatEntity entity) {
        return TEX;
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEAT.get(), SeatRenderer::new);
    }
}
