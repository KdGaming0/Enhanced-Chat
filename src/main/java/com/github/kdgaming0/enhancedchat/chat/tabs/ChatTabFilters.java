/**
 * Hypixel chat channel matching rules.
 *
 * Some of these message patterns were inspired by similar filtering logic in
 * https://github.com/YellowBirb/hypixel-chat-tabs.
 */

package com.github.kdgaming0.enhancedchat.chat.tabs;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Named, self-contained predicates describing which messages belong to each Hypixel channel.
 *
 * <p>Generic status messages ("You're already in this channel!" etc.) are deliberately NOT
 * matched by any channel predicate: they are identical across channels and would otherwise
 * leak into the wrong tab. They remain visible in the ALL tab.
 */
public final class ChatTabFilters {

    public static final Predicate<String> ALL = s -> true;
    public static final Predicate<String> PARTY = ChatTabFilters::isPartyMessage;
    public static final Predicate<String> GUILD = ChatTabFilters::isGuildMessage;
    public static final Predicate<String> PRIVATE_MESSAGE = ChatTabFilters::isPrivateMessage;
    public static final Predicate<String> COOP = ChatTabFilters::isCoopMessage;
    private static final Pattern USER_MESSAGE_PATTERN = Pattern.compile(
            "^(?:(?:Party|Guild|Co-op|Officer) > |(?:To|From) |Friend > )?(?:\\[[^]]+] )*[a-zA-Z0-9_]{1,16}(?: \\[[^]]+])?: .*"
    );
    public static final Predicate<String> USER = ChatTabFilters::isUserMessage;

    // -- User ---------------------------------------------------------

    private ChatTabFilters() {
    }

    private static boolean isUserMessage(String s) {
        return USER_MESSAGE_PATTERN.matcher(s).matches();
    }

    // -- Party --------------------------------------------------------

    private static boolean isPartyMessage(String s) {
        return hasChannelPrefix(s, "Party > ", "P > ")
                || isPartyLifecycle(s)
                || isPartyMembership(s)
                || isPartyChannelStatus(s);
    }

    private static boolean isPartyLifecycle(String s) {
        return s.contains("has invited you to join their party!")
                || s.contains("to the party! They have 60 seconds to accept.")
                || s.contains("has disbanded the party!")
                || s.startsWith("The party was transferred to ")
                || s.equals("The party was disbanded because all invites expired "
                + "and the party was empty");
    }

    private static boolean isPartyMembership(String s) {
        return s.endsWith("joined the party.")
                || s.endsWith("has left the party.")
                || s.endsWith("has been removed from the party.");
    }

    private static boolean isPartyChannelStatus(String s) {
        return s.equals("You are now in the PARTY channel")
                || s.equals("You must be in a party to join the party channel!");
    }

    // -- Guild --------------------------------------------------------

    private static boolean isGuildMessage(String s) {
        return hasChannelPrefix(s, "Guild > ", "G > ")
                || s.equals("You are now in the GUILD channel");
    }

    // -- PM -----------------------------------------------------------

    private static boolean isPrivateMessage(String s) {
        return s.startsWith("To ") || s.startsWith("From ") || s.startsWith("Friend > ");
    }

    // -- Co-op --------------------------------------------------------

    private static boolean isCoopMessage(String s) {
        return s.startsWith("Co-op > ")
                || s.equals("You are now in the SKYBLOCK CO-OP channel");
    }

    // -- Utilities ----------------------------------------------------

    private static boolean hasChannelPrefix(String s, String... prefixes) {
        for (String prefix : prefixes) {
            if (s.startsWith(prefix)) return true;
        }
        return false;
    }
}
