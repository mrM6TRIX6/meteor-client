/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.ISpecialGuiElementRenderState;
import net.minecraft.client.gui.render.state.special.*;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Gives the special elements that can show up inside a transformed gui a pose of their own, instead of the identity
 * matrix all of them share.
 *
 * <p>{@link OversizedItemGuiElementRenderState} is left out because it already forwards to the item it belongs to, and
 * {@link SignGuiElementRenderState} because sign editing draws its model at absolute coordinates while a translate is
 * active, so it relies on the matrix being ignored.
 */
@Mixin({
    EntityGuiElementRenderState.class,
    PlayerSkinGuiElementRenderState.class,
    BookModelGuiElementRenderState.class,
    BannerResultGuiElementRenderState.class
})
public abstract class SpecialGuiElementRenderStateMixin implements SpecialGuiElementRenderState, ISpecialGuiElementRenderState {

    @Unique
    private Matrix3x2f pose;

    @Override
    public Matrix3x2f pose() {
        return pose != null ? pose : SpecialGuiElementRenderState.pose;
    }

    @Override
    public void meteor$setPose(Matrix3x2f pose) {
        this.pose = pose;
    }

}
