package com.yourname.backrooms_complex.item;

import com.yourname.backrooms_complex.ModPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import com.yourname.backrooms_complex.ModNetwork;

/**
 * 「未知污水瓶」：喝下后恢复饮水值，但会付出精神值代价。
 *
 * <h2>饮用动画（完全复用原版水瓶）</h2>
 * <ul>
 *   <li>{@link UseAnim#DRINK} —— 举起瓶子仰头喝的动作；</li>
 *   <li>使用时长 32 tick（1.6 秒），与原版药水一致；</li>
 *   <li>饮用中的「咕嘟」音效由 {@code LivingEntity#triggerItemUseEffects}
 *       在 {@code UseAnim.DRINK} 下自动播放，无需手写；</li>
 *   <li>未喝完就松手 => 不结算、不消耗（原版行为）。</li>
 * </ul>
 */
public class SewageBottleItem extends Item {

    /** 饮用所需 tick 数（原版药水 = 32）。 */
    public static final int DRINK_DURATION = 32;

    public SewageBottleItem(Properties properties) {
        super(properties);
    }

    /** 右键开始饮用：只启动使用动作，不立刻结算。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    /** 原版水瓶同款：仰头饮用动画。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    /** 饮用时长，与原版药水一致。 */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_DURATION;
    }

    /** 饮用完成：结算数值，并把空玻璃瓶还给玩家。 */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            ModPlayerData.addThirst(serverPlayer, ModPlayerData.SEWAGE_THIRST_RESTORE);
            ModPlayerData.addSanity(serverPlayer, -ModPlayerData.SEWAGE_SANITY_COST);
            // 立刻同步给客户端，否则要等下一次每秒结算（最多 1 秒延迟）
            ModNetwork.sendTo(serverPlayer);
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            ItemStack empty = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        return stack;
    }
}
