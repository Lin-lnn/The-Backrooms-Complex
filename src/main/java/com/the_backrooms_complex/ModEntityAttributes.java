package com.the_backrooms_complex;

import com.the_backrooms_complex.entity.BacteriaEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/**
 * 实体属性的注册。
 *
 * <p>{@link EntityAttributeCreationEvent} 属于<b>模组总线</b>（{@code IModBusEvent}）事件，
 * 必须通过 {@link EventBusSubscriber} 注册到模组总线，
 * <b>不能</b>放进注册在通用总线上的 {@code ModEvents}（否则启动即崩溃）。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID)
public final class ModEntityAttributes {

    /** 为「细菌」注册生命值 / 攻击力 / 移动速度 / 跟随范围。 */
    @SubscribeEvent
    public static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BACTERIA.get(), BacteriaEntity.createAttributes().build());
    }

    private ModEntityAttributes() {
    }
}