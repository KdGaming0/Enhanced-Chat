package com.github.kdgaming0.enhancedchat.chat.menu;

import com.github.kdgaming0.enhancedchat.chat.access.ChatAccess;
import com.github.kdgaming0.enhancedchat.chat.render.ChatTextHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jspecify.annotations.Nullable;

/**
 * Maps cursor positions to messages and converts {@link Component}s into copyable strings
 * (plain, body-only, or &-code annotated).
 *
 * <p>Resolution uses the line-to-message mapping maintained by
 * {@link com.github.kdgaming0.enhancedchat.chat.ChatLineTracker}; there is
 * no fallback path because the tracker is updated by the same injections that populate
 * {@code trimmedMessages}, so any resolvable line is guaranteed to have a mapping.
 */
public final class ChatMessageResolver {

    /** Chat area bottom margin (pixels from screen bottom to the bottom of the newest line). */
    private static final int CHAT_BOTTOM_MARGIN = 40;

    private static final Int2ObjectMap<ChatFormatting> FORMAT_BY_RGB = buildColorLookup();

    private ChatMessageResolver() {}

    /** @return the message under the given screen coordinates, or {@code null} if none. */
    public static @Nullable GuiMessage resolve(double screenY) {
        Minecraft mc = Minecraft.getInstance();
        ChatComponent chat = mc.gui.getChat();
        ChatAccess access = (ChatAccess) chat;

        List<GuiMessage.Line> trimmed = access.ec$getTrimmedMessages();
        if (trimmed.isEmpty()) return null;

        double scale = mc.options.chatScale().get();
        double lineSpacing = mc.options.chatLineSpacing().get();
        int entryHeight = (int) (9.0 * (lineSpacing + 1.0));
        if (entryHeight <= 0) return null;

        int screenHeight = mc.getWindow().getGuiScaledHeight();
        double localY = (screenY - screenHeight + CHAT_BOTTOM_MARGIN) / scale;
        int lineIndex = (int) Math.floor(-localY / entryHeight);
        if (lineIndex < 0 || lineIndex >= chat.getLinesPerPage()) return null;

        int trimmedIndex = lineIndex + access.ec$getChatScrollbarPos();
        if (trimmedIndex < 0 || trimmedIndex >= trimmed.size()) return null;

        return access.ec$getLineTracker().parentFor(trimmed.get(trimmedIndex));
    }

    // -----------------------------------------------------------------
    // Text extraction
    // -----------------------------------------------------------------

    /** Plain text with all formatting and compact suffixes stripped. */
    public static String toRawText(Component content) {
        String plain = ChatFormatting.stripFormatting(content.getString());
        return ChatTextHelper.stripCompactSuffix(plain);
    }

    /**
     * Message body only — everything after the first {@code ": "} separator. Falls back to
     * the full plain text if no sender prefix is present.
     */
    public static String toMessageBody(Component content) {
        String plain = toRawText(content);
        int sep = plain.indexOf(": ");
        return sep >= 0 ? plain.substring(sep + 2) : plain;
    }

    /** Text with {@code &}-prefixed color/format codes preserved. */
    public static String toFormattedText(Component content) {
        StringBuilder sb = new StringBuilder();
        Style[] previous = {Style.EMPTY};

        content.getVisualOrderText().accept((index, style, codePoint) -> {
            if (!style.equals(previous[0])) {
                appendStylePrefix(style, sb);
                previous[0] = style;
            }
            sb.appendCodePoint(codePoint);
            return true;
        });
        return ChatTextHelper.stripCompactSuffix(sb.toString());
    }

    // -----------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------

    private static void appendStylePrefix(Style style, StringBuilder sb) {
        sb.append("&r");

        TextColor color = style.getColor();
        if (color != null) {
            ChatFormatting fmt = FORMAT_BY_RGB.get(color.getValue());
            if (fmt != null) sb.append('&').append(fmt.getChar());
        }
        if (style.isBold())          sb.append("&l");
        if (style.isItalic())        sb.append("&o");
        if (style.isUnderlined())    sb.append("&n");
        if (style.isStrikethrough()) sb.append("&m");
        if (style.isObfuscated())    sb.append("&k");
    }

    private static Int2ObjectMap<ChatFormatting> buildColorLookup() {
        Int2ObjectOpenHashMap<ChatFormatting> map = new Int2ObjectOpenHashMap<>(16);
        for (ChatFormatting fmt : ChatFormatting.values()) {
            if (fmt.isColor() && fmt.getColor() != null) {
                map.put((int) fmt.getColor(), fmt);
            }
        }
        return Int2ObjectMaps.unmodifiable(map);
    }
}
