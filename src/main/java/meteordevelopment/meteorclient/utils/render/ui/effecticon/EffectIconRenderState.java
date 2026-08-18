package meteordevelopment.meteorclient.utils.render.ui.effecticon;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;

final class EffectIconRenderState implements SimpleGuiElementRenderState {
    
    private final Matrix3x2f pose;
    private final EffectIconTexture texture;
    private final List<EffectIconQuad> icons = new ArrayList<>(16);
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;
    
    EffectIconRenderState(Matrix3x2f pose, EffectIconTexture texture) {
        this.pose = pose;
        this.texture = texture;
    }
    
    void add(EffectIconQuad icon) {
        icons.add(icon);
        minX = Math.min(minX, icon.x());
        minY = Math.min(minY, icon.y());
        maxX = Math.max(maxX, icon.x() + icon.size());
        maxY = Math.max(maxY, icon.y() + icon.size());
    }
    
    @Override
    public void setupVertices(VertexConsumer consumer) {
        for (EffectIconQuad icon : icons) {
            float x0 = icon.x();
            float y0 = icon.y();
            float x1 = icon.x() + icon.size();
            float y1 = icon.y() + icon.size();
            int color = icon.color();
            
            vertex(consumer, x0, y0, icon.u0(), icon.v0(), color);
            vertex(consumer, x0, y1, icon.u0(), icon.v1(), color);
            vertex(consumer, x1, y1, icon.u1(), icon.v1(), color);
            vertex(consumer, x1, y0, icon.u1(), icon.v0(), color);
        }
    }
    
    private void vertex(VertexConsumer consumer, float x, float y, float u, float v, int color) {
        consumer.vertex(pose, x, y)
            .texture(u, v)
            .color(color);
    }
    
    @Override
    public RenderPipeline pipeline() {
        return EffectIconRenderer.EFFECT_ICON_PIPELINE;
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
        if (icons.isEmpty()) {
            return new ScreenRect(0, 0, 1, 1);
        }
        
        int x = (int) Math.floor(minX);
        int y = (int) Math.floor(minY);
        int width = Math.max(1, (int) Math.ceil(maxX - minX));
        int height = Math.max(1, (int) Math.ceil(maxY - minY));
        return new ScreenRect(x, y, width, height).transformEachVertex(pose);
    }
    
}
