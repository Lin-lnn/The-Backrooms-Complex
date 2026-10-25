package com.the_backrooms_complex.block;

import net.minecraft.world.level.block.WallBlock;

/**
 * 我们的墙变体。
 *
 * <p>只与墙相连，<b>不与栅栏相连</b>：栅栏不是完整方块，原版 {@link WallBlock} 本来
 * 就不会朝它伸出墙侧。早先为了让墙能贴栅栏而额外补了墙侧，导致墙和栅栏互相吸附、
 * 两种结构混成一体，那段逻辑已删除。</p>
 */
public class ConnectingWallBlock extends WallBlock {

    public ConnectingWallBlock(Properties properties) {
        super(properties);
    }
}