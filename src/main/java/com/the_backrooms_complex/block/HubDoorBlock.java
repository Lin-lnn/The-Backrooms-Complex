package com.the_backrooms_complex.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.the_backrooms_complex.ModConfig;
import com.the_backrooms_complex.ModItems;
import com.the_backrooms_complex.worldgen.Level0Teleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 枢纽之门：两格高的门（{@link DoorBlock}），黄色调的云杉木门样式。
 *
 * <p><b>交互逻辑</b>：</p>
 * <ul>
 *   <li>玩家手持「阈界的层级密钥」右键 → 在<b>服务端</b>把玩家传送到后室 Level 0
 *       的真实走廊空位，播放末影人传送音效，返回 {@link ItemInteractionResult#SUCCESS}
 *       （对应 {@code InteractionResult.SUCCESS}，确保客户端-服务端同步）；</li>
 *   <li>未手持密钥 → 返回 {@link ItemInteractionResult#PASS_TO_DEFAULT_BLOCK_INTERACTION}，
 *       交由 {@link DoorBlock} 原本的开关门逻辑处理。</li>
 * </ul>
 *
 * <p>传送后是否消耗密钥由 {@link ModConfig#consumeKeyOnTeleport} 控制，默认不消耗。</p>
 *
 * <p>方块状态（facing / half / hinge / open / powered）由 {@link DoorBlock} 提供，
 * blockstate JSON 按原版门规范展开。</p>
 */
public class HubDoorBlock extends DoorBlock {

    /** 方块编解码器（沿用门的 block_set_type + 属性结构）。 */
    public static final MapCodec<HubDoorBlock> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BlockSetType.CODEC.fieldOf("block_set_type").forGetter(DoorBlock::type),
                    propertiesCodec()
            ).apply(instance, HubDoorBlock::new));

    public HubDoorBlock(BlockSetType type, BlockBehaviour.Properties properties) {
        super(type, properties);
    }

    @Override
    public MapCodec<? extends DoorBlock> codec() {
        return CODEC;
    }

    /**
     * 手持密钥右键 → 传送到后室 Level 0；否则走正常开关门逻辑。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(ModItems.THRESHOLD_KEY.get())) {
            // 未手持密钥：交给默认方块交互（DoorBlock#useWithoutItem 的开关门逻辑）
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            // 客户端只反馈成功，真正的传送在服务端执行并同步
            return ItemInteractionResult.SUCCESS;
        }
        if (!Level0Teleporter.teleportToLevel0(player)) {
            return ItemInteractionResult.FAIL;
        }
        // 播放末影人传送音效
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (ModConfig.consumeKeyOnTeleport) {
            stack.shrink(1);
        }
        return ItemInteractionResult.SUCCESS;
    }
}