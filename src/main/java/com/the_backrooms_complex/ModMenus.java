package com.the_backrooms_complex;

import com.the_backrooms_complex.menu.CrateMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 容器界面类型注册。 */
public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, BackroomsComplex.MODID);

    /**
     * 板条箱界面。
     *
     * <p>客户端工厂只收到 windowId 与玩家背包，所以方块坐标必须通过
     * {@code FriendlyByteBuf} 传过去 —— 这就是必须用 {@link IMenuTypeExtension} 的原因。</p>
     */
    public static final DeferredHolder<MenuType<?>, MenuType<CrateMenu>> CRATE =
            MENUS.register("crate", () -> IMenuTypeExtension.create(
                    (windowId, inv, data) -> new CrateMenu(windowId, inv, data.readBlockPos())));

    private ModMenus() {
    }
}
