/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class EndCrystalEntityRendererMixin {
    
    // Chams
    
    @Shadow
    @Final
    @Mutable
    private static RenderLayer END_CRYSTAL;
    
    @Shadow
    @Final
    private static Identifier TEXTURE;
    
    // Chams - Texture
    
    @Unique
    private Chams chams;
    
    @Shadow
    @Final
    private EndCrystalEntityModel model;
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        chams = Modules.get().get(Chams.class);
    }
    
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"))
    private void render$renderLayer(EndCrystalEntityRenderState endCrystalEntityRenderState, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState arg, CallbackInfo ci) {
        END_CRYSTAL = RenderLayers.entityTranslucent((chams.isActive() && chams.crystals.get() && !chams.crystalsTexture.get()) ? Chams.BLANK : TEXTURE);
    }
    
    // Chams - Scale
    
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void render$scale(EndCrystalEntityRenderState endCrystalEntityRenderState, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState arg, CallbackInfo ci) {
        if (!chams.isActive() || !chams.crystals.get()) {
            return;
        }
        
        float v = chams.crystalsScale.get().floatValue();
        matrices.scale(v, v, v);
    }
    
    // Chams - Color
    
    @WrapWithCondition(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"))
    private <S> boolean render$color(OrderedRenderCommandQueue instance, Model<? super S> model, S state, MatrixStack matrices, RenderLayer renderLayer, int light, int uv, int outlineColor, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlayCommand) {
        if (chams.isActive() && chams.crystals.get()) {
            instance.submitModel(
                model,
                state,
                matrices,
                END_CRYSTAL,
                light,
                uv,
                chams.crystalsColor.get().getPacked(),
                null,
                outlineColor,
                null
            );
            
            return false;
        }
        
        return true;
    }
    
}
