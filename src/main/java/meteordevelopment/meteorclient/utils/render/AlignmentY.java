/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;

public enum AlignmentY implements IDisplayName {
    
    TOP("Top"),
    CENTER("Center"),
    BOTTOM("Bottom");
    
    private final String displayName;
    
    AlignmentY(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
}