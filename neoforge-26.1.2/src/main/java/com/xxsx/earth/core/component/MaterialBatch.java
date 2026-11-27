package com.xxsx.earth.core.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Shared material batch payload (material ID, grade tier, purity in basis points 0..10000, Kelvin temp). */
public record MaterialBatch(String materialId, Grade grade, int purityBps, int temperatureK) {
    public static final Codec<MaterialBatch> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("material_id").forGetter(MaterialBatch::materialId),
            Grade.CODEC.fieldOf("grade").forGetter(MaterialBatch::grade),
            Codec.INT.fieldOf("purity_bps").forGetter(MaterialBatch::purityBps),
            Codec.INT.fieldOf("temperature_k").forGetter(MaterialBatch::temperatureK)
    ).apply(inst, MaterialBatch::new));

    public static final StreamCodec<ByteBuf, MaterialBatch> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MaterialBatch::materialId,
            Grade.STREAM_CODEC, MaterialBatch::grade,
            ByteBufCodecs.VAR_INT, MaterialBatch::purityBps,
            ByteBufCodecs.VAR_INT, MaterialBatch::temperatureK,
            MaterialBatch::new
    );

    public double purityFraction() {
        return Math.max(0.0D, Math.min(1.0D, purityBps / 10000.0D));
    }
}
