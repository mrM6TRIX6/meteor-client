/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.utils;

import meteordevelopment.meteorclient.utils.misc.ITagged;

public enum AlignmentY implements ITagged {
    
    TOP("Top"),
    CENTER("Center"),
    BOTTOM("Bottom");
    
    private final String tag;
    
    AlignmentY(String tag) {
        this.tag = tag;
    }
    
    @Override
    public String getTag() {
        return tag;
    }
    
}