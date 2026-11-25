package com.xxsx.earth.core;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Model-aligned, multi-part voxel geometry shared by block behavior and headless tests. */
final class ProcessingMachineShapes {
    private static final Map<ProcessingMachineBlock.Kind, VoxelShape[]> SHAPES = createShapes();

    private ProcessingMachineShapes() {
    }

    static VoxelShape get(ProcessingMachineBlock.Kind kind, Direction facing) {
        return SHAPES.get(kind)[directionIndex(facing)];
    }

    private static Map<ProcessingMachineBlock.Kind, VoxelShape[]> createShapes() {
        Map<ProcessingMachineBlock.Kind, VoxelShape[]> shapes = new EnumMap<>(ProcessingMachineBlock.Kind.class);
        for (ProcessingMachineBlock.Kind kind : ProcessingMachineBlock.Kind.values()) {
            VoxelShape[] facingShapes = new VoxelShape[4];
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                facingShapes[directionIndex(direction)] = shapeFor(kind, direction);
            }
            shapes.put(kind, facingShapes);
        }
        return shapes;
    }

    private static int directionIndex(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> throw new IllegalArgumentException("Machine facing must be horizontal: " + direction);
        };
    }

    private static VoxelShape shapeFor(ProcessingMachineBlock.Kind kind, Direction facing) {
        double[][] boxes = switch (kind) {
            case CRUSHER -> boxes(
                    1, 0, 1, 15, 2, 15, 3, 2, 3, 13, 12, 14, 1, 3, 2, 4, 13, 15,
                    12, 3, 2, 15, 13, 15, 4, 12, 5, 12, 16, 14, 5, 2, 0, 11, 6, 4);
            case BALL_MILL -> boxes(
                    1, 0, 1, 15, 2, 15, 1, 2, 4, 4, 8, 12, 3, 5, 2, 13, 13, 14,
                    12, 2, 4, 15, 8, 12, 0, 7, 6, 4, 11, 10, 12, 7, 6, 16, 11, 10);
            case SIEVE -> boxes(
                    1, 0, 1, 15, 2, 15, 2, 2, 3, 4, 7, 13, 12, 2, 3, 14, 7, 13,
                    2, 6, 3, 14, 9, 14, 3, 9, 10, 13, 14, 15, 3, 2, 0, 13, 7, 5);
            case MAGNETIC_SEPARATOR -> boxes(
                    1, 0, 1, 15, 2, 15, 3, 5, 3, 13, 13, 13, 1, 3, 4, 4, 15, 12,
                    12, 3, 4, 15, 15, 12, 4, 11, 11, 12, 16, 15, 4, 2, 0, 12, 7, 5);
            case FLOTATION_CELL -> boxes(
                    1, 0, 1, 15, 2, 15, 2, 2, 2, 14, 11, 14, 1, 9, 1, 15, 12, 15,
                    7, 10, 7, 9, 16, 9, 5, 13, 5, 11, 16, 11, 11, 5, 0, 15, 9, 4);
            case ROASTER -> boxes(
                    2, 0, 2, 14, 4, 14, 3, 3, 3, 13, 14, 13, 2, 4, 2, 14, 7, 14,
                    2, 10, 2, 14, 13, 14, 6, 13, 6, 10, 16, 10, 5, 1, 0, 11, 6, 4);
            case REDUCTION_FURNACE -> boxes(
                    1, 0, 1, 15, 5, 15, 3, 4, 3, 13, 15, 13, 2, 3, 2, 14, 8, 14,
                    5, 14, 5, 11, 16, 11, 5, 1, 0, 11, 5, 3, 13, 6, 5, 16, 11, 11);
            case LEACHING_TANK -> boxes(
                    1, 0, 1, 15, 2, 15, 2, 2, 2, 14, 12, 14, 1, 10, 1, 15, 13, 15,
                    7, 11, 7, 9, 16, 9, 5, 13, 5, 11, 16, 11, 3, 5, 0, 8, 10, 3);
            case ELECTROLYTIC_CELL -> boxes(
                    1, 0, 1, 15, 2, 15, 1, 2, 2, 15, 10, 14, 3, 8, 3, 5, 15, 13,
                    7, 8, 3, 9, 15, 13, 11, 8, 3, 13, 15, 13, 1, 13, 1, 15, 16, 4);
            case POWDER_PRESS -> boxes(
                    1, 0, 1, 15, 3, 15, 2, 2, 2, 4, 15, 14, 12, 2, 2, 14, 15, 14,
                    1, 13, 1, 15, 16, 15, 6, 7, 5, 10, 14, 11, 4, 3, 3, 12, 7, 13);
            case CHEMICAL_REACTOR -> boxes(
                    1, 0, 1, 15, 2, 15, 3, 2, 3, 13, 13, 13, 2, 3, 2, 14, 7, 14,
                    2, 9, 2, 14, 12, 14, 5, 12, 5, 11, 16, 11, 12, 5, 5, 16, 9, 11);
            case DISTILLATION_COLUMN -> boxes(
                    1, 0, 2, 15, 5, 14, 5, 3, 5, 11, 16, 11, 3, 5, 3, 13, 7, 13,
                    3, 9, 3, 13, 11, 13, 3, 13, 3, 13, 15, 13, 10, 8, 6, 16, 11, 10);
            case MIXER -> boxes(
                    1, 0, 1, 15, 2, 15, 2, 4, 2, 14, 12, 14, 1, 5, 4, 4, 11, 12,
                    12, 5, 4, 15, 11, 12, 5, 11, 5, 11, 16, 11, 5, 2, 5, 11, 6, 11);
            case CRYSTALLIZER -> boxes(
                    1, 0, 1, 15, 2, 15, 3, 2, 3, 13, 13, 13, 1, 5, 5, 5, 12, 11,
                    5, 11, 5, 11, 16, 11, 5, 1, 5, 11, 6, 11, 12, 6, 4, 16, 11, 12);
            case INDUSTRIAL_KILN -> boxes(
                    1, 0, 1, 15, 2, 15, 2, 2, 3, 5, 7, 13, 2, 5, 2, 14, 13, 14,
                    11, 2, 3, 14, 7, 13, 0, 5, 5, 4, 12, 11, 12, 7, 4, 16, 14, 12);
            case GAS_SEPARATOR -> boxes(
                    1, 0, 1, 15, 2, 15, 2, 2, 3, 7, 15, 13, 9, 2, 3, 14, 15, 13,
                    1, 13, 2, 15, 16, 6, 0, 4, 5, 4, 8, 11, 12, 4, 5, 16, 8, 11);
            case FERTILIZER_GRANULATOR -> boxes(
                    1, 0, 1, 15, 2, 15, 1, 2, 4, 4, 7, 12, 3, 5, 2, 13, 13, 14,
                    12, 2, 4, 15, 8, 12, 0, 8, 5, 5, 15, 12, 11, 4, 4, 16, 12, 13);
            case POLYMERIZER -> boxes(
                    1, 0, 1, 15, 2, 15, 3, 2, 3, 13, 13, 13, 2, 4, 2, 14, 11, 14,
                    5, 12, 5, 11, 16, 11, 0, 7, 5, 5, 11, 11, 11, 4, 5, 16, 8, 11);
            case STEAM_CRACKER -> boxes(
                    1, 0, 2, 10, 15, 14, 2, 3, 0, 9, 13, 4, 9, 7, 3, 14, 16, 13,
                    12, 3, 4, 16, 13, 12, 0, 0, 1, 11, 4, 15, 11, 12, 6, 15, 16, 10);
            case SYNTHESIS_LOOP -> boxes(
                    1, 0, 1, 15, 2, 15, 5, 2, 5, 11, 16, 11, 1, 3, 3, 6, 9, 13,
                    11, 4, 4, 15, 14, 12, 2, 12, 2, 14, 15, 5, 2, 2, 11, 14, 5, 14);
            case ABSORPTION_TOWER -> boxes(
                    1, 0, 1, 15, 5, 15, 5, 3, 5, 11, 16, 11, 3, 5, 3, 13, 7, 13,
                    3, 11, 3, 13, 13, 13, 0, 3, 5, 5, 9, 11, 4, 14, 4, 12, 16, 12);
            case BLOOMERY -> boxes(
                    1, 0, 1, 15, 2, 15, 3, 2, 3, 13, 8, 13, 4, 8, 4, 12, 13, 12,
                    0, 4, 6, 4, 7, 10, 5, 2, 0, 11, 5, 4, 5, 13, 5, 11, 15, 11);
            case BREAD_OVEN -> boxes(
                    1, 0, 1, 15, 3, 15, 2, 3, 2, 14, 5, 14, 3, 5, 3, 13, 10, 13,
                    5, 10, 5, 11, 13, 11, 5, 3, 0, 11, 8, 4, 0, 4, 12, 4, 5, 15);
            case GLASS_KILN -> boxes(
                    1, 0, 1, 15, 4, 15, 2, 4, 2, 14, 11, 14, 3, 11, 3, 13, 14, 13,
                    12, 4, 6, 15, 9, 10, 6, 11, 0, 10, 14, 4, 0, 1, 6, 3, 4, 10);
            case LIME_KILN -> boxes(
                    2, 0, 2, 14, 3, 14, 3, 3, 3, 13, 12, 13, 4, 12, 4, 12, 14, 12,
                    5, 1, 0, 11, 4, 3, 6, 7, 12, 10, 11, 15, 5, 0, 3, 11, 2, 6);
            case PIT_KILN -> boxes(
                    0, 0, 0, 16, 4, 16, 2, 4, 2, 14, 8, 14, 5, 8, 5, 11, 12, 11,
                    6, 2, 0, 10, 5, 3, 7, 11, 7, 9, 14, 9, 4, 12, 4, 12, 13, 12);
            case SHAFT_FURNACE -> boxes(
                    1, 0, 1, 15, 3, 15, 2, 3, 2, 14, 9, 14, 3, 9, 3, 13, 14, 13,
                    0, 4, 6, 4, 8, 10, 5, 2, 0, 11, 5, 4, 5, 14, 5, 11, 16, 11);
            case ANVIL -> boxes(
                    4, 0, 4, 12, 4, 12, 5, 4, 5, 11, 6, 11, 6, 6, 6, 10, 9, 10,
                    4, 9, 4, 12, 12, 12, 12, 10, 6, 16, 12, 10, 1, 2, 5, 4, 9, 11);
            case BAR_COUNTER -> boxes(
                    1, 0, 4, 15, 8, 14, 0, 8, 3, 16, 10, 15, 1, 0, 0, 15, 14, 4,
                    2, 10, 3, 14, 13, 6, 3, 8, 9, 7, 10, 13, 2, 0, 14, 14, 2, 16);
            case CAMPFIRE -> boxes(
                    2, 0, 2, 14, 2, 14, 0, 0, 3, 3, 4, 13, 13, 0, 3, 16, 4, 13,
                    3, 0, 0, 13, 4, 3, 3, 0, 13, 13, 4, 16, 4, 2, 4, 12, 6, 12, 3, 6, 3, 13, 8, 13);
            case CARPENTRY_BENCH -> boxes(
                    1, 0, 3, 4, 9, 13, 12, 0, 3, 15, 9, 13, 0, 9, 2, 16, 12, 14,
                    11, 8, 14, 15, 12, 16, 2, 12, 1, 14, 16, 3, 4, 12, 6, 10, 13, 11);
            case CASTING_TABLE -> boxes(
                    1, 0, 2, 15, 4, 14, 1, 4, 3, 4, 8, 13, 12, 4, 3, 15, 8, 13,
                    5, 4, 0, 11, 7, 4, 4, 4, 4, 12, 6, 12, 2, 0, 14, 14, 4, 16);
            case COFFEE_ROASTER -> boxes(
                    1, 0, 3, 15, 5, 13, 3, 4, 4, 13, 12, 12, 6, 6, 2, 10, 10, 5,
                    4, 12, 4, 12, 16, 10, 5, 12, 10, 11, 15, 14, 1, 0, 13, 9, 3, 16);
            case CONDENSER -> boxes(
                    2, 0, 3, 14, 4, 13, 4, 4, 4, 12, 14, 12, 5, 14, 5, 11, 16, 11,
                    0, 6, 5, 4, 10, 11, 12, 6, 5, 16, 10, 11, 5, 0, 0, 11, 3, 4);
            case CONVERTER -> boxes(
                    1, 0, 2, 15, 4, 14, 2, 4, 3, 14, 12, 13, 4, 12, 4, 12, 16, 12,
                    2, 6, 0, 14, 9, 4, 6, 2, 13, 10, 6, 16, 12, 7, 6, 16, 10, 12);
            case CURING_BARN -> boxes(
                    1, 0, 2, 15, 12, 14, 3, 12, 4, 13, 16, 12, 0, 3, 4, 2, 10, 12,
                    14, 3, 4, 16, 10, 12, 4, 0, 14, 12, 8, 16, 3, 8, 5, 13, 10, 11);
            case FERMENTING_CROCK -> boxes(
                    2, 0, 2, 14, 3, 14, 3, 3, 3, 13, 11, 13, 4, 11, 4, 12, 14, 12,
                    7, 14, 7, 9, 16, 9, 12, 5, 6, 16, 8, 10, 3, 3, 0, 13, 6, 3);
            case FILTER_PRESS -> boxes(
                    1, 0, 2, 15, 4, 14, 2, 4, 3, 6, 14, 13, 8, 4, 3, 12, 14, 13,
                    6, 5, 5, 8, 13, 11, 12, 6, 4, 16, 12, 12, 2, 0, 13, 14, 3, 16);
            case GEM_CUTTER -> boxes(
                    2, 0, 2, 14, 5, 14, 5, 5, 4, 8, 15, 7, 5, 11, 6, 13, 14, 9,
                    10, 5, 9, 14, 10, 13, 0, 8, 3, 4, 13, 8, 2, 0, 14, 14, 2, 16);
            case HERB_PROCESSING_TABLE -> boxes(
                    1, 0, 2, 15, 9, 14, 2, 11, 3, 14, 13, 13, 2, 9, 3, 4, 14, 5,
                    3, 9, 5, 9, 11, 11, 10, 9, 5, 14, 12, 11, 1, 0, 14, 6, 6, 16);
            case LOOM -> boxes(
                    1, 0, 2, 15, 3, 14, 1, 3, 3, 4, 13, 13, 12, 3, 3, 15, 13, 13,
                    5, 5, 4, 11, 12, 12, 4, 12, 5, 12, 14, 11, 3, 8, 2, 13, 10, 4);
            case MILL -> boxes(
                    2, 0, 2, 14, 5, 14, 3, 5, 3, 13, 11, 13, 4, 11, 4, 12, 14, 12,
                    5, 14, 5, 11, 16, 11, 11, 4, 6, 16, 8, 10, 0, 7, 5, 4, 14, 11);
            case NOODLE_PRESS -> boxes(
                    1, 0, 3, 15, 4, 13, 2, 4, 4, 6, 9, 12, 10, 4, 4, 14, 9, 12,
                    6, 9, 5, 10, 15, 11, 5, 3, 3, 11, 6, 13, 2, 0, 13, 14, 3, 16);
            case OIL_PRESS -> boxes(
                    1, 0, 3, 15, 4, 13, 3, 4, 4, 13, 11, 12, 6, 11, 6, 10, 16, 10,
                    4, 12, 4, 12, 16, 9, 13, 5, 6, 16, 9, 10, 4, 4, 12, 12, 9, 15);
            case RICE_HULLER -> boxes(
                    1, 0, 2, 15, 5, 14, 3, 5, 4, 7, 11, 12, 9, 5, 4, 13, 11, 12,
                    4, 11, 5, 12, 16, 11, 3, 2, 12, 13, 5, 16, 0, 6, 5, 4, 10, 11);
            case SMELTER -> boxes(
                    2, 0, 2, 14, 4, 14, 3, 4, 3, 13, 12, 13, 5, 12, 5, 11, 16, 11,
                    6, 2, 13, 10, 6, 16, 13, 6, 5, 16, 10, 11, 3, 0, 13, 13, 3, 16);
            case SMOKEHOUSE -> boxes(
                    2, 3, 3, 14, 14, 13, 1, 14, 2, 15, 16, 14, 3, 0, 4, 13, 4, 12,
                    3, 9, 5, 13, 11, 11, 13, 5, 6, 16, 9, 10, 4, 0, 12, 12, 2, 15);
            case STEAMER -> boxes(
                    2, 0, 2, 14, 6, 14, 3, 6, 3, 13, 12, 13, 4, 12, 4, 12, 15, 12,
                    4, 15, 4, 12, 16, 12, 5, 0, 13, 11, 4, 16, 13, 6, 5, 16, 10, 11);
            case STEAMER_BASKET -> boxes(
                    3, 0, 3, 13, 4, 13, 3, 4, 3, 13, 8, 13, 3, 8, 3, 13, 12, 13,
                    3, 12, 3, 13, 15, 13, 4, 15, 4, 12, 16, 12, 0, 4, 5, 3, 8, 11);
            case STILL -> boxes(
                    2, 0, 2, 14, 5, 14, 3, 5, 3, 13, 12, 13, 6, 12, 6, 10, 16, 10,
                    12, 3, 4, 16, 11, 12, 4, 0, 13, 10, 4, 16, 0, 6, 5, 3, 10, 11);
            case STONE_MILL -> boxes(
                    1, 0, 1, 15, 4, 15, 3, 4, 3, 13, 9, 13, 4, 9, 4, 12, 13, 12,
                    6, 13, 6, 10, 15, 10, 12, 9, 6, 16, 12, 10, 5, 1, 0, 11, 5, 4);
            case STOVE -> boxes(
                    2, 0, 2, 14, 8, 14, 1, 8, 1, 15, 11, 15, 5, 11, 5, 11, 12, 11,
                    5, 1, 13, 11, 6, 16, 13, 2, 5, 16, 7, 11, 3, 0, 13, 13, 3, 16);
            case TABLET_PRESS -> boxes(
                    2, 0, 3, 14, 4, 13, 5, 4, 5, 11, 8, 11, 6, 8, 6, 10, 14, 10,
                    4, 12, 3, 9, 16, 9, 11, 4, 5, 16, 8, 11, 3, 0, 13, 13, 2, 16);
            case TANNERY -> boxes(
                    1, 0, 2, 15, 5, 12, 3, 0, 12, 13, 3, 16, 2, 5, 4, 14, 9, 9,
                    2, 10, 4, 14, 15, 6, 0, 2, 4, 4, 8, 10, 4, 0, 9, 12, 2, 12);
            case TEA_KILN -> boxes(
                    2, 0, 2, 14, 8, 14, 3, 8, 3, 13, 12, 13, 2, 11, 2, 14, 13, 14,
                    7, 12, 7, 9, 16, 9, 5, 0, 13, 11, 5, 16, 12, 5, 4, 16, 8, 12);
            case THICKENER -> boxes(
                    3, 0, 3, 13, 4, 13, 1, 4, 1, 15, 11, 15, 5, 11, 5, 11, 16, 11,
                    14, 5, 3, 16, 9, 13, 2, 8, 2, 7, 12, 7, 0, 9, 5, 3, 13, 11);
            case WATER_TREATMENT_UNIT -> boxes(
                    1, 0, 1, 15, 3, 15, 3, 3, 4, 10, 15, 12, 10, 6, 3, 13, 13, 12,
                    0, 5, 5, 3, 9, 11, 12, 3, 4, 16, 7, 12, 4, 0, 13, 12, 3, 16);
        };
        VoxelShape shape = Shapes.empty();
        for (double[] box : boxes) {
            shape = Shapes.or(shape, rotateBox(box, facing));
        }
        return shape.optimize();
    }

    private static double[][] boxes(double... coordinates) {
        if (coordinates.length % 6 != 0) {
            throw new IllegalArgumentException("Each voxel box needs six coordinates");
        }
        double[][] boxes = new double[coordinates.length / 6][6];
        for (int i = 0; i < boxes.length; i++) {
            System.arraycopy(coordinates, i * 6, boxes[i], 0, 6);
        }
        return boxes;
    }

    private static VoxelShape rotateBox(double[] box, Direction facing) {
        double minX = box[0];
        double minZ = box[2];
        double maxX = box[3];
        double maxZ = box[5];
        return switch (facing) {
            case NORTH -> Block.box(minX, box[1], minZ, maxX, box[4], maxZ);
            case EAST -> Block.box(16 - maxZ, box[1], minX, 16 - minZ, box[4], maxX);
            case SOUTH -> Block.box(16 - maxX, box[1], 16 - maxZ, 16 - minX, box[4], 16 - minZ);
            case WEST -> Block.box(minZ, box[1], 16 - maxX, maxZ, box[4], 16 - minX);
            default -> throw new IllegalArgumentException("Machine facing must be horizontal: " + facing);
        };
    }

}
