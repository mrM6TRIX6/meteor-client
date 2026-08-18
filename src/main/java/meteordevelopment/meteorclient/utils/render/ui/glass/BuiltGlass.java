package meteordevelopment.meteorclient.utils.render.ui.glass;

import net.minecraft.client.gui.DrawContext;

public record BuiltGlass(
    float x,
    float y,
    float width,
    float height,
    float radiusTopLeft,
    float radiusTopRight,
    float radiusBottomRight,
    float radiusBottomLeft,
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
    
    public BuiltGlass(
        float x,
        float y,
        float width,
        float height,
        float[] radius,
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
            color,
            globalAlpha,
            fresnelPower,
            fresnelColor,
            baseAlpha,
            fresnelInvert,
            fresnelMix,
            distortStrength,
            squirt,
            z
        );
    }
    
    public void render(DrawContext context) {
        GlassRenderer.getInstance().draw(context, this);
    }
    
    public boolean visible() {
        return width > 0.0f
            && height > 0.0f
            && globalAlpha > 0.0f
            && (baseAlpha > 0.0f || (fresnelColor >>> 24) != 0);
    }
    
    private static float radiusValue(float[] radius, int index) {
        if (radius == null || index < 0 || index >= radius.length) {
            return 0.0f;
        }
        return radius[index];
    }
    
}
