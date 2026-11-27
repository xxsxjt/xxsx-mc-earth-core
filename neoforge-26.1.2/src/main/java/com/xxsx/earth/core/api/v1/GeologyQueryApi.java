package com.xxsx.earth.core.api.v1;

import com.xxsx.earth.core.DepositBlock;
import com.xxsx.earth.core.DepositBlockEntity;
import com.xxsx.earth.core.DepositSpec;
import com.xxsx.earth.core.EarthCore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** Shared natural mineral deposit & finite reserve query interface for all Earth Series mods. */
public final class GeologyQueryApi {
    public record DepositInfo(
            String id,
            BlockPos center,
            int radius,
            int reserve,
            int initialReserve,
            boolean depleted,
            String mineralFamily,
            double gradeFactor
    ) {}

    public record GeologySample(
            Identifier blockId,
            String rockFamily,
            String mineralFamily,
            String miningStage,
            boolean earthNaturalSource,
            boolean alterationHalo,
            int defaultReserve
    ) {}

    public record RemoteDepositOutline(
            String lodeId,
            String mineralFamily,
            BlockPos nearestPos,
            int distanceBlocks,
            int estimatedReserve,
            int sampledBlocks,
            boolean alterationHalo
    ) {
        public double horizontalDistance() {
            return distanceBlocks;
        }
    }

    public List<DepositSpec> catalog() {
        return DepositSpec.ALL;
    }

    public Optional<DepositSpec> findSpec(String id) {
        return DepositSpec.byId(id);
    }

    public Optional<GeologySample> describe(Identifier blockId) {
        if (blockId == null || !EarthCore.MODID.equals(blockId.getNamespace())) {
            return Optional.empty();
        }
        return DepositSpec.byId(blockId.getPath()).map(spec -> new GeologySample(
                blockId,
                spec.hostRock(),
                spec.mineralFamily(),
                spec.alterationHalo() ? "wallrock_halo" : "primary_ore",
                true,
                spec.alterationHalo(),
                spec.defaultReserve()
        ));
    }

    public Optional<GeologySample> inspect(LevelReader level, BlockPos pos) {
        if (level == null || pos == null) return Optional.empty();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DepositBlock db) {
            DepositSpec spec = db.spec();
            int reserve = spec.defaultReserve();
            if (level.getBlockEntity(pos) instanceof DepositBlockEntity dbe) {
                reserve = dbe.reserve();
            }
            return Optional.of(new GeologySample(
                    Identifier.fromNamespaceAndPath(EarthCore.MODID, spec.id()),
                    spec.hostRock(),
                    spec.mineralFamily(),
                    spec.alterationHalo() ? "wallrock_halo" : "primary_ore",
                    true,
                    spec.alterationHalo(),
                    reserve
            ));
        }
        return Optional.empty();
    }

    public int reserveAt(LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof DepositBlockEntity dbe) {
            return dbe.reserve();
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DepositBlock db) {
            return db.spec().defaultReserve();
        }
        return 0;
    }

    /** Queries all distinct natural deposits within radius blocks, sorted nearest-first (primary ore before halo). */
    public List<RemoteDepositOutline> queryDeposits(LevelReader level, BlockPos origin, int maxRadius) {
        int r = Math.max(4, Math.min(64, maxRadius));
        int step = r <= 24 ? 1 : (r <= 40 ? 2 : 3);
        int minY = Math.max(level.getMinY() + 1, origin.getY() - r);
        int maxY = Math.min(level.getMaxY() - 1, origin.getY() + Math.min(20, r));

        record Acc(DepositSpec spec, BlockPos nearest, double bestDistSq, int reserveSum, int count) {}
        Map<String, Acc> grouped = new LinkedHashMap<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -r; dx <= r; dx += step) {
            for (int dz = -r; dz <= r; dz += step) {
                if (dx * dx + dz * dz > r * r) continue;
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (level instanceof Level lvl && !lvl.isLoaded(cursor.set(x, origin.getY(), z))) continue;

                for (int y = maxY; y >= minY; y -= step) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof DepositBlock db)) continue;

                    DepositSpec spec = db.spec();
                    double d2 = cursor.distSqr(origin);
                    int liveReserve = spec.defaultReserve();
                    if (level.getBlockEntity(cursor) instanceof DepositBlockEntity dbe) {
                        liveReserve = dbe.reserve();
                    }
                    Acc prev = grouped.get(spec.id());
                    if (prev == null) {
                        grouped.put(spec.id(), new Acc(spec, cursor.immutable(), d2, liveReserve, 1));
                    } else {
                        BlockPos bestPos = d2 < prev.bestDistSq ? cursor.immutable() : prev.nearest;
                        double bestD2 = Math.min(d2, prev.bestDistSq);
                        grouped.put(spec.id(), new Acc(spec, bestPos, bestD2, Math.max(prev.reserveSum, liveReserve), prev.count + 1));
                    }
                }
            }
        }

        List<RemoteDepositOutline> result = new ArrayList<>();
        for (Acc acc : grouped.values()) {
            int dist = (int) Math.round(Math.sqrt(acc.bestDistSq));
            result.add(new RemoteDepositOutline(
                    acc.spec.id(),
                    acc.spec.mineralFamily(),
                    acc.nearest,
                    dist,
                    acc.reserveSum,
                    acc.count,
                    acc.spec.alterationHalo()
            ));
        }
        result.sort(Comparator.comparing(RemoteDepositOutline::alterationHalo)
                .thenComparingInt(RemoteDepositOutline::distanceBlocks));
        return result;
    }

    public Optional<String> findNearestDepositId(Level level, BlockPos origin, int maxRadius) {
        return findNearestDeposit(level, origin, maxRadius).map(DepositInfo::id);
    }

    public Optional<DepositInfo> findNearestDeposit(Level level, BlockPos origin, int maxRadius) {
        List<RemoteDepositOutline> list = queryDeposits(level, origin, maxRadius);
        if (list.isEmpty()) return Optional.empty();
        RemoteDepositOutline best = list.get(0);
        DepositSpec spec = DepositSpec.byId(best.lodeId()).orElse(DepositSpec.ALL.get(0));
        return Optional.of(new DepositInfo(
                best.lodeId(), best.nearestPos(), maxRadius,
                best.estimatedReserve(), spec.defaultReserve(),
                best.estimatedReserve() <= 0, best.mineralFamily(), spec.gradeFactor()
        ));
    }
}
