/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.mixininterface.IChatLineScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin implements IChatLineScreen {

    @Shadow
    @Final
    private boolean operatorTabEnabled;

    // The creative screen handles typing itself and never calls super, so everything typed goes to its search box -
    // including whatever is meant for the chat line. Both input methods are intercepted ahead of it so a focused chat
    // line gets the keystroke first.

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(CharInput input, CallbackInfoReturnable<Boolean> cir) {
        TextFieldWidget field = meteor$focusedChatLine();
        if (field != null) {
            cir.setReturnValue(field.charTyped(input));
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (meteor$handleChatLineKey(input)) {
            cir.setReturnValue(true);
        }
    }

    // The bottom row of item group tabs hangs below the background: drawn from backgroundHeight - 4 and clickable down
    // to backgroundHeight + 32, so the chat line starts past the hitbox rather than just past the texture.
    @Override
    public int meteor$chatLineBottomInset() {
        return 32;
    }

    @Unique
    private TextFieldWidget meteor$focusedChatLine() {
        TextFieldWidget field = meteor$getChatLineField();

        if (field != null && field.isFocused() && field.isVisible()) {
            return field;
        }

        return null;
    }

    @ModifyReturnValue(method = "shouldShowOperatorTab(Lnet/minecraft/entity/player/PlayerEntity;)Z", at = @At("RETURN"))
    private boolean meteor$showOperatorTabWithoutOp(boolean original) {
        if (this.operatorTabEnabled && (original || Modules.get().get(InventoryTweaks.class).operatorTab())) {
            return true;
        }
        
        return original;
    }
}