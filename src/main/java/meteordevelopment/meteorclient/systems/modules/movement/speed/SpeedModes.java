/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.speed;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;

public enum SpeedModes implements IDisplayName {
    
    STRAFE("Strafe"),
    VANILLA("Vanilla");
    
    private final String displayName;
    
    SpeedModes(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
}