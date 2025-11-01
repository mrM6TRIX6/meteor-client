/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.systems.modules.misc.BetterMinecraft;
import net.minecraft.util.StringHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringHelper.class)
public abstract class StringHelperMixin {
    
    @ModifyArg(method = "truncateChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StringHelper;truncate(Ljava/lang/String;IZ)Ljava/lang/String;"), index = 1)
    private static int injected(int maxLength) { // this method is only used in one place, to truncate chat messages, so it's fine to do this
        return (Modules.get().get(BetterChat.class).isInfiniteChatBox() ? Integer.MAX_VALUE : maxLength);
    }
    
    @Inject(method = "isValidChar", at = @At(value = "HEAD"), cancellable = true)
    private static void isValidChar(CallbackInfoReturnable<Boolean> info) {
        if (Modules.get().get(BetterMinecraft.class).paragraphs()) {
            info.setReturnValue(true);
        }
    }
    
}
