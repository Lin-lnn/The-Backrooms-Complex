package com.the_backrooms_complex.block;

import net.minecraft.world.level.block.Block;

/**
 * 「潮湿的地板」。
 *
 * <p>除了外观，唯一的行为是<b>让站在上面的玩家移动变慢</b>：</p>
 * <p>{@link #getSpeedFactor()} 返回 {@code 0.9F}，即移动速度为正常值的
 * 90%（比正常慢 10%）。Minecraft 的实体每 tick 会把所在方块的
 * {@code getSpeedFactor()} 乘到移动速度上，因此站在潮湿地板上会有
 * 「陷进湿地毯」的迟缓感。</p>
 *
 * <p>注意：{@code 0.9} 是所有方块里比较轻微的一档（灵魂沙是 0.4、
 * 蜂蜜块是 0.4），叠加上潮湿走廊的昏暗光照，会明显但不至于烦躁。</p>
 */
public class DampFloorBlock extends Block {

    /** 移动速度系数：1.0 = 正常，0.9 = 慢 10%。 */
    public static final float SPEED_FACTOR = 0.9F;

    public DampFloorBlock(Properties properties) {
        super(properties);
    }

    /** 站在此方块上的实体移动速度系数。 */
    @Override
    public float getSpeedFactor() {
        return SPEED_FACTOR;
    }
}
