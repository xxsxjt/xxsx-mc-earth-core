package com.xxsx.earth.core;

import com.xxsx.earth.core.compat.ae2.Ae2Compat;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Backward-compatible facade for the AE2 crank integration. */
public final class Ae2CrankCompat {
    private Ae2CrankCompat() { }

    public static boolean isAe2CrankItem(ItemStack stack) { return Ae2Compat.isAe2CrankItem(stack); }
    public static void register(RegisterCapabilitiesEvent event) { Ae2Compat.register(event); }
}
