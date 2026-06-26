package com.github.kdgaming0.enhancedchat.chat.render;

import com.github.kdgaming0.enhancedchat.chat.ChatLineTracker;
import com.github.kdgaming0.enhancedchat.chat.access.ChatAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Intercepts vanilla chat rendering to draw the selection outline around the currently
 * targeted message.
 *
 * <p>All parent-message lookups are O(1) via {@link ChatLineTracker}; the proxy itself does
 * no searching. Line spacing is sampled once per render pass to avoid repeated option reads
 * in the per-line hot path.
 */
public class ChatGraphicsAccessProxy implements ChatComponent.ChatGraphicsAccess {

    private static final float OUTLINE_ALPHA_FACTOR = 0.35f;
    private static final int OUTLINE_LEFT_INSET = -4;
    private static final int OUTLINE_RIGHT_INSET = 8;
    private final ChatAccess chatAccess;
    private ChatComponent.ChatGraphicsAccess delegate;
    private @Nullable GuiGraphicsExtractor graphics;

    // Sampled once per frame — chat line-spacing doesn't change during a render pass.
    private int entryHeight;
    private int entryBottomToMessageY;

    // Per-line outline accumulation; reset between messages by handleMessage.
    private boolean outlineActive;
    private int outlineMinY = Integer.MAX_VALUE;
    private int outlineMaxY = Integer.MIN_VALUE;
    private float outlineOpacity;

    public ChatGraphicsAccessProxy(
            ChatComponent.ChatGraphicsAccess delegate,
            ChatAccess chatAccess,
            @Nullable GuiGraphicsExtractor graphics) {
        this.chatAccess = chatAccess;
        prepareForFrame(delegate, graphics);
    }

    /**
     * Resets mutable state and updates fields that may change between render passes.
     * Call once before each chat render when reusing a proxy instance.
     */
    public void prepareForFrame(ChatComponent.ChatGraphicsAccess delegate,
                                @Nullable GuiGraphicsExtractor graphics) {
        this.delegate = delegate;
        this.graphics = graphics;
        this.outlineActive = false;
        this.outlineMinY = Integer.MAX_VALUE;
        this.outlineMaxY = Integer.MIN_VALUE;
        this.outlineOpacity = 0f;

        double lineSpacing = net.minecraft.client.Minecraft.getInstance().options.chatLineSpacing().get();
        this.entryHeight = (int) (9.0 * (lineSpacing + 1.0));
        this.entryBottomToMessageY = (int) Math.round(8.0 * (lineSpacing + 1.0) - 4.0 * lineSpacing);
    }

    @Override
    public void updatePose(@NonNull Consumer<Matrix3x2f> updater) {
        delegate.updatePose(updater);
    }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int color) {
        delegate.fill(x0, y0, x1, y1, color);
    }

    @Override
    public boolean handleMessage(int textTop, float opacity, @NonNull FormattedCharSequence message) {
        ChatLineTracker tracker = chatAccess.ec$getLineTracker();

        boolean hovered = delegate.handleMessage(textTop, opacity, message);

        GuiMessage selected = tracker.getSelectedMessage();
        if (selected != null) {
            GuiMessage parent = tracker.parentFor(message);
            int entryBottom = textTop + entryBottomToMessageY;
            int entryTop = entryBottom - entryHeight;
            maybeAccumulateOutline(parent, selected, opacity, entryTop, entryBottom);
        }
        return hovered;
    }

    @Override
    public void handleTag(int x0, int y0, int x1, int y1, float opacity, @NonNull GuiMessageTag tag) {
        // Intentionally empty: the coloured indicator bar is suppressed for all messages.
    }

    @Override
    public void handleTagIcon(
            int left, int bottom, boolean forceVisible,
            @NonNull GuiMessageTag tag, GuiMessageTag.@NonNull Icon icon) {
        delegate.handleTagIcon(left, bottom, forceVisible, tag, icon);
    }

    /**
     * Draws the accumulated outline (one merged rectangle spanning every visible line of the
     * selected message). Call once after the render pass completes.
     */
    public void finishOutline() {
        if (graphics == null || !outlineActive) return;

        int scaledWidth = chatAccess.ec$getScaledWidth();
        int alpha = ARGB.as8BitChannel(outlineOpacity * OUTLINE_ALPHA_FACTOR);
        int color = ARGB.color(alpha, 0xFF, 0xFF, 0xFF);

        int x0 = OUTLINE_LEFT_INSET;
        int x1 = scaledWidth + OUTLINE_RIGHT_INSET;
        int y0 = outlineMinY;
        int y1 = outlineMaxY;

        graphics.fill(x0, y0, x1, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x1, y1, color);
        graphics.fill(x0, y0, x0 + 1, y1, color);
        graphics.fill(x1 - 1, y0, x1, y1, color);

        outlineActive = false;
        outlineMinY = Integer.MAX_VALUE;
        outlineMaxY = Integer.MIN_VALUE;
    }

    // -----------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------

    private void maybeAccumulateOutline(
            @Nullable GuiMessage parent, @Nullable GuiMessage selected,
            float opacity, int entryTop, int entryBottom) {
        if (selected == null || parent != selected) return;
        outlineActive = true;
        outlineOpacity = opacity;
        outlineMinY = Math.min(outlineMinY, entryTop);
        outlineMaxY = Math.max(outlineMaxY, entryBottom);
    }
}
