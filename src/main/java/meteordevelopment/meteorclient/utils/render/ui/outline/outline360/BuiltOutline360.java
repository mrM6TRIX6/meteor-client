package meteordevelopment.meteorclient.utils.render.ui.outline.outline360;

import meteordevelopment.meteorclient.utils.render.color.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public record BuiltOutline360(
    float x,
    float y,
    float width,
    float height,
    float radiusTopLeft,
    float radiusTopRight,
    float radiusBottomRight,
    float radiusBottomLeft,
    float thickness,
    int defaultColor,
    float smoothness,
    float blendDegrees,
    float angleOffsetDegrees,
    List<Outline360Range> ranges
) {
    
    public static final float DEFAULT_SMOOTHNESS = 1.0f;
    public static final float DEFAULT_BLEND_DEGREES = 14.0f;
    public static final int DEFAULT_COLOR = ColorUtil.rgba(48, 48, 48, 255);
    
    public BuiltOutline360 {
        ranges = ranges == null ? List.of() : List.copyOf(ranges);
    }
    
    public BuiltOutline360(
        float x,
        float y,
        float width,
        float height,
        float radius,
        float thickness,
        int defaultColor,
        List<Outline360Range> ranges
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
            thickness,
            defaultColor,
            DEFAULT_SMOOTHNESS,
            DEFAULT_BLEND_DEGREES,
            0.0f,
            ranges
        );
    }
    
    public BuiltOutline360(
        float x,
        float y,
        float width,
        float height,
        float radiusTopLeft,
        float radiusTopRight,
        float radiusBottomRight,
        float radiusBottomLeft,
        float thickness,
        int defaultColor,
        List<Outline360Range> ranges
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
            thickness,
            defaultColor,
            DEFAULT_SMOOTHNESS,
            DEFAULT_BLEND_DEGREES,
            0.0f,
            ranges
        );
    }
    
    public BuiltOutline360 withSmoothness(float smoothness) {
        return new BuiltOutline360(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            defaultColor,
            smoothness,
            blendDegrees,
            angleOffsetDegrees,
            ranges
        );
    }
    
    public BuiltOutline360 withBlendDegrees(float blendDegrees) {
        return new BuiltOutline360(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            defaultColor,
            smoothness,
            blendDegrees,
            angleOffsetDegrees,
            ranges
        );
    }
    
    public BuiltOutline360 withAngleOffset(float angleOffsetDegrees) {
        return new BuiltOutline360(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            thickness,
            defaultColor,
            smoothness,
            blendDegrees,
            angleOffsetDegrees,
            ranges
        );
    }
    
    public void render(DrawContext context) {
        Outline360Renderer.getInstance().draw(context, this);
    }
    
    public boolean visible() {
        return width > 0.0f && height > 0.0f && thickness > 0.0f && (defaultColor >>> 24) != 0;
    }
    
}
