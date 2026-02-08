/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.hud;

import meteordevelopment.meteorclient.utils.misc.ITagged;

public enum YAnchor implements ITagged {
    
    TOP("Top"),
    CENTER("Center"),
    BOTTOM("Bottom");
    
    private final String tag;
    
    YAnchor(String tag) {
        this.tag = tag;
    }
    
    @Override
    public String getTag() {
        return tag;
    }
    
}