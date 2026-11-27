package com.xxsx.earth.core.api.v1;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** Shared structural grounding & support inspection helper for realistic building/machine rules. */
public final class RealitySupportApi {
    public boolean hasSolidSupport(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (!below.isAir() && below.isFaceSturdy(level, pos.below(), Direction.UP)) {
            return true;
        }
        int lateral = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(dir);
            BlockState s = level.getBlockState(side);
            if (!s.isAir() && s.isFaceSturdy(level, side, dir.getOpposite())) {
                lateral++;
            }
        }
        return lateral >= 2;
    }

    public int supportScore(LevelReader level, BlockPos pos) {
        int score = 0;
        BlockState below = level.getBlockState(pos.below());
        if (!below.isAir() && below.isFaceSturdy(level, pos.below(), Direction.UP)) {
            score += 4;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(dir);
            BlockState s = level.getBlockState(side);
            if (!s.isAir() && s.isFaceSturdy(level, side, dir.getOpposite())) {
                score += 1;
            }
        }
        return score;
    }
}
