/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.DoItemUseEvent;
import meteordevelopment.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ResolutionChangedEvent;
import meteordevelopment.meteorclient.events.game.ResourcePacksReloadedEvent;
import meteordevelopment.meteorclient.events.game.ScreenOpenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.mixininterface.IMinecraftClient;
import meteordevelopment.meteorclient.systems.clientsettings.ClientSettings;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.GUIMove;
import meteordevelopment.meteorclient.systems.modules.player.FastUse;
import meteordevelopment.meteorclient.systems.modules.player.MultiActions;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.utils.misc.CPSUtils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.network.OnlinePlayers;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.profiler.Profilers;
import org.jetbrains.annotations.Nullable;
import org.meteordev.starscript.Script;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(value = MinecraftClient.class, priority = 1001)
public abstract class MinecraftClientMixin implements IMinecraftClient {
    
    @Unique
    private boolean doItemUseCalled;
    
    @Unique
    private boolean rightClick;
    
    @Unique
    private long lastTime;
    
    @Unique
    private boolean firstFrame;
    
    @Shadow
    public ClientWorld world;
    
    @Shadow
    @Final
    public Mouse mouse;
    
    @Shadow
    @Final
    private Window window;
    
    @Shadow
    public Screen currentScreen;
    
    @Shadow
    @Final
    public GameOptions options;
    
    @Shadow
    protected abstract void doItemUse();
    
    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;
    
    @Shadow
    private int itemUseCooldown;
    
    @Shadow
    @Nullable
    public ClientPlayerEntity player;
    
