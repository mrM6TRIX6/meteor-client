/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.RenderInventoryEvent;
import meteordevelopment.meteorclient.mixininterface.IChatLineScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookScreen.class)
public abstract class RecipeBookScreenMixin {

    // An open recipe book feeds typing to its own search field before the container sees it, so a focused chat line is
    // served first - same as in the creative screen.

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(CharInput input, CallbackInfoReturnable<Boolean> cir) {
        TextFieldWidget field = ((IChatLineScreen) this).meteor$getChatLineField();

        if (field != null && field.isFocused() && field.isVisible()) {
            cir.setReturnValue(field.charTyped(input));
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (((IChatLineScreen) this).meteor$handleChatLineKey(input)) {
            cir.setReturnValue(true);
        }
    }

    // Animations

    // The recipe book belongs to the container but is drawn in a root layer of its own, after both of the passes the
    // container is transformed in, so it needs the same transform separately or it stays put while the container
    // animates. It returns early when the book is closed, so wrapping the call keeps the push and the pop together.
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/recipebook/RecipeBookWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void onRenderRecipeBook(RecipeBookWidget<?> instance, DrawContext context, int mouseX, int mouseY, float deltaTicks, Operation<Void> original) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        HandledScreenAccessor accessor = (HandledScreenAccessor) this;

        context.getMatrices().pushMatrix();
        MeteorClient.EVENT_BUS.post(RenderInventoryEvent.get(context, accessor.meteor$getX(), accessor.meteor$getY(), screen.backgroundWidth, screen.backgroundHeight));

        original.call(instance, context, mouseX, mouseY, deltaTicks);

        context.getMatrices().popMatrix();
    }

}
