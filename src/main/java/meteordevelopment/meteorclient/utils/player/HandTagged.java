/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.utils.misc.ITagged;
import net.minecraft.util.Hand;

public enum HandTagged implements ITagged {
    
    MAIN_HAND("Main Hand"),
    OFF_HAND("Off Hand");
    
    private final String tag;
    
    HandTagged(String tag) {
        this.tag = tag;
    }
    
    public Hand get() {
        return switch (this) {
            case MAIN_HAND -> Hand.MAIN_HAND;
            case OFF_HAND -> Hand.OFF_HAND;
        };
    }
    
    @Override
    public String getTag() {
        return tag;
    }
    
}
