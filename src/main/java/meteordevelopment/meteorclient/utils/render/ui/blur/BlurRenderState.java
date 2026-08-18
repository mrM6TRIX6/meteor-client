package meteordevelopment.meteorclient.utils.render.ui.blur;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

final class BlurRenderState implements SimpleGuiElementRenderState {
    
    private final BuiltBlur blur;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;
    
    BlurRenderState(Matrix3x2f pose, BuiltBlur blur, ScreenRect scissorArea) {
        this.blur = blur;
        this.pose = pose;
        this.scissorArea = scissorArea;
        ScreenRect transformedBounds = new ScreenRect(
            Math.round(blur.x()),
            Math.round(blur.y()),
            Math.round(blur.width()),
            Math.round(blur.height())
        ).transformEachVertex(this.pose);
        this.bounds = scissorArea == null ? transformedBounds : scissorArea.intersection(transformedBounds);
    }
    
    @Override
    public void setupVertices(VertexConsumer consumer) {
        int batchIndex = BlurFramebuffer.getInstance().reserve(blur);
        if (batchIndex < 0) {
            return;
        }
        
        float x0 = blur.x();
        float y0 = blur.y();
        float x1 = blur.x() + blur.width();
        float y1 = blur.y() + blur.height();
        
        vertex(consumer, x0, y0, blur.colorTopLeft(), batchIndex);
        vertex(consumer, x0, y1, blur.colorBottomLeft(), batchIndex);
        vertex(consumer, x1, y1, blur.colorBottomRight(), batchIndex);
        vertex(consumer, x1, y0, blur.colorTopRight(), batchIndex);
    }
    
    private void vertex(VertexConsumer consumer, float x, float y, int color, int batchIndex) {
        consumer.vertex(pose, x, y).color(color).lineWidth((float) (batchIndex + 1));
    }
    
    @Override
    public RenderPipeline pipeline() {
        return BlurFramebuffer.BATCHED_BLUR_PIPELINE;
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
