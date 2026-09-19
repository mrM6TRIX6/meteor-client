/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.google.common.base.MoreObjects;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.ArmRenderEvent;
import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.fun.HandDerp;
import meteordevelopment.meteorclient.systems.modules.render.HandView;
import meteordevelopment.meteorclient.systems.modules.render.ShaderHands;
import meteordevelopment.meteorclient.utils.render.post.handsflame.HandsItemHitboxTracker;
import meteordevelopment.meteorclient.utils.render.post.shaderhands.ShaderHandsRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    
    @Shadow
    @Final
    private MinecraftClient client;
    
    @Shadow
    private float equipProgressMainHand;
    
    @Shadow
    private float equipProgressOffHand;
    
    @Shadow
    private ItemStack mainHand;
    
    @Shadow
    private ItemStack offHand;
    
    @Shadow
    protected abstract boolean shouldSkipHandAnimationOnSwap(ItemStack from, ItemStack to);
    
    @Unique
    private Hand meteor$outlineHand;
    
    @ModifyVariable(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "STORE", ordinal = 0), index = 6)
    private float modifySwing(float swingProgress) {
        HandView module = Modules.get().get(HandView.class);
        Hand hand = MoreObjects.firstNonNull(client.player.preferredHand, Hand.MAIN_HAND);
        
        if (module.isActive()) {
            if (hand == Hand.OFF_HAND && !client.player.getOffHandStack().isEmpty()) {
                return swingProgress + module.offSwing.get().floatValue();
            }
            if (hand == Hand.MAIN_HAND && !client.player.getMainHandStack().isEmpty()) {
                return swingProgress + module.mainSwing.get().floatValue();
            }
        }
        
        return swingProgress;
    }
    
    @ModifyReturnValue(method = "shouldSkipHandAnimationOnSwap", at = @At("RETURN"))
    private boolean modifySkipSwapAnimation(boolean original) {
        return original || Modules.get().get(HandView.class).skipSwapping();
    }
    
    @ModifyArg(method = "updateHeldItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F", ordinal = 2), index = 0)
    private float modifyEquipProgressMainhand(float value) {
        float f = client.player.getHandEquippingProgress(1F);
        float modified = Modules.get().get(HandView.class).oldAnimations() ? 1 : f * f * f;
        
        return (shouldSkipHandAnimationOnSwap(mainHand, client.player.getMainHandStack()) ? modified : 0) - equipProgressMainHand;
    }
    
    @ModifyArg(method = "updateHeldItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F", ordinal = 3), index = 0)
    private float modifyEquipProgressOffhand(float value) {
        return (shouldSkipHandAnimationOnSwap(offHand, client.player.getOffHandStack()) ? 1 : 0) - equipProgressOffHand;
    }
    
    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", shift = At.Shift.BEFORE))
    private void onRenderItem(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(HeldItemRendererEvent.get(hand, matrices));
    }
    
    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IFFLnet/minecraft/util/Arm;)V"))
    private void onRenderArm(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(ArmRenderEvent.get(hand, matrices));
    }
    
    @Inject(method = "applyEatOrDrinkTransformation", at = @At(value = "INVOKE", target = "Ljava/lang/Math;pow(DD)D", shift = At.Shift.BEFORE), cancellable = true)
    private void cancelTransformations(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, PlayerEntity player, CallbackInfo ci) {
        if (Modules.get().get(HandView.class).disableFoodAnimation()) {
            ci.cancel();
        }
    }
    
    @Redirect(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getMainArm()Lnet/minecraft/util/Arm;"))
    private Arm redirectGetMainArm(AbstractClientPlayerEntity player) {
        HandDerp handDerp = Modules.get().get(HandDerp.class);
        return handDerp.hideFirstPerson() ? handDerp.getOriginalHand() : player.getMainArm();
    }
    
    @Inject(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", at = @At("HEAD"), require = 0)
    private void meteor$captureOutlineHand(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        this.meteor$outlineHand = hand;
    }
    
    @Inject(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", at = @At("TAIL"), require = 0)
    private void meteor$clearOutlineHand(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        this.meteor$outlineHand = null;
    }
    
    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"), require = 0)
    private void meteor$captureShaderHandsScene(float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue collector, ClientPlayerEntity player, int light, CallbackInfo ci) {
        ShaderHands shaderHands = Modules.get().get(ShaderHands.class);
        if (shaderHands == null) {
            return;
        }

        if (shaderHands.needsHandCapture()) {
            ShaderHandsRenderer.captureScene();
        }
    }
    
    @WrapOperation(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/RenderDispatcher;render()V"), require = 0)
    private void meteor$beginShaderHandsCapture(RenderDispatcher instance, Operation<Void> original) {
        ShaderHands shaderHands = Modules.get().get(ShaderHands.class);
        // Flame also needs isolated hand FBO (color + depth) for a stable mask
        boolean capture = shaderHands != null && shaderHands.needsHandCapture();
        
        if (capture && ShaderHandsRenderer.beginHandCapture()) {
            try {
                original.call(instance);
            } catch (Throwable t) {
                ShaderHandsRenderer.endHandCapture();
                throw t;
            }
        } else {
            original.call(instance);
        }
    }
    
    @WrapOperation(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V"), require = 0)
    private void meteor$endShaderHandsCapture(VertexConsumerProvider.Immediate instance, Operation<Void> original) {
        original.call(instance);
        if (ShaderHandsRenderer.isCapturing()) {
            ShaderHandsRenderer.endHandCapture();
        }
    }
    
    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("TAIL"), require = 0)
    private void meteor$compositeShaderHands(float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue collector, ClientPlayerEntity player, int light, CallbackInfo ci) {
        ShaderHands shaderHands = Modules.get().get(ShaderHands.class);
        if (shaderHands == null) {
            return;
        }
        
        if (shaderHands.needsComposite()) {
            shaderHands.composite();
            ShaderHandsRenderer.updateHandMask();
        } else if (shaderHands.isFlameMode() && ShaderHandsRenderer.wasHandCapturedThisFrame()) {
            ShaderHandsRenderer.compositePlain();
            ShaderHandsRenderer.updateHandMask();
            ShaderHandsRenderer.uploadHandBounds();
        }
    }
    
    @WrapOperation(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V"), require = 0)
    private void meteor$stampFlameHitbox(ItemRenderState state, MatrixStack pose, OrderedRenderCommandQueue collector, int light, int overlay, int outlineColor, Operation<Void> original) {
        ShaderHands shaderHands = Modules.get().get(ShaderHands.class);
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        
        if (shaderHands != null && shaderHands.isFlameMode() && this.meteor$outlineHand != null && player != null) {
            HandsItemHitboxTracker.capture(
                meteor$handDisplayContext(player, this.meteor$outlineHand),
                pose,
                state
            );
        }
        
        original.call(state, pose, collector, light, overlay, outlineColor);
    }
    
    @Unique
    private static ItemDisplayContext meteor$handDisplayContext(ClientPlayerEntity player, Hand hand) {
        boolean right = (hand == Hand.MAIN_HAND) == (player.getMainArm() == Arm.RIGHT);
        return right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }
    
}
