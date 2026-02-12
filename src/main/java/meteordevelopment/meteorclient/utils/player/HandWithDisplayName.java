/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.utils.misc.IDisplayName;
import net.minecraft.util.Hand;

public enum HandWithDisplayName implements IDisplayName {
    
    MAIN_HAND("Main Hand"),
    OFF_HAND("Off Hand");
    
    private final String displayName;
    
    HandWithDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public Hand get() {
        return switch (this) {
            case MAIN_HAND -> Hand.MAIN_HAND;
            case OFF_HAND -> Hand.OFF_HAND;
        };
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
}
