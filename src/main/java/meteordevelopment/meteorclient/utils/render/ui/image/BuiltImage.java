package meteordevelopment.meteorclient.utils.render.ui.image;

import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import net.minecraft.util.Identifier;

public record BuiltImage(
    Identifier texture,
    float x,
    float y,
    float size,
    float explicitWidth,
    float explicitHeight,
    float radius,
    float smoothness,
    boolean nearestFilter,
    float rotationDegrees,
    float rotationOriginX,
    float rotationOriginY,
    float u0,
    float v0,
    float u1,
    float v1,
    int colorTopLeft,
    int colorTopRight,
    int colorBottomRight,
    int colorBottomLeft
) {
    
    public static final int DEFAULT_COLOR = ColorUtil.WHITE;
    public static final float DEFAULT_SMOOTHNESS = 1.0f;
    
    public BuiltImage(Identifier texture, float x, float y, float size, float radius) {
        this(texture, x, y, size, 0.0f, 0.0f, radius, DEFAULT_SMOOTHNESS, false, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, DEFAULT_COLOR, DEFAULT_COLOR, DEFAULT_COLOR, DEFAULT_COLOR);
    }
    
    public BuiltImage(Identifier texture, float x, float y, float size, float radius, int color) {
        this(texture, x, y, size, 0.0f, 0.0f, radius, DEFAULT_SMOOTHNESS, false, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, color, color, color, color);
    }
    
    public BuiltImage(Identifier texture, float x, float y, float width, float height, float radius) {
        this(texture, x, y, 0.0f, width, height, radius, DEFAULT_SMOOTHNESS, false, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, DEFAULT_COLOR, DEFAULT_COLOR, DEFAULT_COLOR, DEFAULT_COLOR);
    }
    
    public BuiltImage(Identifier texture, float x, float y, float width, float height, float radius, int color) {
        this(texture, x, y, 0.0f, width, height, radius, DEFAULT_SMOOTHNESS, false, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, color, color, color, color);
    }
    
    public boolean visible() {
        return texture != null
            && (size > 0.0f || (explicitWidth > 0.0f && explicitHeight > 0.0f))
            && (effectiveAlpha(colorTopLeft) > 0 || effectiveAlpha(colorTopRight) > 0 || effectiveAlpha(colorBottomRight) > 0 || effectiveAlpha(colorBottomLeft) > 0);
    }
    
    public boolean hasExplicitDimensions() {
        return explicitWidth > 0.0f && explicitHeight > 0.0f;
    }
    
    public BuiltImage withColors(int... colors) {
        if (colors == null || colors.length == 0) {
            return this;
        }
        if (colors.length == 1) {
            return new BuiltImage(texture, x, y, size, explicitWidth, explicitHeight, radius, smoothness, nearestFilter, rotationDegrees, rotationOriginX, rotationOriginY, u0, v0, u1, v1, colors[0], colors[0], colors[0], colors[0]);
        }
        if (colors.length == 2) {
            return new BuiltImage(texture, x, y, size, explicitWidth, explicitHeight, radius, smoothness, nearestFilter, rotationDegrees, rotationOriginX, rotationOriginY, u0, v0, u1, v1, colors[0], colors[1], colors[1], colors[0]);
        }
        if (colors.length == 3) {
            return new BuiltImage(texture, x, y, size, explicitWidth, explicitHeight, radius, smoothness, nearestFilter, rotationDegrees, rotationOriginX, rotationOriginY, u0, v0, u1, v1, colors[0], colors[1], colors[2], colors[0]);
        }
        return new BuiltImage(texture, x, y, size, explicitWidth, explicitHeight, radius, smoothness, nearestFilter, rotationDegrees, rotationOriginX, rotationOriginY, u0, v0, u1, v1, colors[0], colors[1], colors[2], colors[3]);
    }
    
    public BuiltImage withColor(int color) {
        return new BuiltImage(texture, x, y, size, explicitWidth, explicitHeight, radius, smoothness, nearestFilter, rotationDegrees, rotationOriginX, rotationOriginY, u0, v0, u1, v1, color, color, color, color);
    }
    
    public BuiltImage withRotation(float rotationDegrees, float originX, float originY) {
        return new BuiltImage(texture, x, y, size, explicitWidth, explicitHeight, radius, smoothness, nearestFilter, rotationDegrees, originX, originY, u0, v0, u1, v1, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
    }
    
    public BuiltImage withUv(float u0, float v0, float u1, float v1) {
        return new BuiltImage(texture, x, y, size, explicitWidth, explicitHeight, radius, smoothness, nearestFilter, rotationDegrees, rotationOriginX, rotationOriginY, u0, v0, u1, v1, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
    }
    
    public BuiltImage withSmoothness(float smoothness) {
        return new BuiltImage(texture, x, y, size, explicitWidth, explicitHeight, radius, smoothness, nearestFilter, rotationDegrees, rotationOriginX, rotationOriginY, u0, v0, u1, v1, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
    }
    
    public BuiltImage withNearestFilter() {
        return new BuiltImage(texture, x, y, size, explicitWidth, explicitHeight, radius, smoothness, true, rotationDegrees, rotationOriginX, rotationOriginY, u0, v0, u1, v1, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
    }
    
    private static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }
    
    private static int effectiveAlpha(int color) {
        int alpha = alpha(color);
        return alpha == 0 && (color & 0x00FFFFFF) != 0 ? 255 : alpha;
    }
    
}
