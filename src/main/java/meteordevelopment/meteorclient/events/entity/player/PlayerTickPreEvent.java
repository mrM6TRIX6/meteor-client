/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity.player;

import meteordevelopment.meteorclient.events.Cancellable;

public class PlayerTickPreEvent extends Cancellable {
    
    private static final PlayerTickPreEvent INSTANCE = new PlayerTickPreEvent();
    
    public static PlayerTickPreEvent get() {
        return INSTANCE;
    }
    
}
