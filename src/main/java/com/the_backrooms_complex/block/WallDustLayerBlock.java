package com.the_backrooms_complex.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 铺在地上的墙灰。
 *
 * <p>外观与<b>红石粉</b>一致：没有相邻同类时会显示成"点"，有相邻同类时向该方向拉出"线"，
 * 从而能<b>连接成线</b>。贴图是灰白色。</p>
 *
 * <p><b>但它不传递任何红石信号</b> —— 只是装饰：{@link #isSignalSource} 返回 {@code false}，
 * 也不参与红石网络的计算。</p>
 */
public class WallDustLayerBlock extends Block {

    /** 四向连接属性（与红石粉同名，方便复用原版模型）。 */
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    /** 贴地薄层碰撞箱（高 1 像素）。 */
    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.0625D, 1.0D);

    public WallDustLayerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    /**
     * 放置时按四周相邻的墙灰初始化连接状态。
     *
     * <p><b>只允许放在完整方块的上表面</b>：点击面必须是顶面，且下方方块必须是完整的实心方块。</p>
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        if (ctx.getClickedFace() != Direction.UP) {
            return null;   // 只有点顶面才能铺
        }
        if (!isFullSupport(ctx.getLevel(), pos.below())) {
            return null;   // 下方不是完整方块就不能铺
        }
        return this.connect(this.defaultBlockState(), ctx.getLevel(), pos);
    }

    /** 下方必须是完整实心方块，否则不能存在（会被破坏掉落）。 */
    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return isFullSupport(level, pos.below());
    }

    /**
     * 判定某个位置是否提供<b>完整的平面</b>（能托住墙灰）。
     *
     * <p>用 {@code isFaceFull} 检查"支撑形状的顶面是不是完整的 16×16 正方形"。
     * 因此：</p>
     * <ul>
     *   <li>完整方块 → ✅ 可以放；</li>
     *   <li><b>倒置楼梯</b>（顶面是满的）→ ✅ 可以放；</li>
     *   <li>正置楼梯 / 台阶 / 栅栏 / 半砖 → ❌ 不能放（顶面不是完整平面）。</li>
     * </ul>
     */
    private static boolean isFullSupport(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockState below = level.getBlockState(pos);
        if (below.isAir()) {
            return false;
        }
        VoxelShape support = below.getBlockSupportShape(level, pos);
        return Block.isFaceFull(support, Direction.UP);
    }

    /** 邻居变化时刷新连接状态；相邻的墙灰也要一起刷新。 */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty prop = switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> null;
        };
        if (level instanceof net.minecraft.world.level.Level realLevel) {
            this.checkWashAway(realLevel, pos);
        }
        if (prop == null) {
            return state;
        }
        return state.setValue(prop, neighborState.getBlock() == this);
    }

    private BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        return state
                .setValue(NORTH, level.getBlockState(pos.north()).getBlock() == this)
                .setValue(SOUTH, level.getBlockState(pos.south()).getBlock() == this)
                .setValue(EAST, level.getBlockState(pos.east()).getBlock() == this)
                .setValue(WEST, level.getBlockState(pos.west()).getBlock() == this);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** 不完整方块：不阻挡光照传播。 */
    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /**
     * 被水冲走：上方或所在位置变成水时，掉落自身并消失（与红石粉一致）。
     */
    @Override
    protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        this.checkWashAway(level, pos);
    }


    /** 若被水覆盖则掉落自身并清除。 */
    private void checkWashAway(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        boolean washed = !level.getFluidState(pos).isEmpty()
                || !level.getFluidState(pos.above()).isEmpty();
        if (washed) {
            dropResources(level.getBlockState(pos), level, pos);
            level.removeBlock(pos, false);
        }
    }

    /** 不是红石信号源 —— 也不参与任何红石连接。 */
    @Override
    protected boolean isSignalSource(BlockState state) {
        return false;
    }
}
