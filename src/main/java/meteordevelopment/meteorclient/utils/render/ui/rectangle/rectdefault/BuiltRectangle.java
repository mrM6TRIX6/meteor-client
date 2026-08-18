package meteordevelopment.meteorclient.utils.render.ui.rectangle.rectdefault;

import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import net.minecraft.client.gui.DrawContext;

public record BuiltRectangle(
    float x,
    float y,
    float width,
    float height,
    float radiusTopLeft,
    float radiusTopRight,
    float radiusBottomRight,
    float radiusBottomLeft,
    int colorTopLeft,
    int colorTopRight,
    int colorBottomRight,
    int colorBottomLeft,
    float smoothness
) {
    
    public static final float DEFAULT_SMOOTHNESS = 1.0f;
    public static final int DEFAULT_COLOR = ColorUtil.WHITE;
    
    public BuiltRectangle(float x, float y, float width, float height, float radius, int color) {
        this(x, y, width, height, radius, radius, radius, radius, color, color, color, color, DEFAULT_SMOOTHNESS);
    }
    
    public BuiltRectangle(
        float x,
        float y,
        float width,
        float height,
        float radius,
        int colorTopLeft,
        int colorTopRight,
        int colorBottomRight,
        int colorBottomLeft
    ) {
        this(
            x,
            y,
            width,
            height,
            radius,
            radius,
            radius,
            radius,
            colorTopLeft,
            colorTopRight,
            colorBottomRight,
            colorBottomLeft,
            DEFAULT_SMOOTHNESS
        );
    }
    
    public BuiltRectangle(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        int color
    ) {
        this(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            color,
            color,
            color,
            color,
            DEFAULT_SMOOTHNESS
        );
    }
    
    public BuiltRectangle withSmoothness(float smoothness) {
        return new BuiltRectangle(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            colorTopLeft,
            colorTopRight,
            colorBottomRight,
            colorBottomLeft,
            smoothness
        );
    }
    
    public BuiltRectangle withColor(int color) {
        return new BuiltRectangle(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            color,
            color,
            color,
            color,
            smoothness
        );
    }
    
    public void render(DrawContext context) {
        DefaultRectangleRenderer.getInstance().draw(context, this);
    }
    
    public boolean visible() {
        return width > 0.0f
            && height > 0.0f
            && (((colorTopLeft | colorTopRight | colorBottomRight | colorBottomLeft) >>> 24) != 0);
    }
    
}
