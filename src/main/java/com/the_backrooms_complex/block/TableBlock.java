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
 * 可拼接的桌子。
 *
 * <p>四个方向各有一个布尔属性（north/east/south/west），表示该侧是否连着同类桌子。
 * 模型由 blockstates 的 multipart 拼装：桌面常显，桌腿只在<b>两条相邻边都没有桌子</b>时显示
 * —— 也就是只在<b>拼接矩形的四个顶点</b>保留桌腿，中间的桌腿自动隐藏。</p>
 */
public class TableBlock extends FurnitureBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    public TableBlock(Properties properties, String furnitureId) {
        super(properties, furnitureId);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    /** 放置时按邻接情况初始化四向连接状态。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.joined(this.defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    /** 邻居变化时刷新连接状态（支持不断扩建）。 */
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
        boolean joined = neighborState.getBlock() == this;
        return state.setValue(prop, joined);
    }

    private BlockState joined(BlockState state, BlockGetter level, BlockPos pos) {
        return state
                .setValue(NORTH, level.getBlockState(pos.north()).getBlock() == this)
                .setValue(SOUTH, level.getBlockState(pos.south()).getBlock() == this)
                .setValue(EAST, level.getBlockState(pos.east()).getBlock() == this)
                .setValue(WEST, level.getBlockState(pos.west()).getBlock() == this);
    }

    /** 碰撞箱：桌面部分 + 仍然显示的桌腿，合并成一个盒子范围（保证完全包络）。 */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape base = FurnitureShapes.get(this.furnitureId);
        return Shapes.or(base, Shapes.empty());
    }
}
