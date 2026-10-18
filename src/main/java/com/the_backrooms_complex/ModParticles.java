package com.the_backrooms_complex;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 粒子注册。
 *
 * <p>目前只有 {@link #SCRATCH}：细菌移动时留在地面的黑色半透明划痕。</p>
 */
public final class ModParticles {

    /** 粒子类型注册器。 */
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, BackroomsComplex.MODID);

    /** 「划痕」：贴地渲染的半透明黑色痕迹，存在 3 秒。 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SCRATCH =
            PARTICLE_TYPES.register("scratch", () -> new SimpleParticleType(false) {});

    private ModParticles() {
    }
}