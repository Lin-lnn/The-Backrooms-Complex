package com.yourname.backrooms_complex;

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
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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

    /**
     * 可选保护：禁止在后室维度内放置非本模组命名空间的方块（保持维度纯粹）。
     * 由 {@link ModConfig#enforcePureDimension} 控制，默认关闭。
     */
    /**
     * 手持「水瓶」右键潮湿墙壁 / 潮湿地板 / 潮湿天花板 => 换成「未知污水瓶」。
     */
    /** 玩家登录时补发一次数值，避免刚进服务器 HUD 显示默认值。 */
    /** 饮用类物品回复的饮水值。 */
    public static final int DRINK_THIRST_RESTORE = ModBalance.DRINK_THIRST_RESTORE;

    /**
     * 所有原版药水 / 水瓶 / 牛奶，以及汤类（蘑菇煲、甜菜汤、兔肉煲、迷之炖菜）
     * 饮用或食用后回复 30 点饮水值。
     *
     * <p>判定方式：使用动画为 {@code DRINK}（药水/牛奶等），或属于四种原版汤类
     * （它们用的是 {@code EAT} 动画）。本模组的「未知污水瓶」有自己的数值，排除在外。</p>
     */
    /**
     * 刮纸刀右键「地板」/「潮湿的地板」=> 掉落 1~2 个地毯纤维，并把方块转为剥离态。
     * 剥离态会在 60 秒后复原（混凝土大厅除外）。
     */
    @SubscribeEvent
    public static void onScrapeFloor(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        net.minecraft.world.item.ItemStack held = event.getItemStack();
        if (!held.is(ModItems.SCRAPER.get())) {
            return;
        }
        net.minecraft.world.level.block.state.BlockState state = serverLevel.getBlockState(event.getPos());
        com.yourname.backrooms_complex.block.StrippedCarpetFloorBlock.Origin origin;
        if (state.is(ModBlocks.FLOOR_TILE)) {
            origin = com.yourname.backrooms_complex.block.StrippedCarpetFloorBlock.Origin.FLOOR_TILE;
        } else if (state.is(ModBlocks.DAMP_FLOOR)) {
            origin = com.yourname.backrooms_complex.block.StrippedCarpetFloorBlock.Origin.DAMP_FLOOR;
        } else {
            return;
        }
        net.minecraft.core.BlockPos pos = event.getPos();
        // 转化方块
        serverLevel.setBlockAndUpdate(pos, ModBlocks.STRIPPED_CARPET_FLOOR.get().defaultBlockState()
                .setValue(com.yourname.backrooms_complex.block.StrippedCarpetFloorBlock.ORIGIN, origin));
        com.yourname.backrooms_complex.block.StrippedCarpetFloorBlock.scheduleRestore(serverLevel, pos);
        // 掉落 1~2 个地毯纤维
        int count = 1 + serverLevel.random.nextInt(2);
        net.minecraft.world.item.ItemStack drop = new net.minecraft.world.item.ItemStack(ModItems.CARPET_FIBER.get(), count);
        com.yourname.backrooms_complex.DropHelper.dropFacing(serverLevel, pos, event.getEntity(), drop);
        // 耐久
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp
                && !sp.getAbilities().instabuild && com.yourname.backrooms_complex.ModConfig.scraperUsesDurability) {
            held.hurtAndBreak(1, sp, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }
    @SubscribeEvent
    public static void onDrinkRestoresThirst(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        net.minecraft.world.item.ItemStack stack = event.getItem();
        if (stack.isEmpty()) {
            return;
        }
        // 本模组污水瓶自己结算，避免重复
        if (stack.getItem() instanceof com.yourname.backrooms_complex.item.SewageBottleItem) {
            return;
        }
        boolean isDrink = stack.getUseAnimation() == net.minecraft.world.item.UseAnim.DRINK;
        boolean isStew = stack.is(net.minecraft.world.item.Items.MUSHROOM_STEW)
                || stack.is(net.minecraft.world.item.Items.BEETROOT_SOUP)
                || stack.is(net.minecraft.world.item.Items.RABBIT_STEW)
                || stack.is(net.minecraft.world.item.Items.SUSPICIOUS_STEW);
        if (!isDrink && !isStew) {
            return;
        }
        ModPlayerData.addThirst(player, DRINK_THIRST_RESTORE);
        ModNetwork.sendTo(player);
    }
    @SubscribeEvent
    public static void onPlayerLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            ModNetwork.sendTo(player);
        }
    }
    /**
     * 手持<b>空玻璃瓶</b>右键潮湿墙壁 / 潮湿墙壁踢脚线 / 潮湿地板 / 潮湿天花板
     * => 换成「未知污水瓶」。
     *
     * <p>（已按需求删除"用水瓶获取"的途径，只保留空瓶蘸取。）</p>
     */
    @SubscribeEvent
    public static void onWaterBottleOnDamp(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        net.minecraft.world.item.ItemStack held = event.getItemStack();
        // 只接受空玻璃瓶
        if (!held.is(net.minecraft.world.item.Items.GLASS_BOTTLE)) {
            return;
        }

        net.minecraft.world.level.block.state.BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.is(ModBlocks.WALL_DAMP) || state.is(ModBlocks.WALL_DAMP_PLINTH)
                || state.is(ModBlocks.DAMP_FLOOR) || state.is(ModBlocks.DAMP_CEILING))) {
            return;
        }

        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (!player.getAbilities().instabuild) {
            held.shrink(1);   // 玻璃瓶本身变成污水瓶，不再额外返还空瓶
        }
        net.minecraft.world.item.ItemStack result = new net.minecraft.world.item.ItemStack(ModItems.UNKNOWN_SEWAGE_BOTTLE.get());
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!ModConfig.enforcePureDimension) {
            return;
        }
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }
        if (!ModDimensions.LEVEL_0_WORLD.equals(level.dimension())) {
            return;
        }
        String namespace = event.getPlacedBlock().getBlock().builtInRegistryHolder()
                .key().location().getNamespace();
        if (!BackroomsComplex.MODID.equals(namespace)) {
            event.setCanceled(true);
        }
    }

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

    private ModEvents() {
    }
}