package meteordevelopment.meteorclient.utils.render.ui.msdf;

public record MsdfGlyph(
    float xOffset,
    float yOffset,
    float width,
    float height,
    float advance,
    float u0,
    float v0,
    float u1,
    float v1
) {
    
    public boolean drawable() {
        return width > 0.0f && height > 0.0f && u1 > u0 && v1 > v0;
    }
    
}
