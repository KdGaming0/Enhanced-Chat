package com.github.kdgaming0.enhancedchat;

import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import com.github.kdgaming0.enhancedchat.util.HypixelLocationState;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnhancedChat implements ClientModInitializer {
    public static final String MOD_ID = "enhanced_chat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        EnhancedChatConfig.init(MOD_ID, EnhancedChatConfig.class);
        HypixelLocationState.register();
        LOGGER.info("Enhanced Chat initialized");
    }
}
