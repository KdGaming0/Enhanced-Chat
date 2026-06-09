package com.github.kdgaming0.enhancedchat.chat;

import com.github.kdgaming0.enhancedchat.chat.search.ChatSearchController;
import com.github.kdgaming0.enhancedchat.chat.tabs.ChatTabController;

/**
 * Single entry point for all chat feature runtime state. Exposes the search and tab
 * controllers so callers can observe and mutate them through one well-known object.
 *
 * <p>Exposed as a singleton for convenience, but implemented as a proper instance so tests
 * can construct their own state and so future features can swap the controllers without
 * touching call sites.
 */
public final class ChatFeatureState {

    private static final ChatFeatureState INSTANCE = new ChatFeatureState();

    private final ChatSearchController search = new ChatSearchController();
    private final ChatTabController tabs = new ChatTabController();

    private ChatFeatureState() {}

    public static ChatFeatureState get() {
        return INSTANCE;
    }

    public ChatSearchController search() {
        return search;
    }

    public ChatTabController tabs() {
        return tabs;
    }
}
