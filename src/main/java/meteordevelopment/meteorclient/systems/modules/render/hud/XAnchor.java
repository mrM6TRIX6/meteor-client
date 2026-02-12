/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;

public enum XAnchor implements IDisplayName {
    
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right");
    
    private final String displayName;
    
    XAnchor(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
}