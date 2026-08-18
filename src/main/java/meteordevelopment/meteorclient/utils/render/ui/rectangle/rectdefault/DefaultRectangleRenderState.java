package meteordevelopment.meteorclient.utils.render.ui.rectangle.rectdefault;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class DefaultRectangleRenderState implements SimpleGuiElementRenderState {
    
    private final BuiltRectangle rectangle;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;
    
    DefaultRectangleRenderState(Matrix3x2f pose, BuiltRectangle rectangle, ScreenRect scissorArea) {
        this.rectangle = rectangle;
        this.pose = pose;
        this.scissorArea = scissorArea;
        ScreenRect transformedBounds = new ScreenRect(
            Math.round(rectangle.x()),
            Math.round(rectangle.y()),
            Math.round(rectangle.width()),
            Math.round(rectangle.height())
        ).transformEachVertex(this.pose);
        this.bounds = scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
    }
    
    @Override
    public void setupVertices(VertexConsumer consumer) {
        int batchIndex = DefaultRectangleRenderer.getInstance().reserve(rectangle);
        if (batchIndex < 0) {
            return;
        }
        
        float x0 = rectangle.x();
        float y0 = rectangle.y();
        float x1 = rectangle.x() + rectangle.width();
        float y1 = rectangle.y() + rectangle.height();
        
        vertex(consumer, x0, y0, batchIndex);
        vertex(consumer, x0, y1, batchIndex);
        vertex(consumer, x1, y1, batchIndex);
        vertex(consumer, x1, y0, batchIndex);
    }
    
    private void vertex(VertexConsumer consumer, float x, float y, int batchIndex) {
        consumer.vertex(pose, x, y)
            .color(255, 255, 255, 255)
            .lineWidth((float) (batchIndex + 1));
    }
    
    @Override
    public RenderPipeline pipeline() {
        return DefaultRectangleRenderer.RECTANGLE_PIPELINE;
    }
    
    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.empty();
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