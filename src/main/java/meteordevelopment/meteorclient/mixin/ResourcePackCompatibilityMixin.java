/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterMinecraft;
import net.minecraft.resource.ResourcePackCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResourcePackCompatibility.class)
public class ResourcePackCompatibilityMixin {
    
    @Inject(method = "isCompatible()Z", at = @At("HEAD"), cancellable = true)
    private void onIsCompatible(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get() == null) {
            cir.setReturnValue(true);
            return;
        }
        
        if (Modules.get().get(BetterMinecraft.class).noResourcePacksWarnings()) {
            cir.setReturnValue(true);
        }
    }
    
}
