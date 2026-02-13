/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.input;

import net.minecraft.util.PlayerInput;

public record DirectionalInput(boolean forwards, boolean backwards, boolean left, boolean right) {
    
    public static final DirectionalInput NONE = new DirectionalInput(false, false, false, false);
    public static final DirectionalInput FORWARDS = new DirectionalInput(true, false, false, false);
    public static final DirectionalInput BACKWARDS = new DirectionalInput(false, false, false, false);
    public static final DirectionalInput LEFT = new DirectionalInput(false, false, true, false);
    public static final DirectionalInput RIGHT = new DirectionalInput(false, false, false, true);
    
    public DirectionalInput(PlayerInput input) {
        this(
            input.forward(),
            input.backward(),
            input.left(),
            input.right()
        );
    }
    
    public boolean isMoving() {
        return (forwards && !backwards)
            || (backwards && !forwards)
            || (left && !right)
            || (right && !left);
    }
    
    public DirectionalInput invert() {
        return new DirectionalInput(
            backwards,
            forwards,
            right,
            left
        );
    }
    
}
