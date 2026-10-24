package com.the_backrooms_complex.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 家具方块的碰撞箱。
 *
 * <p>每个方块的碰撞箱 = 其模型<b>所有元素的合并包围盒</b>，因此能<b>完全包住模型</b>，
 * 玩家不会穿模，也不会踩空。包围盒按模型元素逐轴取最小/最大值并换算到方块坐标。</p>
 */
public final class FurnitureShapes {

    /** 已构建的碰撞箱缓存。 */
    private static final Map<String, VoxelShape> CACHE = new ConcurrentHashMap<>();

    /** 取某个家具的碰撞箱；未登记的按整块处理。 */
    public static VoxelShape get(String id) {
        return CACHE.computeIfAbsent(id, FurnitureShapes::build);
    }

    private static VoxelShape build(String id) {
        return switch (id) {
            case "stool_jungle" -> Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.3438, 0.8125);
            case "stool_spruce" -> Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.3438, 0.8125);
            case "stool_cherry" -> Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.3438, 0.8125);
            case "stool_oak" -> Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.3438, 0.8125);
            case "stool_dark_oak" -> Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.3438, 0.8125);
            case "stool_birch" -> Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.3438, 0.8125);
            case "stool_bamboo" -> Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.3438, 0.8125);
            case "stool_mangrove" -> Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.3438, 0.8125);
            case "stool_crimson" -> Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.3438, 0.8125);
            case "stool_warped" -> Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.3438, 0.8125);
            case "stool_acacia" -> Shapes.box(0.1875, 0, 0.1875, 0.8125, 0.3438, 0.8125);
            case "table_jungle" -> Shapes.box(0, 0, 0, 1, 0.75, 1);
            case "table_spruce" -> Shapes.box(0, 0, 0, 1, 0.75, 1);
            case "table_birch" -> Shapes.box(0, 0, 0, 1, 0.75, 1);
            case "table_cherry" -> Shapes.box(0, 0, 0, 1, 0.75, 1);
            case "table_oak" -> Shapes.box(0, 0, 0, 1, 0.75, 1);
            case "table_dark_oak" -> Shapes.box(0, 0, 0, 1, 0.75, 1);
            case "table_bamboo" -> Shapes.box(0, 0, 0, 1, 0.75, 1);
            case "table_mangrove" -> Shapes.box(0, 0, 0, 1, 0.75, 1);
            case "table_crimson" -> Shapes.box(0, 0, 0, 1, 0.75, 1);
            case "table_warped" -> Shapes.box(0, 0, 0, 1, 0.75, 1);
            case "table_acacia" -> Shapes.box(0, 0, 0, 1, 0.75, 1);
            case "chair_jungle" -> Shapes.box(0.125, 0, 0.125, 0.875, 1.0625, 0.875);
            case "chair_spruce" -> Shapes.box(0.125, 0, 0.125, 0.875, 1.0625, 0.875);
            case "chair_birch" -> Shapes.box(0.125, 0, 0.125, 0.875, 1.0625, 0.875);
            case "chair_cherry" -> Shapes.box(0.125, 0, 0.125, 0.875, 1.0625, 0.875);
            case "chair_oak" -> Shapes.box(0.125, 0, 0.125, 0.875, 1.0625, 0.875);
            case "chair_dark_oak" -> Shapes.box(0.125, 0, 0.125, 0.875, 1.0625, 0.875);
            case "chair_bamboo" -> Shapes.box(0.125, 0, 0.125, 0.875, 1.0625, 0.875);
            case "chair_mangrove" -> Shapes.box(0.125, 0, 0.125, 0.875, 1.0625, 0.875);
            case "chair_crimson" -> Shapes.box(0.125, 0, 0.125, 0.875, 1.0625, 0.875);
            case "chair_warped" -> Shapes.box(0.125, 0, 0.125, 0.875, 1.0625, 0.875);
            case "chair_acacia" -> Shapes.box(0.125, 0, 0.125, 0.875, 1.0625, 0.875);
            default -> Shapes.block();
        };
    }

    private FurnitureShapes() {
    }
}
