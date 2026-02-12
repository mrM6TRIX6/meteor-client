/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;

public enum YAnchor implements IDisplayName {
    
    TOP("Top"),
    CENTER("Center"),
    BOTTOM("Bottom");
    
    private final String displayName;
    
    YAnchor(String displayName) {
        this.displayName = displayName;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
}