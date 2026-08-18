package meteordevelopment.meteorclient.utils.render.ui.zippy;

import net.minecraft.client.gui.DrawContext;

public record BuiltZippy(
    float x,
    float y,
    float width,
    float height,
    float radiusTopLeft,
    float radiusTopRight,
    float radiusBottomRight,
    float radiusBottomLeft,
    int color,
    float smoothness,
    float timeOffset
) {
    
    public static final float DEFAULT_SMOOTHNESS = 1.0f;
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;
    
    public BuiltZippy(float x, float y, float width, float height, float radius, int color) {
        this(x, y, width, height, radius, radius, radius, radius, color, DEFAULT_SMOOTHNESS, 0.0f);
    }
    
    public BuiltZippy(
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
            DEFAULT_SMOOTHNESS,
            0.0f
        );
    }
    
    public BuiltZippy withColor(int color) {
        return new BuiltZippy(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            color,
            smoothness,
            timeOffset
        );
    }
    
    public BuiltZippy withSmoothness(float smoothness) {
        return new BuiltZippy(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            color,
            smoothness,
            timeOffset
        );
    }
    
    public BuiltZippy withTimeOffset(float timeOffset) {
        return new BuiltZippy(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            color,
            smoothness,
            timeOffset
        );
    }
    
    public void render(DrawContext context) {
        ZippyRenderer.getInstance().draw(context, this);
    }
    
    public boolean visible() {
        return width > 0.0f && height > 0.0f && (color >>> 24) != 0;
    }
    
}
