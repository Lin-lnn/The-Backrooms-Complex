package com.the_backrooms_complex;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * 自定义伤害类型。
 *
 * <p>{@link #DEHYDRATION} 对应数据包里的
 * {@code data/backrooms_complex/damage_type/dehydration.json}，
 * 其 {@code message_id} 为 {@code dehydration}，因此死亡提示取自语言键
 * {@code death.attack.dehydration}。</p>
 */
public final class ModDamageTypes {

    /** 脱水伤害。 */
    public static final ResourceKey<DamageType> DEHYDRATION =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "dehydration"));

    /** 肠梗阻伤害（食用地毯纤维致死）。 */
    public static final ResourceKey<DamageType> INTESTINAL_OBSTRUCTION =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "intestinal_obstruction"));

    private ModDamageTypes() {
    }
}