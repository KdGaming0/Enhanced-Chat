package com.github.kdgaming0.enhancedchat.chat.render;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a message's raw lines into final display lines, tagging each with an optional
 * {@link CustomChatRenderer}. Called once per message added to the display queue.
 */
public final class ChatLineProcessor {

    private ChatLineProcessor() {
    }

    public static Result process(
            List<FormattedCharSequence> rawLines,
            Font font,
            int width,
            boolean centerEnabled,
            boolean separatorsEnabled) {

        List<FormattedCharSequence> lines = new ArrayList<>(rawLines.size());
        List<CustomChatRenderer> renderers = new ArrayList<>(rawLines.size());

        for (FormattedCharSequence rawSeq : rawLines) {
            String rawStr = ChatTextHelper.getString(rawSeq);
            String trimmedStr = rawStr.trim();

            // Blank lines are preserved verbatim: Hypixel uses them as vertical spacing
            // and reformatting them would collapse intentional gaps between messages.
            if (trimmedStr.isEmpty()) {
                lines.add(rawSeq);
                renderers.add(null);
                continue;
            }

            if (separatorsEnabled && ChatTextHelper.isFullSeparator(trimmedStr)) {
                lines.add(ChatTextHelper.trim(rawSeq));
                renderers.add(new SeparatorRenderer(ChatTextHelper.extractColor(rawSeq), null));

            } else if (separatorsEnabled && ChatTextHelper.isCenteredSeparator(trimmedStr)) {
                lines.add(ChatTextHelper.trim(rawSeq));
                renderers.add(new SeparatorRenderer(
                        ChatTextHelper.extractColor(rawSeq),
                        ChatTextHelper.extractMiddleText(trimmedStr)));

            } else if (centerEnabled && ChatTextHelper.isCenteredText(font, rawStr, trimmedStr)) {
                Component trimmedComponent = ChatTextHelper.toComponent(ChatTextHelper.trim(rawSeq));
                for (FormattedCharSequence wrapped : font.split(trimmedComponent, width)) {
                    lines.add(wrapped);
                    renderers.add(CenteredTextRenderer.INSTANCE);
                }

            } else {
                Component component = ChatTextHelper.toComponent(rawSeq);
                for (FormattedCharSequence wrapped : font.split(component, width)) {
                    lines.add(wrapped);
                    renderers.add(null);
                }
            }
        }
        return new Result(lines, renderers);
    }

    /**
     * Parallel arrays of the same length: {@code lines[i]} is rendered by {@code renderers[i]}.
     */
    public record Result(List<FormattedCharSequence> lines, List<@Nullable CustomChatRenderer> renderers) {
    }
}
