package com.the_backrooms_complex.block.entity;

import com.the_backrooms_complex.ModBlockEntities;
import com.the_backrooms_complex.menu.CrateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 板条箱的方块实体。
 *
 * <p>27 格容器；首次打开时按物资池填 2~4 件；
 * 自然生成的箱子在「连续 10~30 秒无人靠近」后消失，玩家放置的永远保留。</p>
 */
public class CrateBlockEntity extends BlockEntity implements Container, MenuProvider {

    /** 容器格数。 */
    public static final int SIZE = 27;

    /** 被看见的判定半径（格）。 */
    public static final double WATCH_RADIUS = 24.0D;

    /** 消失阈值下限 / 上限（tick）：10 ~ 30 秒。 */
    public static final int LIFETIME_MIN = 200;
    public static final int LIFETIME_MAX = 600;

    /** 可搜刮到的物资池。 */
    private static final Item[] LOOT_POOL = {
            Items.BREAD, Items.COOKED_BEEF, Items.APPLE, Items.POTION, Items.ROTTEN_FLESH,
    };

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    /** 是否已按物资池填充过。 */
    private boolean filled;
    /** 是否自然生成（只有自然生成的会消失）。 */
    private boolean natural;
    /** 连续未被看见的 tick 数。 */
    private int unseenTicks;
    /** 消失阈值（tick）。 */
    private int lifetimeTicks = LIFETIME_MIN;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRATE.get(), pos, state);
    }

    // ==================== 数据 ====================

    /** 首次打开时填充物资。 */
    public void ensureFilled() {
        if (this.filled) {
            return;
        }
        this.filled = true;
        RandomSource random = this.level != null ? this.level.getRandom() : RandomSource.create();
        int count = 2 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            this.items.set(i, new ItemStack(LOOT_POOL[random.nextInt(LOOT_POOL.length)]));
        }
        this.lifetimeTicks = LIFETIME_MIN + random.nextInt(LIFETIME_MAX - LIFETIME_MIN + 1);
        setChanged();
    }

    /** 服务端每 tick 调用：被看见就重置计时，否则累计到阈值后消失。 */
    public void serverTick() {
        if (!this.natural || this.level == null || this.level.isClientSide()) {
            return;
        }
        Player witness = this.level.getNearestPlayer(
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D,
                WATCH_RADIUS, false);
        if (witness != null) {
            this.unseenTicks = 0;
            return;
        }
        if (++this.unseenTicks > this.lifetimeTicks) {
            this.level.removeBlock(this.worldPosition, false);
        }
    }

    public void setNatural(boolean natural) {
        this.natural = natural;
    }

    // ==================== 存档 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putBoolean("Filled", this.filled);
        tag.putBoolean("Natural", this.natural);
        tag.putInt("Unseen", this.unseenTicks);
        tag.putInt("Lifetime", this.lifetimeTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.filled = tag.getBoolean("Filled");
        this.natural = tag.getBoolean("Natural");
        this.unseenTicks = tag.getInt("Unseen");
        this.lifetimeTicks = tag.contains("Lifetime") ? tag.getInt("Lifetime") : LIFETIME_MIN;
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.backrooms_complex.crate");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        ensureFilled();
        return new CrateMenu(windowId, inventory, this);
    }

    // ==================== Container ====================

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(this.items, slot, amount);
        if (!out.isEmpty()) {
            setChanged();
        }
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5D,
                        this.worldPosition.getY() + 0.5D,
                        this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }
}
