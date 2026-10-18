package com.yourname.backrooms_complex.item;

import com.yourname.backrooms_complex.ModBalance;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import com.yourname.backrooms_complex.ModDamageTypes;
import com.yourname.backrooms_complex.ModNetwork;

/**
 * 「地毯纤维」：从地板上刮下来的潮湿地毯碎屑。
 *
 * <h2>食用</h2>
 * <ul>
 *   <li>动画：原版<b>进食</b>（{@link UseAnim#EAT}）；</li>
 *   <li>时长：<b>5 秒</b>（{@value #EAT_DURATION} tick）；</li>
 *   <li>效果：<b>−6 点生命值</b>、<b>+0.5 点饥饿值</b>、
 *       获得 <b>10 秒饥饿</b> 与 <b>3 秒反胃</b>。</li>
 * </ul>
 *
 * <p>吃下去明显是亏的 —— 这是刻意设计的「饥不择食」惩罚。</p>
 */
public class CarpetFiberItem extends Item {

    /** 食用时长：5 秒 = 100 tick。 */
    public static final int EAT_DURATION = 100;
    /** 食用造成的伤害。 */
    public static final float EAT_DAMAGE = ModBalance.FIBER_DAMAGE;
    /** 食用回复的饥饿值。 */
    public static final int EAT_NUTRITION = ModBalance.FIBER_NUTRITION;   // 1 点 = 半格（0.5 格饥饿值）
    /** 饥饿效果时长（10 秒）。 */
    public static final int HUNGER_TICKS = ModBalance.FIBER_HUNGER_TICKS;
    /** 反胃效果时长（3 秒）。 */
    public static final int NAUSEA_TICKS = ModBalance.FIBER_NAUSEA_TICKS;

    public CarpetFiberItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(EAT_NUTRITION)
                .saturationModifier(0.0F)
                .build()));
    }

    /** 右键开始进食。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    /** 原版进食动画。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    /** 食用时长 5 秒。 */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return EAT_DURATION;
    }

    /** 食用完成：扣血、回饥饿、上负面效果。 */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            // -6 点生命值（不触发受击动画/击退，直接扣）
            float next = player.getHealth() - EAT_DAMAGE;
            if (next <= 0.0F) {
                player.setHealth(0.0F);
                net.minecraft.world.damagesource.DamageSource ds =
                    player.damageSources().source(ModDamageTypes.INTESTINAL_OBSTRUCTION);
                // die() 的死亡消息取自 CombatTracker 里记录的最后一个伤害来源，
                // 所以必须先把该来源记进 tracker，否则会退化成通用的"死了"。
                player.getCombatTracker().recordDamage(ds, EAT_DAMAGE);
                player.die(ds);
            } else {
                player.setHealth(next);
            }
            // +0.5 格饥饿值
            player.getFoodData().eat(EAT_NUTRITION, 0.0F);
            // 10 秒饥饿 + 3 秒反胃
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, HUNGER_TICKS, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, 0, false, true, true));
            // 结算后立刻同步 HUD
            ModNetwork.sendTo(player);
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 0.9F);
        return stack;
    }
}
