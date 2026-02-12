/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.elytrafly;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;

public enum ElytraFlyModes implements IDisplayName {
    
    VANILLA("Vanilla"),
    PACKET("Packet"),
    PITCH40("Pitch40"),
    BOUNCE("Bounce");
    
    private final String displayName;
    
    ElytraFlyModes(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
}