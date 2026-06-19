/*
 * <p>Adapted from the {@code ChatComponentMixin} in Ezzenix's ChatAnimation
 * (<a href="https://github.com/Ezzenix/ChatAnimation">github.com/Ezzenix/ChatAnimation</a>,
 * licensed CC0-1.0): the wrap-operation injection point and matrix-translate approach are
 * carried over from that mixin, with the easing function, field naming, and the
 * displayed-lines tracking reworked here.
 */

package com.github.kdgaming0.enhancedchat.mixin.chat;

import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Slides newly-arriving chat messages up into view instead of popping them in instantly.
 *
 * <p>Only triggers when the chat is scrolled to the bottom and the incoming message actually
 * added visible lines (filtered-out messages don't animate).
 */
@Mixin(ChatComponent.class)
public abstract class ChatAnimationMixin {

    @Unique
    private static final float ANIMATION_LINE_FACTOR = 0.8f;

    @Shadow
    private int chatScrollbarPos;
    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;
    @Unique
    private long ec$lastMessageTime;
    @Unique
    private int ec$displaySizeBefore;
    @Unique
    private boolean ec$animationIdle = true;

    @Shadow
    protected abstract int getLineHeight();

    @Unique
    private float ec$displacement() {
        if (!EnhancedChatConfig.enableChatAnimation || chatScrollbarPos != 0) return 0f;
        if (ec$animationIdle) return 0f;

        int duration = EnhancedChatConfig.chatAnimationDurationMs;
        float elapsed = System.currentTimeMillis() - ec$lastMessageTime;
        if (elapsed >= duration) {
            ec$animationIdle = true;
            return 0f;
        }
        float progress = elapsed / (float) duration;
        float eased = 1f - (1f - progress) * (1f - progress); // ease-out quadratic
        return getLineHeight() * ANIMATION_LINE_FACTOR * (1f - eased);
    }

    @WrapOperation(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;"
                            + "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;"
                            + "IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V"))
    private void ec$animateRender(
            ChatComponent instance,
            ChatComponent.ChatGraphicsAccess access,
            int screenHeight, int ticks, ChatComponent.DisplayMode displayMode,
            Operation<Void> original,
            @Local(argsOnly = true) GuiGraphicsExtractor graphics) {

        float dy = ec$displacement();
        if (dy != 0f) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(0f, dy);
        }
        original.call(instance, access, screenHeight, ticks, displayMode);
        if (dy != 0f) graphics.pose().popMatrix();
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;"
                    + "Lnet/minecraft/network/chat/MessageSignature;"
                    + "Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;"
                    + "Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"))
    private void ec$snapshotDisplaySize(
            Component message, MessageSignature sig,
            net.minecraft.client.multiplayer.chat.GuiMessageSource source,
            GuiMessageTag tag, CallbackInfo ci) {
        ec$displaySizeBefore = trimmedMessages.size();
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;"
                    + "Lnet/minecraft/network/chat/MessageSignature;"
                    + "Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;"
                    + "Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("TAIL"))
    private void ec$recordMessageTime(
            Component message, MessageSignature sig,
            net.minecraft.client.multiplayer.chat.GuiMessageSource source,
            GuiMessageTag tag, CallbackInfo ci) {
        // Only start a new animation if the message actually produced visible lines.
        if (trimmedMessages.size() > ec$displaySizeBefore) {
            ec$lastMessageTime = System.currentTimeMillis();
            ec$animationIdle = false;
        }
    }
}
