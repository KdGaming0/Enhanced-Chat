package com.github.kdgaming0.enhancedchat.mixin.chat.tab;

import com.github.kdgaming0.enhancedchat.chat.ChatFeatureState;
import com.github.kdgaming0.enhancedchat.chat.access.ChatAccess;
import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.List;

/**
 * Hides messages that don't match the active chat tab.
 *
 * <p>Priority 900 runs this before
 * {@link com.github.kdgaming0.enhancedchat.mixin.chat.search.ChatSearchFilterMixin}
 * (priority 1000), giving the deterministic filter chain: tab → search → display.
 *
 * <p>When chat tabs are disabled in config, the filter is completely bypassed.
 */
@Mixin(value = ChatComponent.class, priority = 900)
public class ChatTabFilterMixin {

    /** Non-null only during a {@code refreshTrimmedMessages} rebuild cycle. */
    @Unique
    private IdentityHashMap<GuiMessage, Integer> ec$indexCache;

    /**
     * Builds a fast identity-keyed lookup of message → history index before the bulk rebuild
     * begins. Without this, {@code identityIndexOf} is called once per message in a loop that
     * itself iterates all messages — O(n²) for n messages in history.
     */
    @Inject(method = "refreshTrimmedMessages", at = @At("HEAD"))
    private void ec$buildIndexCache(CallbackInfo ci) {
        List<GuiMessage> history = ((ChatAccess) this).ec$getAllMessages();
        IdentityHashMap<GuiMessage, Integer> cache = new IdentityHashMap<>(history.size() * 2);
        for (int i = 0, n = history.size(); i < n; i++) {
            cache.put(history.get(i), i);
        }
        ec$indexCache = cache;
    }

    @Inject(method = "refreshTrimmedMessages", at = @At("TAIL"))
    private void ec$clearIndexCache(CallbackInfo ci) {
        ec$indexCache = null;
    }

    @WrapMethod(method = "addMessageToDisplayQueue")
    private void ec$filterByTab(GuiMessage message, Operation<Void> original) {
        if (!EnhancedChatConfig.enableChatTabs) {
            original.call(message);
            return;
        }

        List<GuiMessage> history = ((ChatAccess) this).ec$getAllMessages();
        int index = ec$indexCache != null
                ? ec$indexCache.getOrDefault(message, -1)
                : identityIndexOf(history, message);

        if (ChatFeatureState.get().tabs().shouldShow(message.content(), history, index)) {
            original.call(message);
        }
    }

    /**
     * Fallback for single-message additions outside of a rebuild cycle. New messages are not
     * yet in {@code allMessages} when {@code addMessageToDisplayQueue} runs, so this typically
     * returns -1, which {@code shouldShow} treats as "no positional context".
     */
    @Unique
    private static int identityIndexOf(List<GuiMessage> list, GuiMessage target) {
        for (int i = 0, n = list.size(); i < n; i++) {
            if (list.get(i) == target) return i;
        }
        return -1;
    }
}