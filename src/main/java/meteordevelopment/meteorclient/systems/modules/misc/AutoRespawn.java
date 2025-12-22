/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.game.ScreenOpenEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoRespawn extends Module {
    
    public AutoRespawn() {
        super(Categories.Player, "AutoRespawn", "Automatically respawns after death.");
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private void onOpenScreen(ScreenOpenEvent event) {
        if (!(event.screen instanceof DeathScreen)) {
            return;
        }
        
        mc.player.requestRespawn();
        event.cancel();
    }
    
}
