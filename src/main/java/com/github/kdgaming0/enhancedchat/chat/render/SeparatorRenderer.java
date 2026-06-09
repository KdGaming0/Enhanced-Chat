package com.github.kdgaming0.enhancedchat.chat.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Draws a separator line as two smooth rectangles flanking optional centered text.
 * When {@code middleText} is null, a single continuous line spans the chat area.
 */
public record SeparatorRenderer(int lineColor, @Nullable String middleText)
        implements CustomChatRenderer {

    /**
     * Hypixel's heavy-block separator; rendered as a thicker line.
     */
    private static final int BLOCK_CHAR = '▬';

    private static final int TEXT_PADDING = 4;
    private static final float SHADOW_FACTOR = 0.25f;

    private static void drawLineSegment(
            GuiGraphicsExtractor graphics, int x, int y, int xEnd, int thickness, int color, int shadow) {
        graphics.fill(x + 1, y + 1, xEnd - 1, y + thickness + 1, shadow);
        graphics.fill(x, y, xEnd - 2, y + thickness, color);
    }

    private static boolean containsBlockChar(FormattedCharSequence text) {
        boolean[] found = {false};
        text.accept((index, style, cp) -> {
            if (cp == BLOCK_CHAR) {
                found[0] = true;
                return false;
            }
            return true;
        });
        return found[0];
    }

    @Override
    public void render(
            GuiGraphicsExtractor graphics,
            Font font,
            FormattedCharSequence text,
            int lineX,
            int textY,
            int lineWidth,
            float alpha) {

        int thickness = containsBlockChar(text) ? 2 : 1;
        int alpha8 = ARGB.as8BitChannel(alpha);
        int color = ARGB.color(alpha8, lineColor);
        int shadow = ARGB.scaleRGB(color, SHADOW_FACTOR);
        int lineY = textY + (font.lineHeight - thickness) / 2;
        int right = lineX + lineWidth;

        if (middleText == null || middleText.isEmpty()) {
            drawLineSegment(graphics, lineX, lineY, right, thickness, color, shadow);
            return;
        }

        int textWidth = font.width(middleText);
        int textX = lineX + (lineWidth - textWidth) / 2;
        graphics.text(font, middleText, textX, textY, color, true);

        int leftEnd = textX - TEXT_PADDING;
        if (leftEnd > lineX + 2) {
            drawLineSegment(graphics, lineX + 1, lineY, leftEnd, thickness, color, shadow);
        }

        int rightStart = textX + textWidth + TEXT_PADDING;
        if (rightStart < right - 2) {
            drawLineSegment(graphics, rightStart, lineY, right, thickness, color, shadow);
        }
    }

    @Override
    public HitTest hitTest(Font font, FormattedCharSequence text, int lineX, int lineWidth) {
        // Even separators with middle text carry no clickable content, so hit-testing is
        // unconditionally off. Mouse interaction on these lines is intentional no-op.
        return HitTest.DISABLED;
    }
}
