package com.the_backrooms_complex.item;

import com.the_backrooms_complex.ModItems;
import com.the_backrooms_complex.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import com.the_backrooms_complex.ModPlayerData;

/**
 * 「罐装杏仁水」（受潮 / 保存完好）。
 *
 * <p>饮用后回复饮水值与饥饿值，并把空罐留给玩家（{@code empty_can}）。
 * 受潮的那罐额外附带 1 秒反胃 I。</p>
 */
public class CannedAlmondWaterItem extends Item {

    private final int durationTicks;
    private final int thirst;
    private final int hunger;
    private final boolean nausea;

    public CannedAlmondWaterItem(Properties properties,
                                 int durationTicks, int thirst, int hunger, boolean nausea) {
        super(properties);
        this.durationTicks = durationTicks;
        this.thirst = thirst;
        this.hunger = hunger;
        this.nausea = nausea;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return this.durationTicks;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            ModPlayerDataProxy.apply(player, this.thirst, this.hunger);
            if (this.nausea) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20, 0, false, true, true));
            }
            ModNetwork.sendTo(player);
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                return new ItemStack(ModItems.EMPTY_CAN.get());
            }
            ItemStack can = new ItemStack(ModItems.EMPTY_CAN.get());
            if (!player.getInventory().add(can)) {
                player.drop(can, false);
            }
        }
        return stack;
    }

    /** 小工具：把数值一次性加上去（内部走 ModPlayerData，保持 HUD 同步）。 */
    private static final class ModPlayerDataProxy {
        static void apply(ServerPlayer player, int thirst, int hunger) {
            ModPlayerData.addThirst(player, thirst);
            if (hunger > 0) {
                player.getFoodData().eat(hunger, 0.0F);
            }
        }
    }
}