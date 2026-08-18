package meteordevelopment.meteorclient.utils.render.ui.glass;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import meteordevelopment.meteorclient.utils.render.ui.blur.BlurFramebuffer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class GlassRenderState implements SimpleGuiElementRenderState {
    
    private final BuiltGlass glass;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;
    
    GlassRenderState(Matrix3x2f pose, BuiltGlass glass, ScreenRect scissorArea) {
        this.glass = glass;
        this.pose = pose;
        this.scissorArea = scissorArea;
        ScreenRect transformedBounds = new ScreenRect(
            Math.round(glass.x()),
            Math.round(glass.y()),
            Math.round(glass.width()),
            Math.round(glass.height())
        ).transformEachVertex(this.pose);
        this.bounds = scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
    }
    
    @Override
    public void setupVertices(VertexConsumer consumer) {
        int batchIndex = GlassRenderer.getInstance().reserve(glass);
        if (batchIndex < 0) {
            return;
        }
        
        float x0 = glass.x();
        float y0 = glass.y();
        float x1 = glass.x() + glass.width();
        float y1 = glass.y() + glass.height();
        
        vertex(consumer, x0, y0, batchIndex);
        vertex(consumer, x0, y1, batchIndex);
        vertex(consumer, x1, y1, batchIndex);
        vertex(consumer, x1, y0, batchIndex);
    }
    
    private void vertex(VertexConsumer consumer, float x, float y, int batchIndex) {
        consumer.vertex(pose, x, y)
            .color(glass.color())
            .lineWidth((float) (batchIndex + 1));
    }
    
    @Override
    public RenderPipeline pipeline() {
        return GlassRenderer.GLASS_PIPELINE;
    }
    
    @Override
    public TextureSetup textureSetup() {
        return BlurFramebuffer.getInstance().textureSetup();
    }
    
    @Override
    public ScreenRect scissorArea() {
        return scissorArea;
    }
    
    @Override
    public ScreenRect bounds() {
        return bounds;
    }
    
}
