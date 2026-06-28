package com.github.kdgaming0.enhancedchat.chat.access;

import com.github.kdgaming0.enhancedchat.chat.ChatLineTracker;
import net.minecraft.client.multiplayer.chat.GuiMessage;

import java.util.List;

/**
 * Duck interface mixed into {@link net.minecraft.client.gui.components.ChatComponent}.
 * Exposes the collaborators external chat features need: raw/display history, scroll state,
 * scaled geometry, and the per-instance {@link ChatLineTracker}.
 */
public interface ChatAccess {

    List<GuiMessage> ec$getAllMessages();

    List<GuiMessage.Line> ec$getTrimmedMessages();

    int ec$getChatScrollbarPos();

    int ec$getScaledWidth();

    void ec$refreshMessages();

    /**
     * Removes every display line belonging to {@code message} from the live display queue,
     * adjusting the scroll offset so the visible region stays put. Lets the compact feature drop
     * a collapsed duplicate in place instead of rebuilding the whole display queue.
     */
    void ec$dropMessageLines(GuiMessage message);

    ChatLineTracker ec$getLineTracker();
}
