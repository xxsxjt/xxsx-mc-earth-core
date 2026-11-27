package com.xxsx.earth.core.api.v1;

import com.xxsx.earth.core.component.Grade;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Shared physical & metallurgical baseline properties for core Earth materials. */
public final class MaterialPropertyApi {
    public record MaterialSpec(
            String id,
            double densityKgM3,
            double meltingPointK,
            double mohsHardness,
            double thermalConductivityWmK,
            Grade defaultGrade
    ) {}

    private static final List<MaterialSpec> CATALOG = List.of(
            new MaterialSpec("iron", 7874.0D, 1811.0D, 4.0D, 80.4D, Grade.COMMON),
            new MaterialSpec("copper", 8960.0D, 1357.8D, 3.0D, 401.0D, Grade.COMMON),
            new MaterialSpec("gold", 19300.0D, 1337.3D, 2.5D, 318.0D, Grade.REFINED),
            new MaterialSpec("tin", 7310.0D, 505.1D, 1.5D, 66.8D, Grade.COMMON),
            new MaterialSpec("aluminum", 2700.0D, 933.5D, 2.75D, 237.0D, Grade.REFINED),
            new MaterialSpec("chromium", 7190.0D, 2180.0D, 8.5D, 93.9D, Grade.REFINED),
            new MaterialSpec("lead", 11340.0D, 600.6D, 1.5D, 35.3D, Grade.COMMON),
            new MaterialSpec("zinc", 7140.0D, 692.7D, 2.5D, 116.0D, Grade.COMMON),
            new MaterialSpec("carbon_coal", 1450.0D, 3823.0D, 2.2D, 0.3D, Grade.COMMON),
            new MaterialSpec("diamond", 3515.0D, 4000.0D, 10.0D, 2200.0D, Grade.EXQUISITE),
            new MaterialSpec("limestone", 2710.0D, 1612.0D, 3.0D, 1.3D, Grade.COMMON),
            new MaterialSpec("salt_halite", 2160.0D, 1074.0D, 2.5D, 6.5D, Grade.COMMON)
    );

    private static final Map<String, MaterialSpec> BY_ID = new LinkedHashMap<>();
    static {
        for (MaterialSpec m : CATALOG) {
            BY_ID.put(m.id(), m);
        }
    }

    public List<MaterialSpec> all() { return CATALOG; }
    public Optional<MaterialSpec> find(String id) { return Optional.ofNullable(BY_ID.get(id)); }
}
