/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.state;

import meteordevelopment.meteorclient.utils.render.color.Color;

public record QuadColorState(Color colorTopLeft, Color colorTopRight, Color colorBottomRight, Color colorBottomLeft) {
    
    public static final QuadColorState TRANSPARENT = new QuadColorState(new Color(0, 0, 0, 0));
    public static final QuadColorState WHITE = new QuadColorState(new Color(255, 255, 255, 255));
    
    public QuadColorState(Color colorLeft, Color colorRight) {
        this(colorLeft, colorRight, colorRight, colorLeft);
    }
    
    public QuadColorState(Color color) {
        this(color, color, color, color);
    }
    
    public static QuadColorState of(Color color) {
        return new QuadColorState(color);
    }
    
    public static QuadColorState of(Color colorLeft, Color colorRight) {
        return new QuadColorState(colorLeft, colorRight);
    }
    
    public static QuadColorState of(Color colorTopLeft, Color colorTopRight, Color colorBottomRight, Color colorBottomLeft) {
        return new QuadColorState(colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
    }
    
    public static QuadColorState of(int rgba) {
        return new QuadColorState(new Color(rgba));
    }
    
    public static QuadColorState of(int r, int g, int b, int a) {
        return new QuadColorState(new Color(r, g, b, a));
    }
    
    public static QuadColorState of(int r, int g, int b) {
        return new QuadColorState(new Color(r, g, b, 255));
    }
    
}
