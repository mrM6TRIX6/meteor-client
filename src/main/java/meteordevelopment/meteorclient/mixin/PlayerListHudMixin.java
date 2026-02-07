/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTab;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    
    @Unique
    private BetterTab betterTab;
    
    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();
    
    @ModifyConstant(constant = @Constant(longValue = 80L), method = "collectPlayerEntries")
    private long modifyCount(long count) {
        if (getBetterTab().isActive()) {
            return (!betterTab.autoTabSize()) ? betterTab.tabSize() : mc.getNetworkHandler().getListedPlayerListEntries().size();
        }
        return count;
    }
    
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"), index = 0)
    private int modifyWidth(int width) {
        return getBetterTab().pingNumbers() ? width + 30 : width;
    }
    
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = At.Shift.BEFORE))
    private void modifyHeight(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6) LocalIntRef p) {
        if (!getBetterTab().isActive()) {
            return;
        }
        
        int newO;
        int newP = 1;
        int totalPlayers = newO = this.collectPlayerEntries().size();
        while (newO > (!betterTab.autoTabSize() ? betterTab.columnHeight() : (totalPlayers <= 100 ? 20 : 20 + totalPlayers / 10))) {
            newO = (totalPlayers + ++newP - 1) / newP;
        }
        
        o.set(newO);
        p.set(newP);
    }
    
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;isEncrypted()Z"))
    private boolean redirectIsEncrypted(ClientConnection connection) {
        return getBetterTab().offlineHeads() || connection.isEncrypted();
    }
    
    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void onRenderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (getBetterTab().pingNumbers()) {
            TextRenderer textRenderer = mc.textRenderer;
            
            int latency = MathHelper.clamp(entry.getLatency(), 0, 9999);
            int color = latency < 150 ? 0xFF00E970 : latency < 300 ? 0xFFE7D020 : 0xFFD74238;
            context.drawTextWithShadow(textRenderer, String.valueOf(latency), x + width - textRenderer.getWidth(String.valueOf(latency)), y, color);
            ci.cancel();
        }
    }
    
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 2))
    private void onRenderPlayerBackground(DrawContext instance, int x1, int y1, int x2, int y2, int color, Operation<Void> original, @Local(ordinal = 13) int w, @Local(ordinal = 0) List<PlayerListEntry> entries) {
        int drawColor = color;
        
        if ((getBetterTab().highlightSelf() || getBetterTab().highlightFriends()) && w < entries.size()) {
            PlayerListEntry entry = entries.get(w);
            
            if (betterTab.highlightSelf() && Objects.equals(entry.getProfile().name(), mc.player.getGameProfile().name())) {
                drawColor = betterTab.selfColor().getPacked();
            } else if (betterTab.highlightFriends() && Friends.get().isFriend(entry)) {
                drawColor = betterTab.friendsColor().getPacked();
            }
        }
        
        original.call(instance, x1, y1, x2, y2, drawColor);
    }
    
    @Unique
    private BetterTab getBetterTab() {
        if (betterTab == null) {
            betterTab = Modules.get().get(BetterTab.class);
        }
        return betterTab;
    }
    
}
