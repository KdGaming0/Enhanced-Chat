package com.github.kdgaming0.enhancedchat.chat.render;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Utilities for analysing Hypixel-formatted chat text.
 */
public final class ChatTextHelper {

    private static final int MIN_SEPARATOR_LEN = 5;
    private static final int MIN_CENTERED_SEPARATOR_LEN = 10;
    private static final String COMPACT_SUFFIX_OPEN = " (×";

    private ChatTextHelper() {
    }

    // -----------------------------------------------------------------
    // Line classification
    // -----------------------------------------------------------------

    public static boolean isFullSeparator(String trimmed) {
        String clean = stripCompactSuffix(trimmed);
        if (clean.length() < MIN_SEPARATOR_LEN) return false;
        for (int i = 0; i < clean.length(); i++) {
            if (!isSeparatorChar(clean.charAt(i))) return false;
        }
        return true;
    }

    public static boolean isCenteredSeparator(String trimmed) {
        String clean = stripCompactSuffix(trimmed);
        if (clean.length() < MIN_CENTERED_SEPARATOR_LEN
                || !isSeparatorChar(clean.charAt(0))
                || !isSeparatorChar(clean.charAt(clean.length() - 1))) {
            return false;
        }
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (!isSeparatorChar(c) && c != ' ') return true;
        }
        return false;
    }

    /**
     * Flattens a chat component to its plain, format-stripped, compact-suffix-free, trimmed text.
     * Shared by the tab filter and the searchable-text builder so the flattening rules stay in
     * one place.
     */
    public static String plainText(Component content) {
        String plain = ChatFormatting.stripFormatting(content.getString());
        return stripCompactSuffix(plain).trim();
    }

    // -----------------------------------------------------------------
    // Compact suffix handling
    // -----------------------------------------------------------------

    public static String stripCompactSuffix(String s) {
        int idx = compactSuffixStart(s);
        return idx < 0 ? s : s.substring(0, idx);
    }

    private static int compactSuffixStart(String s) {
        int idx = s.lastIndexOf(COMPACT_SUFFIX_OPEN);
        if (idx <= 0 || !s.endsWith(")")) return -1;
        for (int i = idx + COMPACT_SUFFIX_OPEN.length(); i < s.length() - 1; i++) {
            if (!Character.isDigit(s.charAt(i))) return -1;
        }
        return idx;
    }

    // -----------------------------------------------------------------
    // Character predicates
    // -----------------------------------------------------------------

    private static boolean isSeparatorChar(char c) {
        return c == '-' || c == '—' || c == '=' || c == '▬';
    }
}
