package com.yourname.backrooms_complex;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 玩家的「饮水值」与「精神值」（各 0~100，界面显示为 10 段）。
 *
 * <h2>数值规则</h2>
 * <ul>
 *   <li><b>饮水值</b>：随时间缓慢下降；奔跑、处于潮湿走廊时下降更快。</li>
 *   <li><b>精神值</b>：在潮湿走廊、完全黑暗中下降；在黄色房间缓慢恢复。</li>
 *   <li>喝「未知污水瓶」恢复饮水值，但会扣精神值（副作用）。</li>
 * </ul>
 *
 * <p>两个值用 {@link AttachmentType} 存在玩家身上，并通过 {@code sync}
 * 自动同步到客户端，供 HUD 读取。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID)
public final class ModPlayerData {

    /** 满值。 */
    public static final int MAX = ModBalance.STAT_MAX;

    // ---- 下降 / 恢复速率（每 20 tick = 1 秒结算一次）----
    /** 饮水值基础下降（每 20 秒 1 点）。 */
    private static final int THIRST_BASE_INTERVAL = ModBalance.THIRST_BASE_INTERVAL;
    /** 奔跑时额外加快（每 8 秒 1 点）。 */
    private static final int THIRST_SPRINT_INTERVAL = ModBalance.THIRST_SPRINT_INTERVAL;
    /** 潮湿环境额外加快（每 6 秒 1 点）。 */
    private static final int THIRST_DAMP_INTERVAL = ModBalance.THIRST_DAMP_INTERVAL;
    /** 精神值在潮湿走廊下降（每 10 秒 1 点）。 */
    private static final int SANITY_DAMP_INTERVAL = ModBalance.SANITY_DAMP_INTERVAL;
    /** 精神值在完全黑暗中下降（每 6 秒 1 点）。 */
    private static final int SANITY_DARK_INTERVAL = ModBalance.SANITY_DARK_INTERVAL;
    /** 精神值在黄色房间恢复（每 5 秒 1 点）。 */
    private static final int SANITY_YELLOW_INTERVAL = ModBalance.SANITY_YELLOW_INTERVAL;

    /** 污水瓶：恢复饮水值 / 扣精神值。 */
    /** 方块变暗红的精神值阈值（跨过此值瞬间给一次黑暗）。 */
    public static final int DARK_RED_THRESHOLD = ModBalance.DARK_RED_THRESHOLD;
    /** 跨越阈值时的黑暗时长：2 秒。 */
    public static final int DARKNESS_TICKS = ModBalance.DARKNESS_TICKS;

    public static final int SEWAGE_THIRST_RESTORE = ModBalance.SEWAGE_THIRST_RESTORE;
    public static final int SEWAGE_SANITY_COST = ModBalance.SEWAGE_SANITY_COST;

    public static final AttachmentType<Integer> THIRST = AttachmentType
            .builder(() -> MAX)
            .serialize(com.mojang.serialization.Codec.INT)
            .build();

    public static final AttachmentType<Integer> SANITY = AttachmentType
            .builder(() -> MAX)
            .serialize(com.mojang.serialization.Codec.INT)
            .build();

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, helper -> {
            helper.register(ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "thirst"), THIRST);
            helper.register(ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "sanity"), SANITY);
        });
    }

    public static int getThirst(ServerPlayer player) {
        return player.getData(THIRST);
    }

    public static int getSanity(ServerPlayer player) {
        return player.getData(SANITY);
    }

    public static void addThirst(ServerPlayer player, int delta) {
        player.setData(THIRST, clamp(player.getData(THIRST) + delta));
    }

    public static void addSanity(ServerPlayer player, int delta) {
        int before = player.getData(SANITY);
        int after = clamp(before + delta);
        player.setData(SANITY, after);

        // 精神值「向下」跨过暗红阈值的那一瞬间：给 2 秒黑暗效果。
        // 只在进入红色状态时触发，恢复时不触发。
        boolean enteringRed = before >= DARK_RED_THRESHOLD && after < DARK_RED_THRESHOLD;
        if (enteringRed && !player.isCreative() && !player.isSpectator()) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DARKNESS,
                    DARKNESS_TICKS, 0, false, false, false));
        }
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

        // 饮水值 < 3：强制结束疾跑（每 tick 检查，松手也没用）
        if (getThirst(player) < 10 && player.isSprinting()) {
            player.setSprinting(false);
        }

        // 饮水值 == 0：每 3 秒（60 tick）扣 1 点血量
        // 不用 hurt() —— 它会附带受击动画、无敌帧和<b>击退</b>，把玩家推开。
        // 直接改血量可以精确控制节奏，且完全没有击退；
        // 归零时手动调用 die()，仍然使用脱水伤害类型，因此死亡提示不变。
        if (getThirst(player) <= 0 && t % 60L == 0L) {
            float next = player.getHealth() - 1.0F;
            if (next <= 0.0F) {
                player.setHealth(0.0F);
                net.minecraft.world.damagesource.DamageSource ds =
                        player.damageSources().source(ModDamageTypes.DEHYDRATION);
                // 同上：先记录到 CombatTracker，死亡提示才会用脱水的那条
                player.getCombatTracker().recordDamage(ds, 1.0F);
                player.die(ds);
            } else {
                player.setHealth(next);
            }
        }

        if (t % 20L != 0L) {
            return;   // 每秒结算一次
        }

        var level = player.serverLevel();
        var biome = level.getBiome(player.blockPosition()).value();
        String biomePath = level.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getResourceKey(biome)
                .map(k -> k.location().getPath())
                .orElse("");

        boolean damp = "damp_corridors".equals(biomePath);
        boolean yellow = "yellow_rooms".equals(biomePath);
        boolean dark = level.getMaxLocalRawBrightness(player.blockPosition()) <= 4;

        // ---- 饮水值 ----
        if (t % THIRST_BASE_INTERVAL == 0L) {
            addThirst(player, -1);
        }
        if (player.isSprinting() && t % THIRST_SPRINT_INTERVAL == 0L) {
            addThirst(player, -1);
        }
        if (damp && t % THIRST_DAMP_INTERVAL == 0L) {
            addThirst(player, -1);
        }

        // ---- 精神值 ----
        if (damp && t % SANITY_DAMP_INTERVAL == 0L) {
            addSanity(player, -1);
        }
        if (dark && t % SANITY_DARK_INTERVAL == 0L) {
            addSanity(player, -1);
        }
        if (yellow && t % SANITY_YELLOW_INTERVAL == 0L) {
            addSanity(player, 1);
        }

        // 结算完毕后把最新数值同步给客户端（HUD 依赖这个包刷新）
        ModNetwork.sendTo(player);
    }

    private ModPlayerData() {
    }
}