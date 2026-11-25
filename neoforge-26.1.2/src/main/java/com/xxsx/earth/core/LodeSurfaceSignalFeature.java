package com.xxsx.earth.core;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Post-ore pass that marks the surface above real ore bodies.
 *
 * <p>Runs once per chunk after {@code underground_ores}. It reads the main-lode blocks already
 * placed in the chunk with {@link LodeSectionScan} (ore features write chunk sections directly and
 * leave no block-entity record), and for every main lode with at least {@link #MIN_BLOCKS} blocks
 * puts a restrained geological signal at the surface above the lode's XZ centroid: solid halo or
 * outcrop rock only on exposed rocky/gravel terrain, or 1-3 loose mineral float pebbles on top of
 * natural soil/grass without replacing grass blocks or encroaching on villages and structures.</p>
 */
public final class LodeSurfaceSignalFeature extends Feature<NoneFeatureConfiguration> {
    public static final int MIN_BLOCKS = 8;
    public static final int MAX_SIGNALS = 1;
    public static final float SIGNAL_CHANCE = 0.28F;
    public static final float OPEN_SOIL_CHANCE = 0.35F;
    public static final float FOREST_CANOPY_CHANCE = 0.15F;
    public static final int STRUCTURE_AVOID_RADIUS = 4;
    public static final int MIN_SURFACE_BLOCKS = 1;
    public static final int MAX_SURFACE_BLOCKS = 3;

    /** A signal to place: which lode, how many blocks backed it, and the surface column. */
    public record Signal(String lodeId, int blocks, int x, int z) {
    }

    public LodeSurfaceSignalFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunkPos = ChunkPos.containing(context.origin());
        ChunkAccess chunk = level.getChunk(chunkPos.x(), chunkPos.z());
        if (!chunk.getAllReferences().isEmpty()) {
            return false;
        }
        List<LodeSectionScan.Hit> hits = LodeSectionScan.scan(chunk, LodeSectionScan.mainLodeBlocks());
        List<Signal> planned = planSignals(hits);
        RandomSource random = context.random();
        if (planned.isEmpty() || random.nextFloat() >= SIGNAL_CHANCE) {
            return false;
        }
        boolean placed = false;
        for (Signal signal : planned) {
            placed |= placeSignal(level, chunkPos, signal, random);
        }
        return placed;
    }

    /**
     * Picks the signals for one chunk: main lodes only (no halos, no host strata), at least
     * {@link #MIN_BLOCKS} blocks each, largest first, at most {@link #MAX_SIGNALS}.
     */
    public static List<Signal> planSignals(Collection<LodeSectionScan.Hit> hits) {
        return LodeSectionScan.planSignals(hits);
    }

    private static boolean placeSignal(WorldGenLevel level, ChunkPos chunk, Signal signal, RandomSource random) {
        Optional<Block> outcropBlock = ProspectingCatalog.surfaceSignalFor(signal.lodeId());
        Optional<SurfaceOrePebbleBlock.PebbleMineral> pebbleMineral =
                SurfaceOrePebbleBlock.PebbleMineral.forLode(signal.lodeId());
        if (outcropBlock.isEmpty() && pebbleMineral.isEmpty()) {
            return false;
        }
        BlockPos centerTop = surface(level, chunk, signal.x(), signal.z());
        if (centerTop == null || hasNearbySettlementOrArtificialBlocks(level, centerTop)) {
            return false;
        }

        BlockState centerGround = level.getBlockState(centerTop);
        boolean rockyCenter = isRockyOutcropSurface(centerGround);
        int targetBlocks;
        if (rockyCenter) {
            targetBlocks = MIN_SURFACE_BLOCKS + random.nextInt(MAX_SURFACE_BLOCKS - MIN_SURFACE_BLOCKS + 1);
        } else if (isPebbleHostSurface(centerGround)) {
            boolean canopy = hasLeafCanopyAbove(level, centerTop);
            float habitatChance = canopy ? FOREST_CANOPY_CHANCE : OPEN_SOIL_CHANCE;
            if (random.nextFloat() >= habitatChance) {
                return false;
            }
            targetBlocks = canopy ? 1 : (1 + random.nextInt(2));
        } else {
            return false;
        }

        int[][] offsets = {
                {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {2, 0}, {-2, 0}, {0, 2}, {0, -2},
                {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
                {2, 1}, {-2, 1}, {1, 2}, {-1, -2}
        };
        for (int i = 1; i < offsets.length; i++) {
            int j = i + random.nextInt(offsets.length - i);
            int[] tmp = offsets[i];
            offsets[i] = offsets[j];
            offsets[j] = tmp;
        }

        int placedCount = 0;
        for (int[] offset : offsets) {
            if (placedCount >= targetBlocks) {
                break;
            }
            BlockPos top = surface(level, chunk, signal.x() + offset[0], signal.z() + offset[1]);
            if (top == null || hasNearbySettlementOrArtificialBlocks(level, top)) {
                continue;
            }
            BlockState ground = level.getBlockState(top);
            if (isRockyOutcropSurface(ground)) {
                // On exposed rock/gravel slopes, mix occasional outcrop/halo rock with loose mineral float.
                if (pebbleMineral.isPresent() && random.nextBoolean() && placePebbleAbove(level, top, pebbleMineral.get())) {
                    placedCount++;
                } else if (outcropBlock.isPresent() && replaceRockyOutcrop(level, top, outcropBlock.get().defaultBlockState())) {
                    placedCount++;
                } else if (pebbleMineral.isPresent() && placePebbleAbove(level, top, pebbleMineral.get())) {
                    placedCount++;
                }
            } else if (isPebbleHostSurface(ground) && pebbleMineral.isPresent()) {
                if (placePebbleAbove(level, top, pebbleMineral.get())) {
                    placedCount++;
                }
            }
        }
        return placedCount > 0;
    }

    /** Top solid block of a column inside this chunk, or {@code null} outside it. */
    private static BlockPos surface(WorldGenLevel level, ChunkPos chunk, int x, int z) {
        if (x < chunk.getMinBlockX() || x > chunk.getMaxBlockX() || z < chunk.getMinBlockZ() || z > chunk.getMaxBlockZ()) {
            return null;
        }
        int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        return y < level.getMinY() ? null : new BlockPos(x, y, z);
    }

    private static boolean replaceRockyOutcrop(WorldGenLevel level, BlockPos pos, BlockState signal) {
        if (pos.getY() < level.getMinY() || !isRockyOutcropSurface(level.getBlockState(pos))) {
            return false;
        }
        if (!level.getFluidState(pos.above()).isEmpty()) {
            return false;
        }
        return level.setBlock(pos, signal, Block.UPDATE_CLIENTS);
    }

    private static boolean placePebbleAbove(WorldGenLevel level, BlockPos top,
                                            SurfaceOrePebbleBlock.PebbleMineral mineral) {
        BlockPos above = top.above();
        if (above.getY() >= level.getMaxY() || !level.getFluidState(above).isEmpty()) {
            return false;
        }
        BlockState ground = level.getBlockState(top);
        if (!isPebbleHostSurface(ground)
                || !Block.isFaceFull(ground.getCollisionShape(level, top), Direction.UP)) {
            return false;
        }
        BlockState currentAbove = level.getBlockState(above);
        if (!canReplaceWithPebble(currentAbove)) {
            return false;
        }
        BlockState pebbleState = EarthOnMinecraft.SURFACE_ORE_PEBBLE.get()
                .defaultBlockState()
                .setValue(SurfaceOrePebbleBlock.MINERAL, mineral);
        return level.setBlock(above, pebbleState, Block.UPDATE_CLIENTS);
    }

    public static boolean canReplaceWithPebble(BlockState aboveState) {
        return aboveState.isAir()
                || aboveState.is(Blocks.SHORT_GRASS)
                || aboveState.is(Blocks.FERN)
                || aboveState.is(Blocks.DEAD_BUSH);
    }

    private static boolean hasLeafCanopyAbove(WorldGenLevel level, BlockPos top) {
        int motionTopY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, top.getX(), top.getZ()) - 1;
        int maxScanY = Math.min(level.getMaxY() - 1, Math.max(motionTopY, top.getY() + 14));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = top.getY() + 1; y <= maxScanY; y++) {
            cursor.set(top.getX(), y, top.getZ());
            if (level.getBlockState(cursor).is(BlockTags.LEAVES)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNearbySettlementOrArtificialBlocks(WorldGenLevel level, BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -STRUCTURE_AVOID_RADIUS; dx <= STRUCTURE_AVOID_RADIUS; dx++) {
            for (int dz = -STRUCTURE_AVOID_RADIUS; dz <= STRUCTURE_AVOID_RADIUS; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                for (int dy = -2; dy <= 3; dy++) {
                    int y = center.getY() + dy;
                    if (y < level.getMinY() || y >= level.getMaxY()) {
                        continue;
                    }
                    cursor.set(x, y, z);
                    if (isArtificialOrVillageBlock(level.getBlockState(cursor))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Exposed bare rock or gravel where a full-block alteration halo or outcrop may replace the surface.
     * Soil, grass, podzol, mycelium, moss, mud, clay, and village surfaces are strictly excluded.
     */
    public static boolean isRockyOutcropSurface(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.FARMLAND)
                || state.is(BlockTags.SUBSTRATE_OVERWORLD)
                || SurfaceSignalRules.isForbiddenHaloSoil(id)) {
            return false;
        }
        return SurfaceSignalRules.isRockyOutcropBlock(id)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(Blocks.STONE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.TERRACOTTA)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.DEEPSLATE);
    }

    /** Alias kept for full-block replacement callers: only exposed rocky outcrops may be replaced. */
    public static boolean isNaturalSurface(BlockState state) {
        return isRockyOutcropSurface(state);
    }

    /**
     * Natural ground that may support a loose mineral pebble in {@code top.above()} without replacing
     * the ground block itself. Excludes leaves, ice, water, snow, farmland, paths, and artificial blocks.
     */
    public static boolean isPebbleHostSurface(BlockState state) {
        if (isArtificialOrVillageBlock(state)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.ICE)
                || state.is(BlockTags.SNOW)
                || state.is(Blocks.WATER)
                || state.is(Blocks.ICE)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.OAK_LOG)
                || state.is(Blocks.OAK_LEAVES)) {
            return false;
        }
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return isRockyOutcropSurface(state)
                || SurfaceSignalRules.isPebbleHostBlock(id)
                || state.is(BlockTags.SUBSTRATE_OVERWORLD)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.CLAY);
    }

    /** Village paths, farmland, and constructed blocks around which surface ore signals must never spawn. */
    public static boolean isArtificialOrVillageBlock(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return SurfaceSignalRules.isArtificialOrVillageBlock(id)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.BRICKS)
                || state.is(Blocks.SMOOTH_STONE)
                || state.is(Blocks.HAY_BLOCK)
                || state.is(Blocks.BELL)
                || state.is(Blocks.COMPOSTER)
                || state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.LANTERN)
                || state.is(Blocks.SOUL_LANTERN)
                || state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.FURNACE)
                || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER)
                || state.is(Blocks.CHEST)
                || state.is(Blocks.BARREL)
                || state.is(Blocks.LADDER)
                || state.is(Blocks.GLASS)
                || state.is(Blocks.GLASS_PANE)
                || isStrippedLogOrWood(state)
                || state.is(BlockTags.PLANKS)
                || state.is(BlockTags.STONE_BRICKS)
                || state.is(BlockTags.FENCES)
                || state.is(BlockTags.FENCE_GATES)
                || state.is(BlockTags.WALLS)
                || state.is(BlockTags.DOORS)
                || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.STAIRS)
                || state.is(BlockTags.SLABS)
                || state.is(BlockTags.BEDS)
                || state.is(BlockTags.WOOL)
                || state.is(BlockTags.WOOL_CARPETS)
                || state.is(BlockTags.ALL_SIGNS)
                || state.is(BlockTags.CAMPFIRES)
                || state.is(BlockTags.CROPS)
                || state.is(Blocks.OAK_PLANKS)
                || state.is(Blocks.OAK_FENCE)
                || state.is(Blocks.STONE_BRICKS)
                || state.getBlock() instanceof SettlementBoardBlock;
    }

    private static boolean isStrippedLogOrWood(BlockState state) {
        return state.is(Blocks.STRIPPED_OAK_LOG)
                || state.is(Blocks.STRIPPED_SPRUCE_LOG)
                || state.is(Blocks.STRIPPED_BIRCH_LOG)
                || state.is(Blocks.STRIPPED_JUNGLE_LOG)
                || state.is(Blocks.STRIPPED_ACACIA_LOG)
                || state.is(Blocks.STRIPPED_DARK_OAK_LOG)
                || state.is(Blocks.STRIPPED_MANGROVE_LOG)
                || state.is(Blocks.STRIPPED_CHERRY_LOG)
                || state.is(Blocks.STRIPPED_PALE_OAK_LOG)
                || state.is(Blocks.STRIPPED_OAK_WOOD)
                || state.is(Blocks.STRIPPED_SPRUCE_WOOD)
                || state.is(Blocks.STRIPPED_BIRCH_WOOD)
                || state.is(Blocks.STRIPPED_JUNGLE_WOOD)
                || state.is(Blocks.STRIPPED_ACACIA_WOOD)
                || state.is(Blocks.STRIPPED_DARK_OAK_WOOD)
                || state.is(Blocks.STRIPPED_MANGROVE_WOOD)
                || state.is(Blocks.STRIPPED_CHERRY_WOOD)
                || state.is(Blocks.STRIPPED_PALE_OAK_WOOD);
    }
}
