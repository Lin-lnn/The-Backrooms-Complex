package com.the_backrooms_complex.block;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * 「金属门」。
 *
 * <p>外观是染成<b>深灰黑色</b>的白桦木门，但它是真正的两格方块门：
 * 放下下半自动生成上半，破坏任一半整体消失，右键即可开关（与原版木门一致）。</p>
 *
 * <p>用于 Level 1「小径」入口 —— 文中写道："你会发现一些单推或双推门，
 * 有时门上还挂着出口标志"。</p>
 */
public class MetalDoorBlock extends DoorBlock {

    public MetalDoorBlock(Properties properties) {
        super(BlockSetType.BIRCH, properties);
    }
}