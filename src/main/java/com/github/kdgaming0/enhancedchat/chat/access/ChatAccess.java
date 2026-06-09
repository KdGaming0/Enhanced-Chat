package com.github.kdgaming0.enhancedchat.chat.access;

import com.github.kdgaming0.enhancedchat.chat.ChatLineTracker;
import java.util.List;
import net.minecraft.client.multiplayer.chat.GuiMessage;

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

    ChatLineTracker ec$getLineTracker();
}
