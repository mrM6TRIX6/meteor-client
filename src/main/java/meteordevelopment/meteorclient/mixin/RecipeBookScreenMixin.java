/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.RenderInventoryEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RecipeBookScreen.class)
public abstract class RecipeBookScreenMixin {

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
