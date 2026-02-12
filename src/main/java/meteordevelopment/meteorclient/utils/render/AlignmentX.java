/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;

public enum AlignmentX implements IDisplayName {
    
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right");
    
    private final String displayName;
    
    AlignmentX(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
}