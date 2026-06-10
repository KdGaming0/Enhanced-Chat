package com.github.kdgaming0.enhancedchat.mixin.chat.search;

import com.github.kdgaming0.enhancedchat.chat.ChatFeatureState;
import com.github.kdgaming0.enhancedchat.chat.access.ChatAccess;
import com.github.kdgaming0.enhancedchat.chat.search.ChatSearchController;
import com.github.kdgaming0.enhancedchat.chat.search.ChatSearchTheme;
import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Adds a borderless search bar to {@link ChatScreen}, toggled with Ctrl+F or kept always-on
 * per config. Layout and colours live in {@link ChatSearchTheme}; query and filter state live
 * in {@link ChatSearchController}. This mixin is just the presentation layer.
 */
@Mixin(ChatScreen.class)
public abstract class ChatSearchScreenMixin extends Screen {

    @Unique private static final int  SEARCH_INPUT_MAX_LEN   = 128;
    @Unique private static final int  MATCH_COUNT_BOX_MARGIN = 4;
    /** Max ms between keystrokes before the debounced refresh fires. */
    @Unique private static final long SEARCH_DEBOUNCE_MS     = 100L;

    // Hint overlay constants
    @Unique private static final String HINT_TEXT            = "Ctrl+F to search";
    @Unique private static final int    HINT_RIGHT_MARGIN    = 6;
    @Unique private static final int    HINT_PADDING         = 3;
    @Unique private static final int    INPUT_BAR_BOTTOM     = 14;

    @Shadow protected EditBox input;

    @Unique private EditBox ec$searchBox;
    @Unique private String  ec$pendingQuery;
    @Unique private long    ec$hintShownAt = -1L;

    // --- Debounce -------------------------------------------------------
    // Coalesces rapid keystrokes so refreshTrimmedMessages runs at most once
    // per debounce window instead of once per character typed.
    @Unique private long    ec$lastQueryChangeAt = -1L;
    @Unique private boolean ec$pendingRefresh    = false;

    // --- Match-count cache ----------------------------------------------
    // countMatching runs regex strip over every message; cache the result
    // and recompute only when the query or message list has changed.
    @Unique private int     ec$cachedMatchCount;
    @Unique private int     ec$cachedTotalCount  = -1;
    @Unique private String  ec$cachedMatchInfo   = "";
    @Unique private int     ec$cachedMatchInfoWidth;
    @Unique private boolean ec$matchCountDirty   = true;

    // font.width(HINT_TEXT) is stable per font; compute once per screen init.
    @Unique private int ec$hintTextWidth = -1;

    protected ChatSearchScreenMixin(Component title) {
        super(title);
    }

    // -- Lifecycle --------------------------------------------------------

    @Inject(method = "init", at = @At("TAIL"))
    private void ec$initSearchBar(CallbackInfo ci) {
        // Reset per-render caches so a font or resolution change is not stale.
        ec$hintTextWidth  = -1;
        ec$matchCountDirty = true;

        if (!EnhancedChatConfig.enableChatSearch) return;

        ChatSearchController search = ChatFeatureState.get().search();

        if (!search.isActive()) {
            ec$hintShownAt = Util.getMillis();
        }

        if (EnhancedChatConfig.alwaysShowChatSearch && !search.isActive()) {
            search.setActive(true);
        }

        if (!search.isActive()) return;

        ec$searchBox = ec$buildSearchBox(search);
        addRenderableWidget(ec$searchBox);
        input.setCanLoseFocus(true);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void ec$clearSearchOnClose(CallbackInfo ci) {
        ChatSearchController search = ChatFeatureState.get().search();
        if (search.isActive()) {
            search.setActive(false);
            ec$pendingRefresh = false;
            ec$refreshChat();
        }
    }

    // -- Keys -------------------------------------------------------------

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void ec$handleSearchKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!EnhancedChatConfig.enableChatSearch) return;

        int key = event.key();
        ChatSearchController search = ChatFeatureState.get().search();

