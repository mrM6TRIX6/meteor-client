package meteordevelopment.meteorclient.utils.render.ui.ripple;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;

public final class RippleRenderState implements SimpleGuiElementRenderState {
    
    private final Matrix3x2f pose;
    private final BuiltRipple ripple;
    private final ScreenRect scissorArea;
    
    public RippleRenderState(Matrix3x2f pose, BuiltRipple ripple, ScreenRect scissorArea) {
        this.pose = pose;
        this.ripple = ripple;
        this.scissorArea = scissorArea;
    }
    
    @Override
    public void setupVertices(VertexConsumer consumer) {
        if (!RippleRenderer.getInstance().reserve(ripple)) {
            return;
        }
        
        consumer.vertex(pose, ripple.x(), ripple.y()).texture(0.0f, 0.0f);
        consumer.vertex(pose, ripple.x(), ripple.y() + ripple.height()).texture(0.0f, 1.0f);
        consumer.vertex(pose, ripple.x() + ripple.width(), ripple.y() + ripple.height()).texture(1.0f, 1.0f);
        consumer.vertex(pose, ripple.x() + ripple.width(), ripple.y()).texture(1.0f, 0.0f);
    }
    
    @Override
    public RenderPipeline pipeline() {
        return RippleRenderer.RIPPLE_PIPELINE;
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
        return new ScreenRect(
            Math.round(ripple.x()),
            Math.round(ripple.y()),
            Math.round(ripple.width()),
            Math.round(ripple.height())
        ).transformEachVertex(pose);
    }
    
}
