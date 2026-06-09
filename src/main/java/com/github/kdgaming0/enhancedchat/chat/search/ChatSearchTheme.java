package com.github.kdgaming0.enhancedchat.chat.search;

import com.github.kdgaming0.enhancedchat.chat.ChatFeatureState;

/**
 * Layout metrics and colour constants for the chat search bar. Lives outside the mixin so the
 * tab mixin can share the vertical offset calculations.
 */
public final class ChatSearchTheme {

    // -- Layout -------------------------------------------------------

    public static final int SEARCH_BAR_HEIGHT = 10;

    /** Gap between the search bar and the chat input below it. */
    private static final int SEARCH_BAR_GAP = 4;

    /** Extra height the search bar claims above the chat input, including its gap. */
    private static final int EXTRA_OFFSET = SEARCH_BAR_HEIGHT + SEARCH_BAR_GAP;

    private static final int SEARCH_BAR_BOTTOM_MARGIN = 12;

    // -- Colours ------------------------------------------------------

    /** Semi-transparent dark fill, matching the vanilla chat input. */
    public static final int BACKGROUND = 0x60000000;

    /** Border and hint text colour. */
    public static final int BORDER = 0xFFAAAAAA;
    public static final int HINT_RGB = 0x00AAAAAA;
    public static final int MATCH_COUNT_TEXT = 0xFFAAAAAA;

    // -- Hint timing --------------------------------------------------

    public static final long HINT_DURATION_MS = 4000L;
    public static final long HINT_FADE_MS = 600L;

    private ChatSearchTheme() {}

    /** Additional vertical space consumed above the input line when search is visible. */
    public static int extraOffset() {
        return ChatFeatureState.get().search().isActive() ? EXTRA_OFFSET : 0;
    }

    /** Y coordinate of the search bar's top edge. */
    public static int searchBarY(int screenHeight) {
        return screenHeight - SEARCH_BAR_BOTTOM_MARGIN - SEARCH_BAR_HEIGHT - SEARCH_BAR_GAP;
    }
}
