package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 方块渲染层设置。
 *
 * <p>「墙灰层」要使用 <b>cutout</b> 渲染层，否则贴图里的透明区域会被渲染成黑色方块。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class ModRenderTypes {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.WALL_DUST_LAYER.get(), RenderType.cutout()));
    }

    private ModRenderTypes() {
    }
}