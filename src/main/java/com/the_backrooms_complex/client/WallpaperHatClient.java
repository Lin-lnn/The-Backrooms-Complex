package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * 墙纸帽的 <b>3D 礼帽模型</b>（客户端）。
 *
 * <h2>为什么必须写代码</h2>
 * <p>原版盔甲一直沿用 {@code HumanoidModel} 的固定几何（8×8×8 的人头盒），
 * 2D 护甲层贴图无论画成什么样都只能贴在那个盒子上，<b>不可能</b>出现帽檐与帽筒。
 * 想做出真正的立体礼帽，必须给物品提供自定义 {@link HumanoidModel}，
 * 通过 {@link IClientItemExtensions#getHumanoidArmorModel} 返回。</p>
 *
 * <h2>挂载方式</h2>
 * <p>1.21.1 里 {@code Item#initializeClient} 已标记废弃，官方推荐改用
 * {@link RegisterClientExtensionsEvent}；NeoForge 的 {@code ClientExtensionsManager}
 * 正是遍历所有物品调用 {@code initializeClient} 的。所以这里用一个
 * {@code Dist.CLIENT} 的 {@code EventBusSubscriber} 注册扩展。</p>
 *
 * <h2>几何</h2>
 * <p>坐标是实体模型空间（+Y 向下），头部立方体为 x∈[-4,4]、y∈[-8,0]、z∈[-4,4]，
 * 所以帽子放在 y ∈ [-15.5, -7.5] 之间，正好"戴"在头顶上。</p>
 * <pre>
 * 帽檐：12×1×7 与 7×1×12 两块十字交叉（近似圆形帽檐），y = -8.5 ~ -7.5
 * 帽筒：8×7×8，y = -15.5 ~ -8.5
 * </pre>
 */
public final class WallpaperHatClient {

    /** 只在客户端构造一次，之后复用同一个 ModelPart。 */
    private static ModelPart bakedRoot;

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        CubeListBuilder hat = CubeListBuilder.create();
        // 帽檐：两块交叉的薄板，形成近似圆形
        hat.texOffs(0, 16).addBox(-7.0F, -6.75F, -6.0F, 14, 1.5F, 12);   // 左右各 -2px，前后各 -1px，厚度 +0.5px
        hat.texOffs(0, 32).addBox(-6.0F, -6.755F, -7.0F, 12, 1.5F, 14);   // 同上，沿 Z 的帽檐
        // 帽筒
        hat.texOffs(0, 0).addBox(-5.0F, -11.5F, -5.0F, 10, 5, 10);

        root.addOrReplaceChild("head", hat, PartPose.ZERO);

        // HumanoidModel 构造时会 getChild 这些部件，必须都存在（留空即可）
        CubeListBuilder empty = CubeListBuilder.create();
        root.addOrReplaceChild("hat", empty, PartPose.ZERO);
        root.addOrReplaceChild("body", empty, PartPose.ZERO);
        root.addOrReplaceChild("right_arm", empty, PartPose.ZERO);
        root.addOrReplaceChild("left_arm", empty, PartPose.ZERO);
        root.addOrReplaceChild("right_leg", empty, PartPose.ZERO);
        root.addOrReplaceChild("left_leg", empty, PartPose.ZERO);

        // 64x64，与 textures/models/armor/wallpaper_hat_model.png 一致
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** 取得（必要时烘焙）3D 礼帽模型。 */
    public static HumanoidModel<LivingEntity> get() {
        if (bakedRoot == null) {
            bakedRoot = createLayer().bakeRoot();
        }
        return new HatModel(bakedRoot);
    }

    /** 换成自定义几何的类人模型；纹理仍由护甲材质提供。 */
    public static final class HatModel extends HumanoidModel<LivingEntity> {
        public HatModel(ModelPart root) {
            super(root);
        }
    }

    private WallpaperHatClient() {
    }

    /** 客户端事件：把 3D 模型挂到「墙纸帽」上。 */
    @EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
    static final class Events {

        private static final IClientItemExtensions HAT_EXTENSIONS = new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                         EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                // 只在头盔槽生效，其它槽位保持原版模型
                if (equipmentSlot != EquipmentSlot.HEAD) {
                    return original;
                }
                return WallpaperHatClient.get();
            }
        };

        @SubscribeEvent
        public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
            event.registerItem(HAT_EXTENSIONS, ModItems.WALLPAPER_HAT);
        }

        private Events() {
        }
    }
}