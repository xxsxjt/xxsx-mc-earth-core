package com.xxsx.earthonminecraft.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Clickable industrial controls without the vanilla beveled button texture. */
final class IndustrialButton extends Button {
    interface Painter {
        void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered);
    }

    private final Painter painter;

    IndustrialButton(int x, int y, int width, int height, Component message,
                     OnPress onPress, Painter painter) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.painter = painter;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (painter != null) {
            painter.draw(g, mouseX, mouseY, isHovered());
        }
    }
}
