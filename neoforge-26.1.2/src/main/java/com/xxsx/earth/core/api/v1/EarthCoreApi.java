package com.xxsx.earth.core.api.v1;

/**
 * Stable v1 cross-mod facade for The Earth Core (earth_core).
 * Consumed by earth_geography, earth_chemistry, earth_engineering, earth_life_sciences, earth_xuanxue, etc.
 */
public final class EarthCoreApi {
    private static final GeologyQueryApi GEOLOGY = new GeologyQueryApi();
    private static final ThermalUnitsApi THERMAL = new ThermalUnitsApi();
    private static final MaterialPropertyApi MATERIALS = new MaterialPropertyApi();
    private static final RealitySupportApi SUPPORT = new RealitySupportApi();

    private EarthCoreApi() {}

    public static GeologyQueryApi geology() { return GEOLOGY; }
    public static ThermalUnitsApi thermal() { return THERMAL; }
    public static MaterialPropertyApi materials() { return MATERIALS; }
    public static RealitySupportApi support() { return SUPPORT; }
}
