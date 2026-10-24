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

    /** 放置时按四周相邻的墙灰初始化连接状态。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.connect(this.defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
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

    /** 不是红石信号源 —— 也不参与任何红石连接。 */
    @Override
    protected boolean isSignalSource(BlockState state) {
        return false;
    }
}
