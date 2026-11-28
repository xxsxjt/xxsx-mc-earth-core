package com.xxsx.earth.core.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Universal multi-component liquid/solution composition stored on buckets, bottles,
 * flasks, or fluid containers across the Earth Series.
 *
 * <p>Solute concentrations are stored in permille (0..1000, where 1000 = 100.0%).</p>
 */
public record SolutionComposition(
        String solventId,
        Map<String, Integer> solutePermille,
        int volumeMb,
        int temperatureK
) {
    public static final SolutionComposition PURE_WATER_BUCKET =
            new SolutionComposition("minecraft:water", Map.of(), 1000, 293);

    public static final Codec<SolutionComposition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("solvent_id", "minecraft:water").forGetter(SolutionComposition::solventId),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("solute_permille", Map.of())
                    .forGetter(SolutionComposition::solutePermille),
            Codec.INT.optionalFieldOf("volume_mb", 1000).forGetter(SolutionComposition::volumeMb),
            Codec.INT.optionalFieldOf("temperature_k", 293).forGetter(SolutionComposition::temperatureK)
    ).apply(inst, SolutionComposition::new));

    public static final StreamCodec<ByteBuf, SolutionComposition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SolutionComposition::solventId,
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
            SolutionComposition::solutePermille,
            ByteBufCodecs.VAR_INT, SolutionComposition::volumeMb,
            ByteBufCodecs.VAR_INT, SolutionComposition::temperatureK,
            SolutionComposition::new
    );

    public SolutionComposition {
        solventId = (solventId == null || solventId.isBlank()) ? "minecraft:water" : solventId;
        Map<String, Integer> clean = new LinkedHashMap<>();
        if (solutePermille != null) {
            for (Map.Entry<String, Integer> e : solutePermille.entrySet()) {
                if (e.getKey() != null && !e.getKey().isBlank() && e.getValue() != null && e.getValue() > 0) {
                    clean.put(e.getKey(), Math.min(1000, e.getValue()));
                }
            }
        }
        solutePermille = Collections.unmodifiableMap(clean);
        volumeMb = Math.max(0, volumeMb);
        temperatureK = Math.max(1, temperatureK);
    }

    public static SolutionComposition singleSolute(String solventId, String soluteId, int permille, int volumeMb, int tempK) {
        int clamped = Math.max(0, Math.min(1000, permille));
        return new SolutionComposition(
                solventId,
                clamped > 0 ? Map.of(soluteId, clamped) : Map.of(),
                volumeMb,
                tempK
        );
    }

    public int concentrationOf(String soluteId) {
        return solutePermille.getOrDefault(soluteId, 0);
    }

    public double percentOf(String soluteId) {
        return concentrationOf(soluteId) / 10.0D;
    }

    public int totalSolutePermille() {
        int sum = 0;
        for (int v : solutePermille.values()) {
            sum += v;
        }
        return Math.min(1000, sum);
    }

    public SolutionComposition withVolume(int newVolumeMb) {
        return new SolutionComposition(solventId, solutePermille, newVolumeMb, temperatureK);
    }
}
