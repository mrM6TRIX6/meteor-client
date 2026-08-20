package meteordevelopment.meteorclient.utils.render.ui.glow;

import net.minecraft.client.gui.DrawContext;

public record BuiltGlow(
    float x, float y, float width, float height,
    float[] radii, int color, int colorTopRight, int colorBottomRight, int colorBottomLeft,
    float intensity, float glowRadius, float alpha,
    int secondColor, float colorOffset, float[] spans, int spanCount,
    boolean leftAligned, boolean bottomAnchored, int splitIndex, boolean cutout
) {
    
    public BuiltGlow(float x, float y, float width, float height, float[] radii, int color, float intensity, float radius, float alpha) {
        this(x, y, width, height, radii, color, color, color, color, intensity, radius, alpha, color, 0.0f, null, 0, false, false, 0, true);
    }
    
    public BuiltGlow(float x, float y, float width, float height, float[] radii, int color, float intensity, float radius, float alpha, int splitIndex) {
        this(x, y, width, height, radii, color, color, color, color, intensity, radius, alpha, color, 0.0f, null, 0, false, false, splitIndex, true);
    }
    
    public BuiltGlow withSecondColor(int color, float offset) {
        return new BuiltGlow(x, y, width, height, radii, this.color, colorTopRight, colorBottomRight, colorBottomLeft, intensity, glowRadius, alpha, color, offset, spans, spanCount, leftAligned, bottomAnchored, splitIndex, cutout);
    }
    
    public BuiltGlow withSpans(float[] spans, int count) {
        return new BuiltGlow(x, y, width, height, radii, color, colorTopRight, colorBottomRight, colorBottomLeft, intensity, glowRadius, alpha, secondColor, colorOffset, spans, count, leftAligned, bottomAnchored, splitIndex, cutout);
    }
    
    public BuiltGlow withAlignment(boolean left, boolean bottom) {
        return new BuiltGlow(x, y, width, height, radii, color, colorTopRight, colorBottomRight, colorBottomLeft, intensity, glowRadius, alpha, secondColor, colorOffset, spans, spanCount, left, bottom, splitIndex, cutout);
    }
    
    public BuiltGlow withSplitIndex(int split) {
        return new BuiltGlow(x, y, width, height, radii, color, colorTopRight, colorBottomRight, colorBottomLeft, intensity, glowRadius, alpha, secondColor, colorOffset, spans, spanCount, leftAligned, bottomAnchored, split, cutout);
    }
    
    public BuiltGlow withCutout(boolean cutout) {
        return new BuiltGlow(x, y, width, height, radii, color, colorTopRight, colorBottomRight, colorBottomLeft, intensity, glowRadius, alpha, secondColor, colorOffset, spans, spanCount, leftAligned, bottomAnchored, splitIndex, cutout);
    }
    
    public int colorTopLeft() {
        return this.color;
    }
    
    public static float padFor(float glowRadius) {
        return glowRadius + 4.0f;
    }
    
    public float effectivePad() {
        return padFor(this.glowRadius);
    }
    
    public boolean visible() {
        return this.width > 0.0f && this.height > 0.0f && ((this.color | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft) >>> 24) != 0;
    }
    
    public void render(DrawContext drawContext) {
        GlowRenderer.getInstance().draw(drawContext, this);
    }
    
}
