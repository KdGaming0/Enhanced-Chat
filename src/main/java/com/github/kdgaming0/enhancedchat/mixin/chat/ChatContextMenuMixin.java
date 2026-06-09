package com.github.kdgaming0.enhancedchat.mixin.chat;

import com.github.kdgaming0.enhancedchat.chat.access.ChatAccess;
import com.github.kdgaming0.enhancedchat.chat.menu.ChatContextMenu;
import com.github.kdgaming0.enhancedchat.chat.menu.ChatMessageResolver;
import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wires a right-click chat context menu into {@link ChatScreen}. The actual menu lives in
 * {@link ChatContextMenu}; this mixin only forwards events.
 */
@Mixin(ChatScreen.class)
public abstract class ChatContextMenuMixin extends Screen {

    @Unique
    private static final int MOUSE_BUTTON_LEFT = 0;
    @Unique
    private static final int MOUSE_BUTTON_RIGHT = 1;

    @Unique
    private final ChatContextMenu ec$contextMenu = new ChatContextMenu();

    protected ChatContextMenuMixin(Component title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ec$onMouseClicked(
            MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {

        if (!EnhancedChatConfig.enableChatContextMenu) return;

        int button = event.button();
        double x = event.x();
        double y = event.y();

        // Left-click inside an open menu: route to the menu's button dispatcher.
        if (button == MOUSE_BUTTON_LEFT && ec$contextMenu.isOpen()) {
            if (ec$contextMenu.mouseClicked(x, y, button)) {
                cir.setReturnValue(true);
            }
            return;
        }

        if (button != MOUSE_BUTTON_RIGHT) return;

        GuiMessage message = ChatMessageResolver.resolve(y);
        if (message == null) {
            ec$contextMenu.close();
            return;
        }

        if (EnhancedChatConfig.rightClickChatCopies) {
            ec$directCopy(message, (int) x, (int) y);
        } else {
            ec$contextMenu.open(message, (int) x, (int) y, this.width, this.height);
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void ec$onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && ec$contextMenu.isOpen()) {
            ec$contextMenu.close();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ec$renderContextMenu(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ec$contextMenu.render(graphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void ec$closeMenuOnRemoved(CallbackInfo ci) {
        ec$contextMenu.close();
    }

    /**
     * Direct copy path: flash the outline briefly, copy raw text, show the toast.
     */
    @Unique
    private void ec$directCopy(GuiMessage message, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        ChatAccess access = (ChatAccess) mc.gui.getChat();

        access.ec$getLineTracker().setSelectedMessage(message);
        mc.keyboardHandler.setClipboard(ChatMessageResolver.toRawText(message.content()));
        ec$contextMenu.notifyCopied(x, y);

        // One-frame flash then clear — the selection is purely visual here.
        mc.schedule(() -> access.ec$getLineTracker().setSelectedMessage(null));
    }
}
