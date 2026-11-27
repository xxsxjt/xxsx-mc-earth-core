package com.xxsx.earth.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record DepositSpec(
        String id,
        String mineralFamily,
        String hostRock,
        boolean alterationHalo,
        int defaultReserve,
        double gradeFactor
) {
    public static final List<DepositSpec> ALL = List.of(
            new DepositSpec("rich_magnetite_ore", "iron", "deepslate", false, 48000, 1.35D),
            new DepositSpec("magnetite_ore", "iron", "stone", false, 32000, 1.00D),
            new DepositSpec("poor_magnetite_ore", "iron", "stone", false, 18000, 0.70D),
            new DepositSpec("magnetite_tuff_halo", "iron", "tuff", true, 6000, 0.35D),
            new DepositSpec("chalcopyrite_ore", "copper", "stone", false, 28000, 1.05D),
            new DepositSpec("chalcopyrite_granite_halo", "copper", "granite", true, 5500, 0.35D),
            new DepositSpec("auriferous_quartz_vein", "gold", "quartz_stone", false, 16000, 1.25D),
            new DepositSpec("auriferous_quartz_calcite_halo", "gold", "calcite", true, 4500, 0.30D),
            new DepositSpec("diamondiferous_kimberlite", "diamond", "kimberlite", false, 12000, 1.40D),
            new DepositSpec("kimberlite", "diamond", "kimberlite", false, 20000, 0.65D),
            new DepositSpec("kimberlite_tuff_halo", "diamond", "tuff", true, 5000, 0.30D),
            new DepositSpec("rare_earth_carbonatite", "rare_earth", "deepslate", false, 14000, 1.20D),
            new DepositSpec("bituminous_coal_seam", "coal", "stone", false, 38000, 1.00D),
            new DepositSpec("bituminous_sandstone_halo", "coal", "sandstone", true, 7000, 0.30D),
            new DepositSpec("anthracite_coal_seam", "coal", "deepslate", false, 30000, 1.30D),
            new DepositSpec("anthracite_deepslate_halo", "coal", "deepslate", true, 6500, 0.30D),
            new DepositSpec("evaporite_salt_bed", "evaporite", "sandstone", false, 42000, 1.00D),
            new DepositSpec("phosphorite_bed", "evaporite", "sandstone", false, 26000, 1.00D),
            new DepositSpec("borate_deposit", "evaporite", "calcite", false, 22000, 1.05D),
            new DepositSpec("limestone_calcite_bed", "carbonate", "calcite", false, 50000, 1.00D),
            new DepositSpec("cassiterite_placer_deposit", "tin", "gravel", false, 22000, 1.05D),
            new DepositSpec("bauxite_laterite_deposit", "aluminum", "laterite", false, 34000, 1.00D),
            new DepositSpec("sedimentary_sandstone_bed", "sedimentary", "sandstone", false, 40000, 0.85D),
            new DepositSpec("chromite_ore", "chromium", "deepslate", false, 18000, 1.15D),
            new DepositSpec("galena_ore", "lead", "stone", false, 24000, 1.00D),
            new DepositSpec("sphalerite_ore", "zinc", "stone", false, 24000, 1.00D),
            new DepositSpec("manganite_ore", "manganese", "deepslate", false, 22000, 1.05D),
            new DepositSpec("uraninite_ore", "uranium", "deepslate", false, 10000, 1.30D),
            new DepositSpec("cinnabar_vein", "mercury", "stone", false, 15000, 1.10D),
            new DepositSpec("emerald_beryl_vein", "emerald", "stone", false, 12000, 1.25D),
            new DepositSpec("lapis_lazuli_ore", "lapis", "stone", false, 20000, 1.10D),
            new DepositSpec("redstone_mineral_ore", "redstone", "deepslate", false, 24000, 1.05D)
    );

    private static final Map<String, DepositSpec> BY_ID = new LinkedHashMap<>();
    static {
        for (DepositSpec spec : ALL) {
            BY_ID.put(spec.id(), spec);
        }
    }

    public static Optional<DepositSpec> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}
