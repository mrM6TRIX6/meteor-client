package meteordevelopment.meteorclient.utils.render.ui.rectangle.rectrotatinggradient;

import net.minecraft.client.gui.DrawContext;

public record BuiltRotatingGradientRectangle(
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
    int thirdColor,
    int fourthColor,
    float smoothness,
    float speed
) {
    
    public static final float DEFAULT_SMOOTHNESS = 1.0f;
    public static final float DEFAULT_SPEED = 1.0f;
    
    public BuiltRotatingGradientRectangle(float x, float y, float width, float height, float radius, int firstColor, int secondColor, int thirdColor, int fourthColor, float speed) {
        this(x, y, width, height, radius, radius, radius, radius, firstColor, secondColor, thirdColor, fourthColor, DEFAULT_SMOOTHNESS, speed);
    }
    
    public BuiltRotatingGradientRectangle(float x, float y, float width, float height, float radius, int firstColor, int secondColor, int thirdColor, int fourthColor) {
        this(x, y, width, height, radius, firstColor, secondColor, thirdColor, fourthColor, DEFAULT_SPEED);
    }
    
    public BuiltRotatingGradientRectangle(float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, int firstColor, int secondColor, int thirdColor, int fourthColor) {
        this(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, firstColor, secondColor, thirdColor, fourthColor, DEFAULT_SMOOTHNESS, DEFAULT_SPEED);
    }
    
    public BuiltRotatingGradientRectangle withSmoothness(float smoothness) {
        return new BuiltRotatingGradientRectangle(
            x, y, width, height,
            radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft,
            firstColor, secondColor, thirdColor, fourthColor,
            smoothness, speed
        );
    }
    
    public BuiltRotatingGradientRectangle withSpeed(float speed) {
        return new BuiltRotatingGradientRectangle(
            x, y, width, height,
            radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft,
            firstColor, secondColor, thirdColor, fourthColor,
            smoothness, speed
        );
    }
    
    public void render(DrawContext context) {
        RotatingGradientRectangleRenderer.getInstance().draw(context, this);
    }
    
    public boolean visible() {
        return width > 0.0f && height > 0.0f && (((firstColor | secondColor | thirdColor | fourthColor) >>> 24) != 0);
    }
    
}