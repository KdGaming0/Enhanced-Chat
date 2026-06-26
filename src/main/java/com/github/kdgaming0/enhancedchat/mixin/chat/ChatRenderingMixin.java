package com.github.kdgaming0.enhancedchat.mixin.chat;

import com.github.kdgaming0.enhancedchat.chat.ChatLineTracker;
import com.github.kdgaming0.enhancedchat.chat.access.ChatAccess;
import com.github.kdgaming0.enhancedchat.chat.render.ChatGraphicsAccessProxy;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Adapter between vanilla {@link ChatComponent} and the chat-rendering feature classes.
 *
 * <p>The mixin holds one piece of persistent state — the per-instance {@link ChatLineTracker}
 * — and delegates every non-trivial decision to the feature classes. When all affected config
 * options are disabled and the player is off Hypixel, the proxy wrap still runs but every
 * lookup short-circuits to {@code null}, preserving vanilla behaviour exactly.
 */
@Mixin(ChatComponent.class)
public abstract class ChatRenderingMixin implements ChatAccess {

    @Unique
    private final ChatLineTracker ec$lineTracker = new ChatLineTracker();
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow @Final private List<GuiMessage>       allMessages;
    @Shadow private int     chatScrollbarPos;
    @Shadow private boolean newMessageSinceScroll;

    // Proxy reuse cache — avoids per-frame allocation when delegate/font/line-spacing are stable.
    @Unique private ChatGraphicsAccessProxy      ec$renderProxy;
    @Unique private ChatComponent.ChatGraphicsAccess ec$lastDelegate;
    @Unique private Font                         ec$lastProxyFont;
    @Unique private double                       ec$lastLineSpacing = -1.0;

    @Shadow private int    getWidth() { throw new AssertionError(); }
    @Shadow private double getScale() { throw new AssertionError(); }
    @Shadow private void   refreshTrimmedMessages() {}

    // ---------- ChatAccess ----------

    @Override public List<GuiMessage>      ec$getAllMessages()     { return allMessages; }
    @Override public List<GuiMessage.Line> ec$getTrimmedMessages() { return trimmedMessages; }
    @Override public int                   ec$getChatScrollbarPos(){ return chatScrollbarPos; }
    @Override public int                   ec$getScaledWidth()     { return Mth.floor(getWidth() / getScale()); }
    @Override public ChatLineTracker       ec$getLineTracker()     { return ec$lineTracker; }

    /**
     * Clamps {@code chatScrollbarPos} to the valid range and snaps the "new message" indicator
     * off when at the bottom.
     *
     * <p>Early-exits when already at the bottom (the common case) to skip the
     * {@code getLinesPerPage()} call, which reads options and performs division.
     */
    @Unique
    private void ec$clampScroll() {
        if (this.chatScrollbarPos <= 0) {
            this.chatScrollbarPos = 0;
            this.newMessageSinceScroll = false;
            return;
        }
        int linesPerPage = ((ChatComponent) (Object) this).getLinesPerPage();
        int maxScroll    = Math.max(0, this.trimmedMessages.size() - linesPerPage);
        this.chatScrollbarPos = Math.min(this.chatScrollbarPos, maxScroll);
        if (this.chatScrollbarPos <= 0) {
            this.chatScrollbarPos = 0;
            this.newMessageSinceScroll = false;
        }
    }

    /**
     * Rebuilds the display queue while preserving the player's scroll offset.
     * This prevents mod features that refresh the display (compact chat, message
     * deletion, tab/search filters) from force-snapping the chat back to the bottom.
     */
    @Unique
    private void ec$clampScrollPost() {
        ec$clampScroll();
    }

    @Override
    public void ec$refreshMessages() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (!mc.isSameThread()) {
            mc.execute(this::ec$refreshMessages);
            return;
        }

        int     savedScroll     = this.chatScrollbarPos;
        boolean savedNewMessage = this.newMessageSinceScroll;

        this.chatScrollbarPos    = 0;
        this.newMessageSinceScroll = false;

        refreshTrimmedMessages();

