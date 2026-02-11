/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.brigadier.StringReader;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringReader.class)
public abstract class StringReaderMixin {
    
    @Inject(method = "isAllowedInUnquotedString", at = @At("RETURN"), remap = false, cancellable = true)
    private static void onIsAllowedInUnquotedString(char c, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(BetterChat.class).unicodeArguments()) {
            cir.setReturnValue(
                Character.isLetterOrDigit(c)
                    || c == '_' || c == '-'
                    || c == '.' || c == '+'
            );
        }
    }
    
}
