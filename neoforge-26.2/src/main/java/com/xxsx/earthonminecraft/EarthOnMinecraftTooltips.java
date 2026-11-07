package com.xxsx.earthonminecraft;

import com.xxsx.earthonminecraft.component.QualityComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.function.Consumer;

final class EarthOnMinecraftTooltips {
    private EarthOnMinecraftTooltips() {
    }

    static void addRouteTips(ItemStack stack, Consumer<Component> lines) {
        RouteGuide.addRouteTips(stack, lines);
    }

    static void addMaterialDetails(ItemStack stack, Consumer<Component> lines, TooltipFlag flag) {
        MaterialChemistry.addDetails(stack, lines, flag);
    }

    static void addVanillaTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"minecraft".equals(id.getNamespace())) {
            return;
        }
        boolean hasChemistry = MaterialChemistry.hasDetails(stack);
        boolean hasRoute = !RouteGuide.routeFor(stack.getItem()).isEmpty();
        boolean hasAssay = QualityComponents.resolveMaterialBatch(stack).isPresent()
                || QualityComponents.resolveMetalBatch(stack).isPresent();
        if (!hasChemistry && !hasRoute && !hasAssay) {
            return;
        }
        event.getToolTip().add(Component.translatable("tooltip.earth_on_minecraft.vanilla.header").withStyle(ChatFormatting.GOLD));
        QualityComponents.appendTooltip(stack, event.getToolTip()::add);
        MaterialChemistry.addDetails(stack, event.getToolTip()::add, event.getFlags());
        RouteGuide.addRouteTips(stack, event.getToolTip()::add);
    }

    static void addForgingDetails(ItemStack stack, java.util.function.Consumer<Component> lines) {
        com.xxsx.earthonminecraft.component.ForgingProcess process = stack.get(
                com.xxsx.earthonminecraft.component.EarthDataComponents.FORGING_PROCESS.get());
        if (process != null) {
            lines.accept(Component.literal("锻造工艺：折叠x" + process.foldCount()
                    + " · 塑形 " + Math.round(process.shapingProgress() * 100) + "% · 锋锐度 "
                    + Math.round(process.edgeSharpness() * 100) + "% · 淬火 "
                    + process.quenchState().getSerializedName() + " · 回火 "
                    + process.temperState().getSerializedName()).withStyle(ChatFormatting.GOLD));
        }
    }
}
