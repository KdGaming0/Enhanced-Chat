package com.github.kdgaming0.enhancedchat.mixin.chat;

import com.github.kdgaming0.enhancedchat.chat.access.ChatAccess;
import com.github.kdgaming0.enhancedchat.chat.compact.CompactMessageHandler;
import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compacts consecutive duplicate chat messages via {@link CompactMessageHandler}. Priority is
 * {@code Integer.MAX_VALUE} so every other chat mixin observes the already-compacted message.
 */
@Mixin(value = ChatComponent.class, priority = Integer.MAX_VALUE)
public abstract class CompactChatMixin {

    @Unique
    private CompactMessageHandler ec$compactHandler;
    @Unique
    private boolean ec$compactedThisMessage;

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
        Component processed = ec$handler().process(message);
        ec$compactedThisMessage = processed != message;
        return processed;
    }

    /**
     * After vanilla finishes adding the compacted message, rebuild the display queue so
     * stale lines from the prior occurrence are discarded.
     */
    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;"
                    + "Lnet/minecraft/network/chat/MessageSignature;"
                    + "Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;"
                    + "Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("TAIL"))
    private void ec$refreshAfterCompact(
            Component message, MessageSignature sig,
            net.minecraft.client.multiplayer.chat.GuiMessageSource source,
            GuiMessageTag tag, CallbackInfo ci) {
        if (ec$compactedThisMessage) {
            ec$compactedThisMessage = false;

            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.isSameThread()) {
                ((ChatAccess) this).ec$refreshMessages();
            } else {
                mc.execute(() -> ((ChatAccess) this).ec$refreshMessages());
            }
        }
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void ec$clearCompact(boolean clearHistory, CallbackInfo ci) {
        ec$handler().clear();
    }
}
