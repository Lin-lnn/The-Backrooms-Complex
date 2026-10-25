package com.the_backrooms_complex.menu;

import com.the_backrooms_complex.ModMenus;
import com.the_backrooms_complex.block.entity.CrateBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 板条箱的容器界面：27 格箱子 + 玩家背包。 */
public class CrateMenu extends AbstractContainerMenu {

    private static final int CRATE_SLOTS = CrateBlockEntity.SIZE;
    private static final int ROWS = 3;

    private final Container container;

    /** 客户端构造：先用空容器占位，物品由容器的内容数据包同步过来。 */
    public CrateMenu(int windowId, Inventory inventory) {
        this(windowId, inventory, new SimpleContainer(CRATE_SLOTS));
    }

    /** 服务端构造：直接用方块实体本身。 */
    public CrateMenu(int windowId, Inventory inventory, Container container) {
        super(ModMenus.CRATE.get(), windowId);
        this.container = container;
        container.startOpen(inventory.player);

        // 箱子 27 格（3 行 9 列）
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        // 玩家背包 3 行
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < CRATE_SLOTS) {
            if (!moveItemStackTo(stack, CRATE_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, CRATE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
