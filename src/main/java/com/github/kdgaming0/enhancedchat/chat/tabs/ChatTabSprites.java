package com.github.kdgaming0.enhancedchat.chat.tabs;

import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.Identifier;

import static com.github.kdgaming0.enhancedchat.EnhancedChat.MOD_ID;

/**
 * Resource-pack-retexturable sprite sets for chat tab buttons.
 */
public final class ChatTabSprites {

    public static final WidgetSprites INACTIVE = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "chat/button"),
            Identifier.fromNamespaceAndPath(MOD_ID, "chat/button_disabled"),
            Identifier.fromNamespaceAndPath(MOD_ID, "chat/button_highlighted"));

    public static final WidgetSprites ACTIVE = new WidgetSprites(
            Identifier.fromNamespaceAndPath(MOD_ID, "chat/button_toggled"),
            Identifier.fromNamespaceAndPath(MOD_ID, "chat/button_disabled"),
            Identifier.fromNamespaceAndPath(MOD_ID, "chat/button_toggled_highlighted"));

    private ChatTabSprites() {
    }
}
