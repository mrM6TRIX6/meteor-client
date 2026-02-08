/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud;

import meteordevelopment.meteorclient.utils.misc.ITagged;

public enum Alignment implements ITagged {
    
    AUTO("Auto"),
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right");
    
    private final String tag;
    
    Alignment(String tag) {
        this.tag = tag;
    }
    
    @Override
    public String getTag() {
        return tag;
    }
    
}