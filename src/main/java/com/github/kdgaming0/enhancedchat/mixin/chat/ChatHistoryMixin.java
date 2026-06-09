package com.github.kdgaming0.enhancedchat.mixin.chat;

import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Overrides the hardcoded chat history limit of 100 with a configurable value, affecting both
 * the display-queue trim and the raw message history.
 */
@Mixin(ChatComponent.class)
public class ChatHistoryMixin {

    @ModifyExpressionValue(
            method = {"addMessageToDisplayQueue", "addMessageToQueue"},
            at = @At(value = "CONSTANT", args = "intValue=100"))
    private int ec$expandHistory(int original) {
        return EnhancedChatConfig.extendedChatHistory
                ? EnhancedChatConfig.chatHistorySize
                : original;
    }
}
