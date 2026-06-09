package com.github.kdgaming0.enhancedchat.chat;

import com.github.kdgaming0.enhancedchat.chat.render.CustomChatRenderer;
import com.github.kdgaming0.enhancedchat.chat.search.ChatSearchController;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Per-ChatComponent bookkeeping shared by every chat feature that needs to correlate a
 * displayed line with its source message or renderer.
 *
 * <p>The map is keyed on the {@link FormattedCharSequence} stored inside each
 * {@link GuiMessage.Line}, which is unique per displayed line. This gives O(1) lookups for:
 * <ul>
 *   <li>hover/click hit-testing (content → renderer),</li>
 *   <li>copy and delete resolution (content/line → parent message),</li>
 *   <li>selection outline rendering (parent identity comparison).</li>
 * </ul>
 *
 * <p>Line registration is bracketed by {@link #beginAddingLinesFor(GuiMessage)} and
 * {@link #finishAddingLines()} so callers don't have to pass the parent to every record call.
 */
public final class ChatLineTracker {

    private final Map<FormattedCharSequence, Entry> byContent = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<GuiMessage, String> searchableText = new Reference2ObjectOpenHashMap<>();
    private @Nullable GuiMessage pendingParent;
    private @Nullable GuiMessage selectedMessage;

    public void beginAddingLinesFor(GuiMessage parent) {
        this.pendingParent = parent;
        this.searchableText.put(parent, ChatSearchController.toSearchable(parent.content()));
    }

    public void finishAddingLines() {
        this.pendingParent = null;
    }

    /**
     * Associates a freshly-added display line with its parent message and optional renderer.
     */
    public void recordLine(GuiMessage.Line line, @Nullable CustomChatRenderer renderer) {
        if (pendingParent == null) return;
        byContent.put(line.content(), new Entry(pendingParent, renderer));
    }

    public void evictLine(GuiMessage.Line line) {
        byContent.remove(line.content());
    }

    /**
     * Discards all per-line state AND any active selection. Use on full history clear.
     */
    public void clearAll() {
        byContent.clear();
        searchableText.clear();
        selectedMessage = null;
    }

    /**
     * Discards per-line state only; leaves the selected message intact so the outline survives
     * display-queue rebuilds (e.g. rescale, tab switch) as long as the message still exists.
     */
    public void clearLineMappings() {
        byContent.clear();
        searchableText.clear();
    }

    public @Nullable CustomChatRenderer rendererFor(FormattedCharSequence content) {
        Entry entry = byContent.get(content);
        return entry == null ? null : entry.renderer;
    }

    public @Nullable GuiMessage parentFor(FormattedCharSequence content) {
        Entry entry = byContent.get(content);
        return entry == null ? null : entry.parent;
    }

    public @Nullable GuiMessage parentFor(GuiMessage.Line line) {
        return parentFor(line.content());
    }

    public @Nullable GuiMessage getSelectedMessage() {
        return selectedMessage;
    }

    public void setSelectedMessage(@Nullable GuiMessage message) {
        this.selectedMessage = message;
    }

    /**
     * Returns the pre-computed searchable plain text for a message, computing on demand if absent.
     */
    public String getSearchableText(GuiMessage message) {
        String cached = searchableText.get(message);
        if (cached != null) return cached;
        return ChatSearchController.toSearchable(message.content());
    }

    private record Entry(GuiMessage parent, @Nullable CustomChatRenderer renderer) {
    }
}
