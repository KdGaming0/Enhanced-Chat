package com.github.kdgaming0.enhancedchat.mixin.chat;

import com.github.kdgaming0.enhancedchat.chat.access.ChatAccess;
import com.github.kdgaming0.enhancedchat.chat.compact.CompactMessageHandler;
import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Compacts consecutive duplicate chat messages via {@link CompactMessageHandler}. Priority is
 * {@code Integer.MAX_VALUE} so every other chat mixin observes the already-compacted message.
 */
@Mixin(value = ChatComponent.class, priority = Integer.MAX_VALUE)
public abstract class CompactChatMixin {

    @Unique
    private CompactMessageHandler ec$compactHandler;

    @Unique
    private CompactMessageHandler ec$handler() {
        if (ec$compactHandler == null) {
            ec$compactHandler = new CompactMessageHandler((ChatAccess) this);
        }
        return ec$compactHandler;
    }

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;"
                    + "Lnet/minecraft/network/chat/MessageSignature;"
                    + "Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;"
                    + "Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private Component ec$compact(Component message) {
        if (!EnhancedChatConfig.compactDuplicateMessages) return message;
        return ec$handler().process(message);
    }

    /**
     * After vanilla finishes adding the message, bind the new history entry (now at index 0) to
     * the compact handler so the next duplicate of this text can be located by identity.
     *
     * <p>A collapsed duplicate's stale prior line is already removed in place by
     * {@link CompactMessageHandler#process} (via {@link ChatAccess#ec$dropMessageLines}), so no
     * display-queue rebuild is scheduled here.
     */
    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;"
                    + "Lnet/minecraft/network/chat/MessageSignature;"
                    + "Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;"
                    + "Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("TAIL"))
    private void ec$afterAddMessage(
            Component message, MessageSignature sig,
            GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        if (!EnhancedChatConfig.compactDuplicateMessages) return;

        List<GuiMessage> all = ((ChatAccess) this).ec$getAllMessages();
        if (!all.isEmpty()) {
            ec$handler().noteAddedMessage(all.getFirst());
        }
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void ec$clearCompact(boolean clearHistory, CallbackInfo ci) {
        ec$handler().clear();
    }
}
