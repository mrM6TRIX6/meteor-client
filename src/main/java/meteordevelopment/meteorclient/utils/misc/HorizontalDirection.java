/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

public enum HorizontalDirection {
    
    SOUTH("South", "Z+", false, 0, 0, 1),
    SOUTH_EAST("South East", "X+ Z+", true, -45, 1, 1),
    WEST("West", "X-", false, 90, -1, 0),
    NORTH_WEST("North West", "X- Z-", true, 135, -1, -1),
    NORTH("North", "Z-", false, 180, 0, -1),
    NORTH_EAST("North East", "X+ Z-", true, -135, 1, -1),
    EAST("East", "X+", false, -90, 1, 0),
    SOUTH_WEST("South West", "X- Z+", true, 45, -1, 1);
    
    public final String name;
    public final String axis;
    public final boolean diagonal;
    public final float yaw;
    public final int offsetX, offsetZ;
    
    HorizontalDirection(String name, String axis, boolean diagonal, float yaw, int offsetX, int offsetZ) {
        this.axis = axis;
        this.name = name;
        this.diagonal = diagonal;
        this.yaw = yaw;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
    }
    
    public HorizontalDirection opposite() {
        return switch (this) {
            case SOUTH -> NORTH;
            case SOUTH_EAST -> NORTH_WEST;
            case WEST -> EAST;
            case NORTH_WEST -> SOUTH_EAST;
            case NORTH -> SOUTH;
            case NORTH_EAST -> SOUTH_WEST;
            case EAST -> WEST;
            case SOUTH_WEST -> NORTH_EAST;
        };
    }
    
    public HorizontalDirection rotateLeft() {
        return switch (this) {
            case SOUTH -> SOUTH_EAST;
            case SOUTH_EAST -> EAST;
            case EAST -> NORTH_EAST;
            case NORTH_EAST -> NORTH;
            case NORTH -> NORTH_WEST;
            case NORTH_WEST -> WEST;
            case WEST -> SOUTH_WEST;
            case SOUTH_WEST -> SOUTH;
        };
    }
    
    public HorizontalDirection rotateLeftSkipOne() {
        return switch (this) {
            case SOUTH -> EAST;
            case EAST -> NORTH;
            case NORTH -> WEST;
            case WEST -> SOUTH;
            case SOUTH_EAST -> NORTH_EAST;
            case NORTH_EAST -> NORTH_WEST;
            case NORTH_WEST -> SOUTH_WEST;
            case SOUTH_WEST -> SOUTH_EAST;
        };
    }
    
    public HorizontalDirection rotateRight() {
        return switch (this) {
            case SOUTH -> SOUTH_WEST;
            case SOUTH_WEST -> WEST;
            case WEST -> NORTH_WEST;
            case NORTH_WEST -> NORTH;
            case NORTH -> NORTH_EAST;
            case NORTH_EAST -> EAST;
            case EAST -> SOUTH_EAST;
            case SOUTH_EAST -> SOUTH;
        };
    }
    
    public static HorizontalDirection get(float yaw) {
        yaw = yaw % 360;
        if (yaw < 0) {
            yaw += 360;
        }
        
        if (yaw >= 337.5 || yaw < 22.5) {
            return SOUTH;
        } else if (yaw >= 22.5 && yaw < 67.5) {
            return SOUTH_WEST;
        } else if (yaw >= 67.5 && yaw < 112.5) {
            return WEST;
        } else if (yaw >= 112.5 && yaw < 157.5) {
            return NORTH_WEST;
        } else if (yaw >= 157.5 && yaw < 202.5) {
            return NORTH;
        } else if (yaw >= 202.5 && yaw < 247.5) {
            return NORTH_EAST;
        } else if (yaw >= 247.5 && yaw < 292.5) {
            return EAST;
        } else if (yaw >= 292.5 && yaw < 337.5) {
            return SOUTH_EAST;
        }
        
        return SOUTH;
    }
}
