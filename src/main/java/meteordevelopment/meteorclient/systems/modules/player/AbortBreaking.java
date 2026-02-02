/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.CancelBlockBreakingEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class AbortBreaking extends Module {
    
    public AbortBreaking() {
        super(Categories.PLAYER, "AbortBreaking", "Allows you to abort breaking without losing the progress. Recommended using it with Multi Actions module.");
    }
    
    @EventHandler
    private void onCancelBlockBreaking(CancelBlockBreakingEvent event) {
        event.cancel();
    }
    
}
