package meteordevelopment.meteorclient.utils.render.ui.glow;

import meteordevelopment.meteorclient.utils.render.color.ColorUtil;

public final class GlowBuilder {

    private float x;
    private float y;
    private float width;
    private float height;
    private float radiusTopLeft;
    private float radiusTopRight;
    private float radiusBottomRight;
    private float radiusBottomLeft;
    private int color = ColorUtil.WHITE;
    private int colorTopRight = ColorUtil.WHITE;
    private int colorBottomRight = ColorUtil.WHITE;
    private int colorBottomLeft = ColorUtil.WHITE;
    private float intensity = 1.0f;
    private float glowRadius = 8.0f;
    private float alpha = 1.0f;
    private int secondColor = ColorUtil.WHITE;
    private float colorOffset = 0.0f;
    private float[] spans = null;
    private int spanCount = 0;
    private boolean leftAligned = false;
    private boolean bottomAnchored = false;
    private int splitIndex = 0;
    private boolean cutout = true;

    public GlowBuilder rectangle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public GlowBuilder rectangle(float x, float y, float width, float height, float cornerRadius) {
        return rectangle(x, y, width, height).radius(cornerRadius);
    }

    public GlowBuilder radius(float radius) {
        this.radiusTopLeft = radius;
        this.radiusTopRight = radius;
        this.radiusBottomRight = radius;
        this.radiusBottomLeft = radius;
        return this;
    }

    public GlowBuilder radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.radiusTopLeft = topLeft;
        this.radiusTopRight = topRight;
        this.radiusBottomRight = bottomRight;
        this.radiusBottomLeft = bottomLeft;
        return this;
    }

    public GlowBuilder color(int color) {
        this.color = color;
        this.colorTopRight = color;
        this.colorBottomRight = color;
        this.colorBottomLeft = color;
        return this;
    }
    
    public GlowBuilder color(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        this.color = topLeft;
        this.colorTopRight = topRight;
        this.colorBottomRight = bottomRight;
        this.colorBottomLeft = bottomLeft;
        return this;
    }

    public GlowBuilder intensity(float intensity) {
        this.intensity = intensity;
        return this;
    }

    public GlowBuilder glowRadius(float glowRadius) {
        this.glowRadius = glowRadius;
        return this;
    }

    public GlowBuilder alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public GlowBuilder secondColor(int color, float offset) {
        this.secondColor = color;
        this.colorOffset = offset;
        return this;
    }

    public GlowBuilder spans(float[] spans, int count) {
        this.spans = spans;
        this.spanCount = count;
        return this;
    }

    public GlowBuilder alignment(boolean leftAligned, boolean bottomAnchored) {
        this.leftAligned = leftAligned;
        this.bottomAnchored = bottomAnchored;
        return this;
    }

    public GlowBuilder splitIndex(int splitIndex) {
        this.splitIndex = splitIndex;
        return this;
    }

    /**
     * {@code true} (default) punches the rectangle out of the glow, leaving only the halo.
     * {@code false} keeps the blurred body, so the glow fills the rectangle too.
     */
    public GlowBuilder cutout(boolean cutout) {
        this.cutout = cutout;
        return this;
    }

    public BuiltGlow build() {
        float[] radii = new float[]{
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft
        };

        return new BuiltGlow(
            x,
            y,
            width,
            height,
            radii,
            color,
            colorTopRight,
            colorBottomRight,
            colorBottomLeft,
            intensity,
            glowRadius,
            alpha,
            secondColor,
            colorOffset,
            spans,
            spanCount,
            leftAligned,
            bottomAnchored,
            splitIndex,
            cutout
        );
    }

}