package com.the_backrooms_complex;

import com.the_backrooms_complex.entity.BacteriaEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 实体注册。
 *
 * <p>目前只有 {@link #BACTERIA}（细菌）。</p>
 */
public final class ModEntities {

    /** 实体类型注册器。 */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, BackroomsComplex.MODID);

    /**
     * 「细菌」：Level 0 潮湿房间中的敌对生物。
     *
     * <ul>
     *   <li>碰撞箱：宽 1.0、高 0.8 格；</li>
     *   <li>分类：{@link MobCategory#MONSTER}；</li>
     *   <li>跟踪距离：10 格（贴脸才看得清，符合"黏液"设定）；</li>
     *   <li>是否可被推动：否（贴着墙不动摇）。</li>
     * </ul>
     */
    public static final DeferredHolder<EntityType<?>, EntityType<BacteriaEntity>> BACTERIA =
            ENTITY_TYPES.register("bacteria", () -> EntityType.Builder
                    .of(BacteriaEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 0.8F)
                    .clientTrackingRange(10)
                    .build("backrooms_complex:bacteria"));

    private ModEntities() {
    }
}
