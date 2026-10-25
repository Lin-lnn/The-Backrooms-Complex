package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * 群系着色的<b>注册入口</b>（顶层类）。
 *
 * <p>之前把 {@code @EventBusSubscriber} 写在 {@link BiomeTint} 的嵌套类里，
 * 结果方块与物品的着色都没有生效（贴图显示为原始灰度）。</p>
 *
 * <p>这里改成<b>顶层</b> {@code @EventBusSubscriber} 类，确保被 NeoForge 扫描到。
 * 逻辑本身仍由 {@link BiomeTint} 提供。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class BiomeTintEvents {

    /** 注册方块着色。 */
    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        Block[] blocks = BiomeTint.tintedBlockArray();
        if (blocks.length > 0) {
            event.register(BiomeTint::getBlockColor, blocks);
        }
    }

    /** 注册物品着色（创造物品栏里的图标）。 */
    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        for (Block b : BiomeTint.tintedBlockArray()) {
            if (b.asItem() != null) {
                event.register(BiomeTint::getItemColor, b);
            }
        }
    }

    private BiomeTintEvents() {
    }
}
