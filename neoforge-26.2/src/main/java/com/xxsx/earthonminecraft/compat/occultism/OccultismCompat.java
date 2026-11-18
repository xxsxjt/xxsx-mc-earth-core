package com.xxsx.earthonminecraft.compat.occultism;

import com.xxsx.earthonminecraft.compat.ICompatUtils;
import net.neoforged.bus.api.IEventBus;

/**
 * Soft integration point for Occultism.  The item tags and recipes are data-driven;
 * keeping this lifecycle hook separate makes the integration safe when Occultism is absent.
 */
public final class OccultismCompat implements ICompatUtils {
    @Override
    public String modId() {
        return "occultism";
    }

    @Override
    public void initial(IEventBus gameBus, IEventBus modBus) {
        // Occultism integration is supplied by conditional data packs.  Do not link its API.
    }
}
