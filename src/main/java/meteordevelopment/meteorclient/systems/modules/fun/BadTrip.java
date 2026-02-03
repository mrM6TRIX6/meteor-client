/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.fun;

import meteordevelopment.meteorclient.events.render.EntityRenderAfterTranslateEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.impl.DoubleSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public class BadTrip extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    public BadTrip() {
        super(Categories.FUN, "BadTrip", "Makes the players wobble.");
    }
    
    @EventHandler
    private void afterTranslate(EntityRenderAfterTranslateEvent event) {
        if (event.renderState instanceof PlayerEntityRenderState playerState) {
            float wobble = ((System.currentTimeMillis() + playerState.id * 100) % 400) / 400F;
            wobble = (wobble > 0.5F ? 1 - wobble : wobble) * 2F;
            wobble = Math.max(0, Math.min(1, wobble));
            event.matrices.scale(wobble * 2F + 1, 1 - 0.5F * wobble, wobble * 2F + 1);
        }
    }
    
}
