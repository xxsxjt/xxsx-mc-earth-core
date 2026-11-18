package com.xxsx.earthonminecraft.compat;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

import java.util.concurrent.Callable;

/** Common lifecycle and soft-loading contract for optional mod integrations. */
public interface ICompatUtils {
    String modId();

    default boolean isModLoaded() {
        return ModList.get().isLoaded(modId());
    }

    default void testLoadedAndRun(Runnable action) {
        if (isModLoaded()) action.run();
    }

    default <T> T testLoadedAndCall(Callable<T> action, T fallback) {
        if (!isModLoaded()) return fallback;
        try {
            return action.call();
        } catch (Exception exception) {
            return fallback;
        }
    }

    void initial(IEventBus gameBus, IEventBus modBus);
}
