/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.entity;

import meteordevelopment.meteorclient.utils.misc.ITagged;

public enum Target implements ITagged {
    
    HEAD("Head"),
    BODY("Body"),
    FEET("Feet");
    
    private final String tag;
    
    Target(String tag) {
        this.tag = tag;
    }
    
    @Override
    public String getTag() {
        return tag;
    }
    
}