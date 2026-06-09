package com.github.kdgaming0.enhancedchat.mixin.chat.search;

import com.github.kdgaming0.enhancedchat.chat.ChatFeatureState;
import com.github.kdgaming0.enhancedchat.chat.access.ChatAccess;
import com.github.kdgaming0.enhancedchat.chat.search.ChatSearchController;
import com.github.kdgaming0.enhancedchat.chat.search.ChatSearchTheme;
import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
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

/**
 * Adds a borderless search bar to {@link ChatScreen}, toggled with Ctrl+F or kept always-on
 * per config. Layout and colours live in {@link ChatSearchTheme}; query and filter state live
 * in {@link ChatSearchController}. This mixin is just the presentation layer.
 */
@Mixin(ChatScreen.class)
public abstract class ChatSearchScreenMixin extends Screen {

    @Unique
    private static final int SEARCH_INPUT_MAX_LEN = 128;

    @Shadow
    protected EditBox input;

    @Unique
    private EditBox ec$searchBox;
    @Unique
    private String ec$pendingQuery;
    @Unique
    private long ec$hintShownAt = -1L;

    protected ChatSearchScreenMixin(Component title) {
        super(title);
    }

    // -- Lifecycle ----------------------------------------------------

    @Unique
    private static int ec$fadeAlpha(long elapsed) {
        long fadeStart = ChatSearchTheme.HINT_DURATION_MS - ChatSearchTheme.HINT_FADE_MS;
        float alpha = elapsed < fadeStart
                ? 1f
                : 1f - (elapsed - fadeStart) / (float) ChatSearchTheme.HINT_FADE_MS;
        return (int) (alpha * 0xFF) & 0xFF;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ec$initSearchBar(CallbackInfo ci) {
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

    // -- Keys ---------------------------------------------------------

    @Inject(method = "removed", at = @At("HEAD"))
    private void ec$clearSearchOnClose(CallbackInfo ci) {
        ChatSearchController search = ChatFeatureState.get().search();
        if (search.isActive()) {
            search.setActive(false);
            ec$refreshChat();
        }
    }

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

    // -- Rendering ----------------------------------------------------

    /**
     * @return {@code true} if Esc was consumed; {@code false} to let the screen close.
     */
    @Unique
    private boolean ec$handleEscape(ChatSearchController search) {
        // First press with text: clear the query but keep the bar open.
        if (ec$searchBox != null && !ec$searchBox.getValue().isEmpty()) {
            ec$searchBox.setValue("");
            search.setQuery("");
            ec$refreshChat();
            return true;
        }
        // Pinned bar with empty query: defer to the screen so Esc closes chat.
        if (EnhancedChatConfig.alwaysShowChatSearch) {
            return false;
        }
        ec$closeSearch();
        return true;
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void ec$renderSearchBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!EnhancedChatConfig.enableChatSearch) return;
        if (!ChatFeatureState.get().search().isActive() || ec$searchBox == null) return;

        int y = ChatSearchTheme.searchBarY(height);
        int x1 = 2, x2 = width - 2;
        int y1 = y - 2, y2 = y + ChatSearchTheme.SEARCH_BAR_HEIGHT + 2;

        graphics.fill(x1, y1, x2, y2, ChatSearchTheme.BACKGROUND);
        // 1px outline
        graphics.fill(x1, y1, x2, y1 + 1, ChatSearchTheme.BORDER);
        graphics.fill(x1, y2 - 1, x2, y2, ChatSearchTheme.BORDER);
        graphics.fill(x1, y1, x1 + 1, y2, ChatSearchTheme.BORDER);
        graphics.fill(x2 - 1, y1, x2, y2, ChatSearchTheme.BORDER);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ec$renderSearchHint(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!EnhancedChatConfig.enableChatSearch) return;
        if (ChatFeatureState.get().search().isActive() || ec$hintShownAt < 0) return;

        long elapsed = Util.getMillis() - ec$hintShownAt;
        if (elapsed >= ChatSearchTheme.HINT_DURATION_MS) return;

        int alpha = ec$fadeAlpha(elapsed);
        String hintText = "Ctrl+F to search";
        int textWidth = font.width(hintText);
        int inputY = height - 14;
        int x = width - textWidth - 6;
        int y = inputY - font.lineHeight - 3;

        graphics.fill(x - 3, y - 2, x + textWidth + 3, y + font.lineHeight + 2, (alpha / 2) << 24);
        int color = (alpha << 24) | ChatSearchTheme.HINT_RGB;
        graphics.text(font, hintText, x, y, color, false);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ec$renderMatchCount(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!EnhancedChatConfig.enableChatSearch) return;

        ChatSearchController search = ChatFeatureState.get().search();
        if (!search.isActive() || ec$searchBox == null || !search.isFiltering()) return;

        ChatAccess access = (ChatAccess) Minecraft.getInstance().gui.getChat();
        int total = access.ec$getAllMessages().size();
        int matching = search.countMatching(access.ec$getAllMessages(), access.ec$getLineTracker());
        String info = matching + "/" + total;
        int infoWidth = font.width(info);
        int infoX = ec$searchBox.getX() + ec$searchBox.getWidth() - infoWidth - 4;
        int infoY = ChatSearchTheme.searchBarY(height)
                + (ChatSearchTheme.SEARCH_BAR_HEIGHT - 8) / 2;
        graphics.text(font, info, infoX, infoY, ChatSearchTheme.MATCH_COUNT_TEXT, false);
    }

    // -- Internal state changes ---------------------------------------

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
        ec$pendingQuery = null;
        ec$refreshChat();
        rebuildWidgets();
        input.setCanLoseFocus(false);
        ec$focusInput();
    }

    @Unique
    private void ec$onQueryChanged(ChatSearchController search, String query) {
        search.setQuery(query);
        ec$refreshChat();
    }

    @Unique
    private void ec$refreshChat() {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.getChat().resetChatScroll();
        ((ChatAccess) mc.gui.getChat()).ec$refreshMessages();
    }

    // -- Focus helpers ------------------------------------------------

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
