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
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;

@Mixin(StatusEffectsDisplay.class)
public abstract class StatusEffectsDisplayMixin {

    @Shadow
    @Final
    private HandledScreen<?> parent;

    // Animations

    // The effects sit next to a container but are drawn before it, outside of its two render passes, so they need the
    // same transform separately or they stay put while the container animates. Wrapping the call rather than the whole
    // method keeps the push and the pop together - the method returns early when there is nothing to draw.
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/StatusEffectsDisplay;drawStatusEffects(Lnet/minecraft/client/gui/DrawContext;Ljava/util/Collection;IIIII)V"))
    private void onDrawStatusEffects(StatusEffectsDisplay instance, DrawContext context, Collection<StatusEffectInstance> effects, int x, int height, int mouseX, int mouseY, int width, Operation<Void> original) {
        context.getMatrices().pushMatrix();

        HandledScreenAccessor accessor = (HandledScreenAccessor) parent;
        MeteorClient.EVENT_BUS.post(RenderInventoryEvent.get(context, accessor.meteor$getX(), accessor.meteor$getY(), parent.backgroundWidth, parent.backgroundHeight));

        original.call(instance, context, effects, x, height, mouseX, mouseY, width);

        context.getMatrices().popMatrix();
    }

}
