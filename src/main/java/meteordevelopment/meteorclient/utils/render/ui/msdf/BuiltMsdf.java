package meteordevelopment.meteorclient.utils.render.ui.msdf;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public record BuiltMsdf(
    MsdfFont font,
    Either<String, Text> text,
    float x,
    float y,
    float size,
    float thickness,
    float smoothness,
    boolean outline,
    float outlineThickness,
    int outlineColor,
    int color,
    float rotationDegrees,
    float rotationOriginX,
    float rotationOriginY,
    boolean fadeLeft,
    boolean fadeRight,
    float fadeLeftX,
    float fadeRightX,
    float fadeWidth,
    float fadeLeftStrength,
    float fadeRightStrength
) {
    
    public static final float DEFAULT_THICKNESS = 0.05f;
    public static final float DEFAULT_SMOOTHNESS = 1.0f;
    
    public BuiltMsdf(MsdfFont font, String text, int x, int y, int size, int color) {
        this(font, Either.left(text), x, y, size, DEFAULT_THICKNESS, DEFAULT_SMOOTHNESS, false, 0f, 0, color, 0f, 0f, 0f, false, false, 0f, 0f, 0f, 0f, 0f);
    }
    
    public BuiltMsdf(MsdfFont font, Text text, int x, int y, int size) {
        this(font, Either.right(text), x, y, size, DEFAULT_THICKNESS, DEFAULT_SMOOTHNESS, false, 0f, 0, 0xFFFFFFFF, 0f, 0f, 0f, false, false, 0f, 0f, 0f, 0f, 0f);
    }
    
    public BuiltMsdf withSmoothness(float smoothness) {
        return new BuiltMsdf(
            font,
            text,
            x,
            y,
            size,
            thickness,
            smoothness,
            outline,
            outlineThickness,
            outlineColor,
            color,
            rotationDegrees,
            rotationOriginX,
            rotationOriginY,
            fadeLeft,
            fadeRight,
            fadeLeftX,
            fadeRightX,
            fadeWidth,
            fadeLeftStrength,
            fadeRightStrength
        );
    }
    
    public BuiltMsdf withOutline(float outlineThickness, int outlineColor) {
        return new BuiltMsdf(
            font,
            text,
            x,
            y,
            size,
            thickness,
            smoothness,
            true,
            outlineThickness,
            outlineColor,
            color,
            rotationDegrees,
            rotationOriginX,
            rotationOriginY,
            fadeLeft,
            fadeRight,
            fadeLeftX,
            fadeRightX,
            fadeWidth,
            fadeLeftStrength,
            fadeRightStrength
        );
    }
    
    public BuiltMsdf withRotation(float rotationDegrees, float originX, float originY) {
        return new BuiltMsdf(
            font,
            text,
            x,
            y,
            size,
            thickness,
            smoothness,
            outline,
            outlineThickness,
            outlineColor,
            color,
            rotationDegrees,
            originX,
            originY,
            fadeLeft,
            fadeRight,
            fadeLeftX,
            fadeRightX,
            fadeWidth,
            fadeLeftStrength,
            fadeRightStrength
        );
    }
    
    public BuiltMsdf withFade(
        float fadeLeftX,
        float fadeRightX,
        float fadeWidth,
        float fadeLeftStrength,
        float fadeRightStrength
    ) {
        return new BuiltMsdf(
            font,
            text,
            x,
            y,
            size,
            thickness,
            smoothness,
            outline,
            outlineThickness,
            outlineColor,
            color,
            rotationDegrees,
            rotationOriginX,
            rotationOriginY,
            fadeLeftStrength > 0.001f,
            fadeRightStrength > 0.001f,
            fadeLeftX,
            fadeRightX,
            fadeWidth,
            fadeLeftStrength,
            fadeRightStrength
        );
    }
    
    public BuiltMsdf withFade(
        float fadeLeftX,
        float fadeRightX,
        float fadeWidth,
        boolean fadeLeft,
        boolean fadeRight
    ) {
        return new BuiltMsdf(
            font,
            text,
            x,
            y,
            size,
            thickness,
            smoothness,
            outline,
            outlineThickness,
            outlineColor,
            color,
            rotationDegrees,
            rotationOriginX,
            rotationOriginY,
            fadeLeft,
            fadeRight,
            fadeLeftX,
            fadeRightX,
            fadeWidth,
            fadeLeft ? 1.0f : 0.0f,
            fadeRight ? 1.0f : 0.0f
        );
    }
    
    public BuiltMsdf withFadeLeft(float x, float width) {
        return withFade(x, 0, width, 1.0f, 0.0f);
    }
    
    public BuiltMsdf withFadeRight(float x, float width) {
        return withFade(0, x, width, 0.0f, 1.0f);
    }
    
    public BuiltMsdf withFade(float leftX, float rightX, float width) {
        return withFade(leftX, rightX, width, 1.0f, 1.0f);
    }
    
    public void render(DrawContext context) {
        MsdfRenderer.getInstance().draw(context, this);
    }
    
    public boolean visible() {
        return font != null
            && text != null
            && !text.map(String::isEmpty, text -> text.getString().isEmpty())
            && size > 0.0f;
    }
    
}