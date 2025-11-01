/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.fun;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public class BadTrip extends Module {
    
    public BadTrip() {
        super(Categories.Fun, "bad-trip", "Makes the players wobble.");
    }
    
    public void applyWobbleEffect(PlayerEntityRenderState state, MatrixStack matrices) {
        float wobble = ((System.currentTimeMillis() + state.id * 100) % 400) / 400F;
        wobble = (wobble > .5 ? 1 - wobble : wobble) * 2F;
        wobble = Math.max(0, Math.min(1, wobble));
        matrices.scale(wobble * 2F + 1, 1 - .5f * wobble, wobble * 2F + 1);
    }
    
}
