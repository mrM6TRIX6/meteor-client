/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.fun.BadTrip;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderManager.class)
public abstract class EntityRenderManagerMixin {
    
    @Shadow
    public Camera camera;
    
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <S extends EntityRenderState> void render(S renderState, CameraRenderState cameraRenderState, double d, double e, double f, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CallbackInfo ci) {
        Entity entity = ((IEntityRenderState) renderState).meteor$getEntity();
        
        if (entity instanceof FakePlayerEntity player && player.hideWhenInsideCamera) {
            int cX = MathHelper.floor(this.camera.getCameraPos().x);
            int cY = MathHelper.floor(this.camera.getCameraPos().y);
            int cZ = MathHelper.floor(this.camera.getCameraPos().z);
            
            if (cX == entity.getBlockX() && cZ == entity.getBlockZ() && (cY == entity.getBlockY() || cY == entity.getBlockY() + 1)) {
                ci.cancel();
            }
        }
    }
    
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private <S extends EntityRenderState> void afterTranslate(S renderState, CameraRenderState cameraRenderState, double d, double e, double f, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CallbackInfo ci) {
        BadTrip badTrip = Modules.get().get(BadTrip.class);
        if (badTrip.isActive() && renderState instanceof PlayerEntityRenderState playerState) {
            float wobble = ((System.currentTimeMillis() + playerState.id * 100) % 400) / 400F;
            wobble = (wobble > 0.5F ? 1 - wobble : wobble) * 2F;
            wobble = Math.max(0, Math.min(1, wobble));
            matrixStack.scale(wobble * 2F + 1, 1 - 0.5F * wobble, wobble * 2F + 1);
        }
    }
    
    // IEntityRenderState
    
    @ModifyExpressionValue(method = "getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderer;getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;"))
    private <E extends Entity> EntityRenderState getAndUpdateRenderState$setEntity(EntityRenderState state, E entity, float tickProgress) {
        ((IEntityRenderState) state).meteor$setEntity(entity);
        return state;
    }
    
}