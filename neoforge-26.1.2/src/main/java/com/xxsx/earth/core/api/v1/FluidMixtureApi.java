package com.xxsx.earth.core.api.v1;

import com.xxsx.earth.core.component.EarthCoreComponents;
import com.xxsx.earth.core.component.SolutionComposition;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Core miscible fluid mixing, concentration diffusion, and container composition engine.
 * Shared across earth_xuanxue (spirit spring dilution), earth_chemistry (solutions/acids),
 * earth_physics (thermal mixing), and future fluid-handling addons.
 */
public final class FluidMixtureApi {

    public record DiffusionStep(int nextA, int nextB, boolean changed) {}

    /**
     * Exact mass-conserving mixture of two miscible liquid volumes.
     */
    public SolutionComposition mix(SolutionComposition a, SolutionComposition b) {
        if (a == null || a.volumeMb() <= 0) {
            return b == null ? SolutionComposition.PURE_WATER_BUCKET.withVolume(0) : b;
        }
        if (b == null || b.volumeMb() <= 0) {
            return a;
        }
        int totalVol = a.volumeMb() + b.volumeMb();
        Set<String> solutes = new LinkedHashSet<>();
        solutes.addAll(a.solutePermille().keySet());
        solutes.addAll(b.solutePermille().keySet());

        Map<String, Integer> mixed = new LinkedHashMap<>();
        for (String id : solutes) {
            long weighted = (long) a.concentrationOf(id) * a.volumeMb()
                    + (long) b.concentrationOf(id) * b.volumeMb();
            int permille = (int) Math.round(weighted / (double) totalVol);
            if (permille > 0) {
                mixed.put(id, Math.min(1000, permille));
            }
        }
        int mixedTemp = (int) Math.round(
                ((long) a.temperatureK() * a.volumeMb() + (long) b.temperatureK() * b.volumeMb())
                        / (double) totalVol
        );
        return new SolutionComposition(a.solventId(), mixed, totalVol, mixedTemp);
    }

    /**
     * Dilutes a solution with pure solvent while strictly conserving solute mass.
     */
    public SolutionComposition diluteWithSolvent(SolutionComposition source, int addedSolventMb, int solventTempK) {
        if (addedSolventMb <= 0) {
            return source;
        }
        SolutionComposition pure = new SolutionComposition(source.solventId(), Map.of(), addedSolventMb, solventTempK);
        return mix(source, pure);
    }

    /**
     * Discrete mass-conserving miscible diffusion step between two adjacent fluid cells.
     * Transfers 1 concentration tier from higher to lower whenever |levelA - levelB| >= 2,
     * guaranteeing {@code nextA + nextB == levelA + levelB} (zero infinite concentration creation).
     */
    public DiffusionStep diffuseStep(int levelA, int levelB, int maxLevel) {
        int a = Math.max(0, Math.min(maxLevel, levelA));
        int b = Math.max(0, Math.min(maxLevel, levelB));
        if (a - b >= 2) {
            return new DiffusionStep(a - 1, b + 1, true);
        }
        if (b - a >= 2) {
            return new DiffusionStep(a + 1, b - 1, true);
        }
        return new DiffusionStep(a, b, false);
    }

    public Optional<SolutionComposition> readComposition(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.get(EarthCoreComponents.SOLUTION_COMPOSITION.get()));
    }

    public void writeComposition(ItemStack stack, SolutionComposition composition) {
        if (stack != null && !stack.isEmpty() && composition != null) {
            stack.set(EarthCoreComponents.SOLUTION_COMPOSITION.get(), composition);
        }
    }

    public void appendCompositionTooltip(ItemStack stack, Consumer<Component> out) {
        Optional<SolutionComposition> opt = readComposition(stack);
        if (opt.isEmpty()) {
            return;
        }
        SolutionComposition c = opt.get();
        double tempC = c.temperatureK() - 273.15D;
        out.accept(Component.translatable(
                "tooltip.earth_core.solution_header",
                c.volumeMb(),
                String.format("%.1f", tempC)
        ).withStyle(ChatFormatting.AQUA));
        if (c.solutePermille().isEmpty()) {
            out.accept(Component.translatable("tooltip.earth_core.solution_pure", c.solventId())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            for (Map.Entry<String, Integer> e : c.solutePermille().entrySet()) {
                out.accept(Component.translatable(
                        "tooltip.earth_core.solution_solute",
                        soluteDisplayName(e.getKey()),
                        String.format("%.1f%%", e.getValue() / 10.0D)
                ).withStyle(ChatFormatting.GOLD));
            }
            int solventPct = Math.max(0, 1000 - c.totalSolutePermille());
            if (solventPct > 0) {
                out.accept(Component.translatable(
                        "tooltip.earth_core.solution_solvent",
                        soluteDisplayName(c.solventId()),
                        String.format("%.1f%%", solventPct / 10.0D)
                ).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static Component soluteDisplayName(String id) {
        String key = "solute." + id.replace(':', '.');
        return Component.translatableWithFallback(key, id);
    }
}
