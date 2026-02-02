/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class NoFOV extends Module {
    
    public NoFOV() {
        super(Categories.RENDER, "NoFOV", "Does not allow your movement speed to affect your FOV.");
    }
    
}