    @Shadow
    @Final
    @Mutable
    private Framebuffer framebuffer;
    
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        MeteorClient.INSTANCE.onInitializeClient();
        firstFrame = true;
    }
    
    @Inject(at = @At("HEAD"), method = "tick")
    private void onTickPre(CallbackInfo ci) {
        OnlinePlayers.update();
        
        doItemUseCalled = false;
        
        Profilers.get().push(MeteorClient.MOD_ID + "_pre_update");
        MeteorClient.EVENT_BUS.post(TickEvent.Pre.get());
        Profilers.get().pop();
        
        if (rightClick && !doItemUseCalled && interactionManager != null) {
            doItemUse();
        }
        rightClick = false;
    }
    
    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo ci) {
        Profilers.get().push(MeteorClient.MOD_ID + "_post_update");
        MeteorClient.EVENT_BUS.post(TickEvent.Post.get());
        Profilers.get().pop();
    }
    
    @Inject(method = "doAttack", at = @At("HEAD"))
    private void onAttack(CallbackInfoReturnable<Boolean> cir) {
        CPSUtils.onAttack();
    }
    
    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onDoItemUse(CallbackInfo ci) {
        doItemUseCalled = true;
    }
    
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;ZZ)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, boolean transferring, boolean stopSound, CallbackInfo ci) {
        if (world != null) {
            MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
        }
    }
    
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof WidgetScreen) {
            screen.mouseMoved(mouse.getX() * window.getScaleFactor(), mouse.getY() * window.getScaleFactor());
        }
        
        ScreenOpenEvent event = ScreenOpenEvent.get(screen);
        MeteorClient.EVENT_BUS.post(event);
        
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
    
    @WrapOperation(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;unpressAll()V"))
    private void onSetScreenKeyBindingUnpressAll(Operation<Void> op) {
        Modules modules = Modules.get();
        if (modules == null) {
            op.call();
            return;
        }
        
        GUIMove guiMove = modules.get(GUIMove.class);
        if (guiMove == null || !guiMove.isActive() || guiMove.skip()) {
            op.call();
            return;
        }
        
        GameOptions options = MeteorClient.mc.options;
        for (KeyBinding kb : KeyBindingAccessor.getKeysById().values()) {
            if (kb == options.forwardKey) {
                continue;
            }
            if (kb == options.leftKey) {
                continue;
            }
            if (kb == options.rightKey) {
                continue;
            }
            if (kb == options.backKey) {
                continue;
            }
            if (guiMove.sneak() && kb == options.sneakKey) {
                continue;
            }
            if (guiMove.sprint() && kb == options.sprintKey) {
                continue;
            }
            if (guiMove.jump() && kb == options.jumpKey) {
                continue;
            }
            ((KeyBindingAccessor) kb).meteor$invokeReset();
        }
    }
    
    @Inject(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isItemEnabled(Lnet/minecraft/resource/featuretoggle/FeatureSet;)Z"))
    private void onDoItemUseHand(CallbackInfo ci, @Local ItemStack itemStack) {
        FastUse fastUse = Modules.get().get(FastUse.class);
        if (fastUse.isActive()) {
            itemUseCooldown = fastUse.getItemUseCooldown(itemStack);
        }
    }
    
    @Inject(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Hand;values()[Lnet/minecraft/util/Hand;"), cancellable = true)
    private void onDoItemUseBeforeHands(CallbackInfo ci) {
        if (MeteorClient.EVENT_BUS.post(DoItemUseEvent.get()).isCancelled()) {
            ci.cancel();
        }
    }
    
    @ModifyExpressionValue(method = "doItemUse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;crosshairTarget:Lnet/minecraft/util/hit/HitResult;", ordinal = 1))
    private HitResult doItemUseMinecraftClientCrosshairTargetProxy(HitResult original) {
        return MeteorClient.EVENT_BUS.post(ItemUseCrosshairTargetEvent.get(original)).target;
    }
    
    @ModifyReturnValue(method = "reloadResources(ZLnet/minecraft/client/MinecraftClient$LoadingContext;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
    private CompletableFuture<Void> onReloadResourcesNewCompletableFuture(CompletableFuture<Void> original) {
        return original.thenRun(() -> MeteorClient.EVENT_BUS.post(ResourcePacksReloadedEvent.get()));
    }
    
    @ModifyArg(method = "updateWindowTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setTitle(Ljava/lang/String;)V"))
    private String setTitle(String original) {
        if (ClientSettings.get() == null || !ClientSettings.get().customWindowTitle.get()) {
            return original;
        }
        
        String customTitle = ClientSettings.get().customWindowTitleText.get();
        Script script = MeteorStarscript.compile(customTitle);
        
        if (script != null) {
            String title = MeteorStarscript.run(script);
            if (title != null) {
                customTitle = title;
            }
        }
        
        return customTitle;
    }
    
    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void onResolutionChanged(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(ResolutionChangedEvent.get());
    }
    
    // Time delta
    
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(CallbackInfo ci) {
        long time = System.currentTimeMillis();
        
        if (firstFrame) {
            lastTime = time;
            firstFrame = false;
        }
        
        RenderUtils.frameTime = (time - lastTime) / 1000.0;
        lastTime = time;
    }
    
    // Multi Actions
    
    @ModifyExpressionValue(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"))
    private boolean doItemUseModifyIsBreakingBlock(boolean original) {
        return !Modules.get().isActive(MultiActions.class) && original;
    }
    
    @ModifyExpressionValue(method = "handleBlockBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean handleBlockBreakingModifyIsUsingItem(boolean original) {
        return !Modules.get().isActive(MultiActions.class) && original;
    }
    
    @ModifyExpressionValue(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z", ordinal = 0))
    private boolean handleInputEventsModifyIsUsingItem(boolean original) {
        return !Modules.get().get(MultiActions.class).attackingEntities() && original;
    }
    
    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z", ordinal = 0, shift = At.Shift.BEFORE))
    private void handleInputEventsInjectStopUsingItem(CallbackInfo ci) {
        if (Modules.get().get(MultiActions.class).attackingEntities() && player.isUsingItem()) {
            if (!options.useKey.isPressed()) {
                interactionManager.stopUsingItem(player);
            }
            // noinspection StatementWithEmptyBody
            while (options.useKey.wasPressed());
        }
    }
    
    // Glow ESP
    
    @ModifyReturnValue(method = "hasOutline", at = @At("RETURN"))
    private boolean hasOutlineModifyIsOutline(boolean original, Entity entity) {
        ESP esp = Modules.get().get(ESP.class);
        if (esp == null) {
            return original;
        }
        if (!esp.isGlow() || esp.shouldSkip(entity)) {
            return original;
        }
        
        return esp.getColor(entity) != null || original;
    }
    
    // Interface
    
    @Override
    public void meteor$rightClick() {
        rightClick = true;
    }
    
    @Override
    public void meteor$setFramebuffer(Framebuffer framebuffer) {
        this.framebuffer = framebuffer;
    }
    
}
