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
    /** 贴墙时的向上攀爬速度（格/tick）。 */
    public static final double CLIMB_SPEED = 0.10D;

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

    /**
     * 不在头顶显示原版名牌（1.21.1 的签名是<b>无参</b>的 shouldShowName）。
     *
     * <p>细菌的名称只由「玉」等模组的信息浮层显示，
     * 头顶再挂一个原版名牌会造成"名字出现两次"的观感。</p>
     */
    @Override
    public boolean shouldShowName() {
        return false;
    }
    /**
     * 真·爬墙：贴墙时关闭重力并给一个向上的速度，贴顶时抵消重力。
     *
     * <p>不再依赖寻路 —— 只要 AI 把它推向墙壁，它就会沿着墙面往上爬；
     * 离开表面后重力恢复，会自然掉下来。</p>
     */
    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide() || !this.isAlive()) {
            return;
        }

        boolean wall = hasWallNearby();
        boolean ceiling = hasCeilingAbove();

        // 贴墙或贴顶时关闭重力
        this.setNoGravity(wall || ceiling);

        if (wall) {
            // 沿墙面向上爬（给一个恒定的向上速度）
            var v = this.getDeltaMovement();
            this.setDeltaMovement(v.x, CLIMB_SPEED, v.z);
        } else if (ceiling) {
            // 贴住天花板：抵消重力，保持水平移动
            var v = this.getDeltaMovement();
            this.setDeltaMovement(v.x, 0.0D, v.z);
        }

        spawnTrailParticles();
    }

    /** 四邻 0.6 格内是否有实心方块。 */
    private boolean hasWallNearby() {
        var box = this.getBoundingBox();
        double cx = (box.minX + box.maxX) * 0.5D;
        double cz = (box.minZ + box.maxZ) * 0.5D;
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var probe = net.minecraft.core.BlockPos.containing(
                    cx + dir.getStepX() * 0.6D, box.minY + 0.4D, cz + dir.getStepZ() * 0.6D);
            if (this.level().getBlockState(probe).isSolidRender(this.level(), probe)) {
                return true;
            }
        }
        return false;
    }

    /** 正上方 0.3 格处是否有实心方块。 */
    private boolean hasCeilingAbove() {
        var box = this.getBoundingBox();
        double cx = (box.minX + box.maxX) * 0.5D;
        double cz = (box.minZ + box.maxZ) * 0.5D;
        var probe = net.minecraft.core.BlockPos.containing(cx, box.maxY + 0.3D, cz);
        return this.level().getBlockState(probe).isSolidRender(this.level(), probe);
    }
    /** 经过的路径留下中量灰黑色滴落粒子。 */
    private void spawnTrailParticles() {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        if (this.tickCount % 4 != 0) {
            return;
        }
        if (this.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-5D
                && Math.abs(this.getDeltaMovement().y) < 1.0E-4D) {
            return;
        }
        net.minecraft.core.particles.BlockParticleOption dust =
                new net.minecraft.core.particles.BlockParticleOption(
                        net.minecraft.core.particles.ParticleTypes.FALLING_DUST,
                        net.minecraft.world.level.block.Blocks.GRAY_CONCRETE.defaultBlockState());
        serverLevel.sendParticles(dust,
                this.getX(), this.getY() + 0.15D, this.getZ(),
                2, 0.22D, 0.05D, 0.22D, 0.0D);
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
