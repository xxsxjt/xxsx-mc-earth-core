package com.xxsx.earth.core.api.v1;

/**
 * Stable v1 cross-mod facade for The Earth Core (earth_core).
 * Provides real shared runtime physics & material engines consumed by earth_geography,
 * earth_xuanxue, earth_chemistry, earth_engineering, earth_life_sciences, earth_magic, etc.
 */
public final class EarthCoreApi {
    private static final GeologyQueryApi GEOLOGY = new GeologyQueryApi();
    private static final ThermalUnitsApi THERMAL = new ThermalUnitsApi();
    private static final MaterialPropertyApi MATERIALS = new MaterialPropertyApi();
    private static final RealitySupportApi SUPPORT = new RealitySupportApi();
    private static final FluidMixtureApi FLUIDS = new FluidMixtureApi();
    private static final ForgingPresetApi FORGING = new ForgingPresetApi();
    private static final AddonCompatApi COMPAT = new AddonCompatApi();

    private EarthCoreApi() {}

    public static GeologyQueryApi geology() { return GEOLOGY; }
    public static ThermalUnitsApi thermal() { return THERMAL; }
    public static MaterialPropertyApi materials() { return MATERIALS; }
    public static RealitySupportApi support() { return SUPPORT; }
    public static FluidMixtureApi fluids() { return FLUIDS; }
    public static ForgingPresetApi forging() { return FORGING; }
    public static AddonCompatApi compat() { return COMPAT; }
}
