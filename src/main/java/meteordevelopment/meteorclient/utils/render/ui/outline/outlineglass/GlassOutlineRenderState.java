package meteordevelopment.meteorclient.utils.render.ui.outline.outlineglass;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import meteordevelopment.meteorclient.utils.render.ui.blur.BlurFramebuffer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class GlassOutlineRenderState implements SimpleGuiElementRenderState {
    
    private final BuiltGlassOutline outline;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;
    
    GlassOutlineRenderState(Matrix3x2f pose, BuiltGlassOutline outline, ScreenRect scissorArea) {
        this.outline = outline;
        this.pose = pose;
        this.scissorArea = scissorArea;
        ScreenRect transformedBounds = new ScreenRect(
            Math.round(outline.x()),
            Math.round(outline.y()),
            Math.round(outline.width()),
            Math.round(outline.height())
        ).transformEachVertex(this.pose);
        this.bounds = scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
    }
    
    @Override
    public void setupVertices(VertexConsumer consumer) {
        int batchIndex = GlassOutlineRenderer.getInstance().reserve(outline);
        if (batchIndex < 0) {
            return;
        }
        
        float x0 = outline.x();
        float y0 = outline.y();
        float x1 = outline.x() + outline.width();
        float y1 = outline.y() + outline.height();
        
        vertex(consumer, x0, y0, batchIndex);
        vertex(consumer, x0, y1, batchIndex);
        vertex(consumer, x1, y1, batchIndex);
        vertex(consumer, x1, y0, batchIndex);
    }
    
    private void vertex(VertexConsumer consumer, float x, float y, int batchIndex) {
        consumer.vertex(pose, x, y)
            .color(outline.color())
            .lineWidth((float) (batchIndex + 1));
    }
    
    @Override
    public RenderPipeline pipeline() {
        return GlassOutlineRenderer.GLASS_OUTLINE_PIPELINE;
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
