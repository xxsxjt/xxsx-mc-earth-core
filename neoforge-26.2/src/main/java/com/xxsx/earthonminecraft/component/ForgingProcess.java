package com.xxsx.earthonminecraft.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

/**
 * Persistent, server-authoritative state for a hot workpiece.  The component is
 * deliberately independent from a block entity so the same substrate can be
 * handed to an anvil, an alchemical forge, a magic wand recipe, or a blessing
 * recipe without losing its physical history.
 */
public record ForgingProcess(
        int schemaVersion,
        long heatedAtTick,
        double initialTempK,
        double coolingRateKPerSecond,
        int foldCount,
        double shapingProgress,
        TargetForm targetForm,
        double edgeSharpness,
        QuenchState quenchState,
        TemperState temperState,
        double microcracks,
        double decarburization,
        double substratePotential) {

    public static final double AMBIENT_K = 293.0;
    public static final double COLD_FORGING_K = 800.0;
    public static final double MIN_HOT_WORK_K = 850.0;
    public static final double FOLD_MIN_K = 950.0;
    public static final double FOLD_MAX_K = 1450.0;
    public static final double QUENCH_MIN_K = 1050.0;
    public static final double QUENCH_MAX_K = 1320.0;
    public static final double OVERHEAT_K = 1520.0;

    public static final ForgingProcess DEFAULT = new ForgingProcess(
            1, 0L, AMBIENT_K, 8.0, 0, 0.0, TargetForm.INGOT_REFINING,
            0.0, QuenchState.UNQUENCHED, TemperState.UNTEMPERED,
            0.0, 0.0, 0.0);

    public static final Codec<ForgingProcess> CODEC = RecordCodecBuilder.<ForgingProcess>create(instance -> instance.group(
            ExtraCodecs.intRange(1, 1).optionalFieldOf("schema_version", 1).forGetter(ForgingProcess::schemaVersion),
            ExtraCodecs.NON_NEGATIVE_LONG.optionalFieldOf("heated_at_tick", 0L).forGetter(ForgingProcess::heatedAtTick),
            Codec.doubleRange(AMBIENT_K, 3000.0).optionalFieldOf("initial_temp_k", AMBIENT_K).forGetter(ForgingProcess::initialTempK),
            Codec.doubleRange(0.01, 100.0).optionalFieldOf("cooling_rate_k_per_second", 8.0).forGetter(ForgingProcess::coolingRateKPerSecond),
            ExtraCodecs.intRange(0, 12).optionalFieldOf("fold_count", 0).forGetter(ForgingProcess::foldCount),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("shaping_progress", 0.0).forGetter(ForgingProcess::shapingProgress),
            TargetForm.CODEC.optionalFieldOf("target_form", TargetForm.INGOT_REFINING).forGetter(ForgingProcess::targetForm),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("edge_sharpness", 0.0).forGetter(ForgingProcess::edgeSharpness),
            QuenchState.CODEC.optionalFieldOf("quench_state", QuenchState.UNQUENCHED).forGetter(ForgingProcess::quenchState),
            TemperState.CODEC.optionalFieldOf("temper_state", TemperState.UNTEMPERED).forGetter(ForgingProcess::temperState),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("microcracks", 0.0).forGetter(ForgingProcess::microcracks),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("decarburization", 0.0).forGetter(ForgingProcess::decarburization),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("substrate_potential", 0.0).forGetter(ForgingProcess::substratePotential)
    ).apply(instance, ForgingProcess::new)).validate(ForgingProcess::validate);

    private static DataResult<ForgingProcess> validate(ForgingProcess value) {
        if (value.schemaVersion != 1) return DataResult.error(() -> "unsupported forging_process schema_version " + value.schemaVersion);
        return DataResult.success(value);
    }

    public static ForgingProcess heated(long tick, double temperatureK) {
        double temp = Math.max(AMBIENT_K, Math.min(3000.0, temperatureK));
        return new ForgingProcess(1, Math.max(0L, tick), temp, 8.0, 0, 0.0,
                TargetForm.INGOT_REFINING, 0.0, QuenchState.UNQUENCHED,
                TemperState.UNTEMPERED, 0.0, 0.0, 0.0);
    }

    /** Temperature after elapsed world ticks, with an optional nearby heat source. */
    public double currentTemperatureK(long nowTick, double nearbyHeatK) {
        long elapsedTicks = Math.max(0L, nowTick - heatedAtTick);
        double cooled = Math.max(AMBIENT_K, initialTempK - coolingRateKPerSecond * elapsedTicks / 20.0);
        if (nearbyHeatK <= AMBIENT_K) return cooled;
        // A workpiece beside a heat source asymptotically follows the source;
        // it never jumps to furnace temperature in a single tick.
        return Math.max(cooled, cooled + (nearbyHeatK - cooled) * 0.12);
    }

    public ForgingProcess withTarget(TargetForm target) {
        return new ForgingProcess(schemaVersion, heatedAtTick, initialTempK, coolingRateKPerSecond,
                foldCount, shapingProgress, target, edgeSharpness, quenchState, temperState,
                microcracks, decarburization, substratePotential);
    }

    public ForgingProcess reheated(long tick, double temperatureK) {
        double temp = Math.max(AMBIENT_K, Math.min(3000.0, temperatureK));
        return new ForgingProcess(schemaVersion, Math.max(0L, tick), temp, coolingRateKPerSecond,
                foldCount, shapingProgress, targetForm, edgeSharpness, quenchState, temperState,
                microcracks, decarburization, substratePotential);
    }

    public boolean shaped() { return shapingProgress >= 0.999; }
    public boolean edged() { return edgeSharpness >= 0.999; }
    public boolean settled() { return temperState == TemperState.TEMPERED || (shaped() && quenchState == QuenchState.UNQUENCHED); }

    public enum TargetForm implements StringRepresentable {
        INGOT_REFINING("ingot_refining"), SWORD_BLADE("sword_blade"), AXE_HEAD("axe_head"),
        PICKAXE_HEAD("pickaxe_head"), SPEAR_TIP("spear_tip"), DAGGER_BLADE("dagger_blade"), HAMMER_HEAD("hammer_head");
        public static final Codec<TargetForm> CODEC = StringRepresentable.fromEnum(TargetForm::values);
        private final String name;
        TargetForm(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
        public TargetForm next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public enum QuenchState implements StringRepresentable {
        UNQUENCHED("unquenched"), WATER_QUENCHED("water_quenched"), OIL_OR_BRINE_QUENCHED("oil_or_brine_quenched"), OVERQUENCHED_CRACKED("overquenched_cracked");
        public static final Codec<QuenchState> CODEC = StringRepresentable.fromEnum(QuenchState::values);
        private final String name;
        QuenchState(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public enum TemperState implements StringRepresentable {
        UNTEMPERED("untempered"), TEMPERED("tempered");
        public static final Codec<TemperState> CODEC = StringRepresentable.fromEnum(TemperState::values);
        private final String name;
        TemperState(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }
}
