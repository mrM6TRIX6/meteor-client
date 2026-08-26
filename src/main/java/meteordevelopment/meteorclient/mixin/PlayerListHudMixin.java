/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.ChangeTabVisibleEvent;
import meteordevelopment.meteorclient.events.render.RenderTabEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTab;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.ClientConnection;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    
    @Unique
    private BetterTab betterTab;

    @Shadow
    private boolean visible;

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @Unique
    private BetterTab getBetterTab() {
        if (betterTab == null) {
            betterTab = Modules.get().get(BetterTab.class);
        }
        return betterTab;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, @Nullable ScoreboardObjective objective, CallbackInfo ci) {
        context.getMatrices().pushMatrix();

        MeteorClient.EVENT_BUS.post(RenderTabEvent.get(context, scaledWindowWidth));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, @Nullable ScoreboardObjective objective, CallbackInfo ci) {
        context.getMatrices().popMatrix();
    }

    @Inject(method = "setVisible", at = @At("HEAD"))
    private void onChangeVisible(boolean visible, CallbackInfo ci) {
        if (this.visible != visible) {
            MeteorClient.EVENT_BUS.post(ChangeTabVisibleEvent.get(visible));
        }
    }
    
    @ModifyConstant(constant = @Constant(longValue = 80L), method = "collectPlayerEntries")
    private long modifyCount(long count) {
        return getBetterTab().modifyCount(count);
    }
    
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = At.Shift.BEFORE))
    private void modifyHeight(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6) LocalIntRef p) {
        getBetterTab().modifyHeight(o, p, collectPlayerEntries().size());
    }
    
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;isEncrypted()Z"))
    private boolean redirectIsEncrypted(ClientConnection connection) {
        return getBetterTab().redirectIsEncrypted(connection.isEncrypted());
    }
    
    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void onRenderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        getBetterTab().onRenderLatencyIcon(context, width, x, y, entry, ci);
    }
    
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 2))
    private void onRenderPlayerBackground(DrawContext context, int x1, int y1, int x2, int y2, int color, Operation<Void> original, @Local(ordinal = 13) int w, @Local(ordinal = 0) List<PlayerListEntry> entries) {
        getBetterTab().onRenderPlayerBackground(context, x1, y1, x2, y2, color, original, w, entries);
    }
    
}
