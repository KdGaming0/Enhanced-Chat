package com.github.kdgaming0.enhancedchat.chat.tabs;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * A small button that uses custom {@link WidgetSprites} for its background,
 * designed for the chat tab bar.
 */
public class ChatTabButton extends Button {

    private final WidgetSprites sprites;

    public ChatTabButton(
            int x, int y, int width, int height,
            Component message, OnPress onPress,
            WidgetSprites sprites) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.sprites = sprites;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                sprites.get(this.active, this.isHoveredOrFocused()),
                this.getX(), this.getY(),
                this.getWidth(), this.getHeight(),
                ARGB.white(this.alpha));
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }
}
