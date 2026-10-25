package com.the_backrooms_complex;

import com.the_backrooms_complex.menu.CrateMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 容器界面类型注册。 */
public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, BackroomsComplex.MODID);

    /**
     * 板条箱界面。
     *
     * <p>照原版木桶的做法：菜单只依赖 {@code (windowId, inventory)}。客户端一开始拿到的
     * 是空容器，但物品栈由容器的内容数据包同步过来，所以不需要传方块坐标。</p>
     */
    public static final DeferredHolder<MenuType<?>, MenuType<CrateMenu>> CRATE =
            MENUS.register("crate", () -> new MenuType<>(CrateMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {
    }
}
