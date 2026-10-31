package com.the_backrooms_complex;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 实体注册。
 *
 * <p>目前只有「座位」（不可见的载具实体）。</p>
 */
public final class ModEntities {

    /** 实体类型注册器。 */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, BackroomsComplex.MODID);

    /**
     * 「座位」：不可见、无碰撞的载具实体，玩家坐上去即可实现真正的坐下效果。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<com.the_backrooms_complex.entity.SeatEntity>> SEAT =
            ENTITY_TYPES.register("seat", () -> EntityType.Builder
                    .of(com.the_backrooms_complex.entity.SeatEntity::new, MobCategory.MISC)
                    .sized(0.001F, 0.001F)
                    .noSummon()
                    .clientTrackingRange(4)
                    .build("backrooms_complex:seat"));

    private ModEntities() {
    }
}
