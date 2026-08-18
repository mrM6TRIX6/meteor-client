package meteordevelopment.meteorclient.utils.render.ui.rectangle.rectgradient;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class GradientRectangleRenderState implements SimpleGuiElementRenderState {
    
    private final BuiltGradientRectangle built;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;
    
    GradientRectangleRenderState(Matrix3x2f pose, BuiltGradientRectangle built, ScreenRect scissorArea) {
        this.built = built;
        this.pose = pose;
        this.scissorArea = scissorArea;
        ScreenRect transformedBounds = new ScreenRect(
            Math.round(built.x()),
            Math.round(built.y()),
            Math.round(built.width()),
            Math.round(built.height())
        ).transformEachVertex(this.pose);
        this.bounds = scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
    }
    
    @Override
    public void setupVertices(VertexConsumer consumer) {
        int batchIndex = GradientRectangleRenderer.getInstance().reserve(built);
        if (batchIndex < 0) {
            return;
        }
        
        float x0 = built.x();
        float y0 = built.y();
        float x1 = built.x() + built.width();
        float y1 = built.y() + built.height();
        
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
        return GradientRectangleRenderer.GRADIENT_RECTANGLE_PIPELINE;
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