package com.the_backrooms_complex.entity;

import com.the_backrooms_complex.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 「细菌」—— Level 0 潮湿房间中的敌对生物。
 *
 * <h2>属性</h2>
 * <table border="1">
 *   <caption>数值</caption>
 *   <tr><th>项</th><th>值</th></tr>
 *   <tr><td>生命值</td><td>{@value #MAX_HEALTH}</td></tr>
 *   <tr><td>攻击力</td><td>{@value #ATTACK_DAMAGE}</td></tr>
 *   <tr><td>移动速度</td><td>{@value #MOVEMENT_SPEED}（玩家 0.1 的 0.8 倍）</td></tr>
 *   <tr><td>跟随范围</td><td>{@value #FOLLOW_RANGE}</td></tr>
 * </table>
 *
 * <h2>接触伤害</h2>
 * <p>除了主动挥击（{@value #ATTACK_DAMAGE} 点），碰到玩家的每
 * {@value #CONTACT_INTERVAL_TICKS} tick 额外造成 {@value #CONTACT_DAMAGE} 点伤害
 * —— 按 20 tick/秒折算约为每秒 6 点。</p>
 *
 * <h2>黏液减速</h2>
 * <p>被它攻击或接触时，玩家获得 {@value #SLOW_TICKS} tick（1 秒）的
 * 移动速度降低（幅度约 30%）。</p>
 */
public class BacteriaEntity extends Monster {

    /** 生命值。 */
    public static final double MAX_HEALTH = 25.0D;
    /** 主动攻击伤害。 */
    public static final double ATTACK_DAMAGE = 4.0D;
    /** 移动速度（玩家基础 0.1，这里取 0.8 倍）。 */
    public static final double MOVEMENT_SPEED = 0.08D;
    /** 跟随目标的范围（格）。 */
    public static final double FOLLOW_RANGE = 24.0D;
    /** 接触伤害的间隔 tick（每 7 tick 一次 ≈ 每秒 6 点）。 */
    public static final int CONTACT_INTERVAL_TICKS = 7;
    /** 每次接触造成的伤害。 */
    public static final float CONTACT_DAMAGE = 2.0F;
    /** 减速效果时长（1 秒）。 */
    public static final int SLOW_TICKS = 20;

    public BacteriaEntity(EntityType<? extends BacteriaEntity> type, Level level) {
        super(type, level);
    }

    /** 属性注册（由 {@code ModEvents} 里的 AttributeEvent 调用）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 经过的路径留下中量灰黑色滴落粒子。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || !this.isAlive()) {
            return;
        }
        // 每 4 tick 在地面留下 1 粒（中量）
        if (this.tickCount % 4 == 0 && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5D) {
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.DRIPPING_DRIPSTONE_WATER,
                        this.getX(), this.getY() + 0.15, this.getZ(),
                        1, 0.22, 0.05, 0.22, 0.0);
            }
        }
    }

    /** 接触玩家：持续伤害 + 减速。 */
    @Override
    public void playerTouch(Player player) {
        super.playerTouch(player);
        if (this.level().isClientSide()) {
            return;
        }
        // 每 CONTACT_INTERVAL_TICKS 才结算一次，避免每 tick 都掉血
        if (this.tickCount % CONTACT_INTERVAL_TICKS != 0) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        player.hurt(this.damageSources().mobAttack(this), CONTACT_DAMAGE);
        applySlowness(player);
    }

    /** 近战命中时附加减速。 */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player player) {
            applySlowness(player);
        }
        return hit;
    }

    private void applySlowness(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 1, false, true, true));
    }

    /** 掉落：细菌组织 1~2 个 + 地毯纤维 0~1 个。 */
    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.BACTERIA_TISSUE.get(), 1 + this.random.nextInt(2)));
        if (this.random.nextFloat() < 0.5F) {
            this.spawnAtLocation(new ItemStack(ModItems.CARPET_FIBER.get()));
        }
    }
}
