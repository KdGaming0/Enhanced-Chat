package com.github.kdgaming0.enhancedchat.chat.render;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for analysing Hypixel-formatted chat text.
 */
public final class ChatTextHelper {

    public static final int HYPIXEL_DEFAULT_WIDTH = 320;

    private static final int CENTER_TOLERANCE_PX = 12;
    private static final int MIN_LEADING_SPACES_FOR_CENTERED = 2;
    private static final int MIN_SEPARATOR_LEN = 5;
    private static final int MIN_CENTERED_SEPARATOR_LEN = 10;
    private static final String COMPACT_SUFFIX_OPEN = " (×";

    private ChatTextHelper() {
    }

    // -----------------------------------------------------------------
    // Conversions
    // -----------------------------------------------------------------

    /**
     * Collects a FormattedCharSequence into a plain String.
     */
    public static String getString(FormattedCharSequence sequence) {
        StringBuilder sb = new StringBuilder();
        sequence.accept((index, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.toString();
    }

    /**
     * Returns a {@link FormattedCharSequence} with leading and trailing spaces stripped,
     * preserving inner formatting. The iteration is snapshotted into primitive arrays once
     * so the returned sequence is cheap to re-accept on every render frame.
     */
    public static FormattedCharSequence trim(FormattedCharSequence sequence) {
        IntList positions = new IntArrayList();
        IntList codePoints = new IntArrayList();
        List<Style> styles = new ArrayList<>();

        sequence.accept((index, style, cp) -> {
            positions.add(index);
            codePoints.add(cp);
            styles.add(style);
            return true;
        });

        int size = codePoints.size();
        int start = 0;
        while (start < size && codePoints.getInt(start) == ' ') start++;
        int end = size - 1;
        while (end >= 0 && codePoints.getInt(end) == ' ') end--;

        final int from = start;
        final int to = end;
        final int[] posArr = positions.toIntArray();
        final int[] cpArr = codePoints.toIntArray();
        final Style[] styleArr = styles.toArray(new Style[0]);

        return sink -> {
            for (int i = from; i <= to; i++) {
                if (!sink.accept(posArr[i], styleArr[i], cpArr[i])) return false;
            }
            return true;
        };
    }

    /**
     * Rebuilds a Component from a sequence so Minecraft can word-wrap it.
     */
    public static Component toComponent(FormattedCharSequence sequence) {
        MutableComponent result = Component.empty();
        StringBuilder buffer = new StringBuilder();
        Style[] current = {Style.EMPTY};

        sequence.accept((index, style, cp) -> {
            if (!style.equals(current[0])) {
                if (!buffer.isEmpty()) {
                    result.append(Component.literal(buffer.toString()).withStyle(current[0]));
                    buffer.setLength(0);
                }
                current[0] = style;
            }
            buffer.appendCodePoint(cp);
            return true;
        });

        if (!buffer.isEmpty()) {
            result.append(Component.literal(buffer.toString()).withStyle(current[0]));
        }
        return result;
    }

    /**
     * First text colour in the sequence, defaulting to opaque white.
     */
    public static int extractColor(FormattedCharSequence sequence) {
        int[] color = {0xFFFFFFFF};
        sequence.accept((index, style, cp) -> {
            if (style.getColor() != null) {
                color[0] = ARGB.opaque(style.getColor().getValue());
                return false;
            }
            return true;
        });
        return color[0];
    }

    // -----------------------------------------------------------------
    // Line classification
    // -----------------------------------------------------------------

    public static boolean isCenteredText(Font font, String raw, String trimmed) {
        String cleanRaw = stripCompactSuffix(raw);
        String cleanTrimmed = stripCompactSuffix(trimmed);

        if (!cleanRaw.startsWith(" ") || cleanTrimmed.isEmpty()) return false;

        int leadingSpaces = indexOfFirstNonSpace(cleanRaw);
        if (leadingSpaces < MIN_LEADING_SPACES_FOR_CENTERED) return false;

        int expectedPx = (HYPIXEL_DEFAULT_WIDTH - font.width(cleanTrimmed)) / 2;
        int actualPx = font.width(cleanRaw.substring(0, leadingSpaces));
        return Math.abs(expectedPx - actualPx) <= CENTER_TOLERANCE_PX;
    }

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

    public static String extractMiddleText(String fullString) {
        String clean = stripCompactSuffix(fullString.trim());
        int start = 0;
        while (start < clean.length() && isDashOrSpace(clean.charAt(start))) start++;
        int end = clean.length() - 1;
        while (end >= 0 && isDashOrSpace(clean.charAt(end))) end--;
        return start <= end ? clean.substring(start, end + 1) : "";
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

    private static boolean isDashOrSpace(char c) {
        return isSeparatorChar(c) || c == ' ';
    }

    private static int indexOfFirstNonSpace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != ' ') return i;
        }
        return -1;
    }
}