        this.chatScrollbarPos    = savedScroll;
        this.newMessageSinceScroll = savedNewMessage;
        ec$clampScroll();
    }

    // ---------- Render proxy ----------

    @WrapOperation(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;"
                            + "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;"
                            + "IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V"))
    private void ec$proxyGraphicsAccess(
            ChatComponent instance,
            ChatComponent.ChatGraphicsAccess access,
            int screenHeight, int ticks, ChatComponent.DisplayMode displayMode,
            Operation<Void> original,
            @Local(argsOnly = true) GuiGraphicsExtractor graphics,
            @Local(argsOnly = true) Font font) {

        double lineSpacing = net.minecraft.client.Minecraft.getInstance().options.chatLineSpacing().get();
        ChatGraphicsAccessProxy proxy = ec$renderProxy;
        if (proxy == null || ec$lastDelegate != access || ec$lastProxyFont != font
                || ec$lastLineSpacing != lineSpacing) {
            proxy              = new ChatGraphicsAccessProxy(access, this, graphics);
            ec$renderProxy     = proxy;
            ec$lastDelegate    = access;
            ec$lastProxyFont   = font;
            ec$lastLineSpacing = lineSpacing;
        } else {
            proxy.prepareForFrame(access, graphics);
        }
        original.call(instance, proxy, screenHeight, ticks, displayMode);
        proxy.finishOutline();
    }

    @WrapOperation(
            method = "captureClickableText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;"
                            + "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;"
                            + "IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V"))
    private void ec$proxyClickableTextAccess(
            ChatComponent instance,
            ChatComponent.ChatGraphicsAccess access,
            int screenHeight, int ticks, ChatComponent.DisplayMode displayMode,
            Operation<Void> original) {

        ChatGraphicsAccessProxy proxy = new ChatGraphicsAccessProxy(access, this, null);
        original.call(instance, proxy, screenHeight, ticks, displayMode);
    }

    // ---------- Line processing ----------

    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"))
    private void ec$beginLineBatch(GuiMessage message, CallbackInfo ci) {
        ec$lineTracker.beginAddingLinesFor(message);
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At("TAIL"))
    private void ec$endLineBatch(GuiMessage message, CallbackInfo ci) {
        ec$lineTracker.finishAddingLines();
    }

    /**
     * Fires immediately after each {@code trimmedMessages.addFirst(...)}, associating the
     * freshly-added display line with its parent message.
     */
    @Inject(
            method = "addMessageToDisplayQueue",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V",
                    shift = At.Shift.AFTER))
    private void ec$registerAddedLine(CallbackInfo ci) {
        ec$lineTracker.recordLine(trimmedMessages.getFirst());
    }

    @WrapOperation(
            method = "addMessageToDisplayQueue",
            at = @At(value = "INVOKE", target = "Ljava/util/List;removeLast()Ljava/lang/Object;"))
    private <E> E ec$evictLine(List<E> instance, Operation<E> original) {
        @SuppressWarnings("unchecked")
        GuiMessage.Line evicted = (GuiMessage.Line) instance.getLast();
        ec$lineTracker.evictLine(evicted);
        return original.call(instance);
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void ec$clearAll(boolean clearHistory, CallbackInfo ci) {
        ec$lineTracker.clearAll();
    }

    @Inject(method = "clearMessages", at = @At("TAIL"))
    private void ec$clampScrollAfterClear(boolean clearHistory, CallbackInfo ci) {
        this.chatScrollbarPos    = 0;
        this.newMessageSinceScroll = false;
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
    private void ec$clearOnRefresh(CallbackInfo ci) {
        // Preserve selection across refresh: the selected GuiMessage still exists in
        // allMessages, so re-deriving its lines is enough to restore the outline.
        ec$lineTracker.clearLineMappings();
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("TAIL"))
    private void ec$clampScrollAfterRefresh(CallbackInfo ci) {
        ec$clampScroll();
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"))
    private void ec$clampScrollBeforeRender(CallbackInfo ci) {
        ec$clampScroll();
    }
}