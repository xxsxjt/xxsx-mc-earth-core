package com.xxsx.earth.core.api.v1;

/** Shared Kelvin/Celsius thermodynamic constants & unit conversions for the entire Earth Series. */
public final class ThermalUnitsApi {
    public static final double ZERO_CELSIUS_K = 273.15D;
    public static final double STP_KELVIN = 288.15D;
    public static final double STP_PRESSURE_KPA = 101.325D;
    public static final double ENVIRONMENTAL_LAPSE_RATE_K_PER_M = 0.0065D;

    public double toKelvin(double celsius) {
        return Math.max(0.0D, celsius + ZERO_CELSIUS_K);
    }

    public double toCelsius(double kelvin) {
        return kelvin - ZERO_CELSIUS_K;
    }

    /** Estimates ambient temperature in Kelvin at Minecraft Y altitude relative to sea level (Y=63). */
    public double ambientKelvinAt(int y, double seaLevelCelsius) {
        double deltaM = Math.max(0.0D, y - 63.0D);
        return toKelvin(seaLevelCelsius - deltaM * ENVIRONMENTAL_LAPSE_RATE_K_PER_M * 10.0D);
    }

    /** Heat energy Q = m * c * dT (Joules). */
    public double sensibleHeatJoules(double massKg, double specificHeatJPerKgK, double deltaK) {
        return Math.max(0.0D, massKg) * Math.max(0.0D, specificHeatJPerKgK) * deltaK;
    }
}
