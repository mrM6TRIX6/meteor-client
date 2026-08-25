/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.pressable;

import meteordevelopment.meteorclient.gui.widgets.WWidget;
import net.minecraft.client.gui.Click;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public abstract class WPressable extends WWidget {
    
    public Runnable action;
    
    protected boolean pressed;
    
    @Override
    public boolean onMouseClicked(Click click, boolean doubled) {
        if (mouseOver && (click.button() == GLFW_MOUSE_BUTTON_LEFT || click.button() == GLFW_MOUSE_BUTTON_RIGHT)) {
            pressed = true;
        }
        
        return pressed;
    }
    
    @Override
    public boolean onMouseReleased(Click click) {
        if (pressed) {
            onPressed(click.button());
            if (action != null) {
                action.run();
            }

            pressed = false;
        }

        return false;
    }

    /**
     * Being held counts as focused so the release still arrives after the cursor has left the view around this widget,
     * which is what {@link meteordevelopment.meteorclient.gui.widgets.containers.WView#propagateEvents} goes by. Without
     * it the press would never be cleared and, since {@link #onMouseClicked} reports it, the next click anywhere would
     * be swallowed and run this action instead.
     */
    @Override
    public boolean isFocused() {
        return pressed || super.isFocused();
    }

    protected void onPressed(int button) {}
    
}
