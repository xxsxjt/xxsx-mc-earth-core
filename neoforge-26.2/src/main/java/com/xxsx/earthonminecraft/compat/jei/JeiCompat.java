package com.xxsx.earthonminecraft.compat.jei;

import com.xxsx.earthonminecraft.compat.ICompatUtils;
import net.neoforged.bus.api.IEventBus;

/** Captures whether the optional JEI integration is available. */
public final class JeiCompat implements ICompatUtils {
    private volatile boolean loaded;

    @Override public String modId() { return "jei"; }

    @Override
    public void initial(IEventBus gameBus, IEventBus modBus) {
        loaded = isModLoaded();
    }

    public boolean isJeiLoaded() { return loaded; }
}
