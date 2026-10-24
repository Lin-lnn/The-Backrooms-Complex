package com.the_backrooms_complex.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.the_backrooms_complex.BackroomsComplex;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.extensions.IDimensionSpecialEffectsExtension;
import org.joml.Matrix4f;

/**
 * 后室维度的客户端视觉特化。
 *
 * <h2>作用</h2>
 * <ul>
 *   <li><b>不渲染云</b>：重写 {@link IDimensionSpecialEffectsExtension#renderClouds} 返回 {@code false}。
 *       1.21.1 的 {@code DimensionType} 已没有 cloud 字段，云只受客户端视频设置的
 *       {@code CloudStatus} 控制，因此必须通过这个 NeoForge 扩展钩子按维度关闭。</li>
 *   <li><b>无天空 / 无地平线地面</b>：{@code SkyType.NONE} + {@code hasGround=false}。</li>
 *   <li><b>雾颜色</b>与亮度无关，直接沿用群系给的雾色（混凝土大厅为暗灰）。</li>
 * </ul>
 *
 * <p>维度 JSON 中通过 {@code "effects": "backrooms_complex:backrooms"} 引用本类。</p>
 */
public class BackroomsDimensionEffects extends DimensionSpecialEffects
        implements IDimensionSpecialEffectsExtension {

    /** 注册名，与 dimension_type/level_0.json 的 effects 字段一致。 */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "backrooms");

    public BackroomsDimensionEffects() {
        // cloudLevel=NaN（没有云高度）、hasGround=false、SkyType.NONE、不打亮光照图、恒定环境光
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    /** 关闭本维度的云渲染。 */
    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
                                double camX, double camY, double camZ,
                                Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return false;
    }

    /** 客户端事件：把本特化注册进维度效果表。 */
    @EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
    static final class Events {

        @SubscribeEvent
        public static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
            event.register(ID, new BackroomsDimensionEffects());
        }

        private Events() {
        }
    }
}