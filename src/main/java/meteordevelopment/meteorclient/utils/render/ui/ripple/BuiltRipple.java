package meteordevelopment.meteorclient.utils.render.ui.ripple;

public record BuiltRipple(
    float x,
    float y,
    float width,
    float height,
    float radiusTopLeft,
    float radiusTopRight,
    float radiusBottomRight,
    float radiusBottomLeft,
    float smoothness,
    float centerX,
    float centerY,
    float rippleRadius,
    float rippleSmoothness,
    int sourceColor,
    int targetColor,
    boolean isTextured
) {
    
    public BuiltRipple(
        float x, float y, float width, float height,
        float radius, float smoothness,
        float centerX, float centerY, float rippleRadius, float rippleSmoothness,
        int sourceColor, int targetColor
    ) {
        this(x, y, width, height, radius, radius, radius, radius, smoothness, centerX, centerY, rippleRadius, rippleSmoothness, sourceColor, targetColor, false);
    }
    
    public boolean visible() {
        return width > 0 && height > 0;
    }
    
}
