/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render.state;

import meteordevelopment.meteorclient.utils.render.color.Color;

public record QuadColorState(Color colorTopLeft, Color colorBottomLeft, Color colorBottomRight, Color colorTopRight) {
    
    public static QuadColorState of(Color color) {
        return new QuadColorState(color, color, color, color);
    }
    
    public static QuadColorState of(Color colorTopLeft, Color colorBottomLeft, Color colorBottomRight, Color colorTopRight) {
        return new QuadColorState(colorTopLeft, colorBottomLeft, colorBottomRight, colorTopRight);
    }
    
    public static QuadColorState ofVertical(Color top, Color bottom) {
        return new QuadColorState(top, bottom, bottom, top);
    }
    
    public static QuadColorState ofHorizontal(Color left, Color right) {
        return new QuadColorState(left, left, right, right);
    }
    
}
