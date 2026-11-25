package com.xxsx.earth.core.compat;

import com.xxsx.earth.core.compat.ae2.Ae2Compat;
import com.xxsx.earth.core.compat.create.CreateCompat;
import com.xxsx.earth.core.compat.jei.JeiCompat;
import com.xxsx.earth.core.compat.occultism.OccultismCompat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

import java.util.List;

/** Central lifecycle entry point for optional integrations. */
public final class EarthCompatFactory {
    private static final List<ICompatUtils> COMPATS = List.of(new Ae2Compat(), new CreateCompat(), new JeiCompat());

    private EarthCompatFactory() { }

    public static void register(IEventBus gameBus, IEventBus modBus) {
        for (ICompatUtils compat : COMPATS) compat.initial(gameBus, modBus);
        if (ModList.get().isLoaded("occultism")) {
            new OccultismCompat().initial(gameBus, modBus);
        }
    }
}
