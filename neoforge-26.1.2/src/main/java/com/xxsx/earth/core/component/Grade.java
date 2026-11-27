package com.xxsx.earth.core.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum Grade {
    IMPURE("impure", 0.65D),
    COMMON("common", 1.00D),
    REFINED("refined", 1.25D),
    PURE("pure", 1.50D),
    EXQUISITE("exquisite", 1.80D);

    public static final Codec<Grade> CODEC = Codec.STRING.xmap(Grade::fromId, Grade::id);
    public static final StreamCodec<ByteBuf, Grade> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Grade::fromId, Grade::id);

    private final String id;
    private final double multiplier;

    Grade(String id, double multiplier) {
        this.id = id;
        this.multiplier = multiplier;
    }

    public String id() { return id; }
    public double multiplier() { return multiplier; }

    public static Grade fromId(String id) {
        if (id == null) return COMMON;
        for (Grade g : values()) {
            if (g.id.equalsIgnoreCase(id)) return g;
        }
        return COMMON;
    }
}
