/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.ApplyTransformationEvent;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Transformation.class)
public abstract class TransformationMixin {
    
    private void onApply(boolean leftHanded, MatrixStack.Entry entry, CallbackInfo ci) {
        ApplyTransformationEvent event = MeteorClient.EVENT_BUS.post(ApplyTransformationEvent.get((Transformation) (Object) this, leftHanded));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
    
}
