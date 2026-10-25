package com.the_backrooms_complex.block;

import com.the_backrooms_complex.block.entity.CrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 板条箱。
 *
 * <p>右键打开 27 格容器界面。自然生成的箱子带 {@code natural = true}：
 * 连续 10~30 秒无人靠近就会消失；玩家自己放置的是 {@code natural = false}，永久保留。</p>
 */
public class CrateBlock extends BaseEntityBlock {

    /** 是否自然生成（只有自然生成的箱子会消失）。 */
    public static final BooleanProperty NATURAL = BooleanProperty.create("natural");

    public CrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(NATURAL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NATURAL);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(CrateBlock::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        CrateBlockEntity entity = new CrateBlockEntity(pos, state);
        entity.setNatural(state.getValue(NATURAL));
        return entity;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof CrateBlockEntity crate) {
                crate.serverTick();
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // 照原版木桶：MenuProvider 由方块实体提供，客户端无需知道坐标
        if (level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
            crate.ensureFilled();
            player.openMenu(crate);
        }
        return InteractionResult.CONSUME;
    }
}
