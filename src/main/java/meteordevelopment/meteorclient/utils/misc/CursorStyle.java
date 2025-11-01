/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import org.lwjgl.glfw.GLFW;

public enum CursorStyle {
    
    DEFAULT,
    CLICK,
    TYPE;
    
    private boolean created;
    private long cursor;
    
    public long getGlfwCursor() {
        if (!created) {
            switch (this) {
                case CLICK -> cursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
                case TYPE -> cursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
            }
            created = true;
        }
        return cursor;
    }
    
}
