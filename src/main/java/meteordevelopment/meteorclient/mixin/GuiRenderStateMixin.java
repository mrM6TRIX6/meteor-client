/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IGuiRenderStateLayer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin implements IGuiRenderStateLayer {
    
    @Unique
    private int meteor$layerSerial;
    
    @Override
    public int meteor$getLayerSerial() {
        return meteor$layerSerial;
    }
    
    @Inject(method = "createNewRootLayer", at = @At("RETURN"))
    private void meteor$trackNextStratum(CallbackInfo ci) {
        meteor$layerSerial++;
    }
    
    @Inject(method = "goUpLayer", at = @At("RETURN"))
    private void meteor$trackUpLayer(CallbackInfo ci) {
        meteor$layerSerial++;
    }
    
    @Inject(method = "clear", at = @At("HEAD"))
    private void meteor$resetLayerSerial(CallbackInfo ci) {
        meteor$layerSerial = 0;
    }
    
}
