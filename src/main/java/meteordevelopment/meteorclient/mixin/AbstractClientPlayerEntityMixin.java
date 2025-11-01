/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoFOV;
import meteordevelopment.meteorclient.utils.misc.FakeClientPlayer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    
    // Player model rendering in main menu
    
    @Inject(method = "getPlayerListEntry", at = @At("HEAD"), cancellable = true)
    private void onGetPlayerListEntry(CallbackInfoReturnable<PlayerListEntry> info) {
        if (mc.getNetworkHandler() == null) {
            info.setReturnValue(FakeClientPlayer.getPlayerListEntry());
        }
    }
    
    // No FOV
    
    @Redirect(method = "getFovMultiplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerAbilities;getWalkSpeed()F"))
    private float redirectFovMultiplier(PlayerAbilities abilities) {
        // Return 0 so that the condition 'if (g != 0.0F)' it didn't work
        return 0.0F;
    }
    
    @ModifyReturnValue(method = "getFovMultiplier", at = @At("RETURN"))
    private float modifyFovMultiplier(float originalFovMultiplier) {
        if (Modules.get().get(NoFOV.class).isActive()) {
            return mc.player.isSprinting() ? originalFovMultiplier + 0.15F : originalFovMultiplier;
        } else {
            return (float) (originalFovMultiplier * (mc.player.getAttributeValue(EntityAttributes.MOVEMENT_SPEED) / mc.player.getAbilities().getWalkSpeed() + 1.0F) / 2.0F);
        }
    }
    
}

