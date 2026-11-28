package com.xxsx.earth.core.api.v1;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/**
 * Shared soft-compatibility helper across the Earth Series:
 * - Graceful optional-addon presence checks (for fallback prerequisites / forging presets)
 * - Unified settlement signpost & Waystones detection (replacing custom magic anchor blocks)
 */
public final class AddonCompatApi {

    public boolean isModLoaded(String modId) {
        if (modId == null || modId.isBlank()) {
            return false;
        }
        try {
            ModList list = ModList.get();
            return list != null && list.isLoaded(modId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean isWaystonesLoaded() {
        return isModLoaded("waystones");
    }

    public boolean isWaystoneBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        Identifier key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return "waystones".equals(key.getNamespace()) && key.getPath().contains("waystone");
    }

    public boolean isSettlementMarkerBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        return isWaystoneBlock(state)
                || state.is(BlockTags.ALL_SIGNS)
                || state.is(BlockTags.BANNERS);
    }

    /**
     * Locates the nearest Waystone or settlement signpost/banner around an origin so
     * settlements and NPCs bind to real world markers instead of artificial anchor blocks.
     */
    public Optional<BlockPos> findNearestSettlementMarker(LevelReader level, BlockPos origin, int radius) {
        BlockPos bestWaystone = null;
        double bestWaystoneDist = Double.MAX_VALUE;
        BlockPos bestSign = null;
        double bestSignDist = Double.MAX_VALUE;
        int r = Math.max(1, Math.min(32, radius));
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -4; dy <= 6; dy++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(p);
                    if (isWaystoneBlock(st)) {
                        double d = p.distSqr(origin);
                        if (d < bestWaystoneDist) {
                            bestWaystoneDist = d;
                            bestWaystone = p.immutable();
                        }
                    } else if (st.is(BlockTags.ALL_SIGNS) || st.is(BlockTags.BANNERS)) {
                        double d = p.distSqr(origin);
                        if (d < bestSignDist) {
                            bestSignDist = d;
                            bestSign = p.immutable();
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(bestWaystone != null ? bestWaystone : bestSign);
    }
}
