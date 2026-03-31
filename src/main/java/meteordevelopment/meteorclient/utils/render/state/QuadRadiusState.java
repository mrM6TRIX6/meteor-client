/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render.state;

import org.joml.Vector4f;

public record QuadRadiusState(double radiusTopLeft, double radiusBottomLeft, double radiusBottomRight, double radiusTopRight) {
    
    public static QuadRadiusState of(double radius) {
        return new QuadRadiusState(radius, radius, radius, radius);
    }
    
    public static QuadRadiusState of(double radiusTopLeft, double radiusBottomLeft, double radiusBottomRight, double radiusTopRight) {
        return new QuadRadiusState(radiusTopLeft, radiusBottomLeft, radiusBottomRight, radiusTopRight);
    }
    
    public static QuadRadiusState ofVertical(double top, double bottom) {
        return new QuadRadiusState(top, bottom, bottom, top);
    }
    
    public static QuadRadiusState ofHorizontal(double left, double right) {
        return new QuadRadiusState(left, left, right, right);
    }
    
    public Vector4f getVec4f() {
        return new Vector4f(
            (float) radiusTopLeft,
            (float) radiusBottomLeft,
            (float) radiusBottomRight,
            (float) radiusTopRight
        );
    }
    
}
