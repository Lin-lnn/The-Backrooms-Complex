package com.the_backrooms_complex;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 事件监听类。
 *
 * <p><b>背景噪音：</b>玩家位于后室维度时，周期性循环播放低音量的荧光灯嗡鸣
 * （原版 {@link SoundEvents#BEACON_AMBIENT}，音量 {@value #AMBIENT_VOLUME}，
 * 每 {@value #AMBIENT_INTERVAL_TICKS} tick 一次，近似连续循环）。
 * 由 {@link ModConfig#enableAmbientHum} 控制。</p>
 *
 * <p><b>方块破坏规则：</b>本模组<strong>不监听</strong> {@link BlockEvent.BreakEvent}，也不做任何
 * 额外拦截。完全依靠注册时的 {@code strength(-1.0F, 3600000.0F)}（见 {@link ModBlocks}）：
 * 生存模式无法挖掘，创造模式可正常破坏。</p>
 *
 * <p><b>首次进入提示：</b>玩家第一次进入后室维度时发送长文本，之后再次进入只发短文本。
 * "首次"标记保存在玩家的 {@code getPersistentData()} 里，并在死亡重生时迁移，
 * 因此退出重进、死亡重生都不会重复触发长文本。</p>
 */
public final class ModEvents {

    /** 玩家 NBT 中的"已首次进入后室"标记键。 */
    private static final String NBT_FIRST_ENTER = "backrooms_complex_first_enter";

    /** 背景嗡鸣的播放间隔（tick）。原版 BEACON_AMBIENT 约 2.7 秒，60 tick = 3 秒可近似无缝循环。 */
    private static final int AMBIENT_INTERVAL_TICKS = 60;

    /** 背景嗡鸣音量（较低，避免盖过其它声音）。 */
    private static final float AMBIENT_VOLUME = 0.25F;

    /** 饮用类物品回复的饮水值。 */
    public static final int DRINK_THIRST_RESTORE = ModBalance.DRINK_THIRST_RESTORE;

    /**
     * 玩家进入后室维度时发送提示：首次进入发长文本，之后每次进入发短文本。
     */
    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!ModDimensions.LEVEL_0_WORLD.equals(event.getTo())) {
            return;
        }
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(NBT_FIRST_ENTER)) {
            data.putBoolean(NBT_FIRST_ENTER, true);
            player.sendSystemMessage(Component.translatable("message.backrooms_complex.enter.first"));
        } else {
            player.sendSystemMessage(Component.translatable("message.backrooms_complex.enter.again"));
        }
    }

    /**
     * 玩家死亡重生 / 从末地返回时，把"首次进入"标记从旧实体迁移到新实体。
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().getBoolean(NBT_FIRST_ENTER)) {
            event.getEntity().getPersistentData().putBoolean(NBT_FIRST_ENTER, true);
        }
    }

    /**
     * 后室维度的背景噪音：周期性向维度内的每名玩家播放低音量荧光灯嗡鸣。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ModConfig.enableAmbientHum) {
            return;
        }
        if (event.getServer().getTickCount() % AMBIENT_INTERVAL_TICKS != 0) {
            return;
        }
        ServerLevel level = event.getServer().getLevel(ModDimensions.LEVEL_0_WORLD);
        if (level == null) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.AMBIENT, AMBIENT_VOLUME, 1.0F);
        }
    }

    /**
     * 服务器启动：把世界种子交给生成器。
     *
     * <p>后室的布局由哈希决定，混入世界种子后同一个种子生成同一张地图，
     * 不同种子则是完全不同的布局。</p>
     */
    @SubscribeEvent
    public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        long seed = event.getServer().overworld().getSeed();
        com.the_backrooms_complex.worldgen.BackroomsSeed.set(seed);
    }

    private ModEvents() {
    }
}
