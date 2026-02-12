/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;

public enum Target implements IDisplayName {
    
    HEAD("Head"),
    BODY("Body"),
    FEET("Feet");
    
    private final String displayName;
    
    Target(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
}