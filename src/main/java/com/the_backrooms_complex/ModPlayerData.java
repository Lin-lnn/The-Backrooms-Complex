package com.the_backrooms_complex;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 玩家的「饮水值」（0~100，界面显示为 10 段）。
 *
 * <p>饮水值随时间缓慢下降，奔跑时额外加快；归零后按固定间隔扣血，
 * 喝污水瓶或罐装杏仁水可以恢复。数值用 {@link AttachmentType} 存在玩家身上，
 * 由 {@link ModNetwork} 同步到客户端供 HUD 读取。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID)
public final class ModPlayerData {

    /** 满值。 */
    public static final int MAX = ModBalance.STAT_MAX;

    /** 饮水值基础下降（每 20 秒 1 点）。 */
    private static final int THIRST_BASE_INTERVAL = ModBalance.THIRST_BASE_INTERVAL;
    /** 奔跑时额外加快（每 8 秒 1 点）。 */
    private static final int THIRST_SPRINT_INTERVAL = ModBalance.THIRST_SPRINT_INTERVAL;

    /** 污水瓶恢复的饮水值。 */
    public static final int SEWAGE_THIRST_RESTORE = ModBalance.SEWAGE_THIRST_RESTORE;

    public static final AttachmentType<Integer> THIRST = AttachmentType
            .builder(() -> MAX)
            .serialize(com.mojang.serialization.Codec.INT)
            .build();

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, helper ->
                helper.register(ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "thirst"),
                        THIRST));
    }

    public static int getThirst(ServerPlayer player) {
        return player.getData(THIRST);
    }

    public static void addThirst(ServerPlayer player, int delta) {
        player.setData(THIRST, clamp(player.getData(THIRST) + delta));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(MAX, v));
    }

    /** 每 tick 结算（只有服务端会改变数值，客户端拿同步结果）。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // 只有生存模式和冒险模式才消耗；创造/旁观模式数值不变
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        long t = player.level().getGameTime();

        // 饮水值 < 10：强制结束疾跑（每 tick 检查，松手也没用）
        if (getThirst(player) < 10 && player.isSprinting()) {
            player.setSprinting(false);
        }

        // 饮水值 == 0：每 3 秒（60 tick）扣 1 点血量。
        // 不用 hurt() —— 它会附带受击动画、无敌帧和击退，把玩家推开。
        // 直接改血量可以精确控制节奏；归零时手动调用 die()，仍使用脱水伤害类型。
        if (getThirst(player) <= 0 && t % ModBalance.DEHYDRATION_INTERVAL_TICKS == 0L) {
            float next = player.getHealth() - ModBalance.DEHYDRATION_DAMAGE;
            if (next <= 0.0F) {
                player.setHealth(0.0F);
                net.minecraft.world.damagesource.DamageSource ds =
                        player.damageSources().source(ModDamageTypes.DEHYDRATION);
                player.getCombatTracker().recordDamage(ds, ModBalance.DEHYDRATION_DAMAGE);
                player.die(ds);
            } else {
                player.setHealth(next);
            }
        }

        if (t % 20L != 0L) {
            return;   // 每秒结算一次
        }

        if (t % THIRST_BASE_INTERVAL == 0L) {
            addThirst(player, -1);
        }
        if (player.isSprinting() && t % THIRST_SPRINT_INTERVAL == 0L) {
            addThirst(player, -1);
        }

        // 结算完毕后把最新数值同步给客户端（HUD 依赖这个包刷新）
        ModNetwork.sendTo(player);
    }

    private ModPlayerData() {
    }
}
