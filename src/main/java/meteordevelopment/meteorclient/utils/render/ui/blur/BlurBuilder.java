package meteordevelopment.meteorclient.utils.render.ui.blur;

import meteordevelopment.meteorclient.utils.render.color.ColorUtil;

public final class BlurBuilder {
    
    private float x;
    private float y;
    private float width;
    private float height;
    private float radiusTopLeft;
    private float radiusTopRight;
    private float radiusBottomRight;
    private float radiusBottomLeft;
    private float smoothness = 1.0f;
    private float blurRadius = 16.0f;
    private int colorTopLeft = ColorUtil.WHITE;
    private int colorTopRight = ColorUtil.WHITE;
    private int colorBottomRight = ColorUtil.WHITE;
    private int colorBottomLeft = ColorUtil.WHITE;
    
    public BlurBuilder rectangle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }
    
    public BlurBuilder rectangle(float x, float y, float width, float height, float cornerRadius) {
        return rectangle(x, y, width, height).radius(cornerRadius);
    }
    
    public BlurBuilder radius(float radius) {
        this.radiusTopLeft = radius;
        this.radiusTopRight = radius;
        this.radiusBottomRight = radius;
        this.radiusBottomLeft = radius;
        return this;
    }
    
    public BlurBuilder radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.radiusTopLeft = topLeft;
        this.radiusTopRight = topRight;
        this.radiusBottomRight = bottomRight;
        this.radiusBottomLeft = bottomLeft;
        return this;
    }
    
    public BlurBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }
    
    public BlurBuilder blurRadius(float blurRadius) {
        this.blurRadius = blurRadius;
        return this;
    }
    
    public BlurBuilder color(int color) {
        return colors(color, color, color, color);
    }
    
    public BlurBuilder colors(int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft) {
        this.colorTopLeft = colorTopLeft;
        this.colorTopRight = colorTopRight;
        this.colorBottomRight = colorBottomRight;
        this.colorBottomLeft = colorBottomLeft;
        return this;
    }
    
    public BuiltBlur build() {
        return new BuiltBlur(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            smoothness,
            blurRadius,
            colorTopLeft,
            colorTopRight,
            colorBottomRight,
            colorBottomLeft
        );
    }
    
}
