/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.utils.misc.ITagged;

public enum ShapeMode implements ITagged {
    
    LINES("Lines"),
    SIDES("Sides"),
    BOTH("Both");
    
    private final String tag;
    
    ShapeMode(String tag) {
        this.tag = tag;
    }
    
    public boolean lines() {
        return this == LINES || this == BOTH;
    }
    
    public boolean sides() {
        return this == SIDES || this == BOTH;
    }
    
    @Override
    public String getTag() {
        return tag;
    }
}
