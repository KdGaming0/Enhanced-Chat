package com.github.kdgaming0.enhancedchat.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class EnhancedChatConfig extends MidnightConfig {

    public static final String CHAT_ENHANCEMENTS = "chat_enhancements";

    @Comment(category = CHAT_ENHANCEMENTS, centered = true)
    public static Comment chatEnhancementsText;

    @Entry(category = CHAT_ENHANCEMENTS)
    public static boolean extendedChatHistory = true;

    @Entry(category = CHAT_ENHANCEMENTS, isSlider = true, min = 100, max = 2048)
    public static int chatHistorySize = 1024;

    @Entry(category = CHAT_ENHANCEMENTS)
    public static boolean compactDuplicateMessages = true;

    @Entry(category = CHAT_ENHANCEMENTS)
    public static boolean compactIgnoreInteractable = true;

    @Entry(category = CHAT_ENHANCEMENTS)
    public static boolean onlyCompactConsecutive = false;

    @Entry(category = CHAT_ENHANCEMENTS, isSlider = true, min = 0, max = 60)
    public static int compactTimeWindowMinutes = 10;

    @Entry(category = CHAT_ENHANCEMENTS)
    public static boolean enableChatTabs = true;

    @Entry(category = CHAT_ENHANCEMENTS)
    public static boolean enableTabFiltering = true;

    @Entry(category = CHAT_ENHANCEMENTS)
    public static boolean enableChatContextMenu = true;

    @Entry(category = CHAT_ENHANCEMENTS)
    public static boolean rightClickChatCopies = false;

    @Entry(category = CHAT_ENHANCEMENTS)
    public static boolean enableChatSearch = true;

    @Entry(category = CHAT_ENHANCEMENTS)
    public static boolean alwaysShowChatSearch = false;

    @Entry(category = CHAT_ENHANCEMENTS)
    public static boolean enableChatAnimation = false;

    @Entry(category = CHAT_ENHANCEMENTS, isSlider = true, min = 50, max = 500)
    public static int chatAnimationDurationMs = 150;
}
