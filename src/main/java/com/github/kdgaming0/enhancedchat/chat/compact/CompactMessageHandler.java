/**
 * Duplicate chat-message compaction for Hypixel and general chat.
 * <p>
 * This feature was inspired by similar duplicate-removal behavior in
 * https://github.com/caoimhebyrne/compact-chat.
 */

package com.github.kdgaming0.enhancedchat.chat.compact;

import com.github.kdgaming0.enhancedchat.chat.access.ChatAccess;
import com.github.kdgaming0.enhancedchat.chat.render.ChatTextHelper;
import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects repeats of identical chat messages and replaces the older occurrence with a single
 * line carrying a {@code (×N)} suffix.
 *
 * <p><b>Three compaction modes</b>, evaluated in order of priority:
 * <ol>
 *   <li><b>Consecutive-only</b> ({@code onlyCompactConsecutive} = true) — only immediate
 *       repeats collapse; any intervening different message resets the counter.</li>
 *   <li><b>Time-windowed</b> ({@code compactTimeWindowMinutes} &gt; 0) — repeats within the
 *       rolling window collapse; stale first-seen timestamps start a fresh streak.</li>
 *   <li><b>Unlimited</b> — every duplicate collapses regardless of distance or age.</li>
 * </ol>
 *
 * <p>When a duplicate is removed, only the duplicate line itself and directly adjacent
 * separator/blank lines that share the same tick are removed. This cleans up Hypixel
 * separator blocks without affecting unrelated messages sent by other mods in the same tick.
 *
 * <p>The {@link #entries} map uses <b>access-order</b> {@link LinkedHashMap}: every
 * {@link Map#get} promotes the entry to the most-recently-used position, so the eldest entry
 * (least-recently-accessed) is the natural eviction candidate once size exceeds
 * {@link #MAX_TRACKED_MESSAGES}. This keeps memory bounded on players who sit in busy lobbies
 * for hours while still tracking a generous working set of recent unique messages.
 */
public final class CompactMessageHandler {

    private static final int MAX_TRACKED_MESSAGES = 512;

    private static final Style COUNT_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final String COUNT_PREFIX = " (×";
    private static final String COUNT_SUFFIX = ")";
    private final ChatAccess chatAccess;
    /**
     * Access-order LRU; see class javadoc for why.
     */
    private final Map<String, Entry> entries =
            new LinkedHashMap<>(64, 0.75f, /* accessOrder */ true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                    return size() > MAX_TRACKED_MESSAGES;
                }
            };
    private @Nullable String previousMessage;
    private @Nullable String pendingAddKey;

    public CompactMessageHandler(ChatAccess chatAccess) {
        this.chatAccess = chatAccess;
    }

    private static boolean isEligibleForCompaction(Entry entry, boolean isConsecutive, long nowMs) {
        if (EnhancedChatConfig.onlyCompactConsecutive) {
            return isConsecutive;
        }

        int windowMinutes = EnhancedChatConfig.compactTimeWindowMinutes;
        if (windowMinutes > 0) {
            long windowMs = (long) windowMinutes * 60_000L;
            return (nowMs - entry.firstSeenMs) <= windowMs;
        }

        return true;
    }

    private static MutableComponent withCountSuffix(Component message, int count) {
        MutableComponent result = message.copy();
        result.append(Component.literal(COUNT_PREFIX + count + COUNT_SUFFIX).setStyle(COUNT_STYLE));
        return result;
    }

    private static boolean isInteractable(Component component) {
        if (component.getStyle().getClickEvent() != null) {
            return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (isInteractable(sibling)) {
                return true;
            }
        }
        return false;
    }

    // -----------------------------------------------------------------
    // Eligibility
    // -----------------------------------------------------------------

    private static boolean isAuxiliaryLine(GuiMessage message) {
        String text = message.content().getString().trim();
        return text.isEmpty() || isSeparator(text);
    }

    private static boolean isSeparator(String trimmed) {
        return ChatTextHelper.isFullSeparator(trimmed) || ChatTextHelper.isCenteredSeparator(trimmed);
    }

    /**
     * Inspects an incoming message. If it qualifies as a compactable duplicate, removes the
     * prior occurrence (along with directly adjacent separator/blank lines) from history
     * and returns a new component with a {@code (×N)} suffix appended; otherwise returns the
     * original message unchanged.
     */
    public Component process(Component message) {
        pendingAddKey = null;
        if (!EnhancedChatConfig.compactDuplicateMessages) return message;

        if (EnhancedChatConfig.compactIgnoreInteractable && isInteractable(message)) return message;

        String raw = ChatTextHelper.stripCompactSuffix(message.getString());
        if (raw.trim().isEmpty() || isSeparator(raw.trim())) return message;

        long nowMs = System.currentTimeMillis();
        boolean isConsecutive = raw.equals(previousMessage);
        previousMessage = raw;
        pendingAddKey = raw;

        Entry entry = entries.get(raw);
        if (entry == null) {
            entries.put(raw, new Entry(nowMs));
            return message;
        }

        if (!isEligibleForCompaction(entry, isConsecutive, nowMs)) {
            entries.put(raw, new Entry(nowMs));
            return message;
        }

        entry.count++;
        if (entry.lastMessage != null) {
            removePreviousDuplicate(entry.lastMessage);
        }
        return withCountSuffix(message, entry.count);
    }

    /**
     * Binds the {@link GuiMessage} vanilla just added (history index 0) to the entry recorded
     * during the matching {@link #process} call, so the next duplicate of this text can be
     * dropped in {@link #removePreviousDuplicate} by identity instead of a full-history text scan.
     *
     * <p>No-op when the processed message was not tracked (feature disabled, interactable, or an
     * auxiliary separator/blank line).
     */
    public void noteAddedMessage(GuiMessage added) {
        if (pendingAddKey == null) return;
        Entry entry = entries.get(pendingAddKey);
        if (entry != null) {
            entry.lastMessage = added;
        }
        pendingAddKey = null;
    }

    // -----------------------------------------------------------------
    // History manipulation
    // -----------------------------------------------------------------

    public void clear() {
        entries.clear();
        previousMessage = null;
    }

    /**
     * Removes the prior occurrence {@code target} and any separator/blank lines that are
     * directly adjacent AND share the same tick.
     */
    private void removePreviousDuplicate(GuiMessage target) {
        List<GuiMessage> msgs = chatAccess.ec$getAllMessages();
        int i = identityIndexOf(msgs, target);
        if (i < 0) return;

        int anchorTick = msgs.get(i).addedTime();
        int lower = i;
        int upper = i;

        while (lower - 1 >= 0
                && msgs.get(lower - 1).addedTime() == anchorTick
                && isAuxiliaryLine(msgs.get(lower - 1))) {
            lower--;
        }
        while (upper + 1 < msgs.size()
                && msgs.get(upper + 1).addedTime() == anchorTick
                && isAuxiliaryLine(msgs.get(upper + 1))) {
            upper++;
        }

        for (int k = lower; k <= upper; k++) {
            chatAccess.ec$dropMessageLines(msgs.get(k));
        }
        msgs.subList(lower, upper + 1).clear();
    }

    private static int identityIndexOf(List<GuiMessage> msgs, GuiMessage target) {
        for (int i = 0, n = msgs.size(); i < n; i++) {
            if (msgs.get(i) == target) return i;
        }
        return -1;
    }

    /**
     * Per-message compaction state.
     */
    private static final class Entry {
        int count;
        long firstSeenMs;
        /**
         * The most recent {@link GuiMessage} carrying this text, set by {@link #noteAddedMessage}
         * after vanilla adds it. Lets {@link #removePreviousDuplicate} find it by identity.
         */
        @Nullable GuiMessage lastMessage;

        Entry(long nowMs) {
            this.count = 1;
            this.firstSeenMs = nowMs;
        }
    }
}
