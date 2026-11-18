package com.xxsx.earthonminecraft.client;

import com.xxsx.earthonminecraft.BatteryBoxMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class BatteryBoxScreen extends AbstractContainerScreen<BatteryBoxMenu> {
    private static final int imageWidth = 176;
    private static final int imageHeight = 168;
    private static final int ENERGY_X = 20;
    private static final int ENERGY_Y = 31;
    private static final int ENERGY_W = 136;
    private static final int ENERGY_H = 10;
    private static final int INPUT_PORT_X = 24;
    private static final int OUTPUT_PORT_X = 142;
    private static final int PORT_Y = 55;

    public BatteryBoxScreen(BatteryBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, imageWidth, imageHeight);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new IndustrialButton(leftPos + 176, topPos + 12, 20, 20,
                Component.empty(), button -> {
                    this.onClose();
                    EarthOnMinecraftClient.openNotebook();
                }, (g, mouseX, mouseY, hovered) ->
                        EarthGuiSupport.drawSideTab(g, leftPos + 176, topPos + 12,
                                EarthGuiSupport.POWER, hovered)))
                .setTooltip(Tooltip.create(Component.translatable("screen.earth_on_minecraft.button.notebook.tooltip")));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        EarthGuiSupport.drawInventoryArmor(g, leftPos, topPos, imageWidth, imageHeight,
                menu.slots, EarthGuiSupport.POWER);
        EarthGuiSupport.drawEnergyBar(g, leftPos + ENERGY_X, topPos + ENERGY_Y,
                ENERGY_W, ENERGY_H, menu.energy(), menu.capacity());
        drawPorts(g);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        drawTooltips(g, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, EarthGuiSupport.trim(font, title, 140), titleLabelX, titleLabelY, 0xFFE2E8EC, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFB8C4CC, false);
        g.text(font, Component.literal(EarthGuiSupport.compactEnergy(menu.energy(), menu.capacity())),
                ENERGY_X, 20, 0xFFE2E8EC, false);
        String percent = energyPercent(menu.energy(), menu.capacity());
        g.text(font, percent, ENERGY_X + ENERGY_W - font.width(percent), 20, 0xFF79D9E9, false);
        Component flow = Component.translatable("screen.earth_on_minecraft.energy.battery_flow");
        g.text(font, flow, 88 - font.width(flow) / 2, 45, 0xFF9AAEB8, false);
        Component throughput = Component.translatable("screen.earth_on_minecraft.energy.battery_throughput",
                menu.transferLimit());
        g.text(font, EarthGuiSupport.fit(font, throughput.getString(), 96),
                88 - font.width(EarthGuiSupport.fit(font, throughput.getString(), 96)) / 2,
                63, 0xFFE2E8EC, false);
    }

    private void drawPorts(GuiGraphicsExtractor g) {
        drawPort(g, leftPos + INPUT_PORT_X, topPos + PORT_Y, EarthGuiSupport.INPUT, true);
        drawPort(g, leftPos + OUTPUT_PORT_X, topPos + PORT_Y, EarthGuiSupport.OUTPUT, false);
        int lineY = topPos + PORT_Y + 5;
        g.fill(leftPos + INPUT_PORT_X + 10, lineY - 1, leftPos + OUTPUT_PORT_X, lineY + 1, 0xFF4C5E67);
        g.fill(leftPos + 83, lineY - 3, leftPos + 88, lineY + 3, EarthGuiSupport.POWER);
    }

    private void drawPort(GuiGraphicsExtractor g, int x, int y, int color, boolean pointsRight) {
        g.fill(x, y, x + 10, y + 10, 0xFF536168);
        g.fill(x + 1, y + 1, x + 9, y + 9, 0xFF10151A);
        g.fill(x + 2, y + 4, x + 8, y + 6, color);
        if (pointsRight) g.fill(x + 6, y + 2, x + 8, y + 8, color);
        else g.fill(x + 2, y + 2, x + 4, y + 8, color);
    }

    private void drawTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (isHovering(ENERGY_X, 20, ENERGY_W, 22, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.earth_on_minecraft.energy.stored", menu.energy(), menu.capacity()),
                    Component.translatable("screen.earth_on_minecraft.energy.battery_transfer", menu.transferLimit()),
                    Component.translatable("screen.earth_on_minecraft.energy.battery_hint"),
                    Component.translatable("screen.earth_on_minecraft.energy.battery_tooltip")), mouseX, mouseY);
        }
        if (isHovering(INPUT_PORT_X - 2, PORT_Y - 2,
                OUTPUT_PORT_X - INPUT_PORT_X + 14, 14, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.earth_on_minecraft.energy.battery_ports_tooltip"),
                    Component.translatable("screen.earth_on_minecraft.energy.battery_transfer", menu.transferLimit())),
                    mouseX, mouseY);
        }
    }

    private static String energyPercent(int energy, int capacity) {
        return Math.min(100, Math.max(0, energy) * 100 / Math.max(1, capacity)) + "%";
    }
}
