package meteordevelopment.meteorclient.utils.render.ui.rectangle.rectgradient;

import net.minecraft.client.gui.DrawContext;

public record BuiltGradientRectangle(
    float x,
    float y,
    float width,
    float height,
    float radiusTopLeft,
    float radiusTopRight,
    float radiusBottomRight,
    float radiusBottomLeft,
    int firstColor,
    int secondColor,
    float smoothness,
    float speed,
    float frequency,
    float angle
) {
    
    public static final float DEFAULT_SMOOTHNESS = 1.0f;
    public static final float DEFAULT_SPEED = 1.0f;
    public static final float DEFAULT_FREQUENCY = 2.0f;
    public static final float DEFAULT_ANGLE = 0.0f;
    
    public BuiltGradientRectangle(float x, float y, float width, float height, float radius, int firstColor, int secondColor, float speed, float frequency, float angle) {
        this(x, y, width, height, radius, radius, radius, radius, firstColor, secondColor, DEFAULT_SMOOTHNESS, speed, frequency, angle);
    }
    
    public BuiltGradientRectangle(float x, float y, float width, float height, float radius, int firstColor, int secondColor, float speed, float frequency) {
        this(x, y, width, height, radius, firstColor, secondColor, speed, frequency, DEFAULT_ANGLE);
    }
    
    public BuiltGradientRectangle(float x, float y, float width, float height, float radius, int firstColor, int secondColor) {
        this(x, y, width, height, radius, firstColor, secondColor, DEFAULT_SPEED, DEFAULT_FREQUENCY, DEFAULT_ANGLE);
    }
    
    public BuiltGradientRectangle(float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, int firstColor, int secondColor) {
        this(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, firstColor, secondColor, DEFAULT_SMOOTHNESS, DEFAULT_SPEED, DEFAULT_FREQUENCY, DEFAULT_ANGLE);
    }
    
    public BuiltGradientRectangle withSmoothness(float smoothness) {
        return new BuiltGradientRectangle(
            x, y, width, height,
            radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft,
            firstColor, secondColor,
            smoothness, speed, frequency, angle
        );
    }
    
    public BuiltGradientRectangle withSpeed(float speed) {
        return new BuiltGradientRectangle(
            x, y, width, height,
            radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft,
            firstColor, secondColor,
            smoothness, speed, frequency, angle
        );
    }
    
    public BuiltGradientRectangle withFrequency(float frequency) {
        return new BuiltGradientRectangle(
            x, y, width, height,
            radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft,
            firstColor, secondColor,
            smoothness, speed, frequency, angle
        );
    }
    
    public BuiltGradientRectangle withAngle(float angle) {
        return new BuiltGradientRectangle(
            x, y, width, height,
            radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft,
            firstColor, secondColor,
            smoothness, speed, frequency, angle
        );
    }
    
    public void render(DrawContext context) {
        GradientRectangleRenderer.getInstance().draw(context, this);
    }
    
    public boolean visible() {
        return width > 0.0f && height > 0.0f && (((firstColor | secondColor) >>> 24) != 0);
    }
    
}