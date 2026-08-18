package meteordevelopment.meteorclient.utils.render.ui.outline.outline360;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class Outline360RenderState implements SimpleGuiElementRenderState {
    
    private final BuiltOutline360 outline;
    private final Matrix3x2f pose;
    private final ScreenRect bounds;
    
    Outline360RenderState(Matrix3x2f pose, BuiltOutline360 outline) {
        this.outline = outline;
        this.pose = pose;
        this.bounds = new ScreenRect(
            Math.round(outline.x()),
            Math.round(outline.y()),
            Math.round(outline.width()),
            Math.round(outline.height())
        ).transformEachVertex(this.pose);
    }
    
    @Override
    public void setupVertices(VertexConsumer consumer) {
        int batchIndex = Outline360Renderer.getInstance().reserve(outline);
        if (batchIndex < 0) {
            return;
        }
        
        float x0 = outline.x();
        float y0 = outline.y();
        float x1 = outline.x() + outline.width();
        float y1 = outline.y() + outline.height();
        
        vertex(consumer, x0, y0, 0, 0, batchIndex);
        vertex(consumer, x0, y1, 0, 255, batchIndex);
        vertex(consumer, x1, y1, 255, 255, batchIndex);
        vertex(consumer, x1, y0, 255, 0, batchIndex);
    }
    
    private void vertex(VertexConsumer consumer, float x, float y, int coordX, int coordY, int batchIndex) {
        consumer.vertex(pose, x, y)
            .color(coordX, coordY, 255, 255)
            .lineWidth((float) (batchIndex + 1));
    }
    
    @Override
    public RenderPipeline pipeline() {
        return Outline360Renderer.OUTLINE_360_PIPELINE;
    }
    
    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }
    
    @Override
    public ScreenRect scissorArea() {
        return null;
    }
    
    @Override
    public ScreenRect bounds() {
        return bounds;
    }
    
}
