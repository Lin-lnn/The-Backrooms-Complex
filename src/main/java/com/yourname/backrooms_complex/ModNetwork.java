package com.yourname.backrooms_complex;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 饮水值 / 精神值的<b>服务端 → 客户端同步</b>。
 *
 * <h2>为什么需要手写这个</h2>
 * <p>NeoForge 21.1 的 {@code AttachmentType.Builder} <b>没有</b> {@code .sync(...)} 方法
 * （只有 {@code serialize} / {@code copyOnDeath} / {@code copyHandler} / {@code build}），
 * 玩家附件<b>不会</b>自动同步到客户端。因此这里用自定义 payload 手动同步。</p>
 *
 * <h2>流程</h2>
 * <pre>
 *   服务端 ModPlayerData 数值变化
 *     -> PacketDistributor.sendToPlayer(新 payload)
 *       -> 客户端 handleClient 写入 CLIENT_THIRST / CLIENT_SANITY
 *         -> ModHud 读取并绘制
 * </pre>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID)
public final class ModNetwork {

    /** 客户端缓存：供 HUD 读取。仅在客户端有意义。 */
    public static volatile int clientThirst = ModPlayerData.MAX;
    public static volatile int clientSanity = ModPlayerData.MAX;

    /** 同步包：两个 0~100 的整数。 */
    public record SyncPayload(int thirst, int sanity) implements CustomPacketPayload {

        public static final Type<SyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "sync_stats"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, SyncPayload::thirst,
                        ByteBufCodecs.VAR_INT, SyncPayload::sanity,
                        SyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(SyncPayload.TYPE, SyncPayload.STREAM_CODEC, ModNetwork::handleClient);
        event.registrar("1").playToClient(TriggerPayload.TYPE, TriggerPayload.STREAM_CODEC, ModNetwork::handleTrigger);
    }

    /** 客户端处理：写入静态缓存。 */
    private static void handleClient(SyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            int prevSanity = clientSanity;
            clientThirst = payload.thirst();
            clientSanity = payload.sanity();

            // 方块染色结果是烘焙进区块网格的：数值变化不会自动重画。
            // 当精神值跨越「暗红阈值」时，必须强制重建区块网格，
            // 否则方块会一直停留在旧颜色（表现为「暗红失效」）。
            boolean crossed = (prevSanity < com.yourname.backrooms_complex.client.BiomeTint.DARK_RED_SANITY)
                    != (clientSanity < com.yourname.backrooms_complex.client.BiomeTint.DARK_RED_SANITY);
            if (crossed) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.levelRenderer != null) {
                    mc.levelRenderer.allChanged();
                }
            }
        });
    }

    /** 客户端处理：按事件名分发。 */
    private static void handleTrigger(TriggerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            switch (payload.event()) {
                case "hallucination" -> com.yourname.backrooms_complex.client.ItemHallucination.trigger();
                case "shadow" -> com.yourname.backrooms_complex.client.ShadowFigure.trigger();
                case "fake_block" -> com.yourname.backrooms_complex.client.FakeBlockHallucination.trigger();
                case "sound" -> com.yourname.backrooms_complex.client.SoundHallucination.trigger();
                case "shake" -> com.yourname.backrooms_complex.client.SanityShake.trigger();
                default -> {
                    // 未知事件名：忽略
                }
            }
        });
    }
    /** 服务端调用：把玩家当前数值推给该玩家的客户端。 */
    public static void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new SyncPayload(ModPlayerData.getThirst(player), ModPlayerData.getSanity(player)));
    }

    /** 指令触发客户端的幻视类事件。 */
    public record TriggerPayload(String event) implements CustomPacketPayload {

        public static final Type<TriggerPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "trigger"));

        public static final StreamCodec<RegistryFriendlyByteBuf, TriggerPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, TriggerPayload::event, TriggerPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 服务端调用：让指定玩家立刻触发某个事件。 */
    public static void sendTrigger(ServerPlayer player, String event) {
        PacketDistributor.sendToPlayer(player, new TriggerPayload(event));
    }

    private ModNetwork() {
    }
}
