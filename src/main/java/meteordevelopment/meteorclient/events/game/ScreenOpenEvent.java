/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.gui.screen.Screen;

public class ScreenOpenEvent extends Cancellable {
    
    private static final ScreenOpenEvent INSTANCE = new ScreenOpenEvent();
    
    public Screen screen;
    
    public static ScreenOpenEvent get(Screen screen) {
        INSTANCE.setCancelled(false);
        INSTANCE.screen = screen;
        return INSTANCE;
    }
    
}
