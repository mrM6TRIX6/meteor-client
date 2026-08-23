/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.engine;

import meteordevelopment.meteorclient.utils.name.IDisplayName;

public enum ShapeMode implements IDisplayName {
    
    LINES("Lines"),
    SIDES("Sides"),
    BOTH("Both");
    
    private final String displayName;
    
    ShapeMode(String displayName) {
        this.displayName = displayName;
    }
    
    public boolean lines() {
        return this == LINES || this == BOTH;
    }
    
    public boolean sides() {
        return this == SIDES || this == BOTH;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
}
