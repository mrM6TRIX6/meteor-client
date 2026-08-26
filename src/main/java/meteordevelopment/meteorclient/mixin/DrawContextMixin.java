/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.mixininterface.ISpecialGuiElementRenderState;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Animations;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.tooltip.MeteorTooltipData;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(value = DrawContext.class)
public abstract class DrawContextMixin {

    @Shadow
    public abstract Matrix3x2fStack getMatrices();

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V", shift = At.Shift.BEFORE))
    private void onDrawTooltip(TextRenderer textRenderer, List<Text> text, Optional<TooltipData> data, int x, int y, @Nullable Identifier texture, CallbackInfo ci, @Local(ordinal = 1) List<TooltipComponent> list) {
        if (data.isPresent() && data.get() instanceof MeteorTooltipData meteorTooltipData) {
            list.add(meteorTooltipData.getComponent());
        }
    }
    
    @ModifyReceiver(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private Optional<TooltipData> onDrawTooltip_modifyIfPresentReceiver(Optional<TooltipData> data, Consumer<TooltipData> consumer) {
        if (data.isPresent() && data.get() instanceof MeteorTooltipData) {
            return Optional.empty();
        }
        return data;
    }
    
    @ModifyArg(method = { "addEntity", "addPlayerSkin", "addBookModel", "addBannerResult" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;addSpecialElement(Lnet/minecraft/client/gui/render/state/special/SpecialGuiElementRenderState;)V"))
    private SpecialGuiElementRenderState addSpecialElement$setPose(SpecialGuiElementRenderState state) {
        ((ISpecialGuiElementRenderState) state).meteor$setPose(new Matrix3x2f(getMatrices()));

        return state;
    }

    // Animations

    // Tooltips are held back until the very end of a screen render, long after the transform a container is drawn
    // with is gone, so an animating container would sit next to a full size tooltip. This is where all of them end up,
    // the creative tab ones and the recipe book ones included.
    @Inject(method = "drawDeferredElements", at = @At("HEAD"), cancellable = true)
    private void onDrawDeferredElements(CallbackInfo ci) {
        if (!Utils.canUpdate()) {
            return;
        }

        if (Modules.get().get(Animations.class).skipInventoryOverlays()) {
            ci.cancel();
        }
    }

}
