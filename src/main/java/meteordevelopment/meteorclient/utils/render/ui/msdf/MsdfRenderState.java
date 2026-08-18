package meteordevelopment.meteorclient.utils.render.ui.msdf;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class MsdfRenderState implements SimpleGuiElementRenderState {
    
    private final Matrix3x2f pose;
    private final TextureSetup textureSetup;
    private final ScreenRect scissorArea;
    private final float distanceRange;
    private final BuiltMsdf built;
    
    private float[] positions = new float[16 * 8];
    private float[] uvs = new float[16 * 4];
    private int[] colors = new int[16 * 4];
    private int quadCount;
    
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;
    
    MsdfRenderState(
        Matrix3x2f pose,
        TextureSetup textureSetup,
        ScreenRect scissorArea,
        float distanceRange,
        BuiltMsdf built
    ) {
        this.pose = pose;
        this.textureSetup = textureSetup;
        this.scissorArea = scissorArea;
        this.distanceRange = distanceRange;
        this.built = built == null ? new BuiltMsdf(null, "", 0, 0, 0, 0) : built;
    }
    
    void add(
        float x, float y, float width, float height,
        MsdfGlyph glyph,
        int color,
        float rotationDegrees, float rotationOriginX, float rotationOriginY,
        boolean fadeLeft, boolean fadeRight,
        float fadeLeftX, float fadeRightX, float fadeWidth,
        float fadeLeftStrength, float fadeRightStrength
    ) {
        ensureCapacity(quadCount + 1);
        
        int positionIndex = quadCount * 8;
        int uvIndex = quadCount * 4;
        int colorIndex = quadCount * 4;
        
        float x1 = x + width;
        float y1 = y + height;
        float tlX = x, tlY = y;
        float blX = x, blY = y1;
        float brX = x1, brY = y1;
        float trX = x1, trY = y;
        
        if (rotationDegrees != 0.0f) {
            float radians = (float) Math.toRadians(rotationDegrees);
            float sin = (float) Math.sin(radians);
            float cos = (float) Math.cos(radians);
            tlX = rotateX(x, y, rotationOriginX, rotationOriginY, sin, cos);
            tlY = rotateY(x, y, rotationOriginX, rotationOriginY, sin, cos);
            blX = rotateX(x, y1, rotationOriginX, rotationOriginY, sin, cos);
            blY = rotateY(x, y1, rotationOriginX, rotationOriginY, sin, cos);
            brX = rotateX(x1, y1, rotationOriginX, rotationOriginY, sin, cos);
            brY = rotateY(x1, y1, rotationOriginX, rotationOriginY, sin, cos);
            trX = rotateX(x1, y, rotationOriginX, rotationOriginY, sin, cos);
            trY = rotateY(x1, y, rotationOriginX, rotationOriginY, sin, cos);
        }
        
        boolean faded = (fadeLeft || fadeRight)
            && fadeWidth > 0.0f
            && fadeRightX > fadeLeftX
            && (fadeLeftStrength > 0.001f || fadeRightStrength > 0.001f);
        
        positions[positionIndex] = tlX;
        positions[positionIndex + 1] = tlY;
        positions[positionIndex + 2] = blX;
        positions[positionIndex + 3] = blY;
        positions[positionIndex + 4] = brX;
        positions[positionIndex + 5] = brY;
        positions[positionIndex + 6] = trX;
        positions[positionIndex + 7] = trY;
        
        uvs[uvIndex] = glyph.u0();
        uvs[uvIndex + 1] = glyph.v0();
        uvs[uvIndex + 2] = glyph.u1();
        uvs[uvIndex + 3] = glyph.v1();
        
        colors[colorIndex] = faded ? fadeColor(color, tlX, fadeLeft, fadeRight, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength) : color;
        colors[colorIndex + 1] = faded ? fadeColor(color, blX, fadeLeft, fadeRight, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength) : color;
        colors[colorIndex + 2] = faded ? fadeColor(color, brX, fadeLeft, fadeRight, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength) : color;
        colors[colorIndex + 3] = faded ? fadeColor(color, trX, fadeLeft, fadeRight, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength) : color;
        
        quadCount++;
        include(tlX, tlY);
        include(blX, blY);
        include(brX, brY);
        include(trX, trY);
    }
    
    @Override
    public void setupVertices(VertexConsumer consumer) {
        int styleIndex = MsdfRenderer.getInstance().reserve(distanceRange, built);
        if (styleIndex < 0) {
            return;
        }
        float indexAsLineWidth = styleIndex;
        
        for (int i = 0; i < quadCount; i++) {
            int positionIndex = i * 8;
            int uvIndex = i * 4;
            int colorIndex = i * 4;
            float u0 = uvs[uvIndex];
            float v0 = uvs[uvIndex + 1];
            float u1 = uvs[uvIndex + 2];
            float v1 = uvs[uvIndex + 3];
            vertex(consumer, positions[positionIndex], positions[positionIndex + 1], u0, v0, colors[colorIndex], indexAsLineWidth);
            vertex(consumer, positions[positionIndex + 2], positions[positionIndex + 3], u0, v1, colors[colorIndex + 1], indexAsLineWidth);
            vertex(consumer, positions[positionIndex + 4], positions[positionIndex + 5], u1, v1, colors[colorIndex + 2], indexAsLineWidth);
            vertex(consumer, positions[positionIndex + 6], positions[positionIndex + 7], u1, v0, colors[colorIndex + 3], indexAsLineWidth);
        }
    }
    
    private void vertex(VertexConsumer consumer, float x, float y, float u, float v, int color, float styleIndex) {
        consumer.vertex(pose, x, y)
            .texture(u, v)
            .color(color)
            .lineWidth(styleIndex);
    }
    
    @Override
    public RenderPipeline pipeline() {
        return MsdfRenderer.MSDF_PIPELINE;
    }
    
    @Override
    public TextureSetup textureSetup() {
        return textureSetup;
    }
    
    @Override
    public ScreenRect scissorArea() {
        return scissorArea;
    }
    
    @Override
    public ScreenRect bounds() {
        if (quadCount == 0) {
            return new ScreenRect(0, 0, 1, 1);
        }
        int x = (int) Math.floor(minX);
        int y = (int) Math.floor(minY);
        int width = Math.max(1, (int) Math.ceil(maxX - minX));
        int height = Math.max(1, (int) Math.ceil(maxY - minY));
        ScreenRect transformedBounds = new ScreenRect(x, y, width, height).transformEachVertex(pose);
        return scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
    }
    
    private void ensureCapacity(int needed) {
        if (needed * 8 <= positions.length) {
            return;
        }
        int cap = Math.max(needed * 8, positions.length * 2);
        float[] newPositions = new float[cap];
        float[] newUvs = new float[cap / 2];
        int[] newColors = new int[cap / 2];
        System.arraycopy(positions, 0, newPositions, 0, quadCount * 8);
        System.arraycopy(uvs, 0, newUvs, 0, quadCount * 4);
        System.arraycopy(colors, 0, newColors, 0, quadCount * 4);
        positions = newPositions;
        uvs = newUvs;
        colors = newColors;
    }
    
    private float rotateX(float x, float y, float ox, float oy, float sin, float cos) {
        float tx = x - ox, ty = y - oy;
        return ox + tx * cos - ty * sin;
    }
    
    private float rotateY(float x, float y, float ox, float oy, float sin, float cos) {
        float tx = x - ox, ty = y - oy;
        return oy + tx * sin + ty * cos;
    }
    
    private void include(float x, float y) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
    }
    
    private int fadeColor(
        int color, float x,
        boolean fadeLeft, boolean fadeRight,
        float fadeLeftX, float fadeRightX, float fadeWidth,
        float fadeLeftStrength, float fadeRightStrength
    ) {
        float alpha = 1.0f;
        if (fadeLeft) {
            float edge = smoothstep(clamp((x - fadeLeftX) / fadeWidth, 0.0f, 1.0f));
            alpha = Math.min(alpha, 1.0f - clamp(fadeLeftStrength, 0.0f, 1.0f) * (1.0f - edge));
        }
        if (fadeRight) {
            float edge = smoothstep(clamp((fadeRightX - x) / fadeWidth, 0.0f, 1.0f));
            alpha = Math.min(alpha, 1.0f - clamp(fadeRightStrength, 0.0f, 1.0f) * (1.0f - edge));
        }
        int oa = (color >>> 24) & 0xFF;
        return (Math.round(oa * alpha) << 24) | (color & 0x00FFFFFF);
    }
    
    private float smoothstep(float v) {
        return v * v * (3.0f - 2.0f * v);
    }
    
    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}