package meteordevelopment.meteorclient.utils.render.ui.blur;

import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import net.minecraft.client.gui.DrawContext;

public record BuiltBlur(
    float x,
    float y,
    float width,
    float height,
    float radiusTopLeft,
    float radiusTopRight,
    float radiusBottomRight,
    float radiusBottomLeft,
    float smoothness,
    float blurRadius,
    int colorTopLeft,
    int colorTopRight,
    int colorBottomRight,
    int colorBottomLeft
) {
    
    private static final int DEFAULT_COLOR = ColorUtil.WHITE;
    
    public BuiltBlur(float x, float y, float width, float height, float radius, float smoothness, float blurRadius) {
        this(x, y, width, height, radius, radius, radius, radius, smoothness, blurRadius, DEFAULT_COLOR);
    }
    
    public BuiltBlur(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        float smoothness,
        float blurRadius,
        int color
    ) {
        this(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, smoothness, blurRadius, color, color, color, color);
    }
    
    public void render(DrawContext context) {
        BlurFramebuffer.getInstance().draw(context, this);
    }
    
    public BuiltBlur withColor(int color) {
        return withColors(color, color, color, color);
    }
    
    public BuiltBlur withColors(int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft) {
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
    
    public int color() {
        return colorTopLeft;
    }
    
    public boolean visible() {
        int colors = colorTopLeft | colorTopRight | colorBottomRight | colorBottomLeft;
        return width > 0.0f && height > 0.0f && (colors >>> 24) != 0 && blurRadius > 0.0f;
    }
    
}
