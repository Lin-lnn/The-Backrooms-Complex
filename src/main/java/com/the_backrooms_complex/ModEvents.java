package com.the_backrooms_complex;

import com.the_backrooms_complex.worldgen.level0.TopologyRuntime;
import com.the_backrooms_complex.worldgen.level1.Level1Flicker;
import com.the_backrooms_complex.entity.BacteriaEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.the_backrooms_complex.ModConfig;

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
 * "首次"标记保存在玩家的 {@code getPersistentData()}（NeoForge 会把它写入
 * {@code playerdata/<uuid>.dat} 的 {@code NeoForgeData} 标签）里，因此<b>退出重进仍然保留</b>；
 * 又因为原版 {@code ServerPlayer#restoreFrom} 在死亡重生时<b>不会</b>复制该数据，
 * 这里额外监听 {@link PlayerEvent.Clone} 把标记迁移到重生后的新玩家实体，
 * 从而保证"首次"只触发一次。</p>
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
     * 玩家进入后室维度时发送提示：
     * 首次进入发长文本，之后每次进入发短文本。
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
            // 首次进入：写入标记（随玩家存档持久化）并发长文本
            data.putBoolean(NBT_FIRST_ENTER, true);
            player.sendSystemMessage(Component.translatable("message.backrooms_complex.enter.first"));
        } else {
            player.sendSystemMessage(Component.translatable("message.backrooms_complex.enter.again"));
        }
    }

    /**
     * 玩家死亡重生 / 从末地返回时，把"首次进入"标记从旧实体迁移到新实体。
     *
     * <p>原版 {@code ServerPlayer#restoreFrom} 不复制 {@code persistentData}，
     * 若不迁移，死亡一次后"首次"会被重复触发。</p>
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().getBoolean(NBT_FIRST_ENTER)) {
            event.getEntity().getPersistentData().putBoolean(NBT_FIRST_ENTER, true);
        }
    }

    /**
     * 后室维度的背景噪音：周期性向维度内的每名玩家播放低音量荧光灯嗡鸣，
     * 形成近似连续循环的环境底噪。
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
            // 在玩家位置播放，音源类型 AMBIENT（环境音通道），音量较低
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.AMBIENT, AMBIENT_VOLUME, 1.0F);
        }
    }

    /**
     * Level 1「闪烁」现象的 tick 钩子。
     *
     * <p>每 tick 检查所有在 Level 1 的玩家，由 {@link com.the_backrooms_complex.worldgen.level1.Level1Flicker}
     * 负责计时、收集灯光、闪烁与恢复。</p>
     */
    @SubscribeEvent
    public static void onLevel1FlickerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        net.minecraft.server.MinecraftServer server = event.getServer();
        net.minecraft.server.level.ServerLevel level =
                server.getLevel(ModDimensions.LEVEL_1_WORLD);
        if (level == null) {
            return;
        }
        // 闪烁现在按维度统一处理一次：内部只看「有玩家所在的层」
        com.the_backrooms_complex.worldgen.level1.Level1Flicker.tick(level);
    }

    /** 菌毯刷怪计时（每 100 tick = 5 秒尝试一次）。 */
    private static final int CARPET_SPAWN_INTERVAL = 100;
    /** 每次尝试检查多少个区块。 */
    private static final int CARPET_SPAWN_SAMPLES = 6;
    /** 16 格内允许的最大细菌数。 */
    public static final int MAX_NEARBY_BACTERIA = 4;
    /** 玩家安全距离（格）。 */
    public static final double PLAYER_SAFE_DISTANCE = 8.0D;

    /**
     * 定期在「菌毯」上生成细菌。
     *
     * <p>规则（来自设计）：</p>
     * <ul>
     *   <li>生成点必须是菌毯方块；</li>
     *   <li>16 格内细菌数 &lt; 4；</li>
     *   <li>8 格内没有玩家；</li>
     *   <li>新细菌会记住这块菌毯，离开 16 格即自动消失。</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onCarpetSpawnTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % CARPET_SPAWN_INTERVAL != 0) {
            return;
        }
        for (var level : server.getAllLevels()) {
            trySpawnOnCarpet(level);
        }
    }

    /** 驱动「视线外打通」：按玩家的 dread（= 1 − 精神值/100）概率触发。 */
    @SubscribeEvent
    public static void onTopologyTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        for (var level : event.getServer().getAllLevels()) {
            for (var player : level.players()) {
                if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) {
                    continue;
                }
                float dread = 1.0F - com.the_backrooms_complex.ModPlayerData.getSanity(sp) / 100.0F;
                com.the_backrooms_complex.worldgen.level0.TopologyRuntime.tick(level, sp, dread);
                com.the_backrooms_complex.worldgen.level0.TopologyRuntime.tickLampRestore(level);
            }
        }
    }
    private static void trySpawnOnCarpet(net.minecraft.server.level.ServerLevel level) {
        // 围绕每个玩家，在 8~40 格外随机采样若干点，找到菌毯后尝试生成
        for (net.minecraft.world.entity.player.Player player : level.players()) {
            for (int i = 0; i < CARPET_SPAWN_SAMPLES; i++) {
                int dx = (level.random.nextBoolean() ? 1 : -1) * (8 + level.random.nextInt(32));
                int dz = (level.random.nextBoolean() ? 1 : -1) * (8 + level.random.nextInt(32));
                int dy = level.random.nextInt(17) - 8;
                var base = player.blockPosition().offset(dx, dy, dz);
                // 在 ±4 格高度内找菌毯
                for (int oy = -4; oy <= 4; oy++) {
                    var carpetPos = base.offset(0, oy, 0);
                    if (!level.getBlockState(carpetPos).is(ModBlocks.MYCELIUM_CARPET.get())) {
                        continue;
                    }
                    var spawnPos = carpetPos.above();
                    if (!level.isEmptyBlock(spawnPos)) {
                        continue;
                    }
                    // 8 格内不能有玩家
                    if (!level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                            new net.minecraft.world.phys.AABB(spawnPos).inflate(PLAYER_SAFE_DISTANCE)).isEmpty()) {
                        continue;
                    }
                    // 16 格内细菌数 < 4
                    var nearby = level.getEntitiesOfClass(BacteriaEntity.class,
                            new net.minecraft.world.phys.AABB(spawnPos).inflate(16.0D));
                    if (nearby.size() >= MAX_NEARBY_BACTERIA) {
                        continue;
                    }
                    var bacteria = ModEntities.BACTERIA.get().create(level);
                    if (bacteria == null) {
                        continue;
                    }
                    bacteria.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                            level.random.nextFloat() * 360.0F, 0.0F);
                    bacteria.setHomeCarpet(carpetPos);
                    level.addFreshEntity(bacteria);
                    break;
                }
            }
        }
    }
    private ModEvents() {
    }
}