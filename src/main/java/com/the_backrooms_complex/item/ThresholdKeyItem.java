package com.the_backrooms_complex.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 阈界的层级密钥（{@code backrooms_complex:threshold_key}）。
 *
 * <p>普通 {@link Item} 子类，仅额外提供一个 tooltip。手持该物品右键「枢纽之门」时，
 * 会被传送到后室 Level 0 的一个有效走廊空位（逻辑见
 * {@link com.the_backrooms_complex.block.HubDoorBlock}）。</p>
 */
public class ThresholdKeyItem extends Item {

    public ThresholdKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.backrooms_complex.threshold_key.tooltip"));
    }
}
