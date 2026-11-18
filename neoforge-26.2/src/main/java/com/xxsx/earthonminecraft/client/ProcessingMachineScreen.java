package com.xxsx.earthonminecraft.client;

import com.xxsx.earthonminecraft.MachineGuiProfile;
import com.xxsx.earthonminecraft.MachineMultiblock;
import com.xxsx.earthonminecraft.ProcessingMachineBlock;
import com.xxsx.earthonminecraft.ProcessingMachineBlockEntity;
import com.xxsx.earthonminecraft.ProcessingMachineMenu;
import com.xxsx.earthonminecraft.RouteGuide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ProcessingMachineScreen extends AbstractContainerScreen<ProcessingMachineMenu> {
    private static final int imageWidth = 176;
    private static final int imageHeight = 188; // this.imageHeight = 188
    private static final Identifier FUEL_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final int TAB_SIZE = 20;
    private static final int REDSTONE_TAB_Y = 12;
    private static final int SIDE_TAB_Y = 34;
    private static final int NOTEBOOK_TAB_Y = 56;
    private static final int SPEC_TAB_Y = 78;
    private static final int ROUTE_X = 74;
    private static final int ROUTE_Y = 21;
    private static final int ROUTE_W = 28;
    private static final int ROUTE_H = 11;
    private static final int CORE_X = 68;
    private static final int CORE_Y = 33;
    private static final int CORE_W = 38;
    private static final int CORE_H = 30;
    private static final int GAUGE_Y = 24;
    private static final int GAUGE_W = 6;
    private static final int GAUGE_H = 52;

    private Button redstoneButton;
    private Button routeButton;
    private Button sideConfigButton;

    public ProcessingMachineScreen(ProcessingMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, imageWidth, imageHeight);
        this.titleLabelX = 26;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 93;
    }

    @Override
    protected void init() {
        super.init();
        redstoneButton = addRenderableWidget(new IndustrialButton(leftPos + 176, topPos + REDSTONE_TAB_Y,
                TAB_SIZE, TAB_SIZE, Component.empty(), b -> cycleRedstoneMode(),
                (g, mouseX, mouseY, hovered) -> {
                    drawTabBase(g, leftPos + 176, topPos + REDSTONE_TAB_Y, palette(), hovered);
                    EarthGuiSupport.drawRedstoneGlyph(g, leftPos + 181, topPos + REDSTONE_TAB_Y + 4,
                            menu.redstoneMode());
                }));
        if (!isMultiblockMachine()) {
            sideConfigButton = addRenderableWidget(new IndustrialButton(leftPos + 176, topPos + SIDE_TAB_Y,
                    TAB_SIZE, TAB_SIZE, Component.empty(), b ->
                            Minecraft.getInstance().setScreen(new MachineSideConfigScreen(menu)),
                    (g, mouseX, mouseY, hovered) -> {
                        drawTabBase(g, leftPos + 176, topPos + SIDE_TAB_Y, palette(), hovered);
                        EarthGuiSupport.drawIoGlyph(g, leftPos + 181, topPos + SIDE_TAB_Y + 5);
                    }));
            sideConfigButton.setTooltip(Tooltip.create(
                    Component.translatable("screen.earth_on_minecraft.side.config.open.tooltip")));
        }
        routeButton = addRenderableWidget(new IndustrialButton(leftPos + ROUTE_X, topPos + ROUTE_Y,
                ROUTE_W, ROUTE_H, Component.empty(), b -> cycleRoute(),
                (g, mouseX, mouseY, hovered) -> drawRouteControl(g, palette(), hovered)));
        addRenderableWidget(new IndustrialButton(leftPos + 176, topPos + NOTEBOOK_TAB_Y,
                TAB_SIZE, TAB_SIZE, Component.empty(), button -> {
                    this.onClose();
                    EarthOnMinecraftClient.openNotebook();
                }, (g, mouseX, mouseY, hovered) -> {
                    drawTabBase(g, leftPos + 176, topPos + NOTEBOOK_TAB_Y, palette(), hovered);
                    EarthGuiSupport.drawNotebookGlyph(g, leftPos + 181, topPos + NOTEBOOK_TAB_Y + 5);
                })).setTooltip(Tooltip.create(
                Component.translatable("screen.earth_on_minecraft.button.notebook.tooltip")));
        syncButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncButtons();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        FamilyPalette palette = palette();
        drawIndustrialPanel(g, palette);
        drawMachineIdentity(g);
        drawGaugeColumn(g, palette);
        drawFuel(g);
        int progressPct = Math.min(100, 100 * menu.progress() / menu.maxProgress());
        drawProcessCoreMotif(g, progressPct);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        drawControlTabs(g, palette());
        drawStatusPill(g);
        drawTooltips(g, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, trimmedTitle(), titleLabelX, titleLabelY, 0xFFE2E8EC, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFB8C4CC, false);
    }

    private FamilyPalette palette() {
        return FamilyPalette.forFamily(menu.processFamily());
    }

    private void drawIndustrialPanel(GuiGraphicsExtractor g, FamilyPalette p) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, p.outerFrame());
        g.fill(leftPos + 2, topPos + 2, leftPos + 174, topPos + 186, p.slotBorder());
        g.fill(leftPos + 4, topPos + 4, leftPos + 172, topPos + 184, p.mainPanel());
        g.fill(leftPos + 6, topPos + 20, leftPos + 170, topPos + 90, p.workBayBg());
        g.fill(leftPos + 6, topPos + 2, leftPos + 170, topPos + 18, p.mainPanel());
        g.fill(leftPos + 7, topPos + 19, leftPos + 169, topPos + 20, p.accent());
        g.fill(leftPos + 7, topPos + 91, leftPos + 169, topPos + 92, p.slotBorder());
        g.fill(leftPos + 7, topPos + 99, leftPos + 169, topPos + 100, p.accent());
        g.fill(leftPos + 7, topPos + 102, leftPos + 169, topPos + 181, p.mainPanel());
        g.fill(leftPos + CORE_X - 2, topPos + CORE_Y - 2,
                leftPos + CORE_X + CORE_W + 2, topPos + CORE_Y + CORE_H + 2, p.outerFrame());
        g.fill(leftPos + CORE_X - 1, topPos + CORE_Y - 1,
                leftPos + CORE_X + CORE_W + 1, topPos + CORE_Y + CORE_H + 1, p.slotBorder());
        g.fill(leftPos + CORE_X, topPos + CORE_Y,
                leftPos + CORE_X + CORE_W, topPos + CORE_Y + CORE_H, 0xFF0B1114);

        drawFlowRails(g, p);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (i < ProcessingMachineBlockEntity.SLOT_COUNT && !slot.isActive()) continue;
            drawSlotBase(g, leftPos + slot.x, topPos + slot.y, p);
            if (i < ProcessingMachineBlockEntity.SLOT_COUNT && slot.getItem().isEmpty()) {
                drawSlotGhost(g, i, leftPos + slot.x, topPos + slot.y, p);
            }
        }
        Slot primaryOutput = menu.getSlot(ProcessingMachineBlockEntity.SLOT_OUTPUT_START);
        if (primaryOutput.isActive()) {
            drawPrimaryOutputFrame(g, leftPos + primaryOutput.x, topPos + primaryOutput.y, p);
        }
        for (int x = 8; x < 169; x += 4) {
            g.fill(leftPos + x, topPos + 96, leftPos + x + 2, topPos + 97, p.slotBorder());
        }
    }

    private static void drawSlotBase(GuiGraphicsExtractor g, int x, int y, FamilyPalette p) {
        g.fill(x - 1, y - 1, x + 17, y + 17, p.outerFrame());
        g.fill(x, y, x + 16, y + 16, p.slotBorder());
        g.fill(x + 1, y + 1, x + 15, y + 15, 0xFF172025);
        g.fill(x + 1, y + 1, x + 15, y + 2, 0xFF29343A);
    }

    private static void drawPrimaryOutputFrame(GuiGraphicsExtractor g, int x, int y, FamilyPalette p) {
        g.fill(x - 2, y - 2, x + 18, y, p.accent());
        g.fill(x - 2, y + 16, x + 18, y + 18, p.accent());
        g.fill(x - 2, y, x, y + 16, p.accent());
        g.fill(x + 16, y, x + 18, y + 16, p.accent());
        for (int[] corner : new int[][] {{-2, -2}, {16, -2}, {-2, 16}, {16, 16}}) {
            g.fill(x + corner[0], y + corner[1], x + corner[0] + 2, y + corner[1] + 2, p.textLight());
        }
    }

    private static void drawSlotGhost(GuiGraphicsExtractor g, int slot, int x, int y, FamilyPalette p) {
        int color = p.accent() & 0x00FFFFFF | 0x66000000;
        if (slot == ProcessingMachineBlockEntity.SLOT_INPUT) {
            g.fill(x + 3, y + 3, x + 13, y + 5, color);
            g.fill(x + 4, y + 5, x + 6, y + 9, color);
            g.fill(x + 10, y + 5, x + 12, y + 9, color);
            g.fill(x + 6, y + 9, x + 10, y + 12, color);
            g.fill(x + 6, y + 12, x + 7, y + 14, color);
            g.fill(x + 9, y + 12, x + 10, y + 14, color);
        } else if (slot >= ProcessingMachineBlockEntity.SLOT_SECONDARY_START
                && slot < ProcessingMachineBlockEntity.SLOT_SECONDARY_START + 4) {
            g.fill(x + 7, y + 3, x + 9, y + 13, color);
            g.fill(x + 3, y + 7, x + 13, y + 9, color);
            g.fill(x + 10, y + 10, x + 13, y + 13, color);
            g.fill(x + 11, y + 9, x + 12, y + 14, color);
        } else if (slot == ProcessingMachineBlockEntity.SLOT_REAGENT) {
            g.fill(x + 6, y + 2, x + 10, y + 4, color);
            g.fill(x + 7, y + 4, x + 9, y + 8, color);
            g.fill(x + 4, y + 10, x + 12, y + 14, color);
            g.fill(x + 5, y + 8, x + 6, y + 12, color);
            g.fill(x + 10, y + 8, x + 11, y + 12, color);
            g.fill(x + 5, y + 10, x + 11, y + 11, color);
        } else if (slot == ProcessingMachineBlockEntity.SLOT_OUTPUT_START) {
            g.fill(x + 3, y + 3, x + 6, y + 4, color);
            g.fill(x + 3, y + 3, x + 4, y + 6, color);
            g.fill(x + 10, y + 3, x + 13, y + 4, color);
            g.fill(x + 12, y + 3, x + 13, y + 6, color);
            g.fill(x + 3, y + 12, x + 6, y + 13, color);
            g.fill(x + 3, y + 10, x + 4, y + 13, color);
            g.fill(x + 10, y + 12, x + 13, y + 13, color);
            g.fill(x + 12, y + 10, x + 13, y + 13, color);
            g.fill(x + 7, y + 7, x + 9, y + 9, color);
        }
    }

    private void drawFlowRails(GuiGraphicsExtractor g, FamilyPalette p) {
        int railY = topPos + 48;
        int flow = menu.active() ? p.accent() : p.slotBorder();
        int leftRailStart = menu.guiProfile().secondarySlotCount() > 0 ? 65 : 55;
        int rightRailEnd = menu.guiProfile().visibleOutputSlots() > 4 ? 110 : 114;
        g.fill(leftPos + leftRailStart, railY - 2, leftPos + CORE_X - 2, railY + 2, p.outerFrame());
        g.fill(leftPos + leftRailStart + 1, railY - 1, leftPos + CORE_X - 2, railY + 1, p.slotBorder());
        g.fill(leftPos + CORE_X + CORE_W + 2, railY - 2, leftPos + rightRailEnd, railY + 2, p.outerFrame());
        g.fill(leftPos + CORE_X + CORE_W + 2, railY - 1, leftPos + rightRailEnd - 1, railY + 1, p.slotBorder());
        if (menu.active()) {
            int phase = (menu.progress() / 3) % 6;
            for (int x = leftRailStart + 2 + phase; x < CORE_X - 3; x += 6) {
                g.fill(leftPos + x, railY - 1, leftPos + x + 2, railY + 1, flow);
            }
            for (int x = CORE_X + CORE_W + 3 + phase; x < rightRailEnd - 2; x += 6) {
                g.fill(leftPos + x, railY - 1, leftPos + x + 2, railY + 1, flow);
            }
        }
    }

    private void drawMachineIdentity(GuiGraphicsExtractor g) {
        Item machineItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(
                "earth_on_minecraft", menu.kind().blockId()));
        ItemStack icon = new ItemStack(machineItem);
        if (!icon.isEmpty()) {
            g.item(icon, leftPos + 8, topPos + 3);
        }
    }

    private boolean showFluidGauge() {
        return menu.guiProfile().usesFluidGauge() || menu.tankAmount() > 0 || menu.outputTankAmount() > 0;
    }

    private int gaugeCount() {
        int count = 0;
        if (menu.guiProfile().usesEnergyGauge()) count++;
        if (menu.guiProfile().usesTempGauge()) count++;
        if (showFluidGauge()) count++;
        return count;
    }

    private static int gaugeX(int index, int count) {
        if (count >= 3) return 8 + index * 7;
        return 10 + index * 8;
    }

    private void drawGaugeColumn(GuiGraphicsExtractor g, FamilyPalette p) {
        int count = gaugeCount();
        int index = 0;
        MachineGuiProfile profile = menu.guiProfile();
        if (profile.usesEnergyGauge()) {
            drawEnergyGauge(g, leftPos + gaugeX(index++, count), topPos + GAUGE_Y, p);
        }
        if (profile.usesTempGauge()) {
            drawTemperatureGauge(g, leftPos + gaugeX(index++, count), topPos + GAUGE_Y, p);
        }
        if (showFluidGauge()) {
            drawTank(g, leftPos + gaugeX(index, count), topPos + GAUGE_Y, p);
        }
    }

    private static void drawGaugeFrame(GuiGraphicsExtractor g, int x, int y, FamilyPalette p) {
        g.fill(x - 1, y - 1, x + GAUGE_W + 1, y + GAUGE_H + 1, p.outerFrame());
        g.fill(x, y, x + GAUGE_W, y + GAUGE_H, p.slotBorder());
        g.fill(x + 1, y + 1, x + GAUGE_W - 1, y + GAUGE_H - 1, 0xFF0A1115);
    }

    private void drawEnergyGauge(GuiGraphicsExtractor g, int x, int y, FamilyPalette p) {
        drawGaugeFrame(g, x, y, p);
        int innerH = GAUGE_H - 2;
        int filled = 0;
        int color = 0xFF39B7D4;
        if (menu.powerMode().grid()) {
            filled = innerH * menu.energyStored() / Math.max(1, menu.energyCapacity());
        } else if (menu.powerMode().manualFallback()) {
            filled = menu.driveState() == ProcessingMachineBlockEntity.DriveState.KINETIC_ACTIVE
                    ? innerH : innerH * Math.min(160, menu.manualWorkTicks()) / 160;
            color = 0xFF7DBB68;
        } else if (menu.hasBurningFuel()) {
            filled = innerH * menu.burnTime() / Math.max(1, menu.burnTimeTotal());
            color = 0xFFE18A36;
        }
        filled = Math.min(innerH, filled);
        if (filled > 0) {
            g.fill(x + 1, y + GAUGE_H - 1 - filled, x + GAUGE_W - 1, y + GAUGE_H - 1, color);
            g.fill(x + 1, y + GAUGE_H - 1 - filled, x + 2, y + GAUGE_H - 1, 0xFF9ADCE8);
        }
        for (int i = 1; i <= 4; i++) {
            int tickY = y + i * GAUGE_H / 5;
            g.fill(x, tickY, x + GAUGE_W, tickY + 1, p.slotBorder());
        }
    }

    private void drawTemperatureGauge(GuiGraphicsExtractor g, int x, int y, FamilyPalette p) {
        drawGaugeFrame(g, x, y, p);
        int ambient = ProcessingMachineBlockEntity.AMBIENT_TEMP_K;
        int maxTemp = Math.max(ambient + 100, Math.max(menu.maxTemperatureK(), menu.targetTemperatureK()));
        int current = Math.max(ambient, menu.temperatureK());
        int innerH = GAUGE_H - 5;
        int filled = Math.min(innerH, Math.max(1,
                innerH * (current - ambient) / Math.max(1, maxTemp - ambient)));
        int barColor = current >= menu.targetTemperatureK() && menu.targetTemperatureK() > ambient
                ? 0xFFE86A24 : current > ambient + 30 ? 0xFFD99B26 : 0xFF4B779D;
        g.fill(x + 2, y + GAUGE_H - 4 - filled, x + 4, y + GAUGE_H - 4, barColor);
        g.fill(x + 1, y + GAUGE_H - 4, x + 5, y + GAUGE_H - 1, barColor);
        int target = menu.targetTemperatureK();
        if (target > ambient) {
            int targetOffset = Math.min(innerH - 1, Math.max(1,
                    innerH * (target - ambient) / Math.max(1, maxTemp - ambient)));
            int markerY = y + GAUGE_H - 4 - targetOffset;
            g.fill(x, markerY, x + GAUGE_W, markerY + 1, 0xFFFFE082);
        }
    }

    private void drawTank(GuiGraphicsExtractor g, int x, int y, FamilyPalette p) {
        drawGaugeFrame(g, x, y, p);
        int capacity = menu.tankCapacity();
        int innerH = GAUGE_H - 2;
        int inFilled = Math.min(innerH, innerH * menu.tankAmount() / capacity);
        int outFilled = Math.min(innerH, innerH * menu.outputTankAmount() / capacity);
        if (inFilled > 0) {
            g.fill(x + 1, y + GAUGE_H - 1 - inFilled, x + 3, y + GAUGE_H - 1, 0xFF3E8FD0);
        }
        if (outFilled > 0) {
            g.fill(x + 3, y + GAUGE_H - 1 - outFilled, x + 5, y + GAUGE_H - 1, 0xFFD9822B);
        }
        g.fill(x + 2, y + 2, x + 3, y + GAUGE_H - 2, 0xFF27424F);
    }

    private void drawFuel(GuiGraphicsExtractor g) {
        if (!menu.guiProfile().usesFuelOrDriveSlot()) return;
        Slot fuelSlot = menu.getSlot(ProcessingMachineBlockEntity.SLOT_FUEL);
        int sx = leftPos + fuelSlot.x;
        int sy = topPos + fuelSlot.y;
        if (fuelSlot.getItem().isEmpty()) {
            if (menu.acceptsLocalFuel()) {
                g.fill(sx + 6, sy + 4, sx + 10, sy + 12, 0xFF654333);
                g.fill(sx + 4, sy + 8, sx + 12, sy + 13, 0xFF654333);
                g.fill(sx + 7, sy + 2, sx + 9, sy + 7, 0xFF8B5938);
            } else {
                drawDriveGlyph(g, sx + 3, sy + 3, 0xFF526068);
            }
        }
        if (menu.acceptsLocalFuel() && menu.burnTimeTotal() > 0) {
            int flameHeight = menu.guiProfile().secondarySlotCount() == 1 ? 10 : 14;
            int lit = Math.min(flameHeight, flameHeight * menu.burnTime() / menu.burnTimeTotal());
            if (lit > 0) {
                int flameY = menu.guiProfile().secondarySlotCount() >= 2 ? sy + 1 : sy - flameHeight - 1;
                int flameX = menu.guiProfile().secondarySlotCount() == 1 ? sx + 18 : sx + 1;
                int flameWidth = menu.guiProfile().secondarySlotCount() == 1 ? 2 : 14;
                g.blitSprite(RenderPipelines.GUI_TEXTURED, FUEL_SPRITE, 14, 14, 0, 14 - lit,
                        flameX, flameY + flameHeight - lit, flameWidth, lit);
            }
        }
    }

    private static void drawDriveGlyph(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x + 3, y, x + 7, y + 10, color);
        g.fill(x, y + 3, x + 10, y + 7, color);
        g.fill(x + 2, y + 2, x + 8, y + 8, color);
        g.fill(x + 4, y + 4, x + 6, y + 6, 0xFF172025);
    }

    private void drawProcessCoreMotif(GuiGraphicsExtractor g, int progressPct) {
        int x = leftPos + CORE_X;
        int y = topPos + CORE_Y;
        int p = Math.max(0, Math.min(100, progressPct));
        int accent = palette().accent();
        int lit = Math.max(1, p * 26 / 100);
        switch (menu.processFamily()) {
            case COMMINUTION -> {
                g.fill(x + 2, y + 4, x + 12, y + 7, 0xFF7D6A56);
                g.fill(x + 5, y + 7, x + 12, y + 11, 0xFF7D6A56);
                g.fill(x + 8, y + 11, x + 12, y + 16, 0xFF7D6A56);
                g.fill(x + 26, y + 4, x + 36, y + 7, 0xFF7D6A56);
                g.fill(x + 26, y + 7, x + 33, y + 11, 0xFF7D6A56);
                g.fill(x + 26, y + 11, x + 30, y + 16, 0xFF7D6A56);
                for (int i = 0; i < 5; i++) {
                    int py = y + 6 + (i * 5 + p / 8) % 17;
                    g.fill(x + 17 + (i % 2) * 3, py, x + 19 + (i % 2) * 3, py + 2, accent);
                }
                g.fill(x + 10, y + 25, x + 28, y + 27, 0xFF5B5147);
                g.fill(x + 17, y + 21, x + 21, y + 27, accent);
            }
            case CLASSIFICATION -> {
                for (int layer = 0; layer < 2; layer++) {
                    int sy = y + 7 + layer * 10;
                    g.fill(x + 4, sy + 3, x + 34, sy + 5, 0xFF698493);
                    for (int i = 0; i < 8; i++) {
                        g.fill(x + 5 + i * 4, sy + i % 2, x + 6 + i * 4, sy + 5, 0xFF9BB3BD);
                    }
                }
                g.fill(x + 17, y + 3, x + 21, y + 25, 0xFF324D5C);
                g.fill(x + 5, y + 27, x + 5 + lit, y + 29, accent);
                g.fill(x + 33, y + 18, x + 35, y + 27, 0xFFB9D2DE);
            }
            case WET_PROCESS -> {
                g.fill(x + 3, y + 5, x + 6, y + 26, 0xFF477F78);
                g.fill(x + 32, y + 5, x + 35, y + 26, 0xFF477F78);
                g.fill(x + 3, y + 23, x + 35, y + 27, 0xFF477F78);
                g.fill(x + 6, y + 14, x + 32, y + 23, 0xFF226C82);
                g.fill(x + 6, y + 14, x + 32, y + 16, 0xFF55C3C4);
                for (int i = 0; i < 6; i++) {
                    int by = y + 20 - (i * 4 + p / 7) % 15;
                    g.fill(x + 8 + i * 4, by, x + 10 + i * 4, by + 2, 0xFFA1E2D7);
                }
            }
            case THERMAL -> {
                g.fill(x + 3, y + 7, x + 35, y + 27, 0xFF68382C);
                g.fill(x + 7, y + 3, x + 31, y + 7, 0xFF8B4B36);
                g.fill(x + 6, y + 8, x + 32, y + 25, 0xFF251716);
                int melt = 2 + p * 7 / 100;
                g.fill(x + 8, y + 23 - melt, x + 30, y + 23, 0xFFD56B2D);
                g.fill(x + 8, y + 23 - melt, x + 30, y + 24 - melt, 0xFFFFC45B);
                for (int i = 0; i < 6; i++) {
                    int fh = 3 + (i + p / 9) % 6;
                    g.fill(x + 8 + i * 4, y + 26 - fh, x + 10 + i * 4, y + 27, 0xFFFF7A2E);
                }
            }
            case ELECTROCHEMICAL -> {
                g.fill(x + 4, y + 4, x + 10, y + 25, 0xFF9D6548);
                g.fill(x + 28, y + 4, x + 34, y + 25, 0xFF9D6548);
                g.fill(x + 10, y + 13, x + 28, y + 25, 0xFF174E68);
                for (int i = 0; i < 6; i++) {
                    int ex = x + 11 + i * 3;
                    int ey = y + 8 + (i * 3 + p / 8) % 10;
                    g.fill(ex, ey, ex + 2, ey + 2, i % 2 == 0 ? 0xFFA4F3EF : 0xFF7D8FE4);
                }
                g.fill(x + 12, y + 26, x + 12 + lit / 2, y + 28, accent);
            }
            case FORMING -> {
                g.fill(x + 14, y + 2, x + 24, y + 6, 0xFF8A7D96);
                int headY = y + 6 + p * 9 / 100;
                g.fill(x + 9, headY, x + 29, headY + 5, 0xFF756A80);
                g.fill(x + 5, y + 24, x + 33, y + 28, 0xFF55505F);
                g.fill(x + 10, y + 21, x + 28, y + 24, 0xFF8E8498);
                g.fill(x + 14, y + 19, x + 24, y + 22, p > 70 ? accent : 0xFFB65F4B);
            }
            case REACTION -> {
                g.fill(x + 5, y + 5, x + 33, y + 27, 0xFF785B3E);
                g.fill(x + 8, y + 7, x + 30, y + 24, 0xFF251F1B);
                for (int cy = y + 8; cy < y + 23; cy += 5) {
                    g.fill(x + 4, cy, x + 8, cy + 2, 0xFFC28A4B);
                    g.fill(x + 30, cy, x + 34, cy + 2, 0xFFC28A4B);
                }
                int level = 4 + p * 10 / 100;
                g.fill(x + 9, y + 23 - level, x + 29, y + 23, 0xFF9B682E);
                g.fill(x + 9, y + 23 - level, x + 29, y + 24 - level, 0xFFE0AE5C);
                g.fill(x + 1, y + 4, x + 14, y + 6, 0xFF72919A);
                g.fill(x + 24, y + 4, x + 37, y + 6, 0xFF72919A);
            }
            case COLUMN -> {
                g.fill(x + 12, y + 2, x + 26, y + 28, 0xFF35515A);
                g.fill(x + 14, y + 3, x + 24, y + 27, 0xFF15282E);
                for (int tray = 0; tray < 3; tray++) {
                    int ty = y + 20 - tray * 7;
                    int color = p >= (tray + 1) * 25 ? accent : 0xFF527680;
                    g.fill(x + 10, ty, x + 28, ty + 2, color);
                    g.fill(x + 14 + tray * 3, ty - 3, x + 18 + tray * 3, ty, color);
                }
                g.fill(x + 18, y + 1, x + 20, y + 4, 0xFFA2C4C9);
            }
            case MIXING -> {
                g.fill(x + 4, y + 6, x + 34, y + 27, 0xFF4F5C38);
                g.fill(x + 7, y + 9, x + 31, y + 24, 0xFF293326);
                g.fill(x + 18, y + 3, x + 20, y + 23, 0xFFA1AB83);
                int offset = (p / 10) % 5;
                g.fill(x + 8 + offset, y + 12, x + 19, y + 15, 0xFF87945D);
                g.fill(x + 19, y + 18, x + 30 - offset, y + 21, 0xFF87945D);
                g.fill(x + 7, y + 25, x + 7 + lit, y + 27, accent);
            }
            case CRYSTALLIZATION -> {
                g.fill(x + 3, y + 20, x + 35, y + 27, 0xFF49546F);
                g.fill(x + 6, y + 17, x + 32, y + 22, 0xFF343E64);
                int growth = 2 + p * 14 / 100;
                g.fill(x + 17, y + 20 - growth, x + 21, y + 21, 0xFFA6A4E5);
                g.fill(x + 14, y + 17 - growth / 2, x + 18, y + 21, 0xFF777FC4);
                g.fill(x + 21, y + 15 - growth / 2, x + 25, y + 21, 0xFF8D8BD4);
                g.fill(x + 18, y + 18 - growth, x + 20, y + 20 - growth, 0xFFE0D9FF);
                for (int i = 0; i < 4; i++) {
                    g.fill(x + 7 + i * 7, y + 24, x + 10 + i * 7, y + 26, accent);
                }
            }
        }
    }

    private void drawControlTabs(GuiGraphicsExtractor g, FamilyPalette p) {
        drawTabBase(g, leftPos + 176, topPos + SPEC_TAB_Y, p, false);
        drawBlueprintGlyph(g, leftPos + 181, topPos + SPEC_TAB_Y + 4, p.accent());
    }

    private void drawRouteControl(GuiGraphicsExtractor g, FamilyPalette p, boolean hovered) {
        if (menu.routeCount() <= 1) return;
        int x = leftPos + ROUTE_X;
        int y = topPos + ROUTE_Y;
        g.fill(x, y, x + ROUTE_W, y + ROUTE_H, hovered ? p.accent() : p.outerFrame());
        g.fill(x + 1, y + 1, x + ROUTE_W - 1, y + ROUTE_H - 1, p.slotBorder());
        g.fill(x + 2, y + 2, x + ROUTE_W - 2, y + ROUTE_H - 2, p.mainPanel());
        String route = routeButtonLabel().getString();
        g.text(font, route, x + (ROUTE_W - font.width(route)) / 2, y + 2, p.textLight(), false);
    }

    private static void drawTabBase(GuiGraphicsExtractor g, int x, int y, FamilyPalette p, boolean hovered) {
        g.fill(x - 2, y + 3, x + 2, y + TAB_SIZE - 3, p.slotBorder());
        g.fill(x, y, x + TAB_SIZE, y + TAB_SIZE, hovered ? p.accent() : p.outerFrame());
        g.fill(x + 1, y + 1, x + TAB_SIZE - 1, y + TAB_SIZE - 1, p.slotBorder());
        g.fill(x + 2, y + 2, x + TAB_SIZE - 2, y + TAB_SIZE - 2, p.mainPanel());
        g.fill(x + TAB_SIZE - 2, y + 3, x + TAB_SIZE - 1, y + TAB_SIZE - 3, p.accent());
    }

    private static void drawBlueprintGlyph(GuiGraphicsExtractor g, int x, int y, int accent) {
        g.fill(x, y, x + 11, y + 12, 0xFF284D65);
        g.fill(x + 2, y + 2, x + 9, y + 3, 0xFFA8D4E1);
        g.fill(x + 2, y + 5, x + 5, y + 9, 0xFFA8D4E1);
        g.fill(x + 6, y + 6, x + 9, y + 10, accent);
        g.fill(x + 3, y + 7, x + 4, y + 8, 0xFF284D65);
    }

    private void drawStatusPill(GuiGraphicsExtractor g) {
        int x = leftPos + 112;
        int y = topPos + 4;
        int color = statusColor();
        g.fill(x, y, x + 58, y + 12, 0xFF0B1114);
        g.fill(x + 1, y + 1, x + 57, y + 11, 0xFF273238);
        g.fill(x + 2, y + 2, x + 56, y + 10, 0xFF11191D);
        int pulse = menu.active() && (menu.progress() / 5) % 2 == 0 ? 0xFF8AF0A5 : color;
        g.fill(x + 4, y + 4, x + 9, y + 9, pulse);
        g.fill(x + 5, y + 3, x + 8, y + 4, 0x66FFFFFF);
        g.text(font, EarthGuiSupport.fit(font, statusLine(), 46), x + 12, y + 2, color, false);
    }

    private int statusColor() {
        if (!menu.structureValid()) return 0xFFE06C3E;
        ItemStack input = menu.getSlot(ProcessingMachineBlockEntity.SLOT_INPUT).getItem();
        if (!input.isEmpty() && !ProcessingMachineBlock.acceptsInput(menu.kind(), input)) return 0xFFE06C3E;
        if ((!input.isEmpty() || menu.selectedRecipe().isPresent())
                && (redstonePaused() || menu.outputsBlocked() || menu.fluidOutputsBlocked()
                || menu.secondaryInputsMissing() || menu.fluidInputsMissing() || menu.catalystMissing()
                || menu.selectedRecipe().map(recipe -> recipe.requiresCoke() && !hasCokeCharge()).orElse(false))) {
            return 0xFFE06C3E;
        }
        if (menu.active()) return 0xFF38C96B;
        ProcessingMachineBlockEntity.DriveState ds = menu.driveState();
        if (ds == ProcessingMachineBlockEntity.DriveState.FUEL_TEMP_LOW
                || ds == ProcessingMachineBlockEntity.DriveState.MANUAL_WAIT
                || ds == ProcessingMachineBlockEntity.DriveState.KINETIC_WAIT) return 0xFFE06C3E;
        if (ds == ProcessingMachineBlockEntity.DriveState.HEATING) return 0xFFE0A13E;
        if ((!input.isEmpty() || menu.selectedRecipe().isPresent()) && menu.needsExternalPower()
                && !menu.gridPowered() && menu.energyStored() < Math.max(1, menu.energyPerTick())
                && !menu.hasBurningFuel() && !fuelSlotHasFuel()) return 0xFFE06C3E;
        return 0xFF7B8C94;
    }

    private String statusLine() {
        if (!menu.structureValid()) {
            if (menu.faultExpectedRole().isPresent()) {
                return Component.translatable("screen.earth_on_minecraft.machine.structure_fault_short",
                        String.format("(%+d,%+d,%+d)", menu.faultDx(), menu.faultDy(), menu.faultDz())).getString();
            }
            return localized("screen.earth_on_minecraft.machine.structure_missing_short");
        }
        ItemStack input = menu.getSlot(ProcessingMachineBlockEntity.SLOT_INPUT).getItem();
        if (input.isEmpty() && menu.selectedRecipe().isEmpty()) {
            return localized("screen.earth_on_minecraft.machine.empty_input_short");
        }
        return menu.selectedRecipe().map(recipe -> {
            if (redstonePaused()) return localized("screen.earth_on_minecraft.machine.redstone_paused_short");
            if (recipe.requiresCoke() && !hasCokeCharge()) {
                return localized("screen.earth_on_minecraft.machine.missing_reductant_short");
            }
            if (menu.catalystMissing()) return localized("screen.earth_on_minecraft.machine.missing_catalyst_short");
            if (menu.secondaryInputsMissing()) return localized("screen.earth_on_minecraft.machine.missing_secondary_short");
            if (menu.fluidInputsMissing()) return localized("screen.earth_on_minecraft.machine.missing_fluid_short");
            if (menu.outputsBlocked() || menu.fluidOutputsBlocked()) {
                return localized("screen.earth_on_minecraft.machine.outputs_full_short");
            }
            ProcessingMachineBlockEntity.DriveState ds = menu.driveState();
            if (ds == ProcessingMachineBlockEntity.DriveState.FUEL_TEMP_LOW) {
                return localized("screen.earth_on_minecraft.machine.fuel_temp_low_short");
            }
            if (ds == ProcessingMachineBlockEntity.DriveState.HEATING) {
                return Component.translatable("screen.earth_on_minecraft.machine.heating_short",
                        menu.temperatureK(), menu.targetTemperatureK()).getString();
            }
            if (ds == ProcessingMachineBlockEntity.DriveState.MANUAL_WAIT) {
                return localized("screen.earth_on_minecraft.machine.manual_wait_short");
            }
            if (ds == ProcessingMachineBlockEntity.DriveState.KINETIC_WAIT) {
                return localized("screen.earth_on_minecraft.machine.kinetic_wait_short");
            }
            if (menu.active()) return localized("screen.earth_on_minecraft.machine.running");
            if (menu.needsExternalPower() && !menu.gridPowered()
                    && menu.energyStored() < Math.max(1, menu.energyPerTick())
                    && !menu.hasBurningFuel() && !fuelSlotHasFuel()
                    && menu.temperatureK() < menu.targetTemperatureK()) {
                return localized(menu.acceptsLocalFuel()
                        ? "screen.earth_on_minecraft.machine.missing_power_short"
                        : "screen.earth_on_minecraft.machine.missing_grid_power_short");
            }
            return localized("screen.earth_on_minecraft.machine.recipe_ready");
        }).orElseGet(() -> localized(input.isEmpty()
                ? "screen.earth_on_minecraft.machine.empty_input_short"
                : "screen.earth_on_minecraft.machine.unsupported_input_short"));
    }

    private void drawTooltips(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (isHovering(176, REDSTONE_TAB_Y, TAB_SIZE, TAB_SIZE, mouseX, mouseY)) {
            ProcessingMachineBlockEntity.RedstoneMode redstone = menu.redstoneMode();
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.earth_on_minecraft.redstone.current",
                            Component.translatable(redstone.labelKey())),
                    Component.translatable(redstone.descriptionKey()),
                    Component.translatable("screen.earth_on_minecraft.redstone.tooltip")), mouseX, mouseY);
        }
        int count = gaugeCount();
        int index = 0;
        if (menu.guiProfile().usesEnergyGauge()) {
            int x = gaugeX(index++, count);
            if (isHovering(x - 1, GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2, mouseX, mouseY)) {
                g.setComponentTooltipForNextFrame(font, powerTooltip(), mouseX, mouseY);
            }
        }
        if (menu.guiProfile().usesTempGauge()) {
            int x = gaugeX(index++, count);
            if (isHovering(x - 1, GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2, mouseX, mouseY)) {
                g.setComponentTooltipForNextFrame(font, temperatureTooltip(), mouseX, mouseY);
            }
        }
        if (showFluidGauge()) {
            int x = gaugeX(index, count);
            if (isHovering(x - 1, GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2, mouseX, mouseY)) {
                g.setComponentTooltipForNextFrame(font, List.of(
                        Component.translatable("screen.earth_on_minecraft.machine.tank",
                                menu.tankAmount(), menu.tankCapacity()),
                        Component.translatable("screen.earth_on_minecraft.machine.output_tank",
                                menu.outputTankAmount(), menu.tankCapacity()),
                        Component.translatable("screen.earth_on_minecraft.machine.tank.tooltip")), mouseX, mouseY);
            }
        }
        if (isHovering(78, 64, 20, 20, mouseX, mouseY) && menu.guiProfile().usesReagentSlot()) {
            if (menu.kind() == ProcessingMachineBlock.Kind.REDUCTION_FURNACE) {
                g.setComponentTooltipForNextFrame(font,
                        List.of(Component.translatable("screen.earth_on_minecraft.machine.reductant.tooltip")), mouseX, mouseY);
            } else if (menu.selectedRecipe().map(ProcessingMachineBlock.Recipe::hasCatalyst).orElse(false)) {
                ItemStack cat = menu.selectedRecipe().get().catalystStack();
                g.setComponentTooltipForNextFrame(font,
                        List.of(Component.translatable("screen.earth_on_minecraft.machine.catalyst.tooltip", cat.getItemName())),
                        mouseX, mouseY);
            }
        }
        if (isHovering(112, 4, 58, 12, mouseX, mouseY)
                || isHovering(CORE_X - 2, CORE_Y - 2, CORE_W + 4, CORE_H + 4, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, operatingTooltip(), mouseX, mouseY);
        }
        if (isHovering(176, SPEC_TAB_Y, TAB_SIZE, TAB_SIZE, mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>(machineSpecTooltip());
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.temp.readout",
                    menu.temperatureK(), menu.targetTemperatureK(), menu.maxTemperatureK()));
            tooltip.addAll(powerTooltip());
            g.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
        if (menu.routeCount() > 1 && isHovering(ROUTE_X, ROUTE_Y, ROUTE_W, ROUTE_H, mouseX, mouseY)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("screen.earth_on_minecraft.machine.route.current",
                            menu.selectedRouteIndex() + 1, menu.routeCount()),
                    menu.selectedRecipe().map(recipe -> Component.literal(recipe.note()))
                            .orElse(Component.translatable("screen.earth_on_minecraft.machine.empty_input")),
                    Component.translatable("screen.earth_on_minecraft.machine.route.tooltip")), mouseX, mouseY);
        }
    }

    private List<Component> operatingTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(stateTooltip());
        tooltip.add(Component.literal(detailedStatusLine()));
        tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.temp.readout",
                menu.temperatureK(), menu.targetTemperatureK(), menu.maxTemperatureK()));
        if (menu.requiredPressureKpa() > 101) {
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.pressure.readout",
                    menu.requiredPressureKpa()));
        }
        menu.selectedRecipe().ifPresent(recipe -> {
            if (recipe.hasEquation()) tooltip.add(Component.literal(recipe.equation()));
            if (recipe.hasCatalyst()) {
                tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.catalyst.tooltip",
                        recipe.catalystStack().getItemName()));
            }
        });
        if (menu.powerMode().grid()) {
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.eou.buffer",
                    menu.energyStored(), menu.energyCapacity(), menu.energyPerTick()));
        }
        tooltip.add(Component.translatable(menu.driveState().labelKey()));
        tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.spec",
                secondsPerOperation(), menu.energyPerTick()));
        tooltip.add(Component.translatable(menu.powerMode().labelKey()));
        if (!menu.structureValid()) {
            MachineMultiblock.Pattern pattern = MachineMultiblock.patternFor(menu.kind());
            tooltip.add(Component.translatable(pattern.screenKey()));
            appendStructureFaultTooltip(tooltip);
        }
        return tooltip;
    }

    private String detailedStatusLine() {
        if (!menu.structureValid()) {
            if (menu.faultExpectedRole().isPresent()) {
                BlockPos faultPos = menu.pos().offset(menu.faultDx(), menu.faultDy(), menu.faultDz());
                String coords = faultPos.getX() + ", " + faultPos.getY() + ", " + faultPos.getZ();
                Component roleName = Component.translatable(menu.faultExpectedRole().get().labelKey());
                return Component.translatable("screen.earth_on_minecraft.machine.structure_fault_coord", coords,
                        String.format("(%+d, %+d, %+d)", menu.faultDx(), menu.faultDy(), menu.faultDz()),
                        roleName).getString();
            }
            return localized("screen.earth_on_minecraft.machine.structure_missing");
        }
        ItemStack input = menu.getSlot(ProcessingMachineBlockEntity.SLOT_INPUT).getItem();
        if (input.isEmpty() && menu.selectedRecipe().isEmpty()) {
            return localized(menu.acceptsLocalFuel()
                    ? "screen.earth_on_minecraft.machine.empty_input_fuel"
                    : "screen.earth_on_minecraft.machine.empty_input_grid");
        }
        return menu.selectedRecipe().map(recipe -> {
            if (redstonePaused()) return localized("screen.earth_on_minecraft.machine.redstone_paused");
            if (recipe.requiresCoke() && !hasCokeCharge()) {
                return localized("screen.earth_on_minecraft.machine.missing_reductant");
            }
            if (menu.catalystMissing()) {
                return Component.translatable("screen.earth_on_minecraft.machine.missing_catalyst",
                        recipe.catalystStack().getItemName()).getString();
            }
            if (menu.secondaryInputsMissing()) return localized("screen.earth_on_minecraft.machine.missing_secondary");
            if (menu.fluidInputsMissing()) return localized("screen.earth_on_minecraft.machine.missing_fluid");
            if (menu.outputsBlocked() || menu.fluidOutputsBlocked()) {
                return localized("screen.earth_on_minecraft.machine.outputs_full");
            }
            ProcessingMachineBlockEntity.DriveState ds = menu.driveState();
            if (ds == ProcessingMachineBlockEntity.DriveState.FUEL_TEMP_LOW) {
                return Component.translatable("screen.earth_on_minecraft.machine.fuel_temp_low",
                        menu.maxTemperatureK(), menu.targetTemperatureK()).getString();
            }
            if (ds == ProcessingMachineBlockEntity.DriveState.HEATING) {
                return Component.translatable("screen.earth_on_minecraft.machine.heating",
                        menu.temperatureK(), menu.targetTemperatureK()).getString();
            }
            if (ds == ProcessingMachineBlockEntity.DriveState.MANUAL_WAIT) {
                return localized("screen.earth_on_minecraft.machine.manual_wait");
            }
            if (ds == ProcessingMachineBlockEntity.DriveState.KINETIC_WAIT) {
                return localized("screen.earth_on_minecraft.machine.kinetic_wait");
            }
            if (menu.needsExternalPower() && !menu.gridPowered()
                    && menu.energyStored() < Math.max(1, menu.energyPerTick())
                    && !menu.hasBurningFuel() && !fuelSlotHasFuel()
                    && menu.temperatureK() < menu.targetTemperatureK()) {
                return localized(menu.acceptsLocalFuel()
                        ? "screen.earth_on_minecraft.machine.missing_power"
                        : "screen.earth_on_minecraft.machine.missing_grid_power");
            }
            return recipeSummary(recipe);
        }).orElseGet(() -> localized(input.isEmpty()
                ? "screen.earth_on_minecraft.machine.empty_input"
                : "screen.earth_on_minecraft.machine.unsupported_input"));
    }

    private void cycleRedstoneMode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return;
        int id = switch (menu.redstoneMode()) {
            case ALWAYS -> ProcessingMachineMenu.BUTTON_REDSTONE_REQUIRE_SIGNAL;
            case REQUIRE_SIGNAL -> ProcessingMachineMenu.BUTTON_REDSTONE_REQUIRE_NO_SIGNAL;
            case REQUIRE_NO_SIGNAL -> ProcessingMachineMenu.BUTTON_REDSTONE_ALWAYS;
        };
        mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void cycleRoute() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null && menu.routeCount() > 1) {
            mc.gameMode.handleInventoryButtonClick(menu.containerId, ProcessingMachineMenu.BUTTON_ROUTE_NEXT);
        }
    }

    private void syncButtons() {
        if (redstoneButton != null) redstoneButton.setMessage(Component.empty());
        if (routeButton != null) {
            routeButton.visible = menu.routeCount() > 1;
            routeButton.active = menu.routeCount() > 1;
            routeButton.setMessage(Component.empty());
        }
    }

    private Component routeButtonLabel() {
        return menu.routeCount() <= 1 ? Component.empty()
                : Component.literal((menu.selectedRouteIndex() + 1) + "/" + menu.routeCount());
    }

    private Component trimmedTitle() {
        return EarthGuiSupport.trim(font, title, 84);
    }

    private String recipeSummary(ProcessingMachineBlock.Recipe recipe) {
        if (Minecraft.getInstance().getLanguageManager().getSelected()
                .toLowerCase(java.util.Locale.ROOT).startsWith("zh")) return recipe.note();
        return localized("screen.earth_on_minecraft.machine.recipe_ready") + ": "
                + RouteGuide.describeOutputs(recipe);
    }

    private boolean fuelSlotHasFuel() {
        if (!menu.acceptsLocalFuel()) return false;
        return ProcessingMachineBlockEntity.getFuelTicks(
                menu.getSlot(ProcessingMachineBlockEntity.SLOT_FUEL).getItem()) > 0;
    }

    private boolean hasCokeCharge() {
        ItemStack reagent = menu.getSlot(ProcessingMachineBlockEntity.SLOT_REAGENT).getItem();
        return !reagent.isEmpty() && reagent.getItem() == com.xxsx.earthonminecraft.EarthOnMinecraft.COKE.get();
    }

    private boolean redstonePaused() {
        var level = Minecraft.getInstance().level;
        return level != null && !menu.redstoneMode().allows(level.hasNeighborSignal(menu.pos()));
    }

    private List<Component> powerTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(menu.powerMode().labelKey()));
        tooltip.add(Component.translatable(menu.driveState().labelKey()));
        if (menu.powerMode().grid()) {
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.eou.buffer",
                    menu.energyStored(), menu.energyCapacity(), menu.energyPerTick()));
        }
        tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.spec",
                secondsPerOperation(), menu.energyPerTick()));
        if (menu.powerMode().passive()) {
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.passive.tooltip"));
        } else if (menu.acceptsLocalFuel()) {
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.fuel.tooltip"));
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.fuel.examples"));
            ItemStack fuel = menu.getSlot(ProcessingMachineBlockEntity.SLOT_FUEL).getItem();
            int fuelCap = ProcessingMachineBlockEntity.getFuelMaxTemperatureK(fuel);
            if (fuelCap > 0) {
                tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.temp.fuel_cap", fuelCap));
            }
        } else if (menu.powerMode().manualFallback()) {
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.manual.tooltip"));
        } else {
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.grid_only.tooltip"));
        }
        return tooltip;
    }

    private List<Component> temperatureTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.temp.readout",
                menu.temperatureK(), menu.targetTemperatureK(), menu.maxTemperatureK()));
        tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.temp.tooltip"));
        ItemStack fuel = menu.getSlot(ProcessingMachineBlockEntity.SLOT_FUEL).getItem();
        int fuelCap = ProcessingMachineBlockEntity.getFuelMaxTemperatureK(fuel);
        if (fuelCap > 0) {
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.temp.fuel_cap", fuelCap));
        }
        return tooltip;
    }

    private void appendStructureFaultTooltip(List<Component> tooltip) {
        menu.faultExpectedRole().ifPresent(role -> {
            BlockPos faultPos = menu.pos().offset(menu.faultDx(), menu.faultDy(), menu.faultDz());
            String coords = faultPos.getX() + ", " + faultPos.getY() + ", " + faultPos.getZ();
            String rel = String.format("(%+d, %+d, %+d)", menu.faultDx(), menu.faultDy(), menu.faultDz());
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.structure_progress",
                    menu.faultMatchedParts(), Math.max(1, menu.faultTotalParts())));
            tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.structure_fault_expected",
                    coords, rel, Component.translatable(role.labelKey())));
            var level = Minecraft.getInstance().level;
            if (level != null) {
                Component actualName = level.getBlockState(faultPos).getBlock().getName();
                tooltip.add(Component.translatable("screen.earth_on_minecraft.machine.structure_fault_actual", actualName));
            }
        });
    }

    private List<Component> machineSpecTooltip() {
        List<Component> result = new ArrayList<>();
        result.add(Component.translatable(menu.processFamily().labelKey()));
        result.add(Component.translatable(menu.kind().setpointKey()));
        result.add(Component.translatable("screen.earth_on_minecraft.machine.spec",
                secondsPerOperation(), menu.energyPerTick()));
        result.add(Component.translatable(menu.powerMode().labelKey()));
        if (isMultiblockMachine()) {
            result.add(Component.translatable(MachineMultiblock.patternFor(menu.kind()).screenKey()));
            result.add(Component.translatable("screen.earth_on_minecraft.machine.interface.tooltip.2"));
        }
        return result;
    }

    private Component stateTooltip() {
        return Component.translatable(menu.active()
                ? "screen.earth_on_minecraft.machine.state.running.tooltip"
                : "screen.earth_on_minecraft.machine.state.idle.tooltip");
    }

    private int secondsPerOperation() {
        return Math.max(1, (menu.maxProgress() + 19) / 20);
    }

    private boolean isMultiblockMachine() {
        return MachineMultiblock.patternFor(menu.kind()) != MachineMultiblock.Pattern.NONE;
    }

    private static String localized(String key) {
        return Language.getInstance().getOrDefault(key);
    }

    private record FamilyPalette(int outerFrame, int mainPanel, int workBayBg, int accent,
                                 int slotBorder, int textLight) {
        static FamilyPalette forFamily(ProcessingMachineBlock.ProcessFamily family) {
            return switch (family) {
                case COMMINUTION -> new FamilyPalette(0xFF211B17, 0xFF3B322B, 0xFF181512,
                        0xFFB08A57, 0xFF6E5B48, 0xFFE2E8EC);
                case CLASSIFICATION -> new FamilyPalette(0xFF16232C, 0xFF2D424F, 0xFF111D25,
                        0xFF6FA6C0, 0xFF4D6B7B, 0xFFE2E8EC);
                case WET_PROCESS -> new FamilyPalette(0xFF102825, 0xFF254943, 0xFF0D201F,
                        0xFF53B7A7, 0xFF3E766E, 0xFFE2E8EC);
                case THERMAL -> new FamilyPalette(0xFF2B1713, 0xFF4B2B23, 0xFF211311,
                        0xFFE19A45, 0xFF86513A, 0xFFFFE7CF);
                case ELECTROCHEMICAL -> new FamilyPalette(0xFF111D30, 0xFF203854, 0xFF0D1828,
                        0xFF7EB4DE, 0xFF8B5D52, 0xFFE2E8EC);
                case FORMING -> new FamilyPalette(0xFF211D29, 0xFF3C3548, 0xFF191620,
                        0xFFA493BD, 0xFF695C78, 0xFFE2E8EC);
                case REACTION -> new FamilyPalette(0xFF2B2114, 0xFF4B3A23, 0xFF211A11,
                        0xFFE2AE59, 0xFF896B3C, 0xFFFFECD0);
                case COLUMN -> new FamilyPalette(0xFF14272B, 0xFF2C474E, 0xFF102126,
                        0xFF79B8BE, 0xFF4D7780, 0xFFE2E8EC);
                case MIXING -> new FamilyPalette(0xFF202619, 0xFF3B482B, 0xFF192015,
                        0xFFA1B96A, 0xFF64764A, 0xFFE8EDDF);
                case CRYSTALLIZATION -> new FamilyPalette(0xFF1D2035, 0xFF343B5B, 0xFF15182B,
                        0xFFA5A2E0, 0xFF626B9A, 0xFFE8E8FA);
            };
        }
    }
}
