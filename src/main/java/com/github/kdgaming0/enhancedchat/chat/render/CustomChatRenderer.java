package com.github.kdgaming0.enhancedchat.chat.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;

/**
 * Alternative draw strategy for a single chat line. Implementations replace the vanilla
 * left-aligned draw and describe, via {@link #hitTest}, how the vanilla hover/click pass
 * should be adapted to match where the text was actually drawn.
 */
public interface CustomChatRenderer {

    /**
     * @param graphics  current graphics context
     * @param font      active font
     * @param text      wrapped line content
     * @param lineX     left edge of the chat area in scaled space (usually 0)
     * @param textY     text baseline Y
     * @param lineWidth chat width in scaled pixels
     * @param alpha     opacity, 0–1
     */
    void render(
            GuiGraphicsExtractor graphics,
            Font font,
            FormattedCharSequence text,
            int lineX,
            int textY,
            int lineWidth,
            float alpha);

    /**
     * Describes how the vanilla hit-test should handle this line. Default is to run it
     * unchanged at the natural position.
     */
    default HitTest hitTest(Font font, FormattedCharSequence text, int lineX, int lineWidth) {
        return HitTest.PASSTHROUGH;
    }

    /**
     * Explicit alternative to a -1 / 0 / positive int sentinel. Tells the graphics proxy
     * whether to skip the vanilla hit-test or to translate it by {@link #offsetX()} before
     * running.
     */
    record HitTest(Kind kind, int offsetX) {

        public static final HitTest PASSTHROUGH = new HitTest(Kind.ENABLED, 0);
        public static final HitTest DISABLED = new HitTest(Kind.DISABLED, 0);

        public static HitTest shifted(int offsetX) {
            return offsetX == 0 ? PASSTHROUGH : new HitTest(Kind.ENABLED, offsetX);
        }

        public boolean isEnabled() {
            return kind == Kind.ENABLED;
        }

        public enum Kind { ENABLED, DISABLED }
    }
}
