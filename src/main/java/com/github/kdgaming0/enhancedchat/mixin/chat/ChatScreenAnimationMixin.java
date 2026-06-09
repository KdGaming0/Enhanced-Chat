package com.github.kdgaming0.enhancedchat.mixin.chat;

import com.github.kdgaming0.enhancedchat.config.EnhancedChatConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Slides the chat input bar up when the chat screen opens. Cubic ease-out over a duration
 * capped at {@link #MAX_DURATION_MS}.
 *
 * <p>Inspired by Ezzenix's ChatAnimation (CC-BY-NC-SA 4.0); this is an original implementation.
 */
@Mixin(ChatScreen.class)
public class ChatScreenAnimationMixin {

    @Unique
    private static final float MAX_DURATION_MS = 500f;
    @Unique
    private static final float DURATION_MULTIPLIER = 2.5f;
    @Unique
    private static final float BASE_DISPLACEMENT_PX = 8f;

    @Unique private boolean ec$initialized;
    @Unique private long ec$openTime;
    @Unique private boolean ec$barIdle = true;

    @Unique
    private float ec$barDisplacement() {
        if (!EnhancedChatConfig.enableChatAnimation) return 0f;

        Minecraft mc = Minecraft.getInstance();
        if (!ec$initialized && mc.player != null && !mc.player.isSleeping()) {
            ec$initialized = true;
            ec$openTime = System.currentTimeMillis();
            ec$barIdle = false;
        }

        if (ec$barIdle) return 0f;

        float duration = Math.min(
                EnhancedChatConfig.chatAnimationDurationMs * DURATION_MULTIPLIER,
                MAX_DURATION_MS);
        float elapsed = System.currentTimeMillis() - ec$openTime;
        if (elapsed >= duration) {
            ec$barIdle = true;
            return 0f;
        }
        float t = 1f - elapsed / duration;
        float eased = 1f - t * t * t; // cubic ease-out
        float scale = (float) mc.getWindow().getGuiScale();
        return (1f - eased) * BASE_DISPLACEMENT_PX * (scale / 2f);
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private void ec$animateBackground(
            GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color,
            Operation<Void> original) {
        ec$withDisplacement(graphics, () -> original.call(graphics, x1, y1, x2, y2, color));
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;"
                            + "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void ec$animateWidgets(
            ChatScreen instance, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
            Operation<Void> original) {
        ec$withDisplacement(graphics,
                () -> original.call(instance, graphics, mouseX, mouseY, partialTick));
    }

    @Unique
    private void ec$withDisplacement(GuiGraphicsExtractor graphics, Runnable body) {
        float dy = ec$barDisplacement();
        if (dy != 0f) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(0f, dy);
        }
        body.run();
        if (dy != 0f) graphics.pose().popMatrix();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void ec$resetOnClose(CallbackInfo ci) {
        ec$initialized = false;
        ec$barIdle = true;
    }
}
