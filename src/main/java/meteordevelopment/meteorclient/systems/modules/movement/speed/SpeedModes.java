/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.speed;

import meteordevelopment.meteorclient.utils.misc.ITagged;

public enum SpeedModes implements ITagged {
    
    STRAFE("Strafe"),
    VANILLA("Vanilla");
    
    private final String tag;
    
    SpeedModes(String tag) {
        this.tag = tag;
    }
    
    @Override
    public String getTag() {
        return tag;
    }
    
}