/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

public enum Dimension {
    
    OVERWORLD,
    NETHER,
    END;
    
    public Dimension opposite() {
        return switch (this) {
            case OVERWORLD -> NETHER;
            case NETHER -> OVERWORLD;
            default -> this;
        };
    }
    
}
