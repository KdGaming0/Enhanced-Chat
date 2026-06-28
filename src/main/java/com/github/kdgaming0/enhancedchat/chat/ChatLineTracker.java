package com.github.kdgaming0.enhancedchat.chat;

import com.github.kdgaming0.enhancedchat.chat.render.ChatTextHelper;
import com.github.kdgaming0.enhancedchat.chat.search.ChatSearchController;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Per-ChatComponent bookkeeping shared by every chat feature that needs to correlate a
 * displayed line with its source message or renderer.
 *
 * <p>The map is keyed on the {@link FormattedCharSequence} stored inside each
 * {@link GuiMessage.Line}, which is unique per displayed line. This gives O(1) lookups for:
 * <ul>
 *   <li>copy and delete resolution (content/line → parent message),</li>
 *   <li>selection outline rendering (parent identity comparison).</li>
 * </ul>
 *
 * <p>Line registration is bracketed by {@link #beginAddingLinesFor(GuiMessage)} and
 * {@link #finishAddingLines()} so callers don't have to pass the parent to every record call.
 */
public final class ChatLineTracker {

    private final Map<FormattedCharSequence, Entry> byContent = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<GuiMessage, List<GuiMessage.Line>> linesByMessage = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<GuiMessage, String> searchableText = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<GuiMessage, String> tabText = new Reference2ObjectOpenHashMap<>();
    private @Nullable GuiMessage pendingParent;
    private @Nullable GuiMessage selectedMessage;

    /**
     * Pre-computes the searchable/tab plain text for {@code parent}, but only for the feature
     * that is actually filtering. When neither search nor tabs are filtering — the common case —
     * this skips a full component flatten (+ regex strip + lowercase) per message, which on a
     * {@code refreshTrimmedMessages} rebuild is paid once for every message in history.
     * {@link #getSearchableText}/{@link #getTabText} fall back to on-demand computation, so the
     * caches are purely an optimization.
     */
    public void beginAddingLinesFor(GuiMessage parent) {
        this.pendingParent = parent;
        ChatFeatureState state = ChatFeatureState.get();
        if (state.search().isFiltering()) {
            this.searchableText.put(parent, ChatSearchController.toSearchable(parent.content()));
        }
        if (state.tabs().isFiltering()) {
            this.tabText.put(parent, ChatTextHelper.plainText(parent.content()));
        }
    }

    public void finishAddingLines() {
        this.pendingParent = null;
    }

    /**
     * Associates a freshly-added display line with its parent message, in both directions:
     * line&rarr;parent for hover/copy/delete resolution, and parent&rarr;lines so the compact
     * feature can drop a collapsed message's lines without a full rebuild.
     */
    public void recordLine(GuiMessage.Line line) {
        if (pendingParent == null) return;
        byContent.put(line.content(), new Entry(pendingParent));
        List<GuiMessage.Line> lines = linesByMessage.get(pendingParent);
        if (lines == null) {
            lines = new ArrayList<>(2);
            linesByMessage.put(pendingParent, lines);
        }
        lines.add(line);
    }

    public void evictLine(GuiMessage.Line line) {
        Entry entry = byContent.remove(line.content());
        if (entry == null) return;
        List<GuiMessage.Line> lines = linesByMessage.get(entry.parent);
        if (lines != null) {
            lines.remove(line);
            if (lines.isEmpty()) linesByMessage.remove(entry.parent);
        }
    }

    /**
     * Removes all display lines belonging to {@code message} from the tracker and returns them so
     * the caller can drop them from the live display queue. Used by the compact feature to discard
     * a collapsed duplicate in place, avoiding a full display-queue rebuild. Returns an empty list
     * when the message has no tracked lines (already trimmed or never displayed).
     */
    public List<GuiMessage.Line> takeLinesFor(GuiMessage message) {
        List<GuiMessage.Line> lines = linesByMessage.remove(message);
        if (lines == null) return List.of();
        for (GuiMessage.Line line : lines) {
            byContent.remove(line.content());
        }
        return lines;
    }

    /**
     * Discards all per-line state AND any active selection. Use on full history clear.
     */
    public void clearAll() {
        byContent.clear();
        linesByMessage.clear();
        searchableText.clear();
        tabText.clear();
        selectedMessage = null;
    }

    /**
     * Discards per-line state only; leaves the selected message intact so the outline survives
     * display-queue rebuilds (e.g. rescale, tab switch) as long as the message still exists.
     */
    public void clearLineMappings() {
        byContent.clear();
        linesByMessage.clear();
        searchableText.clear();
        tabText.clear();
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

    /**
     * Returns the pre-computed tab plain text for a message, computing on demand if absent.
     */
    public String getTabText(GuiMessage message) {
        String cached = tabText.get(message);
        if (cached != null) return cached;
        return ChatTextHelper.plainText(message.content());
    }

    private record Entry(GuiMessage parent) {
    }
}
