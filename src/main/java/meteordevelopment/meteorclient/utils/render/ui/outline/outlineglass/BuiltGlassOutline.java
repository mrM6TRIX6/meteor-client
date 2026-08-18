package meteordevelopment.meteorclient.utils.render.ui.outline.outlineglass;

import net.minecraft.client.gui.DrawContext;

public record BuiltGlassOutline(
    float x,
    float y,
    float width,
    float height,
    float radiusTopLeft,
    float radiusTopRight,
    float radiusBottomRight,
    float radiusBottomLeft,
    float thickness,
    int color,
    float globalAlpha,
    float fresnelPower,
    int fresnelColor,
    float baseAlpha,
    boolean fresnelInvert,
    float fresnelMix,
    float distortStrength,
    float squirt,
    float smoothness,
    float z
) {
    
    public static final float DEFAULT_SMOOTHNESS = 1.0f;
    
    public BuiltGlassOutline(
        float x,
        float y,
        float width,
        float height,
        float[] radius,
        float thickness,
        int color,
        float globalAlpha,
        float fresnelPower,
        int fresnelColor,
        float baseAlpha,
        boolean fresnelInvert,
        float fresnelMix,
        float distortStrength,
        float squirt,
        float z
    ) {
        this(
            x,
            y,
            width,
            height,
            radiusValue(radius, 0),
            radiusValue(radius, 1),
            radiusValue(radius, 2),
            radiusValue(radius, 3),
            thickness,
            color,
            globalAlpha,
            fresnelPower,
            fresnelColor,
            baseAlpha,
            fresnelInvert,
            fresnelMix,
            distortStrength,
            squirt,
            DEFAULT_SMOOTHNESS,
            z
        );
    }
    
    public void render(DrawContext context) {
        GlassOutlineRenderer.getInstance().draw(context, this);
    }
    
    public boolean visible() {
        return width > 0.0f
            && height > 0.0f
            && thickness > 0.0f
            && globalAlpha > 0.0f
            && ((color >>> 24) != 0 || baseAlpha > 0.0f || (fresnelColor >>> 24) != 0);
    }
    
    private static float radiusValue(float[] radius, int index) {
        if (radius == null || index < 0 || index >= radius.length) {
            return 0.0f;
        }
        return radius[index];
    }
    
}
