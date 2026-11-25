package com.xxsx.earth.core;

import com.xxsx.earth.core.component.QualityComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class GuidedMaterialItem extends Item {
    public GuidedMaterialItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.earth_core.material.header").withStyle(ChatFormatting.GOLD));
        EarthOnMinecraftTooltips.addMaterialDetails(stack, lines, flag);
        QualityComponents.appendTooltip(stack, lines);
        EarthOnMinecraftTooltips.addForgingDetails(stack, lines);
        EarthOnMinecraftTooltips.addRouteTips(stack, lines);
        lines.accept(Component.translatable("tooltip.earth_core.material.notebook").withStyle(ChatFormatting.DARK_GRAY));
    }
}
