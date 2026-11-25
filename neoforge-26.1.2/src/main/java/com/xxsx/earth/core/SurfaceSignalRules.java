package com.xxsx.earth.core;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;
import java.util.Set;

/**
 * Registry-independent geological surface signal rules shared by {@link LodeSurfaceSignalFeature},
 * {@link SurfaceOrePebbleBlock}, and unit tests.
 */
public final class SurfaceSignalRules {
    public static final VoxelShape PEBBLE_SHAPE = Shapes.box(
            2.0D / 16.0D, 0.0D, 2.0D / 16.0D,
            14.0D / 16.0D, 3.0D / 16.0D, 14.0D / 16.0D);

    private static final Set<String> FORBIDDEN_HALO_SOILS = Set.of(
            "minecraft:grass_block",
            "minecraft:dirt",
            "minecraft:coarse_dirt",
            "minecraft:rooted_dirt",
            "minecraft:podzol",
            "minecraft:mycelium",
            "minecraft:moss_block",
            "minecraft:pale_moss_block",
            "minecraft:mud",
            "minecraft:muddy_mangrove_roots",
            "minecraft:clay",
            "minecraft:dirt_path",
            "minecraft:farmland"
    );

    private static final Set<String> ROCKY_OUTCROP_BLOCKS = Set.of(
            "minecraft:stone",
            "minecraft:granite",
            "minecraft:diorite",
            "minecraft:andesite",
            "minecraft:tuff",
            "minecraft:deepslate",
            "minecraft:calcite",
            "minecraft:gravel",
            "minecraft:sandstone",
            "minecraft:red_sandstone",
            "minecraft:terracotta",
            "minecraft:white_terracotta",
            "minecraft:orange_terracotta",
            "minecraft:magenta_terracotta",
            "minecraft:light_blue_terracotta",
            "minecraft:yellow_terracotta",
            "minecraft:lime_terracotta",
            "minecraft:pink_terracotta",
            "minecraft:gray_terracotta",
            "minecraft:light_gray_terracotta",
            "minecraft:cyan_terracotta",
            "minecraft:purple_terracotta",
            "minecraft:blue_terracotta",
            "minecraft:brown_terracotta",
            "minecraft:green_terracotta",
            "minecraft:red_terracotta",
            "minecraft:black_terracotta",
            "minecraft:dripstone_block",
            "minecraft:basalt",
            "minecraft:smooth_basalt"
    );

    private static final Set<String> PEBBLE_SOIL_AND_SAND_BLOCKS = Set.of(
            "minecraft:grass_block",
            "minecraft:dirt",
            "minecraft:coarse_dirt",
            "minecraft:rooted_dirt",
            "minecraft:podzol",
            "minecraft:mycelium",
            "minecraft:moss_block",
            "minecraft:pale_moss_block",
            "minecraft:mud",
            "minecraft:clay",
            "minecraft:sand",
            "minecraft:red_sand"
    );

    private static final Set<String> ARTIFICIAL_OR_VILLAGE_EXACT = Set.of(
            "minecraft:dirt_path",
            "minecraft:farmland",
            "minecraft:cobblestone",
            "minecraft:mossy_cobblestone",
            "minecraft:bricks",
            "minecraft:mud_bricks",
            "minecraft:stone_bricks",
            "minecraft:mossy_stone_bricks",
            "minecraft:cracked_stone_bricks",
            "minecraft:chiseled_stone_bricks",
            "minecraft:smooth_stone",
            "minecraft:hay_block",
            "minecraft:bell",
            "minecraft:composter",
            "minecraft:torch",
            "minecraft:wall_torch",
            "minecraft:lantern",
            "minecraft:soul_lantern",
            "minecraft:crafting_table",
            "minecraft:furnace",
            "minecraft:blast_furnace",
            "minecraft:smoker",
            "minecraft:chest",
            "minecraft:barrel",
            "minecraft:ladder",
            "minecraft:glass",
            "minecraft:glass_pane",
            "earth_core:settlement_board"
    );

    private SurfaceSignalRules() {
    }

    public static boolean isForbiddenHaloSoil(Identifier blockId) {
        return blockId != null && FORBIDDEN_HALO_SOILS.contains(blockId.toString());
    }

    public static boolean isRockyOutcropBlock(Identifier blockId) {
        if (blockId == null || isForbiddenHaloSoil(blockId) || isArtificialOrVillageBlock(blockId)) {
            return false;
        }
        return ROCKY_OUTCROP_BLOCKS.contains(blockId.toString());
    }

    public static boolean isPebbleHostBlock(Identifier blockId) {
        if (blockId == null || isArtificialOrVillageBlock(blockId)) {
            return false;
        }
        String id = blockId.toString();
        String path = blockId.getPath();
        if (path.endsWith("_leaves") || path.endsWith("_log") || path.endsWith("_wood")
                || path.contains("ice") || path.contains("snow") || "minecraft:water".equals(id)
                || "minecraft:lava".equals(id)) {
            return false;
        }
        return isRockyOutcropBlock(blockId) || PEBBLE_SOIL_AND_SAND_BLOCKS.contains(id);
    }

    public static boolean isArtificialOrVillageBlock(Identifier blockId) {
        if (blockId == null) {
            return false;
        }
        String id = blockId.toString();
        if (ARTIFICIAL_OR_VILLAGE_EXACT.contains(id)) {
            return true;
        }
        if ("minecraft".equals(blockId.getNamespace())) {
            String path = blockId.getPath();
            return path.startsWith("stripped_")
                    || path.endsWith("_planks")
                    || path.endsWith("_stairs")
                    || path.endsWith("_slab")
                    || path.endsWith("_fence")
                    || path.endsWith("_fence_gate")
                    || path.endsWith("_door")
                    || path.endsWith("_trapdoor")
                    || path.endsWith("_wall")
                    || path.endsWith("_bed")
                    || path.endsWith("_wool")
                    || path.endsWith("_carpet")
                    || path.endsWith("_sign");
        }
        return false;
    }

    public static Optional<String> pebbleMineralKeyForLode(String lodeId) {
        if (lodeId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(switch (lodeId) {
            case "poor_magnetite_lens", "magnetite_lens", "rich_magnetite_lens",
                    "chromite_lens", "manganite_lens" -> "magnetite";
            case "chalcopyrite_vein" -> "chalcopyrite";
            case "cassiterite_placer_deposit", "bauxite_laterite_deposit" -> "cassiterite";
            case "bituminous_coal_seam", "anthracite_coal_seam" -> "coal";
            case "galena_lens", "sphalerite_lens", "cinnabar_vein",
                    "redstone_mineral_body", "uraninite_lens" -> "galena";
            case "auriferous_quartz_vein", "kimberlite_pipe", "diamondiferous_kimberlite_pipe",
                    "emerald_beryl_vein", "lapis_lazuli_body", "rare_earth_carbonatite" -> "auriferous_quartz";
            case "limestone_calcite_bed", "phosphorite_bed",
                    "evaporite_salt_bed", "borate_deposit" -> "limestone";
            default -> null;
        });
    }
}
