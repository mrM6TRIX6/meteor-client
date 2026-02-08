/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.utils;

import meteordevelopment.meteorclient.utils.misc.ITagged;

public enum AlignmentX implements ITagged {
    
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right");
    
    private final String tag;
    
    AlignmentX(String tag) {
        this.tag = tag;
    }
    
    @Override
    public String getTag() {
        return tag;
    }
    
}
