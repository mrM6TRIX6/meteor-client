/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.elytrafly;

import meteordevelopment.meteorclient.utils.misc.ITagged;

public enum ElytraFlyModes implements ITagged {
    
    VANILLA("Vanilla"),
    PACKET("Packet"),
    PITCH40("Pitch40"),
    BOUNCE("Bounce");
    
    private final String tag;
    
    ElytraFlyModes(String tag) {
        this.tag = tag;
    }
    
    @Override
    public String getTag() {
        return tag;
    }
    
}