package com.xxsx.earth.core;

import com.xxsx.earth.core.compat.create.CreateCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Backward-compatible facade for Create kinetic checks. */
public final class CreateKineticCompat {
    private CreateKineticCompat() { }

    public static boolean isCreateCrankItem(ItemStack stack) { return CreateCompat.isCreateCrankItem(stack); }
    public static boolean isWrenchItem(ItemStack stack) { return CreateCompat.isWrenchItem(stack); }
    public static boolean hasAdjacentCreateKineticPower(Level level, BlockPos pos) {
        return CreateCompat.hasAdjacentCreateKineticPower(level, pos);
    }
    public static boolean isCreateKineticItem(ItemStack stack) { return CreateCompat.isCreateKineticItem(stack); }
}
