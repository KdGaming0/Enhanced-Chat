package com.github.kdgaming0.enhancedchat.chat.tabs;

import com.github.kdgaming0.enhancedchat.chat.render.ChatTextHelper;
import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;

/**
 * Owns the currently active chat tab and decides per-message visibility.
 *
 * <p>Separator lines are grouped with their surrounding messages by arrival tick: Hypixel
 * emits a channel block (banner, profile view, etc.) within a single tick, so every line in
 * that block — including the top and bottom separator borders — shares the same
 * {@link GuiMessage#addedTime()}. A separator is shown under a tab iff at least one
 * non-separator message in its tick-group matches that tab.
 */
public final class ChatTabController {

    private ChatTab activeTab = ChatTab.ALL;

    public ChatTab getActiveTab() {
        return activeTab;
    }

    public void setActiveTab(ChatTab tab) {
        this.activeTab = tab;
    }

    /**
     * @param indexInHistory position of the message in {@code allMessages}, or {@code -1} if
     *                       the message has not yet been added to history.
     */
    public boolean shouldShow(Component message, List<GuiMessage> allMessages, int indexInHistory) {
        if (!EnhancedChatConfig.enableChatTabs) return true;
        if (!com.github.kdgaming0.enhancedchat.util.HypixelLocationState.isOnHypixel()) return true;
        if (activeTab == ChatTab.ALL) return true;

        String plain = plainText(message);

        if (isSeparator(plain)) {
            return separatorBelongsToActiveTab(allMessages, indexInHistory);
        }
        return activeTab.matches(plain);
    }

    // -----------------------------------------------------------------
    // Separator handling
    // -----------------------------------------------------------------

    /**
     * Scans the separator's tick-group for a non-separator message that matches the active
     * tab. Both directions are bounded by the tick boundary, so the scan size equals the
     * block size — typically a handful of messages for Hypixel banners and summaries.
     *
     * <p>When {@code index == -1} the separator is mid-insert and not yet in history. The
     * newest message at index 0 is either (a) part of the separator's own block and shares
     * its tick, or (b) from a different tick, in which case the separator stands alone.
     */
    private boolean separatorBelongsToActiveTab(List<GuiMessage> allMessages, int index) {
        if (allMessages.isEmpty()) return false;

        int anchorTick = index < 0
                ? allMessages.getFirst().addedTime()
                : allMessages.get(index).addedTime();

        // allMessages is newest-first: indices > current walk backwards in time (older),
        // indices < current walk forwards in time (newer).
        int start = Math.max(0, index);
        for (int i = start; i < allMessages.size(); i++) {
            GuiMessage candidate = allMessages.get(i);
            if (candidate.addedTime() != anchorTick) break;
            if (matchesAsNonSeparator(candidate)) return true;
        }
        for (int i = index - 1; i >= 0; i--) {
            GuiMessage candidate = allMessages.get(i);
            if (candidate.addedTime() != anchorTick) break;
            if (matchesAsNonSeparator(candidate)) return true;
        }
        return false;
    }

    private boolean matchesAsNonSeparator(GuiMessage message) {
        String plain = plainText(message.content());
        if (isSeparator(plain)) return false;
        return activeTab.matches(plain);
    }

    // -----------------------------------------------------------------
    // Text helpers
    // -----------------------------------------------------------------

    private static String plainText(Component message) {
        String plain = ChatFormatting.stripFormatting(message.getString());
        return ChatTextHelper.stripCompactSuffix(plain).trim();
    }

    private static boolean isSeparator(String plain) {
        return ChatTextHelper.isFullSeparator(plain) || ChatTextHelper.isCenteredSeparator(plain);
    }
}
