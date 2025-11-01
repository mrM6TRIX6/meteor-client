/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import net.minecraft.util.math.Direction;

public enum CardinalDirection {
    
    NORTH,
    EAST,
    SOUTH,
    WEST;
    
    public Direction toDirection() {
        return switch (this) {
            case NORTH -> Direction.NORTH;
            case EAST -> Direction.EAST;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
        };
    }
    
    public static CardinalDirection fromDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> EAST;
            case EAST -> WEST;
            case DOWN, UP -> null;
        };
    }
    
}
