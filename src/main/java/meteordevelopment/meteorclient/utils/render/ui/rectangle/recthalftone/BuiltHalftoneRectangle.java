package meteordevelopment.meteorclient.utils.render.ui.rectangle.recthalftone;

import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import net.minecraft.client.gui.DrawContext;

public record BuiltHalftoneRectangle(
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
    float smoothness,
    int dotColor,
    float dotSize,
    float dotSpacing
) {
    
    public static final float DEFAULT_SMOOTHNESS = 1.0f;
    public static final int DEFAULT_COLOR = ColorUtil.WHITE;
    public static final int DEFAULT_DOT_COLOR = ColorUtil.rgba(0, 0, 0, 204);
    public static final float DEFAULT_DOT_SIZE = 1.25f;
    public static final float DEFAULT_DOT_SPACING = 4.0f;
    
    public BuiltHalftoneRectangle(
        float x,
        float y,
        float width,
        float height,
        float radius,
        int color,
        int dotColor,
        float dotSize,
        float dotSpacing
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
            color,
            color,
            color,
            color,
            DEFAULT_SMOOTHNESS,
            dotColor,
            dotSize,
            dotSpacing
        );
    }
    
    public BuiltHalftoneRectangle(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        int color,
        int dotColor,
        float dotSize,
        float dotSpacing
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
            DEFAULT_SMOOTHNESS,
            dotColor,
            dotSize,
            dotSpacing
        );
    }
    
    public BuiltHalftoneRectangle withSmoothness(float smoothness) {
        return new BuiltHalftoneRectangle(
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
            smoothness,
            dotColor,
            dotSize,
            dotSpacing
        );
    }
    
    public BuiltHalftoneRectangle withColor(int color) {
        return new BuiltHalftoneRectangle(
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
            smoothness,
            dotColor,
            dotSize,
            dotSpacing
        );
    }
    
    public BuiltHalftoneRectangle withDots(int dotColor, float dotSize, float dotSpacing) {
        return new BuiltHalftoneRectangle(
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
            smoothness,
            dotColor,
            dotSize,
            dotSpacing
        );
    }
    
    public void render(DrawContext context) {
        HalftoneRectangleRenderer.getInstance().draw(context, this);
    }
    
    public boolean visible() {
        int colors = colorTopLeft | colorTopRight | colorBottomRight | colorBottomLeft | dotColor;
        return width > 0.0f && height > 0.0f && (colors >>> 24) != 0;
    }
    
}