        if (key == GLFW.GLFW_KEY_F && event.hasControlDown()) {
            ec$toggleSearch();
            cir.setReturnValue(true);
            return;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE && search.isActive() && ec$handleEscape(search)) {
            cir.setReturnValue(true);
        }
    }

    // -- Tick (debounce) --------------------------------------------------

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void ec$checkDebounce(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ec$pendingRefresh && Util.getMillis() - ec$lastQueryChangeAt >= SEARCH_DEBOUNCE_MS) {
            ec$pendingRefresh = false;
            ec$refreshChat();
        }
    }

    // -- Rendering --------------------------------------------------------

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void ec$renderSearchBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!EnhancedChatConfig.enableChatSearch) return;
        if (!ChatFeatureState.get().search().isActive() || ec$searchBox == null) return;

        int y  = ChatSearchTheme.searchBarY(height);
        int x1 = 2, x2 = width - 2;
        int y1 = y - 2, y2 = y + ChatSearchTheme.SEARCH_BAR_HEIGHT + 2;

        graphics.fill(x1, y1, x2, y2, ChatSearchTheme.BACKGROUND);
        // 1 px outline
        graphics.fill(x1, y1, x2,      y1 + 1, ChatSearchTheme.BORDER);
        graphics.fill(x1, y2 - 1, x2,  y2,     ChatSearchTheme.BORDER);
        graphics.fill(x1, y1, x1 + 1,  y2,     ChatSearchTheme.BORDER);
        graphics.fill(x2 - 1, y1, x2,  y2,     ChatSearchTheme.BORDER);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ec$renderSearchHint(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!EnhancedChatConfig.enableChatSearch) return;
        if (ChatFeatureState.get().search().isActive() || ec$hintShownAt < 0) return;

        long elapsed = Util.getMillis() - ec$hintShownAt;
        if (elapsed >= ChatSearchTheme.HINT_DURATION_MS) return;

        if (ec$hintTextWidth < 0) {
            ec$hintTextWidth = font.width(HINT_TEXT);
        }

        int alpha = ec$fadeAlpha(elapsed);
        int x = width - ec$hintTextWidth - HINT_RIGHT_MARGIN;
        int y = height - INPUT_BAR_BOTTOM - font.lineHeight - HINT_PADDING;

        graphics.fill(x - HINT_PADDING, y - 2, x + ec$hintTextWidth + HINT_PADDING, y + font.lineHeight + 2,
                (alpha / 2) << 24);
        graphics.text(font, HINT_TEXT, x, y, (alpha << 24) | ChatSearchTheme.HINT_RGB, false);
    }

    /**
     * Renders "matched / total" beside the search box. The count is only recomputed when the
     * query changes or new messages arrive — not on every render frame.
     */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ec$renderMatchCount(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!EnhancedChatConfig.enableChatSearch) return;

        ChatSearchController search = ChatFeatureState.get().search();
        if (!search.isActive() || ec$searchBox == null || !search.isFiltering()) return;

        ChatAccess access = (ChatAccess) Minecraft.getInstance().gui.getChat();
        if (ec$matchCountDirty || access.ec$getAllMessages().size() != ec$cachedTotalCount) {
            ec$recomputeMatchCount(search, access);
        }

        int infoX = ec$searchBox.getX() + ec$searchBox.getWidth() - ec$cachedMatchInfoWidth - MATCH_COUNT_BOX_MARGIN;
        int infoY = ChatSearchTheme.searchBarY(height) + (ChatSearchTheme.SEARCH_BAR_HEIGHT - 8) / 2;
        graphics.text(font, ec$cachedMatchInfo, infoX, infoY, ChatSearchTheme.MATCH_COUNT_TEXT, false);
    }

    // -- Helpers ----------------------------------------------------------

    @Unique
    private void ec$recomputeMatchCount(ChatSearchController search, ChatAccess access) {
        List<GuiMessage> messages = access.ec$getAllMessages();
        ec$cachedTotalCount    = messages.size();
        ec$cachedMatchCount    = search.countMatching(messages, access.ec$getLineTracker());
        ec$cachedMatchInfo     = ec$cachedMatchCount + "/" + ec$cachedTotalCount;
        ec$cachedMatchInfoWidth = font.width(ec$cachedMatchInfo);
        ec$matchCountDirty     = false;
    }

    @Unique
    private static int ec$fadeAlpha(long elapsed) {
        long fadeStart = ChatSearchTheme.HINT_DURATION_MS - ChatSearchTheme.HINT_FADE_MS;
        float alpha = elapsed < fadeStart
                ? 1f
                : 1f - (elapsed - fadeStart) / (float) ChatSearchTheme.HINT_FADE_MS;
        return (int) (alpha * 0xFF) & 0xFF;
    }

    @Unique
    private boolean ec$handleEscape(ChatSearchController search) {
        if (ec$searchBox != null && !ec$searchBox.getValue().isEmpty()) {
            // setValue triggers the responder which schedules a deferred refresh;
            // cancel it and do an immediate one instead.
            ec$searchBox.setValue("");
            ec$pendingRefresh = false;
            ec$refreshChat();
            return true;
        }
        if (EnhancedChatConfig.alwaysShowChatSearch) {
            return false; // Let the screen handle Escape (close chat).
        }
        ec$closeSearch();
        return true;
    }

    @Unique
    private EditBox ec$buildSearchBox(ChatSearchController search) {
        Minecraft mc = Minecraft.getInstance();
        int y = ChatSearchTheme.searchBarY(height);

        EditBox box = new EditBox(
                mc.font, 4, y, width - 8, ChatSearchTheme.SEARCH_BAR_HEIGHT,
                Component.literal("Search chat..."));
        box.setHint(Component.literal("Search chat... (Ctrl+F)"));
        box.setMaxLength(SEARCH_INPUT_MAX_LEN);
        box.setResponder(value -> ec$onQueryChanged(search, value));
        box.setBordered(false);
        box.setCanLoseFocus(true);

        if (ec$pendingQuery != null) {
            box.setValue(ec$pendingQuery);
            ec$pendingQuery = null;
        } else {
            box.setValue(search.getQuery());
        }
        return box;
    }

    @Unique
    private void ec$toggleSearch() {
        ChatSearchController search = ChatFeatureState.get().search();
        if (!search.isActive()) {
            ec$openSearch();
            return;
        }
        if (!EnhancedChatConfig.alwaysShowChatSearch) {
            ec$closeSearch();
            return;
        }
        ec$pingPongFocus();
    }

    @Unique
    private void ec$openSearch() {
        ChatFeatureState.get().search().setActive(true);
        ec$pendingQuery = "";
        rebuildWidgets();
        ec$focusSearchBox();
    }

    @Unique
    private void ec$closeSearch() {
        ChatSearchController search = ChatFeatureState.get().search();
        search.setActive(false);
        search.setQuery("");
        ec$pendingQuery   = null;
        ec$pendingRefresh = false;
        ec$refreshChat();
        rebuildWidgets();
        input.setCanLoseFocus(false);
        ec$focusInput();
    }

    /**
     * Called by the EditBox responder on every keystroke. Rather than rebuilding the full
     * display queue immediately, we record the change time and let {@link #ec$tickDebounce}
     * fire the refresh once typing has paused.
     */
    @Unique
    private void ec$onQueryChanged(ChatSearchController search, String query) {
        search.setQuery(query);
        ec$matchCountDirty   = true;
        ec$lastQueryChangeAt = Util.getMillis();
        ec$pendingRefresh    = true;
    }

    @Unique
    private void ec$refreshChat() {
        ChatComponent chat = Minecraft.getInstance().gui.getChat();
        chat.resetChatScroll();
        ((ChatAccess) chat).ec$refreshMessages();
        ec$matchCountDirty = true;
    }

    // -- Focus helpers ----------------------------------------------------

    @Unique
    private void ec$focusSearchBox() {
        if (ec$searchBox == null) return;
        setFocused(ec$searchBox);
        ec$searchBox.setFocused(true);
        input.setFocused(false);
    }

    @Unique
    private void ec$focusInput() {
        setFocused(input);
        input.setFocused(true);
        if (ec$searchBox != null) ec$searchBox.setFocused(false);
    }

    @Unique
    private void ec$pingPongFocus() {
        if (ec$searchBox == null) return;
        if (ec$searchBox.isFocused()) ec$focusInput();
        else ec$focusSearchBox();
    }
}