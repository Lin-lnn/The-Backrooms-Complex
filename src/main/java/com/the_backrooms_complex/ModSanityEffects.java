package com.the_backrooms_complex;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 低精神值的身体影响（服务端）。
 *
 * <h2>分档效果</h2>
 * <table border="1">
 *   <caption>精神值分档</caption>
 *   <tr><th>精神值</th><th>效果</th></tr>
 *   <tr><td>&lt; 60</td><td>移动速度 −5%</td></tr>
 *   <tr><td>&lt; 40</td><td>移动速度 −10%（覆盖上一档）</td></tr>
 *   <tr><td>&lt; 20</td><td>额外：造成伤害 −20%，受到伤害 +20%</td></tr>
 *   <tr><td>&lt; 2</td><td>额外：每 5 秒扣 4 点血量</td></tr>
 * </table>
 *
 * <p>模糊与抖动由客户端类负责（{@code DehydrationBlur} / {@code SanityShake}），
 * 本类只处理服务端能决定的部分。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID)
public final class ModSanityEffects {

    // ==================== 阈值 ====================

    /** 一档：移动速度 −5%。 */
    public static final int TIER_ONE = 60;
    /** 二档：移动速度 −10%。 */
    public static final int TIER_TWO = 40;
    /** 三档：伤害修正 ±20%。 */
    public static final int TIER_THREE = 20;
    /** 四档：持续掉血。 */
    public static final int TIER_FOUR = 2;

    // ==================== 数值 ====================

    /** 一档的移动速度倍率（−5%）。 */
    public static final double SPEED_MOD_ONE = -0.05D;
    /** 二档的移动速度倍率（−10%）。 */
    public static final double SPEED_MOD_TWO = -0.10D;
    /** 三档：造成的伤害倍率（−20%）。 */
    public static final double ATTACK_MULTIPLIER = 0.80D;
    /** 三档：受到的伤害倍率（+20%）。 */
    public static final double DAMAGE_TAKEN_MULTIPLIER = 1.20D;
    /** 四档：每次扣除的血量。 */
    public static final float DAMAGE_PER_TICK_CYCLE = 4.0F;
    /** 四档：掉血间隔（5 秒 = 100 tick）。 */
    public static final int DAMAGE_INTERVAL_TICKS = 100;

    /** 移动速度修饰器的唯一标识（用于安全地增删）。 */
    private static final ResourceLocation SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "sanity_slow");

    /**
     * 每 tick 刷新移动速度修饰器。
     *
     * <p>用"先移除再加"的方式保证任意时刻只有一个 modifier，
     * 避免数值被叠加成 −15% 之类的错误结果。</p>
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        int sanity = ModPlayerData.getSanity(player);

        // 先移除旧修饰器
        if (speed.getModifier(SPEED_ID) != null) {
            speed.removeModifier(SPEED_ID);
        }

        double amount = 0.0D;
        if (sanity < TIER_TWO) {
            amount = SPEED_MOD_TWO;
        } else if (sanity < TIER_ONE) {
            amount = SPEED_MOD_ONE;
        }
        if (amount != 0.0D) {
            speed.addTransientModifier(new AttributeModifier(
                    SPEED_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }

        // 四档：每 5 秒扣血（无击退，与脱水伤害同风格）
        long gameTime = player.level().getGameTime();
        if (sanity < TIER_FOUR && gameTime % DAMAGE_INTERVAL_TICKS == 0L) {
            float next = player.getHealth() - DAMAGE_PER_TICK_CYCLE;
            if (next <= 0.0F) {
                player.setHealth(0.0F);
                var source = player.damageSources().source(ModDamageTypes.DEHYDRATION);
                player.getCombatTracker().recordDamage(source, DAMAGE_PER_TICK_CYCLE);
                player.die(source);
            } else {
                player.setHealth(next);
            }
        }
    }

    /** 三档：伤害修正（造成 −20% / 受到 +20%）。 */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (victim.isCreative() || victim.isSpectator()) {
            return;
        }
        int sanity = ModPlayerData.getSanity(victim);

        // 受到伤害 +20%
        if (sanity < TIER_THREE) {
            event.setNewDamage(event.getNewDamage() * (float) DAMAGE_TAKEN_MULTIPLIER);
        }

        // 造成伤害 −20%（攻击者是本玩家时）
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && ModPlayerData.getSanity(attacker) < TIER_THREE) {
            event.setNewDamage(event.getNewDamage() * (float) ATTACK_MULTIPLIER);
        }
    }

    /** 玩家死亡/登出时清理修饰器，避免残留。 */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        AttributeInstance speed = event.getOriginal().getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(SPEED_ID) != null) {
            speed.removeModifier(SPEED_ID);
        }
    }

    private ModSanityEffects() {
    }
}