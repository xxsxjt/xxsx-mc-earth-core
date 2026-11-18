package com.xxsx.earthonminecraft.compat;

import com.xxsx.earthonminecraft.compat.ae2.Ae2Compat;
import com.xxsx.earthonminecraft.compat.create.CreateCompat;
import com.xxsx.earthonminecraft.compat.jei.JeiCompat;
import com.xxsx.earthonminecraft.compat.occultism.OccultismCompat;
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
