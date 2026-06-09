package com.github.kdgaming0.enhancedchat.mixin.chat.search;

import com.github.kdgaming0.enhancedchat.chat.ChatFeatureState;
import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Hides messages that don't match the active search query. Priority 1000 so it runs after
 * {@link com.github.kdgaming0.enhancedchat.mixin.chat.tab.ChatTabFilterMixin}
 * (priority 900), giving the deterministic filter chain: tab → search → display.
 */
@Mixin(value = ChatComponent.class)
public class ChatSearchFilterMixin {

    @WrapMethod(method = "addMessageToDisplayQueue")
    private void ec$filterBySearch(GuiMessage message, Operation<Void> original) {
        if (!EnhancedChatConfig.enableChatSearch
                || ChatFeatureState.get().search().matches(message)) {
            original.call(message);
        }
    }
}
