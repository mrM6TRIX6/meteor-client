/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Nametags;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    
    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, CallbackInfoReturnable<Text> cir) {
        if (PostProcessShaders.rendering) {
            cir.setReturnValue(null);
        }
        if (Modules.get().get(NoRender.class).noNametags()) {
            cir.setReturnValue(null);
        }
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }
        if (Modules.get().get(Nametags.class).playerNametags() && !(EntityUtils.getGameMode(player) == null && Modules.get().get(Nametags.class).excludeBots())) {
            cir.setReturnValue(null);
        }
    }
    
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(NoRender.class).noEntity(entity)) {
            cir.setReturnValue(false);
        }
        if (Modules.get().get(NoRender.class).noFallingBlocks() && entity instanceof FallingBlockEntity) {
            cir.setReturnValue(false);
        }
    }
    
}
