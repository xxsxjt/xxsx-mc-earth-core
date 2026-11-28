package com.xxsx.earth.core;

import com.mojang.logging.LogUtils;
import com.xxsx.earth.core.api.v1.EarthCoreApi;
import com.xxsx.earth.core.component.EarthCoreComponents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EarthCore.MODID)
public final class EarthCore {
    public static final String MODID = "earth_core";
    public static final String VERSION = "0.4.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EarthCore(IEventBus modBus) {
        EarthCoreComponents.DATA_COMPONENTS.register(modBus);
        EarthCoreContent.BLOCKS.register(modBus);
        EarthCoreContent.ITEMS.register(modBus);
        EarthCoreContent.BLOCK_ENTITIES.register(modBus);
        EarthCoreContent.CREATIVE_TABS.register(modBus);
        LOGGER.info("[earth_core] loaded real foundation v{} ({} deposits, FluidMixtureApi, ForgingPresetApi[{} presets], AddonCompatApi)",
                VERSION,
                EarthCoreApi.geology().catalog().size(),
                EarthCoreApi.forging().defaultPresets().size());
    }
}
