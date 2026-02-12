/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;

public enum Safety implements IDisplayName {
    
    SAFE("Safe"),
    SUICIDE("Suicide");
    
    private final String displayName;
    
    Safety(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
}