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
 * Universal metal/alloy/forging composition attached to ingots, blades, tools, or weapons.
 * Specialized forging or chemistry addons can populate exact alloy ratios; when absent,
 * {@link com.xxsx.earth.core.api.v1.ForgingPresetApi} resolves sensible default presets.
 */
public record MaterialComposition(
        String presetId,
        Map<String, Integer> alloyPermille,
        float purity,
        int temperStage
) {
    public static final Codec<MaterialComposition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("preset_id", "iron").forGetter(MaterialComposition::presetId),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("alloy_permille", Map.of())
                    .forGetter(MaterialComposition::alloyPermille),
            Codec.FLOAT.optionalFieldOf("purity", 0.90F).forGetter(MaterialComposition::purity),
            Codec.INT.optionalFieldOf("temper_stage", 0).forGetter(MaterialComposition::temperStage)
    ).apply(inst, MaterialComposition::new));

    public static final StreamCodec<ByteBuf, MaterialComposition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MaterialComposition::presetId,
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT),
            MaterialComposition::alloyPermille,
            ByteBufCodecs.FLOAT, MaterialComposition::purity,
            ByteBufCodecs.VAR_INT, MaterialComposition::temperStage,
            MaterialComposition::new
    );

    public MaterialComposition {
        presetId = (presetId == null || presetId.isBlank()) ? "iron" : presetId;
        Map<String, Integer> clean = new LinkedHashMap<>();
        if (alloyPermille != null) {
            for (Map.Entry<String, Integer> e : alloyPermille.entrySet()) {
                if (e.getKey() != null && !e.getKey().isBlank() && e.getValue() != null && e.getValue() > 0) {
                    clean.put(e.getKey(), Math.min(1000, e.getValue()));
                }
            }
        }
        alloyPermille = Collections.unmodifiableMap(clean);
        purity = Math.max(0.05F, Math.min(1.0F, purity));
        temperStage = Math.max(0, Math.min(5, temperStage));
    }
}
