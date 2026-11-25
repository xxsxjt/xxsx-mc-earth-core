package com.xxsx.earth.core.client;

import com.xxsx.earth.core.EarthOnMinecraft;
import com.xxsx.earth.core.EnergyGeneratorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class EnergyGeneratorScreen extends AbstractContainerScreen<EnergyGeneratorMenu> {
    private static final Identifier FUEL_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final int imageWidth = 176;
    private static final int imageHeight = 168;
    private static final int ENERGY_X = 76;
    private static final int ENERGY_Y = 31;
    private static final int ENERGY_W = 92;
    private static final int ENERGY_H = 9;
    private static final int STATUS_Y = 45;

    public EnergyGeneratorScreen(EnergyGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, imageWidth, imageHeight);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        int accent = menu.steamTurbineGenerator() ? 0xFF55B8D0 : 0xFFD58442;
        addRenderableWidget(new IndustrialButton(leftPos + 176, topPos + 12, 20, 20,
                Component.empty(), button -> {
                    this.onClose();
                    EarthOnMinecraftClient.openNotebook();
                }, (g, mouseX, mouseY, hovered) ->
                        EarthGuiSupport.drawSideTab(g, leftPos + 176, topPos + 12, accent, hovered)))
                .setTooltip(Tooltip.create(Component.translatable("screen.earth_core.button.notebook.tooltip")));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        int accent = menu.steamTurbineGenerator() ? 0xFF55B8D0 : 0xFFD58442;
        EarthGuiSupport.drawInventoryArmor(g, leftPos, topPos, imageWidth, imageHeight, menu.slots, accent);
        drawGeneratorIdentity(g);
        drawFuel(g);
        EarthGuiSupport.drawEnergyBar(g, leftPos + ENERGY_X, topPos + ENERGY_Y,
                ENERGY_W, ENERGY_H, menu.energy(), menu.capacity());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        drawStatus(g);
        drawTooltips(g, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, EarthGuiSupport.trim(font, title, 140), titleLabelX, titleLabelY, 0xFFE2E8EC, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFB8C4CC, false);
        g.text(font, Component.literal(EarthGuiSupport.compactEnergy(menu.energy(), menu.capacity())),
                ENERGY_X, 20, 0xFFE2E8EC, false);
        Component source = Component.translatable(menu.steamTurbineGenerator()
                ? "screen.earth_core.energy.steam_source"
                : "screen.earth_core.energy.fuel");
        g.text(font, source, 26, 47, 0xFF9AAEB8, false);
        String rate = "+" + menu.generationPerTick() + " / " + menu.transferPerTick() + " EOU/t";
        g.text(font, EarthGuiSupport.fit(font, rate, ENERGY_W), ENERGY_X, 59, 0xFFE2E8EC, false);
    }

    private void drawGeneratorIdentity(GuiGraphicsExtractor g) {
        int accent = menu.steamTurbineGenerator() ? 0xFF55B8D0 : 0xFFD58442;
        int x = leftPos + 8;
        int y = topPos + 21;
        g.fill(x - 2, y - 2, x + 58, y, accent);
        ItemStack icon = new ItemStack(menu.steamTurbineGenerator()
                ? EarthOnMinecraft.STEAM_TURBINE_GENERATOR.get()
                : EarthOnMinecraft.COMBUSTION_GENERATOR.get());
        g.item(icon, x, y + 1);
        String type = Component.translatable(menu.steamTurbineGenerator()
                ? "screen.earth_core.energy.type.steam_turbine"
                : "screen.earth_core.energy.type.combustion").getString();
        g.text(font, EarthGuiSupport.fit(font, type, 38), x + 19, y + 5, accent, false);
    }

    private void drawFuel(GuiGraphicsExtractor g) {
        if (menu.burnTimeTotal() <= 0) return;
        int lit = Math.min(14, 14 * menu.burnTime() / menu.burnTimeTotal());
        if (lit > 0) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, FUEL_SPRITE, 14, 14, 0, 14 - lit,
                    leftPos + 39, topPos + 42 + 14 - lit, 14, lit);
        }
    }

    private void drawStatus(GuiGraphicsExtractor g) {
        int x = leftPos + ENERGY_X;
        int y = topPos + STATUS_Y;
        g.fill(x, y, x + ENERGY_W, y + 11, 0xFF10181D);
        g.fill(x + 1, y + 1, x + ENERGY_W - 1, y + 10, 0xFF29363D);
        int color = statusColor();
        g.fill(x + 3, y + 3, x + 8, y + 8, color);
        String text = EarthGuiSupport.fit(font, statusLine().getString(), ENERGY_W - 15);
        g.text(font, text, x + 11, y + 1, color, false);
    }

    private Component statusLine() {
        if (menu.active()) return Component.translatable("screen.earth_core.machine.running");
        if (menu.energy() >= menu.capacity()) return Component.translatable("screen.earth_core.energy.full");
        return Component.translatable("screen.earth_core.energy.generator_waiting_short");
    }

    private int statusColor() {
        if (menu.active()) return 0xFF38C96B;
        return menu.energy() >= menu.capacity() ? 0xFF7B8C94 : 0xFFE06C3E;
    }

    private void drawTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (isHovering(38, 57, 18, 18, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable(menu.steamTurbineGenerator()
                            ? "screen.earth_core.energy.steam_source.tooltip"
                            : "screen.earth_core.energy.generator_fuel"),
                    Component.translatable(menu.steamTurbineGenerator()
                            ? "screen.earth_core.energy.steam_source.examples"
                            : "screen.earth_core.machine.fuel.examples")), mouseX, mouseY);
        }
        if (isHovering(ENERGY_X, 20, ENERGY_W, 21, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.earth_core.energy.stored", menu.energy(), menu.capacity()),
                    Component.translatable("screen.earth_core.energy.generator_hint"),
                    Component.translatable("screen.earth_core.energy.generator_tooltip",
                            menu.generationPerTick(), menu.transferPerTick())), mouseX, mouseY);
        }
        if (isHovering(ENERGY_X, STATUS_Y, ENERGY_W, 11, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(stateTooltip(), statusLine()), mouseX, mouseY);
        }
    }

    private Component stateTooltip() {
        return Component.translatable(menu.active()
                ? "screen.earth_core.energy.state.running.tooltip"
                : "screen.earth_core.energy.state.idle.tooltip");
    }
}
