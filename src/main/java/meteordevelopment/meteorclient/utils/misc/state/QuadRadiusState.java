/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.state;

public record QuadRadiusState(double radiusTopLeft, double radiusTopRight, double radiusBottomRight, double radiusBottomLeft) {
    
    public static final QuadRadiusState NO_ROUND = new QuadRadiusState(0.0, 0.0, 0.0, 0.0);
    
    public QuadRadiusState(double radius) {
        this(radius, radius, radius, radius);
    }
    
    public static QuadRadiusState of(double radius) {
        return new QuadRadiusState(radius);
    }
    
    public static QuadRadiusState of(double radiusTopLeft, double radiusTopRight, double radiusBottomRight, double radiusBottomLeft) {
        return new QuadRadiusState(radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft);
    }
    
    public static QuadRadiusState of(double top, double bottom) {
        return new QuadRadiusState(top, top, bottom, bottom);
    }
    
    public static QuadRadiusState of(double left, double right, double bottom) {
        return new QuadRadiusState(left, right, bottom, right);
    }
    
    public static QuadRadiusState ofTop(double topRadius) {
        return new QuadRadiusState(topRadius, topRadius, 0.0, 0.0);
    }
    
    public static QuadRadiusState ofBottom(double bottomRadius) {
        return new QuadRadiusState(0.0, 0.0, bottomRadius, bottomRadius);
    }
    
    public static QuadRadiusState ofLeft(double leftRadius) {
        return new QuadRadiusState(leftRadius, 0.0, 0.0, leftRadius);
    }
    
    public static QuadRadiusState ofRight(double rightRadius) {
        return new QuadRadiusState(0.0, rightRadius, rightRadius, 0.0);
    }
    
    public static QuadRadiusState ofAll(double radius) {
        return new QuadRadiusState(radius);
    }
    
    public static QuadRadiusState ofVertical(double top, double bottom) {
        return new QuadRadiusState(top, top, bottom, bottom);
    }
    
    public static QuadRadiusState ofHorizontal(double left, double right) {
        return new QuadRadiusState(left, right, right, left);
    }
    
}
