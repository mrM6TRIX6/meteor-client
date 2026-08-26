/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import org.joml.Matrix3x2f;

public interface ISpecialGuiElementRenderState {
    
    void meteor$setPose(Matrix3x2f pose);

}
