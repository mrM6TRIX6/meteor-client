package meteordevelopment.meteorclient.utils.render.item;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class MeteorItemRenderState implements SimpleGuiElementRenderState {
    
    private static final int FLOATS_PER_QUAD = 16;
    
    private final Matrix3x2f pose;
    private final ItemTexture texture;
    private float[] quads = new float[FLOATS_PER_QUAD * 16];
    private int[] colors = new int[16];
    private float[] glints = new float[16];
    private int quadCount;
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;
    
    MeteorItemRenderState(Matrix3x2f pose, ItemTexture texture) {
        this.pose = new Matrix3x2f(pose);
        this.texture = texture;
    }
    
    void add(CachedItemQuad quad, float x, float y, float size, int color, float glintStrength) {
        ensureCapacity(quadCount + 1);
        
        int offset = quadCount * FLOATS_PER_QUAD;
        float x0 = x + quad.x0() * size;
        float y0 = y + quad.y0() * size;
        float x1 = x + quad.x1() * size;
        float y1 = y + quad.y1() * size;
        float x2 = x + quad.x2() * size;
        float y2 = y + quad.y2() * size;
        float x3 = x + quad.x3() * size;
        float y3 = y + quad.y3() * size;
        
        quads[offset] = x0;
        quads[offset + 1] = y0;
        quads[offset + 2] = quad.u0();
        quads[offset + 3] = quad.v0();
        quads[offset + 4] = x1;
        quads[offset + 5] = y1;
        quads[offset + 6] = quad.u1();
        quads[offset + 7] = quad.v1();
        quads[offset + 8] = x2;
        quads[offset + 9] = y2;
        quads[offset + 10] = quad.u2();
        quads[offset + 11] = quad.v2();
        quads[offset + 12] = x3;
        quads[offset + 13] = y3;
        quads[offset + 14] = quad.u3();
        quads[offset + 15] = quad.v3();
        colors[quadCount] = color;
        glints[quadCount] = glintStrength;
        quadCount++;
        
        include(x0, y0);
        include(x1, y1);
        include(x2, y2);
        include(x3, y3);
    }
    
    @Override
    public void setupVertices(VertexConsumer consumer) {
        for (int i = 0; i < quadCount; i++) {
            int batchIndex = CustomItemRenderer.getInstance().reserve(glints[i]);
            if (batchIndex < 0) {
                continue;
            }
            
            int offset = i * FLOATS_PER_QUAD;
            int color = colors[i];
            vertex(consumer, quads[offset], quads[offset + 1], quads[offset + 2], quads[offset + 3], color, batchIndex);
            vertex(consumer, quads[offset + 4], quads[offset + 5], quads[offset + 6], quads[offset + 7], color, batchIndex);
            vertex(consumer, quads[offset + 8], quads[offset + 9], quads[offset + 10], quads[offset + 11], color, batchIndex);
            vertex(consumer, quads[offset + 12], quads[offset + 13], quads[offset + 14], quads[offset + 15], color, batchIndex);
        }
    }
    
    private void vertex(VertexConsumer consumer, float x, float y, float u, float v, int color, int batchIndex) {
        consumer.vertex(pose, x, y)
            .texture(u, v)
            .color(color)
            .lineWidth((float) (batchIndex + 1));
    }
    
    @Override
    public RenderPipeline pipeline() {
        return CustomItemRenderer.ITEM_PIPELINE;
    }
    
    @Override
    public TextureSetup textureSetup() {
        return texture.setup();
    }
    
    @Override
    public ScreenRect scissorArea() {
        return null;
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
        return new ScreenRect(x, y, width, height).transformEachVertex(pose);
    }
    
    private void include(float x, float y) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
    }
    
    private void ensureCapacity(int targetQuads) {
        if (targetQuads <= colors.length) {
            return;
        }
        
        int newCapacity = Math.max(targetQuads, colors.length * 2);
        float[] nextQuads = new float[newCapacity * FLOATS_PER_QUAD];
        int[] nextColors = new int[newCapacity];
        float[] nextGlints = new float[newCapacity];
        System.arraycopy(quads, 0, nextQuads, 0, quadCount * FLOATS_PER_QUAD);
        System.arraycopy(colors, 0, nextColors, 0, quadCount);
        System.arraycopy(glints, 0, nextGlints, 0, quadCount);
        quads = nextQuads;
        colors = nextColors;
        glints = nextGlints;
    }
    
}
