package com.the_backrooms_complex.client;

import com.the_backrooms_complex.BackroomsComplex;
import com.the_backrooms_complex.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 精神值过低时的「物品幻视」事件。
 *
 * <h2>规则</h2>
 * <ul>
 *   <li>精神值 &lt; {@link #THRESHOLD}（30）时，每 1 tick 有 {@link #CHANCE}（0.005%）概率触发；</li>
 *   <li>持续 {@link #DURATION}（100 tick = 5 秒）；</li>
 *   <li>效果：把<b>手持物品的名称</b>换成随机一件<b>原版物品</b>的名字；</li>
 *   <li>只影响显示名称，<b>物品本身、图标、模型都不变</b>；</li>
 *   <li>结束（或手持物被换掉）时恢复原名；</li>
 *   <li>开始与结束各有 0.5 秒的极强模糊过渡（合计 1 秒）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID, value = Dist.CLIENT)
public final class ItemHallucination {

    /** 触发阈值。 */
    public static final int THRESHOLD = 30;
    /** 每 tick 触发概率：0.005%。 */
    public static final float CHANCE = 0.00005F;
    /** 持续 tick：5 秒。 */
    public static final int DURATION = 100;
    /** 过渡模糊时长：0.5 秒（首尾各一次，合计 1 秒）。 */
    public static final int FADE_TICKS = 10;

    /** 确定性随机源 */
    private static final RandomSource RNG = RandomSource.create();

    /** 剩余 tick；0 表示未在幻视中。 */
    private static int remaining = 0;
    /** 被影响的那一份物品。 */
    private static ItemStack affected = ItemStack.EMPTY;
    /** 原本的自定义名称（可能不存在）。 */
    private static Component savedName = null;
    private static boolean hadName = false;

    /** 幻视带来的额外模糊强度 0~1，供 DehydrationBlur 读取。 */
    public static float blurBoost = 0.0F;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            remaining = 0;
            blurBoost = 0.0F;
            return;
        }

        if (remaining > 0) {
            remaining--;
            // 首尾各 0.5 秒的模糊过渡
            if (remaining > DURATION - FADE_TICKS) {
                blurBoost = (DURATION - remaining) / (float) FADE_TICKS;      // 淡入
            } else if (remaining < FADE_TICKS) {
                blurBoost = remaining / (float) FADE_TICKS;                   // 淡出
            } else {
                blurBoost = 1.0F;
            }
            if (remaining == 0) {
                restore();
                blurBoost = 0.0F;
            }
            return;
        }

        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        if (ModNetwork.clientSanity >= THRESHOLD) {
            return;
        }
        if (RNG.nextFloat() < CHANCE) {
            start(mc);
        }
    }

    private static void start(Minecraft mc) {
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }
        // 保存原名称
        hadName = held.has(DataComponents.CUSTOM_NAME);
        savedName = hadName ? held.get(DataComponents.CUSTOM_NAME) : null;

        // 随机一件原版物品，把名字套上去
        Item fake = randomVanillaItem();
        if (fake == null) {
            return;
        }
        held.set(DataComponents.CUSTOM_NAME, Component.literal(fake.getDescription().getString()));

        affected = held;
        remaining = DURATION;
        blurBoost = 0.0F;
    }

    private static void restore() {
        if (affected.isEmpty()) {
            return;
        }
        if (hadName && savedName != null) {
            affected.set(DataComponents.CUSTOM_NAME, savedName);
        } else {
            affected.remove(DataComponents.CUSTOM_NAME);
        }
        affected = ItemStack.EMPTY;
        savedName = null;
        hadName = false;
    }

    /** 从原版物品里随便挑一件（排除空气与命令方块等不可见项）。 */
    private static Item randomVanillaItem() {
        for (int attempt = 0; attempt < 24; attempt++) {
            Item candidate = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .byId(RNG.nextInt(net.minecraft.core.registries.BuiltInRegistries.ITEM.size()));
            if (candidate == null || candidate == Items.AIR) {
                continue;
            }
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(candidate);
            if (!"minecraft".equals(id.getNamespace())) {
                continue;   // 只要原版物品
            }
            return candidate;
        }
        return Items.STICK;
    }

    /** 供指令直接触发。 */
    public static boolean isActive() {
        return remaining > 0;
    }

    /** 供指令直接触发。 */
    public static void trigger() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) { start(mc); }
    }

    private ItemHallucination() {
    }
}