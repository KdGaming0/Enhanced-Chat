package com.github.kdgaming0.enhancedchat.mixin.chat;

import com.github.kdgaming0.enhancedchat.chat.ChatLineTracker;
import com.github.kdgaming0.enhancedchat.chat.access.ChatAccess;
import com.github.kdgaming0.enhancedchat.chat.render.ChatGraphicsAccessProxy;
import com.github.kdgaming0.enhancedchat.chat.render.ChatLineProcessor;
import com.github.kdgaming0.enhancedchat.chat.render.CustomChatRenderer;
import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
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
    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;
    @Shadow
    @Final
    private List<GuiMessage> allMessages;
    @Shadow
    private int chatScrollbarPos;
    @Shadow
    private boolean newMessageSinceScroll;
    // Proxy reuse cache — avoids per-frame allocation when delegate/font/line-spacing are stable.
    @Unique
    private ChatGraphicsAccessProxy ec$renderProxy;
    @Unique
    private ChatComponent.ChatGraphicsAccess ec$lastDelegate;
    @Unique
    private Font ec$lastProxyFont;
    @Unique
    private double ec$lastLineSpacing = -1.0;

    @Shadow
    private int getWidth() {
        throw new AssertionError();
    }

    @Shadow
    private double getScale() {
        throw new AssertionError();
    }

    @Shadow
    private void refreshTrimmedMessages() {
    }

    // ---------- ChatAccess ----------

    @Override
    public List<GuiMessage> ec$getAllMessages() {
        return allMessages;
    }

    @Override
    public List<GuiMessage.Line> ec$getTrimmedMessages() {
        return trimmedMessages;
    }

    @Override
    public int ec$getChatScrollbarPos() {
        return chatScrollbarPos;
    }

    @Override
    public int ec$getScaledWidth() {
        return Mth.floor(getWidth() / getScale());
    }

    @Override
    public ChatLineTracker ec$getLineTracker() {
        return ec$lineTracker;
    }

    /**
     * Rebuilds the display queue while preserving the player's scroll offset.
     * This prevents mod features that refresh the display (compact chat, message
     * deletion, tab/search filters) from force-snapping the chat back to the bottom.
     */
    @Unique
    private void ec$clampScroll() {
        int linesPerPage = ((ChatComponent) (Object) this).getLinesPerPage();
        int maxScroll = Math.max(0, this.trimmedMessages.size() - linesPerPage);
        this.chatScrollbarPos = Math.min(this.chatScrollbarPos, maxScroll);
        if (this.chatScrollbarPos <= 0) {
            this.chatScrollbarPos = 0;
            this.newMessageSinceScroll = false;
        }
    }

    @Override
    public void ec$refreshMessages() {
        // Ensure UI rebuilds only happen on the main thread to prevent concurrent
        // modification exceptions during render frames.
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (!mc.isSameThread()) {
            mc.execute(this::ec$refreshMessages);
            return;
        }

        int savedScroll = this.chatScrollbarPos;
        boolean savedNewMessage = this.newMessageSinceScroll;

        // Pin to bottom while the queue is temporarily empty/small so that
        // addMessageToDisplayQueue's internal scrollChat(1) calls become no-ops.
        this.chatScrollbarPos = 0;
        this.newMessageSinceScroll = false;

        refreshTrimmedMessages();

        this.chatScrollbarPos = savedScroll;
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
            proxy = new ChatGraphicsAccessProxy(access, this, graphics, font);
            ec$renderProxy = proxy;
            ec$lastDelegate = access;
            ec$lastProxyFont = font;
            ec$lastLineSpacing = lineSpacing;
        } else {
            proxy.prepareForFrame(access, graphics, font);
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

        ChatGraphicsAccessProxy proxy = new ChatGraphicsAccessProxy(
                access, this, null, net.minecraft.client.Minecraft.getInstance().font);
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

    @WrapOperation(
            method = "addMessageToDisplayQueue",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage;splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;"))
    private List<FormattedCharSequence> ec$processLines(
            GuiMessage instance, Font font, int width,
            Operation<List<FormattedCharSequence>> original,
            @Share("ec_renderers") LocalRef<List<CustomChatRenderer>> renderersRef) {

        boolean centerEnabled = EnhancedChatConfig.centerHypixelText;
        boolean separatorsEnabled = EnhancedChatConfig.smoothSeparators;

        // True vanilla path when no rendering feature is active or we're off Hypixel.
        if (!com.github.kdgaming0.enhancedchat.util.HypixelLocationState.isOnHypixel()
                || (!centerEnabled && !separatorsEnabled)) {
            renderersRef.set(null);
            return original.call(instance, font, width);
        }

        // Step 1: bypass initial word-wrapping to inspect raw \n-separated lines.
        List<FormattedCharSequence> rawLines = original.call(instance, font, Integer.MAX_VALUE);

        // Step 2: classify and re-wrap, producing per-line renderers.
        ChatLineProcessor.Result result = ChatLineProcessor.process(
                rawLines, font, width, centerEnabled, separatorsEnabled);

        renderersRef.set(result.renderers());
        return result.lines();
    }

    /**
     * Fires immediately after each {@code trimmedMessages.addFirst(...)}. The {@code i} local
     * (ordinal=1) is the line index within the current message.
     */
    @Inject(
            method = "addMessageToDisplayQueue",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V",
                    shift = At.Shift.AFTER))
    private void ec$registerAddedLine(
            CallbackInfo ci,
            @Share("ec_renderers") LocalRef<List<CustomChatRenderer>> renderersRef,
            @Local(ordinal = 1) int lineIndex) {

        GuiMessage.Line added = trimmedMessages.getFirst();

        List<CustomChatRenderer> renderers = renderersRef.get();
        CustomChatRenderer renderer = (renderers != null && lineIndex < renderers.size())
                ? renderers.get(lineIndex)
                : null;

        ec$lineTracker.recordLine(added, renderer);
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
        this.chatScrollbarPos = 0;
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
