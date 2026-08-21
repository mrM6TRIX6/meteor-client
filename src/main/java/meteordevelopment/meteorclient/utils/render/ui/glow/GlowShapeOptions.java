package meteordevelopment.meteorclient.utils.render.ui.glow;

/**
 * Per-lambda glow settings. One {@link GlowRenderer#addShape} call = one options object = one atlas region with
 * its own blur radius.
 */
public final class GlowShapeOptions {

    float radius = 8.0f;
    float intensity = 1.0f;
    float alpha = 1.0f;
    float expand = 0.0f;
    float resolution = 1.0f;
    boolean cutout = true;

    boolean hasBounds;
    float boundsX;
    float boundsY;
    float boundsWidth;
    float boundsHeight;

    public GlowShapeOptions() {}

    public GlowShapeOptions(float radius) {
        this.radius = radius;
    }

    /** Blur radius of this group, in gui pixels. */
    public GlowShapeOptions radius(float radius) {
        this.radius = radius;
        return this;
    }

    public GlowShapeOptions intensity(float intensity) {
        this.intensity = intensity;
        return this;
    }

    public GlowShapeOptions alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    /** Extra padding around the captured content, on top of the automatic {@code radius + 4}. */
    public GlowShapeOptions expand(float expand) {
        this.expand = expand;
        return this;
    }

    /** Atlas texels per gui pixel. Values above 1 sharpen the captured content at the cost of atlas space. */
    public GlowShapeOptions resolution(float resolution) {
        this.resolution = resolution;
        return this;
    }

    /** When true (default) the content itself is punched out of the result, leaving only the halo. */
    public GlowShapeOptions cutout(boolean cutout) {
        this.cutout = cutout;
        return this;
    }

    /**
     * Overrides the automatic union of the captured element bounds. Coordinates are gui pixels, in the same space
     * the captured elements ended up in after their pose was applied.
     */
    public GlowShapeOptions bounds(float x, float y, float width, float height) {
        this.hasBounds = true;
        this.boundsX = x;
        this.boundsY = y;
        this.boundsWidth = width;
        this.boundsHeight = height;
        return this;
    }

    public GlowShapeOptions autoBounds() {
        this.hasBounds = false;
        return this;
    }

    public float radius() {
        return radius;
    }

    public float pad() {
        return BuiltGlow.padFor(radius) + Math.max(0.0f, expand);
    }

    public GlowShapeOptions copyFrom(GlowShapeOptions other) {
        this.radius = other.radius;
        this.intensity = other.intensity;
        this.alpha = other.alpha;
        this.expand = other.expand;
        this.resolution = other.resolution;
        this.cutout = other.cutout;
        this.hasBounds = other.hasBounds;
        this.boundsX = other.boundsX;
        this.boundsY = other.boundsY;
        this.boundsWidth = other.boundsWidth;
        this.boundsHeight = other.boundsHeight;
        return this;
    }

}
