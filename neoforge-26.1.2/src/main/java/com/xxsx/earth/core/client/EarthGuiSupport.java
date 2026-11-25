package com.xxsx.earth.core.client;

import com.xxsx.earth.core.ProcessingMachineBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import java.util.List;

final class EarthGuiSupport {
    static final int TEXT = 0xFF404040;
    static final int MUTED = 0xFF606060;
    static final int WARNING = 0xFFAA3322;
    static final int POWER = 0xFF1E7C9A;
    static final int INPUT = 0xFF2D74C4;
    static final int OUTPUT = 0xFFC46A22;
    static final int BOTH = 0xFF2E8B57;

    private EarthGuiSupport() {
    }

    static void drawInset(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xFF555555);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFFFFFFF);
        g.fill(x + 2, y + 2, x + width - 1, y + height - 1, 0xFF8B8B8B);
    }

    static void drawEnergyBar(GuiGraphicsExtractor g, int x, int y, int width, int height,
                              int energy, int capacity) {
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF3B3B3B);
        g.fill(x, y, x + width, y + height, 0xFF10151A);
        int filled = Math.min(width, width * Math.max(0, energy) / Math.max(1, capacity));
        if (filled > 0) {
            g.fill(x, y, x + filled, y + height, 0xFF31A9C9);
            g.fill(x, y, x + filled, y + 2, 0xFF82DFF0);
        }
    }

    static void drawNotebookGlyph(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 1, y + 1, x + 5, y + 10, 0xFFE5D6A6);
        g.fill(x + 5, y + 2, x + 9, y + 10, 0xFFF4E9C6);
        g.fill(x + 4, y + 1, x + 6, y + 10, 0xFF6D4C2D);
        g.fill(x + 2, y + 3, x + 4, y + 4, 0xFF7A684B);
        g.fill(x + 6, y + 4, x + 8, y + 5, 0xFF7A684B);
    }

    static void drawIoGlyph(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 1, y + 3, x + 8, y + 5, INPUT);
        g.fill(x + 1, y + 2, x + 3, y + 6, INPUT);
        g.fill(x + 3, y + 7, x + 10, y + 9, OUTPUT);
        g.fill(x + 8, y + 6, x + 10, y + 10, OUTPUT);
    }

    static void drawRedstoneGlyph(GuiGraphicsExtractor g, int x, int y,
                                  ProcessingMachineBlockEntity.RedstoneMode mode) {
        if (mode == ProcessingMachineBlockEntity.RedstoneMode.ALWAYS) {
            g.fill(x + 1, y + 1, x + 10, y + 10, 0xFF7A1D1D);
            g.fill(x + 2, y + 2, x + 9, y + 9, 0xFFB52A2A);
            for (int i = 1; i < 9; i++) {
                g.fill(x + i, y + 9 - i, x + i + 2, y + 11 - i, 0xFFE05A4F);
            }
            return;
        }

        int head = mode == ProcessingMachineBlockEntity.RedstoneMode.REQUIRE_SIGNAL
                ? 0xFFFF5A32 : 0xFF5A4740;
        int glow = mode == ProcessingMachineBlockEntity.RedstoneMode.REQUIRE_SIGNAL
                ? 0xFFFFD05A : 0xFF81716B;
        g.fill(x + 4, y + 4, x + 7, y + 10, 0xFF6B3D2A);
        g.fill(x + 2, y + 2, x + 9, y + 5, head);
        g.fill(x + 4, y + 1, x + 7, y + 3, glow);
    }

    static void drawInventoryArmor(GuiGraphicsExtractor g, int leftPos, int topPos,
                                   int imageWidth, int imageHeight, List<Slot> slots, int accent) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF172126);
        g.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF536168);
        g.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF28353B);
        g.fill(leftPos + 6, topPos + 6, leftPos + imageWidth - 6, topPos + 18, 0xFF34444C);
        g.fill(leftPos + 7, topPos + 18, leftPos + imageWidth - 7, topPos + 19, accent);
        g.fill(leftPos + 7, topPos + 70, leftPos + imageWidth - 7, topPos + 80, 0xFF1D292E);
        g.fill(leftPos + 7, topPos + 80, leftPos + imageWidth - 7, topPos + 81, accent);
        for (Slot slot : slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF11191D);
            g.fill(x, y, x + 16, y + 16, 0xFF536168);
            g.fill(x + 1, y + 1, x + 15, y + 15, 0xFF202C32);
            g.fill(x + 1, y + 1, x + 15, y + 2, 0xFF35444B);
        }
    }

    static void drawSideTab(GuiGraphicsExtractor g, int x, int y, int accent, boolean hovered) {
        g.fill(x - 2, y + 3, x + 2, y + 17, 0xFF536168);
        g.fill(x, y, x + 20, y + 20, hovered ? accent : 0xFF172126);
        g.fill(x + 1, y + 1, x + 19, y + 19, 0xFF536168);
        g.fill(x + 2, y + 2, x + 18, y + 18, 0xFF28353B);
        g.fill(x + 18, y + 3, x + 19, y + 17, accent);
        drawNotebookGlyph(g, x + 5, y + 5);
    }

    static Component trim(Font font, Component text, int width) {
        String raw = text.getString();
        if (font.width(raw) <= width) {
            return text;
        }
        return Component.literal(fit(font, raw, width));
    }

    static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        String ellipsis = "...";
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ellipsis))) + ellipsis;
    }

    static String compactEnergy(int energy, int capacity) {
        return compact(energy) + "/" + compact(capacity) + " EOU";
    }

    private static String compact(int value) {
        if (value >= 1_000_000) {
            return value / 1_000_000 + "m";
        }
        if (value >= 1000) {
            return value / 1000 + "k";
        }
        return Integer.toString(value);
    }
}
