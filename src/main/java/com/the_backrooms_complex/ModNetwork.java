package com.the_backrooms_complex;

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
 * 饮水值的<b>服务端 → 客户端同步</b>。
 *
 * <p>NeoForge 21.1 的 {@code AttachmentType.Builder} 没有 {@code .sync(...)}，
 * 玩家附件不会自动同步到客户端，因此这里用自定义 payload 手动推送。
 * 客户端只把数值写入静态缓存，HUD 每帧读取。</p>
 */
@EventBusSubscriber(modid = BackroomsComplex.MODID)
public final class ModNetwork {

    /** 客户端缓存：供 HUD 读取。仅在客户端有意义。 */
    public static volatile int clientThirst = ModPlayerData.MAX;

    /** 同步包：一个 0~100 的整数。 */
    public record SyncPayload(int thirst) implements CustomPacketPayload {

        public static final Type<SyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(BackroomsComplex.MODID, "sync_thirst"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, SyncPayload::thirst,
                        SyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(SyncPayload.TYPE, SyncPayload.STREAM_CODEC,
                ModNetwork::handleClient);
    }

    /** 客户端处理：写入静态缓存。 */
    private static void handleClient(SyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientThirst = payload.thirst());
    }

    /** 服务端调用：把玩家当前饮水值推给该玩家的客户端。 */
    public static void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncPayload(ModPlayerData.getThirst(player)));
    }

    private ModNetwork() {
    }
}
