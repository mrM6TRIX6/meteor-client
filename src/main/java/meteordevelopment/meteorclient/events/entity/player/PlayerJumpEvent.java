/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity.player;

import meteordevelopment.meteorclient.events.Cancellable;

public class PlayerJumpEvent extends Cancellable {
    
    private static final PlayerJumpEvent INSTANCE = new PlayerJumpEvent();
    
    public float motion;
    public float yaw;
    
    public static PlayerJumpEvent get(float motion, float yaw) {
        INSTANCE.motion = motion;
        INSTANCE.yaw = yaw;
        
        return INSTANCE;
    }
    
}
