package com.github.kdgaming0.enhancedchat.chat.tabs;

import java.util.function.Predicate;

/**
 * A Hypixel channel tab: its label, the command that switches into the channel, and its filter.
 */
public enum ChatTab {
    ALL("A", "/chat a", ChatTabFilters.ALL),
    USER("User", "", ChatTabFilters.USER),
    PARTY("P", "/chat p", ChatTabFilters.PARTY),
    GUILD("G", "/chat g", ChatTabFilters.GUILD),
    PM("PM", "", ChatTabFilters.PRIVATE_MESSAGE),
    COOP("Coop", "/chat skyblock-coop", ChatTabFilters.COOP);

    private final String label;
    private final String command;
    private final Predicate<String> filter;

    ChatTab(String label, String command, Predicate<String> filter) {
        this.label = label;
        this.command = command;
        this.filter = filter;
    }

    public String label() {
        return label;
    }

    public String command() {
        return command;
    }

    public boolean matches(String plainText) {
        return filter.test(plainText);
    }
}
