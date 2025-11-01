/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.input;

import org.lwjgl.glfw.GLFW;

public enum KeyAction {
    
    PRESS,
    REPEAT,
    RELEASE;
    
    public static KeyAction get(int action) {
        return switch (action) {
            case GLFW.GLFW_PRESS -> PRESS;
            case GLFW.GLFW_RELEASE -> RELEASE;
            default -> REPEAT;
        };
    }
    
}
