package meteordevelopment.meteorclient.utils.render.ui.glow;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

final class GlowRenderState implements SimpleGuiElementRenderState {
    
    private final BuiltGlow glow;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;
    private final GlowCapture capture;
    
    GlowRenderState(Matrix3x2f pose, BuiltGlow glow, ScreenRect scissor, GlowCapture capture) {
        this.glow = glow;
        this.pose = new Matrix3x2f((Matrix3x2fc) pose);
        this.scissorArea = scissor;
        float pad = BuiltGlow.padFor(GlowRenderer.getInstance().submitRadiusHint(glow.glowRadius()));
        int x = (int) Math.floor(glow.x() - pad);
        int y = (int) Math.floor(glow.y() - pad);
        int w = (int) Math.ceil(glow.width() + pad * 2.0f);
        int h = (int) Math.ceil(glow.height() + pad * 2.0f);
        ScreenRect transformed = new ScreenRect(x, y, Math.max(1, w), Math.max(1, h)).transformEachVertex((Matrix3x2fc) (Object) this.pose);
        this.bounds = scissor == null ? transformed : scissor.intersection(transformed);
        this.capture = capture;
    }
    
    @Override
    public ScreenRect bounds() {
        return this.bounds;
    }
    
    @Override
    public RenderPipeline pipeline() {
        return GlowRenderer.GLOW_COMPOSITE_PIPELINE;
    }
    
    @Override
    public ScreenRect scissorArea() {
        return this.scissorArea;
    }
    
    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }
    
    @Override
    public void setupVertices(VertexConsumer vertices) {
        int id = GlowRenderer.getInstance().reserve(this.glow, this.capture);
        if (id < 0) {
            return;
        }
        float pad = this.capture.prepared ? this.capture.effectivePad : this.glow.effectivePad();
        float x1 = this.glow.x() - pad;
        float y1 = this.glow.y() - pad;
        float x2 = this.glow.x() + this.glow.width() + pad;
        float y2 = this.glow.y() + this.glow.height() + pad;
        this.vertex(vertices, x1, y1, 0, 0, id);
        this.vertex(vertices, x1, y2, 0, 255, id);
        this.vertex(vertices, x2, y2, 255, 255, id);
        this.vertex(vertices, x2, y1, 255, 0, id);
    }
    
    private void vertex(VertexConsumer vc, float x, float y, int u, int v, int id) {
        vc.vertex((Matrix3x2fc) (Object) this.pose, x, y).color(u, v, 255, 255).lineWidth((float) (id + 1));
    }
    
}
